import 'package:aerie_mobile/features/chat/sse_controller.dart';
import 'package:aerie_mobile/features/chat/sse_status_capsule.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _wrap(SseHealth health) => MaterialApp(
      home: Scaffold(
        appBar: AppBar(actions: [SseStatusCapsule(health: health)]),
      ),
    );

void main() {
  group('SseStatusCapsule', () {
    testWidgets('connected 展示实时在线', (tester) async {
      await tester.pumpWidget(_wrap(SseHealth.connected));
      expect(find.text('实时在线'), findsOneWidget);
    });

    testWidgets('reconnecting 展示重连中', (tester) async {
      await tester.pumpWidget(_wrap(SseHealth.reconnecting));
      expect(find.text('重连中'), findsOneWidget);
    });

    testWidgets('disconnected 展示未连接', (tester) async {
      await tester.pumpWidget(_wrap(SseHealth.disconnected));
      expect(find.text('未连接'), findsOneWidget);
    });
  });
}
