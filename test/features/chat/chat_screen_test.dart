import 'package:aerie_mobile/features/chat/chat_notifier.dart';
import 'package:aerie_mobile/features/chat/chat_screen.dart';
import 'package:aerie_mobile/features/chat/models/chat_message.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

ChatMessage _msg(String id, int order, String role, String content) =>
    ChatMessage(
      messageId: id,
      messageOrder: order,
      conversationId: 'c1',
      turnId: 't1',
      role: role,
      content: content,
      attachments: const [],
      replyToId: null,
      replyToContent: null,
      replyToRole: null,
      createdAt: DateTime.utc(2026, 8, 14, 10).add(Duration(seconds: order)),
    );

Widget _wrap(List<ChatMessage> messages) => ProviderScope(
      overrides: [
        chatMessagesProvider.overrideWith(
          (ref) => Stream<List<ChatMessage>>.value(messages),
        ),
      ],
      child: const MaterialApp(home: ChatScreen()),
    );

void main() {
  group('ChatScreen', () {
    testWidgets('渲染本地消息列表与输入栏', (tester) async {
      await tester.pumpWidget(
        _wrap([
          _msg('m1', 1, 'assistant', '你好，伊塔在线。'),
          _msg('m2', 2, 'user', '早上好'),
        ]),
      );
      await tester.pumpAndSettle();

      expect(find.text('你好，伊塔在线。'), findsOneWidget);
      expect(find.text('早上好'), findsOneWidget);
      expect(find.byType(TextField), findsOneWidget);
      expect(find.byIcon(Icons.send), findsOneWidget);
      expect(find.byIcon(Icons.attach_file), findsOneWidget);
    });
  });
}