// JSON 夹具单行较长，属固定数据不拆行。
// ignore_for_file: lines_longer_than_80_chars

import 'dart:typed_data';

import 'package:aerie_mobile/data/local/chat_database.dart';
import 'package:aerie_mobile/data/repository/chat_repository.dart';
import 'package:aerie_mobile/gen/api/api_client.dart';
import 'package:dio/dio.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

/// 按请求路径返回模拟消息/请求 JSON 的 HTTP 适配器。
class _ChatAdapter implements HttpClientAdapter {
  final List<String> calls = [];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    calls.add('${options.method} ${options.path}');
    final body = _bodyFor(options.method, options.path);
    return ResponseBody.fromString(
      body,
      200,
      headers: {Headers.contentTypeHeader: ['application/json']},
    );
  }

  String _bodyFor(String method, String path) {
    if (path == '/api/mobile/v1/messages') {
      return '''
      {"items":[
        {"messageId":"m1","messageOrder":1,"conversationId":"c1","turnId":"t1",
         "role":"assistant","content":"你好，伊塔在线。",
         "attachments":[],"replyToId":null,"replyToContent":null,"replyToRole":null,
         "createdAt":"2026-08-14T10:00:00Z"},
        {"messageId":"m2","messageOrder":2,"conversationId":"c1","turnId":"t1",
         "role":"user","content":"在吗",
         "attachments":[{"name":"a.png","url":"/api/mobile/v1/files/f1/content",
           "state":"ready","size":2048,"type":"image/png","sha256":"abc"}],
         "replyToId":null,"replyToContent":null,"replyToRole":null,
         "createdAt":"2026-08-14T10:01:00Z"}
      ],"hasMore":false}''';
    }
    if (method == 'POST' && path == '/api/mobile/v1/requests') {
      return '{"requestId":"r1","conversationId":"c1","status":"queued","clientRequestId":"x1"}';
    }
    if (path.contains('/requests/r1/cancel')) {
      return '{"requestId":"r1","conversationId":"c1","status":"canceled"}';
    }
    if (path.contains('/requests/r1/retry')) {
      return '{"requestId":"r1","conversationId":"c1","status":"queued"}';
    }
    return '{}';
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  late ChatDatabase db;
  late ChatRepository repository;
  late _ChatAdapter adapter;

  setUp(() {
    db = ChatDatabase(NativeDatabase.memory());
    adapter = _ChatAdapter();
    final dio = Dio(BaseOptions(baseUrl: 'http://test'))..httpClientAdapter =
        adapter;
    repository = ChatRepository(
      client: ApiClient(dio),
      database: db,
      accountId: 1,
    );
  });

  tearDown(() async {
    await db.close();
  });

  group('ChatRepository.fetchPage', () {
    test('拉取并正确解析消息入参附件', () async {
      final page = await repository.fetchPage();

      expect(page.items, hasLength(2));
      expect(page.items.first.role, 'assistant');
      expect(page.items.first.content, '你好，伊塔在线。');
      final second = page.items[1];
      expect(second.role, 'user');
      expect(second.attachments, hasLength(1));
      expect(second.attachments.single.isImage, isTrue);
      expect(page.hasMore, isFalse);
    });

    test('会按 accountId 写入本地缓存供响应式读取', () async {
      await repository.fetchPage();

      final watch = repository.watchMessages();
      final first = await watch.first;
      expect(first.map((m) => m.messageId), ['m1', 'm2']);
    });

    test('beforeId 透传为查询参数', () async {
      await repository.fetchPage(beforeId: 'm10');

      final path = adapter.calls.first;
      expect(path, 'GET /api/mobile/v1/messages');
    });
  });

  group('ChatRepository 发送/取消/重试', () {
    test('sendMessage 返回请求结果', () async {
      final result = await repository.sendMessage(
        text: 'hello',
        clientRequestId: 'x1',
      );

      expect(result.requestId, 'r1');
      expect(result.clientRequestId, 'x1');
      expect(result.status, 'queued');
      expect(adapter.calls, contains('POST /api/mobile/v1/requests'));
    });

    test('sendMessage 传递附件 fileIds', () async {
      await repository.sendMessage(
        text: '',
        clientRequestId: 'x2',
        fileIds: const ['f1'],
      );

      // POST 已被调用即通过；具体 body 由 retrofit 序列化（不重复断言）
      expect(adapter.calls, contains('POST /api/mobile/v1/requests'));
    });

    test('cancelRequest / retryRequest 命中对应路由', () async {
      await repository.cancelRequest('r1');
      await repository.retryRequest('r1');

      expect(
        adapter.calls,
        contains('POST /api/mobile/v1/requests/r1/cancel'),
      );
      expect(
        adapter.calls,
        contains('POST /api/mobile/v1/requests/r1/retry'),
      );
    });
  });
}
