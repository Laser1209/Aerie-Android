import 'package:aerie_mobile/features/chat/models/chat_message.dart';
import 'package:aerie_mobile/features/chat/models/task_status.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';

/// 消息气泡（§3.2.10 / T2.1）。
///
/// 对方（assistant）左对齐白气泡，用户（user）右对齐浅粉底气泡；附件渲染
/// 为文件卡（图标 + 名 + 大小），图片附件用 [CachedNetworkImage] 懒加载；
/// 可叠加任务状态徽章。纯展示，便于 Widget 测试。
class MessageBubble extends StatelessWidget {
  /// 创建 [MessageBubble]。
  ///
  /// [baseUrl] 用于把相对附件 URL 拼成绝对地址（取移动网关 Debug/Release
  /// baseUrl）；为空时图片附件回退为文件卡。
  const MessageBubble({
    required this.message,
    this.baseUrl,
    super.key,
    this.taskStatus,
  });

  /// 待展示的消息。
  final ChatMessage message;

  /// 服务端 baseUrl（拼接图片附件绝对地址）。
  final String? baseUrl;

  /// 可选任务状态（发送中/已取消/失败等）。
  final TaskStatus? taskStatus;

  bool get _isUser => message.role == 'user';

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    final bubbleColor = _isUser ? const Color(0xFFFCE4EC) : scheme.surface;
    final textColor = _isUser ? const Color(0xFF3A222B) : scheme.onSurface;

    return Align(
      alignment: _isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!_isUser) ...[
            CircleAvatar(radius: 16, backgroundColor: scheme.primaryContainer),
            const SizedBox(width: 8),
          ],
          Flexible(
            child: Container(
              constraints: const BoxConstraints(maxWidth: 300),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: bubbleColor,
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(16),
                  topRight: const Radius.circular(16),
                  bottomLeft: Radius.circular(_isUser ? 16 : 4),
                  bottomRight: Radius.circular(_isUser ? 4 : 16),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (message.content.isNotEmpty)
                    Text(
                      message.content,
                      style: textTheme.bodyMedium?.copyWith(color: textColor),
                    ),
                  if (message.content.isNotEmpty &&
                      message.attachments.isNotEmpty)
                    const SizedBox(height: 8),
                  for (final attachment in message.attachments)
                    _AttachmentCard(
                      attachment: attachment,
                      isUser: _isUser,
                      baseUrl: baseUrl,
                    ),
                  const SizedBox(height: 2),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (taskStatus != null &&
                          taskStatus != TaskStatus.completed) ...[
                        _TaskBadge(status: taskStatus!),
                        const SizedBox(width: 6),
                      ],
                      Text(
                        _formatTime(message.createdAt),
                        style: textTheme.labelSmall?.copyWith(
                          color: scheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          if (_isUser) ...[
            const SizedBox(width: 8),
            CircleAvatar(radius: 16, backgroundColor: scheme.primary),
          ],
        ],
      ),
    );
  }

  String _formatTime(DateTime t) {
    final hour = t.hour.toString().padLeft(2, '0');
    final min = t.minute.toString().padLeft(2, '0');
    return '$hour:$min';
  }
}

/// 任务状态徽标（发送中/失败/已取消）。
class _TaskBadge extends StatelessWidget {
  const _TaskBadge({required this.status});

  final TaskStatus status;

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (status) {
      TaskStatus.sending => ('发送中', const Color(0xFF7FA8D9)),
      TaskStatus.downloading => ('下载中', const Color(0xFF7FA8D9)),
      TaskStatus.canceled => ('已取消', const Color(0xFF8A7B82)),
      TaskStatus.failed => ('失败', const Color(0xFFE87C7C)),
      TaskStatus.completed => ('', Colors.transparent),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: TextStyle(fontSize: 11, color: color),
      ),
    );
  }
}

/// 附件卡片：图片懒加载缩略图，其他类型显示文件卡。
class _AttachmentCard extends StatelessWidget {
  const _AttachmentCard({
    required this.attachment,
    required this.isUser,
    this.baseUrl,
  });

  final ChatAttachment attachment;
  final bool isUser;
  final String? baseUrl;

  String? get _imageUrl {
    final base = baseUrl;
    if (!attachment.isImage || base == null) return null;
    final url = attachment.url;
    return url.startsWith('http') ? url : '$base$url';
  }

  @override
  Widget build(BuildContext context) {
    final imageUrl = _imageUrl;
    if (imageUrl != null) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: CachedNetworkImage(
          imageUrl: imageUrl,
          width: 184,
          fit: BoxFit.cover,
          placeholder: (_, _) => Container(
            width: 184,
            height: 120,
            color: const Color(0xFFFCE4EC),
            child: const Center(
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
          ),
          errorWidget: (_, _, _) => const SizedBox(
            width: 184,
            height: 120,
            child: Icon(Icons.broken_image),
          ),
        ),
      );
    }
    return Container(
      width: 180,
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.6),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFF0E4E4)),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.insert_drive_file,
            size: 22,
            color: Color(0xFF7FA8D9),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  attachment.name,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 13,
                    color: Color(0xFF3D2F35),
                  ),
                ),
                Text(
                  _formatSize(attachment.size),
                  style: const TextStyle(
                    fontSize: 11,
                    color: Color(0xFF8A7B82),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

String _formatSize(int bytes) {
  if (bytes < 1024) return '$bytes B';
  if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
  return '${(bytes / 1024 / 1024).toStringAsFixed(1)} MB';
}
