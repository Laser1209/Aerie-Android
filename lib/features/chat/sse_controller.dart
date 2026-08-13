import 'dart:async';
import 'dart:convert';

/// 连接健康态（供 AppBar 连接状态胶囊渲染）。
enum SseHealth {
  /// 已连接。
  connected,

  /// 断线重连中。
  reconnecting,

  /// 已断开（主动停止或失败）。
  disconnected,
}

/// 解析后的一条 SSE 事件。
class SseEvent {
  /// 创建 [SseEvent]。
  const SseEvent({required this.id, required this.type, required this.data});

  /// 事件 ID（用于 Last-Event-ID 续传与去重）。
  final String? id;

  /// 事件类型（`event:` 字段，缺省为空）。
  final String type;

  /// 数据（`data:` 字段，多行用换行连接）。
  final String data;
}

/// 流式 SSE 控制器（§3.2.6 / T2.2）。
///
/// 通过注入的 [openStream] 打开 HTTP 字节流（dio `ResponseType.stream`），
/// 按 `\n\n` 切帧解析 `id:`/`event:`/`data:` 字段，忽略 `:` 注释（心跳）；
/// 按事件 ID 去重（重连回放跳过已处理）；断线后按指数退避 + 抖动重连，
/// 并暴露 [health] 供连接状态胶囊驱动。
class SseController {
  /// 创建 [SseController]。
  ///
  /// `openStream` 接收上一事件 ID（可为空）返回字节流；`backDelay` 返回第
  /// `attempt`（从 1 起）次重连前的等待时长，为空时用默认指数退避。
  SseController({
    required this.openStream,
    Duration Function(int attempt)? backDelay,
  }) : _backDelay = backDelay ?? _defaultBackoff;

  /// 打开一次连接的字节流工厂。
  final Future<Stream<List<int>>> Function(String? lastEventId) openStream;

  final Duration Function(int attempt) _backDelay;

  final _eventsController = StreamController<SseEvent>.broadcast();
  final _healthController = StreamController<SseHealth>.broadcast();

  /// 事件流。
  Stream<SseEvent> get events => _eventsController.stream;

  /// 健康态流。
  Stream<SseHealth> get healthStream => _healthController.stream;

  SseHealth _health = SseHealth.disconnected;

  /// 当前健康态。
  SseHealth get health => _health;

  String? _lastEventId;
  final Set<String> _seen = {};
  int _attempt = 0;
  bool _running = false;

  /// 已连接期间收到的最近事件 ID 队列（去重窗口）。
  final List<String> _seenOrder = [];

  StreamSubscription<List<int>>? _sub;

  /// 启动（幂等）。
  void start() {
    if (_running) return;
    _running = true;
    unawaited(_connect());
  }

  /// 停止（幂等），关闭底层流并置 disconnected。
  void stop() {
    if (!_running) return;
    _running = false;
    if (_sub != null) {
      unawaited(_sub!.cancel());
    }
    _sub = null;
    _health = SseHealth.disconnected;
    _healthController.add(SseHealth.disconnected);
  }

  Future<void> _connect() async {
    if (!_running) return;
    Stream<List<int>> stream;
    try {
      stream = await openStream(_lastEventId);
    } on Object catch (_) {
      await _scheduleReconnect();
      return;
    }
    _attempt = 0;
    _health = SseHealth.connected;
    _healthController.add(SseHealth.connected);
    final parser = _FrameParser();
    _sub = stream.listen(
      (bytes) => parser.add(bytes).forEach(_dispatch),
      onError: (Object _) => _scheduleReconnect(),
      onDone: _scheduleReconnect,
      cancelOnError: true,
    );
  }

  void _dispatch(SseEvent event) {
    final id = event.id;
    if (id != null && id.isNotEmpty) {
      _lastEventId = id;
      if (_seen.contains(id)) {
        return; // 重连回放跳过已处理
      }
      _seen.add(id);
      _seenOrder.add(id);
      if (_seenOrder.length > 256) {
        _seen.remove(_seenOrder.removeAt(0));
      }
    }
    _eventsController.add(event);
  }

  Future<void> _scheduleReconnect() async {
    await _sub?.cancel();
    _sub = null;
    if (!_running) return;
    _attempt++;
    _health = SseHealth.reconnecting;
    _healthController.add(SseHealth.reconnecting);
    final delay = _backDelay(_attempt);
    if (delay > Duration.zero) {
      await Future<void>.delayed(delay);
    }
    if (_running) {
      await _connect();
    }
  }

  /// 默认指数退避：1s→2s→4s→…→30s 封顶，叠加抖动。
  static Duration _defaultBackoff(int attempt) {
    var seconds = 1 << (attempt - 1);
    if (seconds > 30) seconds = 30;
    final jitter = 0.8 + DateTime.now().microsecond % 40 / 100.0;
    return Duration(milliseconds: (seconds * 1000 * jitter).round());
  }
}

/// 累计字节缓冲并切出完整 SSE 帧（按 `\n\n` 分隔）。
class _FrameParser {
  final List<int> _buffer = [];

  List<SseEvent> add(List<int> bytes) {
    _buffer.addAll(bytes);
    final events = <SseEvent>[];
    for (;;) {
      final index = _indexDouble(_buffer);
      if (index < 0) break;
      final frame = _buffer.sublist(0, index);
      _buffer.removeRange(0, index + 2);
      final event = _parseFrame(frame);
      if (event != null) events.add(event);
    }
    return events;
  }

  /// 找到首个连续 `\n\n` 中第一个 `\n` 的下标；无则返回 -1。
  int _indexDouble(List<int> buf) {
    for (var i = 0; i + 1 < buf.length; i++) {
      if (buf[i] == 10 && buf[i + 1] == 10) return i;
    }
    return -1;
  }

  SseEvent? _parseFrame(List<int> frame) {
    final decoded = utf8.decode(frame, allowMalformed: true);
    String? id;
    var type = '';
    final dataLines = <String>[];
    for (final raw in decoded.split('\n')) {
      final line = raw.endsWith('\r') ? raw.substring(0, raw.length - 1) : raw;
      if (line.isEmpty) continue;
      if (line.startsWith(':')) continue; // 心跳注释
      if (line.startsWith('id:')) {
        id = line.substring(3).trim();
      } else if (line.startsWith('event:')) {
        type = line.substring(6).trim();
      } else if (line.startsWith('data:')) {
        final value = line.substring(5);
        dataLines.add(
          value == '' ? '' : value.substring(value.startsWith(' ') ? 1 : 0),
        );
      }
    }
    if (dataLines.isEmpty) return null;
    final data = dataLines.join('\n');
    if (type.isEmpty && data.isEmpty) return null;
    return SseEvent(id: id, type: type, data: data);
  }
}
