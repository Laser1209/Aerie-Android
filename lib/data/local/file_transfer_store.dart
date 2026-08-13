import 'package:aerie_mobile/data/local/chat_database.dart';
import 'package:drift/drift.dart';

/// 文件传输状态存储（§3.2.5 续传点）。
///
/// 记录上传 / 下载已处理字节数，供断点续传使用。
class FileTransferStore {
  /// 创建 [FileTransferStore]。
  FileTransferStore(this._db);

  final ChatDatabase _db;

  /// 读取上传续传点（已上传字节数）。
  Future<int> uploadedBytes(String id) async {
    final query = _db.select(_db.fileTransfers)
      ..where((table) => table.id.equals(id));
    final row = await query.getSingleOrNull();
    return row?.uploadedBytes ?? 0;
  }

  /// 读取下载续传点（已下载字节数）。
  Future<int> downloadedBytes(String id) async {
    final query = _db.select(_db.fileTransfers)
      ..where((table) => table.id.equals(id));
    final row = await query.getSingleOrNull();
    return row?.downloadedBytes ?? 0;
  }

  /// 更新上传续传点。
  Future<void> setUploadedBytes(String id, int bytes) async {
    await _db.into(_db.fileTransfers).insertOnConflictUpdate(
          FileTransfersCompanion.insert(
            id: id,
            status: 'uploading',
            uploadedBytes: Value(bytes),
          ),
        );
  }

  /// 更新下载续传点。
  Future<void> setDownloadedBytes(String id, int bytes) async {
    await _db.into(_db.fileTransfers).insertOnConflictUpdate(
          FileTransfersCompanion.insert(
            id: id,
            status: 'downloading',
            downloadedBytes: Value(bytes),
          ),
        );
  }

  /// 删除续传点（传输完成 / 取消后清理）。
  Future<void> clear(String id) async {
    final query = _db.delete(_db.fileTransfers)
      ..where((table) => table.id.equals(id));
    await query.go();
  }
}
