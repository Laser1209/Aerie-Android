import 'dart:convert';

import 'package:aerie_mobile/data/local/chat_database.dart';
import 'package:aerie_mobile/features/chat/models/chat_message.dart';
import 'package:aerie_mobile/gen/api/api_client.dart';
import 'package:aerie_mobile/gen/models/submit_request.dart';
import 'package:drift/drift.dart';

/// 一页消息。
class MessagePage {
  /// 创建 [MessagePage]。
  const MessagePage({required this.items, required this.hasMore});

  /// 按时间升序的消息。
  final List<ChatMessage> items;

  /// 是否还有更早消息可翻页。
  final bool hasMore;
}

/// 发送结果（`POST /requests` 返回）。
class SubmitResult {
  /// 创建 [SubmitResult]。
  const SubmitResult({
    required this.requestId,
    required this.conversationId,
    required this.status,
    required this.clientRequestId,
  });

  /// 请求 ID。
  final String requestId;

  /// 会话 ID。
  final String conversationId;

  /// 状态。
  final String status;

  /// 客户端幂等 ID。
  final String clientRequestId;
}

/// 聊天仓库（§3.2.3 / T2.1）。
///
/// 封装 7891 消息/请求 API，并将拉取到的消息按 [accountId] 隔离写入本地
/// drift 缓存，供 [watchMessages] 响应式驱动 UI。本节点暂不接 SSE 增量
/// （T2.2 实现），网络均通过注入的 [ApiClient]，测试注入 mock。
class ChatRepository {
  /// 创建 [ChatRepository]。
  ChatRepository({
    required this.client,
    required this.database,
    required this.accountId,
  });

  /// OpenAPI 生成的 7891 客户端。
  final ApiClient client;

  /// drift 本地缓存。
  final ChatDatabase database;

  /// 当前登录账号 ID（隔离数据）。
  final int accountId;

  /// 分页拉取消息并写入本地缓存。
  ///
  /// [beforeId] 为空表示拉取最新一页；否则拉取该消息更早的历史。
  Future<MessagePage> fetchPage({int limit = 50, String? beforeId}) async {
    final raw = await client.messagesApiMobileV1MessagesGet(
      limit: limit,
      beforeId: beforeId,
    ) as Map<String, dynamic>;

    final itemsRaw = raw['items'] as List<dynamic>? ?? const [];
    final hasMore = raw['hasMore'] as bool? ?? false;
    final items = itemsRaw
        .whereType<Map<String, dynamic>>()
        .map(ChatMessage.fromJson)
        .toList();

    await _upsert(items);
    return MessagePage(items: items, hasMore: hasMore);
  }

  /// 发送一条请求（文本 + 可选附件）。
  ///
  /// [clientRequestId] 为客户端幂等键；服务端按此去重。
  Future<SubmitResult> sendMessage({
    required String text,
    required String clientRequestId,
    List<String> fileIds = const [],
    String? replyToId,
  }) async {
    final raw = await client.submitRequestApiMobileV1RequestsPost(
      body: SubmitRequest(
        clientRequestId: clientRequestId,
        text: text,
        fileIds: fileIds.isEmpty ? null : fileIds,
        replyToId: int.tryParse(replyToId ?? '') ?? 0,
      ),
    ) as Map<String, dynamic>;

    return SubmitResult(
      requestId: raw['requestId'] as String? ?? '',
      conversationId: raw['conversationId'] as String? ?? '',
      status: raw['status'] as String? ?? '',
      clientRequestId: raw['clientRequestId'] as String? ?? clientRequestId,
    );
  }

  /// 取消一条待确认请求。
  Future<void> cancelRequest(String requestId) async {
    await client.cancelRequestApiMobileV1RequestsRequestIdCancelPost(
      requestId: requestId,
    );
  }

  /// 重试一条请求。
  Future<void> retryRequest(String requestId) async {
    await client.retryRequestApiMobileV1RequestsRequestIdRetryPost(
      requestId: requestId,
    );
  }

  /// 响应式监听本地消息（按 messageOrder 升序）。
  Stream<List<ChatMessage>> watchMessages() {
    final query = database.select(database.messages)
      ..where((t) => t.accountId.equals(accountId))
      ..orderBy([(t) => OrderingTerm.asc(t.messageOrder)]);
    return query.watch().map((rows) => rows.map(_fromRow).toList());
  }

  ChatMessage _fromRow(Message row) => ChatMessage(
        messageId: row.messageId,
        messageOrder: row.messageOrder,
        conversationId: row.conversationId,
        turnId: row.turnId,
        role: row.role,
        content: row.content,
        attachments: _decodeAttachments(row.attachments),
        replyToId: row.replyToId,
        replyToContent: row.replyToContent,
        replyToRole: row.replyToRole,
        createdAt: row.createdAt,
      );

  Future<void> _upsert(List<ChatMessage> items) async {
    for (final message in items) {
      await database.into(database.messages).insertReturning(
            MessagesCompanion.insert(
              messageId: message.messageId,
              messageOrder: message.messageOrder,
              accountId: accountId,
              conversationId: message.conversationId,
              turnId: message.turnId,
              role: message.role,
              content: message.content,
              attachments: Value(_encodeAttachments(message.attachments)),
              replyToId: Value(message.replyToId),
              replyToContent: Value(message.replyToContent),
              replyToRole: Value(message.replyToRole),
              createdAt: message.createdAt,
            ),
            mode: InsertMode.insertOrReplace,
          );
    }
  }

  static String _encodeAttachments(List<ChatAttachment> attachments) {
    final maps = attachments
        .map(
          (a) => <String, Object?>{
            'name': a.name,
            'url': a.url,
            'state': a.state,
            'size': a.size,
            'type': a.type,
            'sha256': a.sha256,
          },
        )
        .toList();
    return jsonEncode(maps);
  }

  static List<ChatAttachment> _decodeAttachments(String raw) {
    if (raw.isEmpty) return const [];
    try {
      final list = jsonDecode(raw) as List<dynamic>;
      return list
          .whereType<Map<String, dynamic>>()
          .map(ChatAttachment.fromJson)
          .toList();
    } on Exception catch (_) {
      return const [];
    }
  }
}
