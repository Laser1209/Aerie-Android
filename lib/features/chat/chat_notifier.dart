// 状态机公开成员均已用中文注释；忽略逐字段 doc 以保持简洁。
// ignore_for_file: public_member_api_docs

import 'package:aerie_mobile/data/repository/chat_repository.dart';
import 'package:aerie_mobile/features/chat/models/chat_message.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 发送操作状态。
enum ChatSendStatus {
  /// 空闲（可输入）。
  initial,

  /// 提交中（按钮禁用）。
  sending,

  /// 提交成功。
  sent,

  /// 提交失败（展示错误并可重试）。
  failed,
}

/// 发送区的 UI 状态。
class ChatSendState {
  /// 创建 [ChatSendState]。
  const ChatSendState({required this.status, this.error, this.requestId});

  const ChatSendState.initial() : this(status: ChatSendStatus.initial);

  final ChatSendStatus status;

  /// 失败原因（仅 [ChatSendStatus.failed] 时有值）。
  final String? error;

  /// 提交成功的请求 ID（仅 [ChatSendStatus.sent] 时有值）。
  final String? requestId;
}

/// 会话仓库注入点（须在应用根 override，接入真实 7891 客户端）。
final chatRepositoryProvider =
    Provider<ChatRepository>((ref) => throw StateError(
          'chatRepositoryProvider 未在应用装配点注入',
        ));

/// 本地消息流（由 drift 响应式驱动，与后续 SSE 增量保持一致）。
final chatMessagesProvider = StreamProvider<List<ChatMessage>>((ref) {
  return ref.watch(chatRepositoryProvider).watchMessages();
});

/// 发送区状态机（T2.1）Provider。
final chatNotifierProvider =
    NotifierProvider<ChatNotifier, ChatSendState>(ChatNotifier.new);

/// 发送区状态机（T2.1）。
class ChatNotifier extends Notifier<ChatSendState> {
  @override
  ChatSendState build() => const ChatSendState.initial();

  ChatRepository get _repository => ref.read(chatRepositoryProvider);

  Future<void> send({
    required String text,
    String? clientRequestId,
    List<String> fileIds = const [],
  }) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) return;
    state = const ChatSendState(status: ChatSendStatus.sending);
    try {
      final result = await _repository.sendMessage(
        text: trimmed,
        clientRequestId:
            clientRequestId ?? 'c-${DateTime.now().microsecondsSinceEpoch}',
        fileIds: fileIds,
      );
      state = ChatSendState(
        status: ChatSendStatus.sent,
        requestId: result.requestId,
      );
    } on Exception catch (_) {
      state = const ChatSendState(
        status: ChatSendStatus.failed,
        error: 'send_failed',
      );
    }
  }

  /// 重置为可输入态（发送成功后调用）。
  void reset() => state = const ChatSendState.initial();
}
