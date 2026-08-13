// drift 表字段 getter 为声明式数据定义，字段名自解释，无需逐字段文档。
// ignore_for_file: public_member_api_docs

import 'package:drift/drift.dart';

part 'chat_database.g.dart';

/// 聊天消息表（§3.2.2：accountId 隔离、messageOrder 排序）。
class Messages extends Table {
  TextColumn get messageId => text()();

  IntColumn get messageOrder => integer()();

  IntColumn get accountId => integer()();

  TextColumn get conversationId => text()();

  TextColumn get turnId => text()();

  TextColumn get role => text()();

  TextColumn get content => text()();

  TextColumn get attachments => text().withDefault(const Constant('[]'))();

  TextColumn get replyToId => text().nullable()();

  TextColumn get replyToContent => text().nullable()();

  TextColumn get replyToRole => text().nullable()();

  DateTimeColumn get createdAt => dateTime()();

  @override
  Set<Column> get primaryKey => {messageId};
}

/// 待确认请求表（§3.2.2：待确认队列）。
class Requests extends Table {
  TextColumn get requestId => text()();

  IntColumn get accountId => integer()();

  TextColumn get content => text()();

  TextColumn get status => text()();

  DateTimeColumn get createdAt => dateTime()();

  @override
  Set<Column> get primaryKey => {requestId};
}

/// 文件传输状态表（§3.2.5：上传 / 下载续传点）。
class FileTransfers extends Table {
  TextColumn get id => text()();

  IntColumn get uploadedBytes => integer().withDefault(const Constant(0))();

  IntColumn get downloadedBytes => integer().withDefault(const Constant(0))();

  TextColumn get status => text()();

  @override
  Set<Column> get primaryKey => {id};
}

/// 移动端本地数据库（§3.2.2 drift）。
@DriftDatabase(tables: [Messages, Requests, FileTransfers])
class ChatDatabase extends _$ChatDatabase {
  /// 创建 [ChatDatabase]。
  ChatDatabase(super.e);

  @override
  int get schemaVersion => 1;
}
