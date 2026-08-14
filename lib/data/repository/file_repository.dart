// 公开成员均已中文注释；会话/模型字段为数据声明。
// ignore_for_file: public_member_api_docs

import 'dart:convert';
import 'dart:io';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:aerie_mobile/data/local/file_transfer_store.dart';
import 'package:crypto/crypto.dart';
import 'package:dio/dio.dart';

/// 上传会话元信息（`POST /uploads` 返回）。
class UploadSession {
  /// 创建 [UploadSession]。
  const UploadSession({
    required this.uploadId,
    required this.partSize,
    required this.partCount,
    required this.uploadedParts,
  });

  /// 从 JSON 构造。
  factory UploadSession.fromJson(Map<String, dynamic> json) => UploadSession(
        uploadId: json['uploadId'] as String? ?? '',
        partSize: (json['partSize'] as num?)?.toInt() ?? 0,
        partCount: (json['partCount'] as num?)?.toInt() ?? 0,
        uploadedParts: (json['uploadedParts'] as List<dynamic>? ?? const [])
            .whereType<num>()
            .map((e) => e.toInt())
            .where((p) => p > 0)
            .toList(),
      );

  final String uploadId;
  final int partSize;
  final int partCount;
  final List<int> uploadedParts;
}

/// 文件双向传输仓库（§3.2.5 / T2.3）。
///
/// 上传：流式 SHA-256 → create 会话 → 分块 PUT（`X-Part-SHA256`）→
/// complete；断点续传跳过已传块。下载：`Range` 断点续传落盘 + 续传点
/// 持久化。所有请求经注入的 [dio]，测试注入 mock 适配器。
class FileRepository {
  /// 创建 [FileRepository]。
  FileRepository({required this.dio, required this.store});

  final Dio dio;
  final FileTransferStore store;

  static const String _partHeader = 'X-Part-SHA256';

  String get _base => dio.options.baseUrl;

  /// 流式计算整文件 SHA-256（分块避免整读进内存，1MB/块）。
  Future<String> computeSha256(File file) async {
    final out = _DigestSink();
    final input = sha256.startChunkedConversion(out);
    final raf = file.openSync();
    try {
      final buffer = Uint8List(1 << 20);
      while (true) {
        final n = raf.readIntoSync(buffer);
        if (n <= 0) break;
        input.add(buffer.sublist(0, n));
      }
    } finally {
      raf.closeSync();
      input.close();
    }
    return out.digestHex;
  }

  /// 创建上传会话。
  Future<UploadSession> createUpload({
    required String fileName,
    required int size,
    required String sha256,
    required String mimeType,
  }) async {
    final response = await dio.post<dynamic>(
      '$_base/api/mobile/v1/files/uploads',
      data: jsonEncode({
        'clientUploadId': _newId(),
        'fileName': fileName,
        'size': size,
        'sha256': sha256,
        'mimeType': mimeType,
      }),
      options: Options(contentType: 'application/json'),
    );
    return UploadSession.fromJson(response.data as Map<String, dynamic>);
  }

  /// 上传单个分块（带 `X-Part-SHA256` 头）。
  Future<void> uploadPart({
    required String uploadId,
    required int partNumber,
    required List<int> bytes,
    required String partSha256,
  }) async {
    await dio.put<void>(
      '$_base/api/mobile/v1/files/uploads/$uploadId/parts/$partNumber',
      data: bytes,
      options: Options(
        contentType: 'application/octet-stream',
        headers: {_partHeader: partSha256},
      ),
    );
  }

  /// 完成上传，返回 fileId。
  Future<String> completeUpload(String uploadId) async {
    final response = await dio.post<dynamic>(
      '$_base/api/mobile/v1/files/uploads/$uploadId/complete',
    );
    final data = response.data as Map<String, dynamic>;
    return data['fileId'] as String? ?? '';
  }

  /// 上传并完成；若会话已存在则跳过已传分块，返回 fileId。
  Future<String> uploadAndFinalize({
    required String filePath,
    required String fileName,
    required String mimeType,
    void Function(int uploadedBytes)? onProgress,
  }) async {
    final file = File(filePath);
    final size = await file.length();
    final hash = await computeSha256(file);
    final session = await createUpload(
      fileName: fileName,
      size: size,
      sha256: hash,
      mimeType: mimeType,
    );
    final partSize = session.partSize;
    final raf = file.openSync();
    var uploaded = 0;
    var partNumber = 1;
    try {
      while (true) {
        final bytes = raf.readSync(partSize);
        if (bytes.isEmpty) break;
        if (!session.uploadedParts.contains(partNumber)) {
          await uploadPart(
            uploadId: session.uploadId,
            partNumber: partNumber,
            bytes: bytes,
            partSha256: sha256.convert(bytes).toString(),
          );
          uploaded += bytes.length;
          await store.setUploadedBytes(session.uploadId, uploaded);
          onProgress?.call(uploaded);
        }
        partNumber++;
      }
    } finally {
      raf.closeSync();
    }
    final fileId = await completeUpload(session.uploadId);
    await store.clear(session.uploadId);
    return fileId;
  }

  /// Range 断点下载到 [localPath]，返回累计字节数。
  Future<int> downloadResume({
    required String fileId,
    required String localPath,
    void Function(int received, int total)? onProgress,
  }) async {
    var received = await store.downloadedBytes(fileId);
    final file = File(localPath);
    if (!file.existsSync()) {
      file.parent.createSync(recursive: true);
      file.createSync();
    }
    final raf = await file.open(mode: FileMode.append);
    try {
      final response = await dio.get<ResponseBody>(
        '$_base/api/mobile/v1/files/$fileId/content',
        options: Options(
          responseType: ResponseType.stream,
          headers: {'Range': 'bytes=$received-'},
        ),
      );
      final total =
          _totalFrom(response.headers.value('content-range'), received);
      final body = response.data!;
      await for (final bytes in body.stream) {
        await raf.writeFrom(bytes);
        received += bytes.length;
        await store.setDownloadedBytes(fileId, received);
        onProgress?.call(received, total);
      }
    } finally {
      await raf.close();
    }
    await store.clear(fileId);
    return received;
  }

  int _totalFrom(String? range, int current) {
    if (range == null || range.isEmpty) return -1;
    final match = RegExp(r'/(\d+)\s*$').firstMatch(range);
    return match == null ? -1 : int.parse(match.group(1)!);
  }

  static String _newId() {
    final now = DateTime.now().microsecondsSinceEpoch;
    final rand = math.Random().nextInt(0x7fffffff);
    return 'up-$now-$rand';
  }
}

/// 收集 SHA-256 最终摘要并输出 hex 的小 sink。
class _DigestSink implements Sink<Digest> {
  Digest? _digest;

  @override
  void add(Digest data) => _digest = data;

  @override
  void close() {}

  String get digestHex => _digest?.toString() ?? '';
}