import 'package:aerie_mobile/data/local/auth_store.dart';
import 'package:aerie_mobile/data/repository/auth_repository.dart';
import 'package:aerie_mobile/features/auth/auth_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// 内存令牌存储（避免 Widget 测试触碰平台 flutter_secure_storage）。
class _MemoryTokenStore implements AuthTokenStore {
  @override
  Future<void> setAccessToken(String token) async {}

  @override
  Future<void> setRefreshToken(String token) async {}
}

/// 以指定 state 构造仓库并包裹在一次渲染中。
Future<void> _pump(WidgetTester tester, AuthState state) async {
  final repo = AuthRepository(store: _MemoryTokenStore())..state = state;
  await tester.pumpWidget(
    ProviderScope(
      overrides: [authRepositoryProvider.overrideWithValue(repo)],
      child: const MaterialApp(home: AuthScreen()),
    ),
  );
}

void main() {
  group('AuthScreen', () {
    testWidgets('渲染用户名/密码输入框与登录按钮', (tester) async {
      await _pump(tester, const AuthUnknown());

      expect(find.text('Aerie 登录'), findsOneWidget);
      expect(find.text('用户名'), findsOneWidget);
      expect(find.text('密码'), findsOneWidget);
      expect(find.text('登录'), findsOneWidget);
      expect(find.byType(TextField), findsNWidgets(2));
      expect(find.byType(FilledButton), findsOneWidget);
    });

    testWidgets('AuthFailed 时展示错误文案且登录按钮可用', (tester) async {
      await _pump(tester, const AuthFailed('bad credentials'));

      expect(find.text('bad credentials'), findsOneWidget);
      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNotNull,
      );
    });

    testWidgets('AuthLoading 时禁用按钮并显示进度指示', (tester) async {
      await _pump(tester, const AuthLoading());

      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNull,
      );
    });
  });
}
