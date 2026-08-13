import 'dart:io';

/// 日志级别（§3.2.9 分级：debug/info/warn/error，线上默认 warn 起）。
enum LogLevel {
  /// 调试级别（请求 / 响应摘要）。
  debug,

  /// 信息级别（登录 / 传输完成）。
  info,

  /// 警告级别（重试 / 降级）。
  warn,

  /// 错误级别（异常）。
  error,
}

/// 移动端结构化日志器（§3.2.9）。
///
/// - 分级：debug / info / warn / error
/// - 脱敏：见 [AppLogger.redact]
/// - 落盘轮转：单文件 ≤ 5MB，保留最近 3 份
/// - 不写系统 console（避免 Xcode / Logcat 泄漏）
class AppLogger {
  /// 创建日志器。
  ///
  /// [logDirectory] 为应用文档目录下的 `logs/` 目录。
  /// [minLevel] 控制最低记录级别，低于该级别的日志被直接丢弃。
  AppLogger({
    required this.logDirectory,
    this.minLevel = LogLevel.warn,
  });

  /// 单文件最大字节数（5MB）。
  static const int maxFileSizeBytes = 5 * 1024 * 1024;

  /// 最多保留的日志文件份数（3 份）。
  static const int maxFileCount = 3;

  /// 应用文档目录下的 `logs/` 目录。
  final Directory logDirectory;

  /// 最低记录级别，线上默认 warn。
  final LogLevel minLevel;

  /// 记录 debug 级别日志（请求 / 响应摘要）。
  Future<void> debug(String message) => _write(LogLevel.debug, message);

  /// 记录 info 级别日志（登录 / 传输完成）。
  Future<void> info(String message) => _write(LogLevel.info, message);

  /// 记录 warn 级别日志（重试 / 降级）。
  Future<void> warn(String message) => _write(LogLevel.warn, message);

  /// 记录 error 级别日志（异常）。
  Future<void> error(
    String message, [
    Object? error,
    StackTrace? stackTrace,
  ]) =>
      _write(LogLevel.error, message, error, stackTrace);

  /// 脱敏（§3.2.9）：禁止记录访问 / 刷新令牌、配对码、密码、文件真实路径、
  /// 签名私钥；文件名 / 账号名打码。
  static String redact(String message) {
    var result = message;
    const sensitiveKeys = [
      'token',
      'access_token',
      'refresh_token',
      'authorization',
      'password',
      'pair_code',
      'pairing_code',
      'private_key',
    ];
    for (final key in sensitiveKeys) {
      final pattern = RegExp(
        '($key)([=: ]+)([^\\s,;]+)',
        caseSensitive: false,
      );
      result = result.replaceAllMapped(pattern, (match) {
        return '${match.group(1)}${match.group(2)}***';
      });
    }
    return result;
  }

  Future<void> _write(
    LogLevel level,
    String message, [
    Object? error,
    StackTrace? stackTrace,
  ]) async {
    if (level.index < minLevel.index) {
      return;
    }
    await _ensureDirectory();
    await _rotateIfNeeded();
    final file = _currentFile();
    final line = _formatLine(level, message, error, stackTrace);
    await file.writeAsString('$line\n', mode: FileMode.append, flush: true);
  }

  Future<void> _ensureDirectory() async {
    if (!logDirectory.existsSync()) {
      await logDirectory.create(recursive: true);
    }
  }

  Future<void> _rotateIfNeeded() async {
    final current = _currentFile();
    if (!current.existsSync()) {
      return;
    }
    if (current.lengthSync() < maxFileSizeBytes) {
      return;
    }
    for (var i = maxFileCount - 1; i >= 1; i--) {
      final oldFile = _logFile(i);
      final olderFile = _logFile(i + 1);
      if (oldFile.existsSync()) {
        if (olderFile.existsSync()) {
          await olderFile.delete();
        }
        await oldFile.rename(olderFile.path);
      }
    }
    await current.rename(_logFile(1).path);
  }

  File _currentFile() {
    final separator = Platform.pathSeparator;
    return File('${logDirectory.path}${separator}app.log');
  }

  File _logFile(int index) {
    final separator = Platform.pathSeparator;
    return File('${logDirectory.path}${separator}app.log.$index');
  }

  String _formatLine(
    LogLevel level,
    String message,
    Object? error,
    StackTrace? stackTrace,
  ) {
    final timestamp = DateTime.now().toIso8601String();
    final safeMessage = redact(message);
    final buffer = StringBuffer()
      ..write('[$timestamp]')
      ..write('[')
      ..write(level.name)
      ..write('] ')
      ..write(safeMessage);
    if (error != null) {
      buffer.write(' | error=$error');
    }
    if (stackTrace != null) {
      buffer.write(' | stack=$stackTrace');
    }
    return buffer.toString();
  }
}
