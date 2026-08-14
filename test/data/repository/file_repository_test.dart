import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:aerie_mobile/data/local/chat_database.dart';
import 'package:aerie_mobile/data/local/file_transfer_store.dart';
import 'package:aerie_mobile/data/repository/file_repository.dart';
import 'package:crypto/crypto.dart';
import 'package:dio/dio.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:path/path.dart' as p;

String _sha256Utf8(String s) => sha256.convert(utf8.encode(s)).toString();

/// 按路径返回预置响应的 HTTP 适配器（记录请求以便断言）。
class _FileAdapter implements HttpClientAdapter {
  _FileAdapter({this.skipUploadParts = const {}});

  final Set<int> skipUploadParts;
  final List<String> calls = [];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    final path = options.uri.path;
    calls.add('${options.method} $path');
    if (options.method == 'POST' && path.endsWith('/uploads')) {
      final uploaded = skipUploadParts.isEmpty ? '[]' : '[1]';
      return _json('{"uploadId":"u1","partSize":100,"partCount":1,'
          '"uploadedParts":$uploaded}');
    }
    if (options.method == 'PUT' && path.contains('/parts/')) {
      return _empty();
    }
    if (options.method == 'POST' && path.endsWith('/complete')) {
      return _json('{"fileId":"f1"}');
    }
    if (options.method == 'GET' && path.endsWith('/content')) {
      return ResponseBody.fromString(
        'HELLO',
        200,
        headers: {
          Headers.contentTypeHeader: ['application/octet-stream'],
          'content-range': ['bytes 0-4/5'],
        },
      );
    }
    return _empty();
  }

  ResponseBody _json(String body) => ResponseBody.fromString(
        body,
        200,
        headers: {Headers.contentTypeHeader: ['application/json']},
      );

  ResponseBody _empty() => ResponseBody.fromString('', 204);

  @override
  void close({bool force = false}) {}
}

Future<String> _writeTemp(String name, String content) async {
  final dir = await Directory.systemTemp.createTemp('ft');
  final file = File(p.join(dir.path, name));
  await file.writeAsString(content);
  return file.path;
}

Future<String> _tempDest(String name) async {
  final dir = await Directory.systemTemp.createTemp('fd');
  return p.join(dir.path, name);
}

void main() {
  late ChatDatabase db;
  late FileTransferStore store;
  late Dio dio;
  late FileRepository repository;
  late _FileAdapter adapter;

  setUp(() {
    db = ChatDatabase(NativeDatabase.memory());
    store = FileTransferStore(db);
    adapter = _FileAdapter();
    dio = Dio(BaseOptions(baseUrl: 'http://test'))..httpClientAdapter = adapter;
    repository = FileRepository(dio: dio, store: store);
  });

  tearDown(() async {
    await db.close();
  });

  group('computeSha256', () {
    test('空文件哈希为 e3b0c442…', () async {
      final path = await _writeTemp('empty.bin', '');
      final hash = await repository.computeSha256(File(path));
      expect(
        hash,
        'e3b0c44298fc1c149afbf4c8996fb924'
        '27ae41e4649b934ca495991b7852b855',
      );
    });

    test('与 crypto 全量哈希一致', () async {
      final path = await _writeTemp('a.bin', 'hello world');
      final hash = await repository.computeSha256(File(path));
      expect(hash, _sha256Utf8('hello world'));
    });
  });

  group('upload', () {
    test('一次性上传分块并完成，进度上报且续传点清空', () async {
      final path = await _writeTemp('up.txt', 'hello world');
      final progress = <int>[];
      final fileId = await repository.uploadAndFinalize(
        filePath: path,
        fileName: 'up.txt',
        mimeType: 'text/plain',
        onProgress: progress.add,
      );

      expect(fileId, 'f1');
      expect(progress.last, 11);
      expect(
        adapter.calls,
        contains('PUT /api/mobile/v1/files/uploads/u1/parts/1'),
      );
      expect(
        adapter.calls,
        contains('POST /api/mobile/v1/files/uploads/u1/complete'),
      );
      expect(await store.uploadedBytes('u1'), 0);
    });

    test('跳过已传分块（断点续传）', () async {
      adapter = _FileAdapter(skipUploadParts: const {1});
      dio = Dio(BaseOptions(baseUrl: 'http://test'))..httpClientAdapter = adapter;
      repository = FileRepository(dio: dio, store: store);

      final path = await _writeTemp('up.txt', 'hello world');
      final fileId = await repository.uploadAndFinalize(
        filePath: path,
        fileName: 'up.txt',
        mimeType: 'text/plain',
      );

      expect(fileId, 'f1');
      expect(adapter.calls.where((c) => c.startsWith('PUT')), isEmpty);
    });
  });

  group('download', () {
    test('Range 断点下载落盘并清除续传点', () async {
      final dest = await _tempDest('d.bin');
      final received = await repository.downloadResume(
        fileId: 'f1',
        localPath: dest,
      );

      expect(received, 5);
      expect(await File(dest).readAsString(), 'HELLO');
      expect(await store.downloadedBytes('f1'), 0);
      expect(
        adapter.calls,
        contains('GET /api/mobile/v1/files/f1/content'),
      );
    });
  });
}