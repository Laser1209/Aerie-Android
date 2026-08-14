import 'package:aerie_mobile/data/remote/mobile_gateway_client.dart';
import 'package:aerie_mobile/features/chat/chat_notifier.dart';
import 'package:aerie_mobile/features/chat/message_bubble.dart';
import 'package:aerie_mobile/features/chat/models/chat_message.dart';
import 'package:aerie_mobile/features/chat/models/task_status.dart';
import 'package:aerie_mobile/features/chat/sse_status_capsule.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 聊天屏（§3.2.4 / T2.1）。
///
/// 上半为本地消息流（drift 响应式列表），下半为输入栏（附件 + 发送胶囊）。
/// 依赖 `chatRepositoryProvider` 已在应用根装配。
class ChatScreen extends ConsumerStatefulWidget {
  /// 创建 [ChatScreen]。
  const ChatScreen({super.key});

  @override
  ConsumerState<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends ConsumerState<ChatScreen> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final text = _controller.text.trim();
    if (text.isEmpty) return;
    final notifier = ref.read(chatNotifierProvider.notifier);
    _controller.clear();
    await notifier.send(text: text);
  }

  @override
  Widget build(BuildContext context) {
    final messages = ref.watch(chatMessagesProvider);
    final sendState = ref.watch(chatNotifierProvider);
    final connectionHealth = ref.watch(connectionHealthProvider);
    final sending = sendState.status == ChatSendStatus.sending;

    return Scaffold(
      appBar: AppBar(
        title: const Text('会话'),
        actions: [SseStatusCapsule(health: connectionHealth)],
      ),
      body: Column(
        children: [
          Expanded(
            child: messages.when(
              data: (items) => ListView.builder(
                reverse: true,
                padding: const EdgeInsets.all(16),
                itemCount: items.length,
                itemBuilder: (context, index) {
                  final item = items[items.length - 1 - index];
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    child: MessageBubble(
                      message: item,
                      baseUrl: MobileGatewayClient.defaultBaseUrl(),
                      taskStatus: _taskStatusFor(item),
                    ),
                  );
                },
              ),
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (e, _) => Center(child: Text('加载失败：$e')),
            ),
          ),
          const Divider(height: 1),
          _InputBar(
            controller: _controller,
            sending: sending,
            onSubmitted: _submit,
          ),
        ],
      ),
    );
  }

  /// 本地消息无任务状态；SSE/文件阶段再回填（T2.2/T2.3）。
  TaskStatus? _taskStatusFor(ChatMessage _) => null;
}

/// 底部输入栏（胶囊 + 附件 + 发送钮）。
class _InputBar extends StatelessWidget {
  const _InputBar({
    required this.controller,
    required this.sending,
    required this.onSubmitted,
  });

  final TextEditingController controller;
  final bool sending;
  final VoidCallback onSubmitted;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 8, 12, 10),
        child: Row(
          children: [
            IconButton(
              icon: const Icon(Icons.attach_file),
              onPressed: sending ? null : () {},
              tooltip: '附件（T2.3 接入）',
            ),
            Expanded(
              child: TextField(
                controller: controller,
                enabled: !sending,
                minLines: 1,
                maxLines: 4,
                textInputAction: TextInputAction.send,
                onSubmitted: sending ? null : (_) => onSubmitted(),
                decoration: InputDecoration(
                  hintText: '输入消息…',
                  isDense: true,
                  filled: true,
                  fillColor: scheme.surfaceContainerHighest,
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 10,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(999),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 8),
            if (sending)
              const Padding(
                padding: EdgeInsets.all(12),
                child: SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              )
            else
              FilledButton(
                onPressed: onSubmitted,
                style: FilledButton.styleFrom(
                  shape: const CircleBorder(),
                  padding: const EdgeInsets.all(14),
                  backgroundColor: const Color(0xFFF5A3B7),
                ),
                child: const Icon(Icons.send, size: 20, color: Colors.white),
              ),
          ],
        ),
      ),
    );
  }
}
