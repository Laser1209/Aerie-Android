/// 聊天领域模型（§3.2.2 / T2.1）。
///
/// 与 7891 `/messages` 返回的消息 JSON 字段一一对应，
/// 由 `chat_repository` 映射后驱动本地 drift 缓存与 UI。
library;

/// 消息附件（`attachments` 列表项，见 `resolve_request_attachments`）。
class ChatAttachment {
  /// 创建 [ChatAttachment]。
  const ChatAttachment({
    required this.name,
    required this.url,
    required this.size,
    required this.type,
    this.state,
    this.sha256,
  });

  /// 从服务端 JSON 构造。
  factory ChatAttachment.fromJson(Map<String, dynamic> json) => ChatAttachment(
        name: json['name'] as String? ?? '',
        url: json['url'] as String? ?? '',
        state: json['state'] as String?,
        size: (json['size'] as num?)?.toInt() ?? 0,
        type: json['type'] as String? ?? '',
        sha256: json['sha256'] as String?,
      );

  /// 展示名。
  final String name;

  /// 相对下载地址（如 `/api/mobile/v1/files/{id}/content`）。
  final String url;

  /// 状态（ready）。
  final String? state;

  /// 字节数。
  final int size;

  /// MIME 类型。
  final String type;

  /// 完整文件或分块 SHA-256。
  final String? sha256;

  /// 是否为图片（按 MIME 判断，用于懒加载缩略图）。
  bool get isImage => type.startsWith('image/');
}

/// 单条聊天消息。
class ChatMessage {
  /// 创建 [ChatMessage]。
  const ChatMessage({
    required this.messageId,
    required this.messageOrder,
    required this.conversationId,
    required this.turnId,
    required this.role,
    required this.content,
    required this.attachments,
    required this.createdAt,
    required this.replyToId,
    required this.replyToContent,
    required this.replyToRole,
  });

  /// 从服务端消息 JSON 构造。
  factory ChatMessage.fromJson(Map<String, dynamic> json) {
    final raw = json['attachments'];
    return ChatMessage(
      messageId: json['messageId'] as String? ?? '',
      messageOrder: (json['messageOrder'] as num?)?.toInt() ?? 0,
      conversationId: json['conversationId'] as String? ?? '',
      turnId: json['turnId'] as String? ?? '',
      role: json['role'] as String? ?? '',
      content: json['content'] as String? ?? '',
      attachments: (raw as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(ChatAttachment.fromJson)
          .toList(),
      replyToId: json['replyToId'] as String?,
      replyToContent: json['replyToContent'] as String?,
      replyToRole: json['replyToRole'] as String?,
      createdAt: DateTime.tryParse(json['createdAt'] as String? ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0),
    );
  }

  /// 消息 ID。
  final String messageId;

  /// 服务端顺序号（rowid）。
  final int messageOrder;

  /// 会话 ID。
  final String conversationId;

  /// 轮次 ID。
  final String turnId;

  /// 角色（user / assistant / system）。
  final String role;

  /// 文本内容。
  final String content;

  /// 附件列表。
  final List<ChatAttachment> attachments;

  /// 引用的消息 ID（空则无引用）。
  final String? replyToId;

  /// 引用消息内容。
  final String? replyToContent;

  /// 引用消息角色。
  final String? replyToRole;

  /// 创建时间。
  final DateTime createdAt;
}
