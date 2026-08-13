import 'package:aerie_mobile/data/local/chat_database.dart';
import 'package:aerie_mobile/data/local/file_transfer_store.dart';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late ChatDatabase db;

  setUp(() {
    db = ChatDatabase(NativeDatabase.memory());
  });

  tearDown(() async {
    await db.close();
  });

  group('ChatDatabase', () {
    test('插入并查询消息，按 messageOrder 升序', () async {
      await db.into(db.messages).insert(
            MessagesCompanion.insert(
              messageId: 'm2',
              messageOrder: 2,
              accountId: 1,
              conversationId: 'c1',
              turnId: 't1',
              role: 'assistant',
              content: '第二条',
              createdAt: DateTime(2026, 8, 13),
            ),
          );
      await db.into(db.messages).insert(
            MessagesCompanion.insert(
              messageId: 'm1',
              messageOrder: 1,
              accountId: 1,
              conversationId: 'c1',
              turnId: 't1',
              role: 'user',
              content: '第一条',
              createdAt: DateTime(2026, 8, 13),
            ),
          );

      final query = db.select(db.messages)
        ..orderBy([(table) => OrderingTerm.asc(table.messageOrder)]);
      final rows = await query.get();

      expect(rows, hasLength(2));
      expect(rows.first.messageId, 'm1');
      expect(rows.last.messageId, 'm2');
    });

    test('按 accountId 隔离查询', () async {
      await db.into(db.messages).insert(
            MessagesCompanion.insert(
              messageId: 'a1',
              messageOrder: 1,
              accountId: 1,
              conversationId: 'c1',
              turnId: 't1',
              role: 'user',
              content: '账号 1',
              createdAt: DateTime(2026, 8, 13),
            ),
          );
      await db.into(db.messages).insert(
            MessagesCompanion.insert(
              messageId: 'a2',
              messageOrder: 2,
              accountId: 2,
              conversationId: 'c1',
              turnId: 't1',
              role: 'user',
              content: '账号 2',
              createdAt: DateTime(2026, 8, 13),
            ),
          );

      final query = db.select(db.messages)
        ..where((table) => table.accountId.equals(1));
      final rows = await query.get();

      expect(rows, hasLength(1));
      expect(rows.single.messageId, 'a1');
    });
  });

  group('FileTransferStore', () {
    test('续传点读写与清理', () async {
      final store = FileTransferStore(db);

      await store.setUploadedBytes('upload-1', 4096);
      expect(await store.uploadedBytes('upload-1'), 4096);

      await store.setDownloadedBytes('upload-1', 2048);
      expect(await store.downloadedBytes('upload-1'), 2048);

      await store.clear('upload-1');
      expect(await store.uploadedBytes('upload-1'), 0);
    });
  });
}
