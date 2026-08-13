import 'package:aerie_mobile/features/chat/models/task_status.dart';
import 'package:aerie_mobile/features/files/transfer_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _wrap({required TaskStatus status, double progress = 0.5}) {
  return MaterialApp(
    home: Scaffold(
      body: TransferCard(
        fileName: '报告.pdf',
        progress: progress,
        status: status,
      ),
    ),
  );
}

void main() {
  group('TransferCard', () {
    testWidgets('展示文件名/进度百分比/状态', (tester) async {
      await tester.pumpWidget(_wrap(progress: 0.6, status: TaskStatus.sending));

      expect(find.text('报告.pdf'), findsOneWidget);
      expect(find.text('60%'), findsOneWidget);
      expect(find.text('发送中'), findsOneWidget);
    });

    testWidgets('下载中展示对应状态', (tester) async {
      await tester.pumpWidget(_wrap(status: TaskStatus.downloading));

      expect(find.text('下载中'), findsOneWidget);
    });

    testWidgets('失败展示失败状态', (tester) async {
      await tester.pumpWidget(_wrap(status: TaskStatus.failed));

      expect(find.text('失败'), findsOneWidget);
    });
  });
}
