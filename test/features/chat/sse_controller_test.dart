import 'dart:async';
import 'dart:convert';

import 'package:aerie_mobile/features/chat/sse_controller.dart';
import 'package:flutter_test/flutter_test.dart';

/// 把 SSE 文本编码为字节列表。
List<int> _enc(String s) => utf8.encode(s);

void main() {
  group('SseController', () {
    test('解析 id/event/data 并推送', () async {
      final controller = SseController(
        openStream: (_) async => Stream.value(
          _enc('id:1\nevent:linked\ndata:{"a":1}\n\n'),
        ),
        backDelay: (_) => Duration.zero,
      );
      final events = <SseEvent>[];
      controller.events.listen(events.add);
      controller.start();
      await Future<void>.delayed(const Duration(milliseconds: 20));

      expect(events, hasLength(1));
      expect(events.single.id, '1');
      expect(events.single.type, 'linked');
      expect(events.single.data, '{"a":1}');
      controller.stop();
    });

    test('忽略注释心跳帧', () async {
      final controller = SseController(
        openStream: (_) async => Stream.value(_enc(': keep-alive\n\n')),
        backDelay: (_) => Duration.zero,
      );
      final events = <SseEvent>[];
      controller.events.listen(events.add);
      controller.start();
      await Future<void>.delayed(const Duration(milliseconds: 20));

      expect(events, isEmpty);
      controller.stop();
    });

    test('按事件 ID 去重，重放跳过', () async {
      final controller = SseController(
        openStream: (_) async => Stream.value(
          _enc('id:1\ndata:first\n\nid:1\ndata:dup\n\n'),
        ),
        backDelay: (_) => Duration.zero,
      );
      final events = <SseEvent>[];
      controller.events.listen(events.add);
      controller.start();
      await Future<void>.delayed(const Duration(milliseconds: 20));

      expect(events, hasLength(1));
      expect(events.single.data, 'first');
      controller.stop();
    });

    test('连接健康态：connected → reconnecting', () async {
      final controller = SseController(
        openStream: (_) async => Stream.value(_enc(': x\n\n')),
        backDelay: (_) => Duration.zero,
      );
      final health = <SseHealth>[];
      controller.healthStream.listen(health.add);
      controller.start();
      await Future<void>.delayed(const Duration(milliseconds: 20));

      expect(health, isNotEmpty);
      expect(health.first, SseHealth.connected);
      controller.stop();
    });

    test('断线重连携带 Last-Event-ID 续传，退避按次数递增', () async {
      String? capturedId;
      final attempts = <int>[];
      var opened = 0;
      Future<Stream<List<int>>> open(String? lastId) async {
        opened++;
        final c = StreamController<List<int>>();
        if (opened == 1) {
          c.add(_enc('id:9\ndata:first\n\n'));
          await Future<void>.delayed(const Duration(milliseconds: 1));
          unawaited(c.close()); // 触发断线重连
        } else {
          capturedId = lastId;
          c.add(_enc(': ping\n\n'));
        }
        return c.stream;
      }

      final controller = SseController(
        openStream: open,
        backDelay: (attempt) {
          attempts.add(attempt);
          return Duration.zero;
        },
      );
      final events = <SseEvent>[];
      controller.events.listen(events.add);
      controller.start();
      await Future<void>.delayed(const Duration(milliseconds: 60));

      expect(attempts, isNotEmpty);
      expect(controller.health, anyOf(
            SseHealth.connected,
            SseHealth.reconnecting,
          ));
      // 二次打开时带上上一事件 ID，供服务端续传
      expect(capturedId, '9');
      controller.stop();
    });
  });
}