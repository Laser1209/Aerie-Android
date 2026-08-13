import 'package:aerie_mobile/features/chat/message_bubble.dart';
import 'package:aerie_mobile/features/chat/models/chat_message.dart';
import 'package:aerie_mobile/features/chat/models/task_status.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

ChatMessage _message({
  String role = 'assistant',
  String content = '你好',
  List<ChatAttachment> attachments = const [],
}) {
  return ChatMessage(
    messageId: 'm1',
    messageOrder: 1,
    conversationId: 'c1',
    turnId: 't1',
    role: role,
    content: content,
    attachments: attachments,
    replyToId: null,
    replyToContent: null,
    replyToRole: null,
    createdAt: DateTime(2026, 8, 14, 10, 30),
  );
}

Widget _wrap(ChatMessage message, {TaskStatus? taskStatus}) => MaterialApp(
      home: Scaffold(
        body: MessageBubble(message: message, taskStatus: taskStatus),
      ),
    );

void main() {
  group('MessageBubble', () {
    testWidgets('assistant 消息展示内容', (tester) async {
      await tester.pumpWidget(_wrap(_message(content: '早上好')));

      expect(find.text('早上好'), findsOneWidget);
      expect(find.byIcon(Icons.send), findsNothing);
    });

    testWidgets('user 消息右对齐并展示附件文件卡', (tester) async {
      const attachment = ChatAttachment(
        name: '报告.pdf',
        url: '/api/mobile/v1/files/f1/content',
        state: 'ready',
        size: 1048576,
        type: 'application/pdf',
      );
      await tester.pumpWidget(
        _wrap(_message(role: 'user', attachments: [attachment])),
      );

      expect(find.text('报告.pdf'), findsOneWidget);
      expect(find.text('1.0 MB'), findsOneWidget);
    });

    testWidgets('发送中展示任务徽章', (tester) async {
      await tester.pumpWidget(
        _wrap(_message(role: 'user'), taskStatus: TaskStatus.sending),
      );

      expect(find.text('发送中'), findsOneWidget);
    });

    testWidgets('已完成不展示徽章', (tester) async {
      await tester.pumpWidget(
        _wrap(_message(role: 'user'), taskStatus: TaskStatus.completed),
      );

      expect(find.text('发送中'), findsNothing);
      expect(find.text('已完成'), findsNothing);
      expect(find.text('已取消'), findsNothing);
    });
  });
}
