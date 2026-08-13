import 'package:aerie_mobile/data/local/auth_store.dart';
import 'package:aerie_mobile/data/repository/auth_repository.dart';
import 'package:flutter_test/flutter_test.dart';

/// 内存令牌存储（替代平台 flutter_secure_storage）。
class _MemoryTokenStore implements AuthTokenStore {
  String? accessToken;
  String? refreshToken;

  @override
  Future<void> setAccessToken(String token) async {
    accessToken = token;
  }

  @override
  Future<void> setRefreshToken(String token) async {
    refreshToken = token;
  }
}

void main() {
  late _MemoryTokenStore store;
  late AuthRepository repo;

  setUp(() {
    store = _MemoryTokenStore();
    repo = AuthRepository(store: store);
  });

  const account = AccountInfo(
    accountId: 'acct_1',
    username: 'aerie',
    role: 'owner',
    actorId: 'actor_1',
  );

  group('AuthRepository 登录状态机', () {
    test('未注入 loginCall 时置 AuthFailed', () async {
      await repo.login('aerie', 'password123');

      expect(repo.state, isA<AuthFailed>());
      expect((repo.state as AuthFailed).message, 'login_unavailable');
    });

    test('登录成功写入令牌并置 AuthAuthenticated', () async {
      repo = AuthRepository(
        store: store,
        loginCall: (username, pass) async => const LoginResult(
          accessToken: 'access-1',
          refreshToken: 'refresh-1',
          account: account,
        ),
      );

      await repo.login('aerie', 'password123');

      expect(repo.state, isA<AuthAuthenticated>());
      expect((repo.state as AuthAuthenticated).account.username, 'aerie');
      expect(store.accessToken, 'access-1');
      expect(store.refreshToken, 'refresh-1');
    });

    test('登录失败：置 AuthFailed 且不写令牌', () async {
      repo = AuthRepository(
        store: store,
        loginCall: (username, pass) async {
          throw Exception('bad credentials');
        },
      );

      await repo.login('aerie', 'wrong');

      expect(repo.state, isA<AuthFailed>());
      expect((repo.state as AuthFailed).message, 'login_failed');
      expect(store.accessToken, isNull);
      expect(store.refreshToken, isNull);
    });

    test('登出清空令牌并回到 AuthUnknown', () async {
      repo = AuthRepository(
        store: store,
        loginCall: (username, pass) async => const LoginResult(
          accessToken: 'access-1',
          refreshToken: 'refresh-1',
          account: account,
        ),
      );
      await repo.login('aerie', 'password123');
      expect(repo.state, isA<AuthAuthenticated>());

      await repo.logout();

      expect(repo.state, isA<AuthUnknown>());
      expect(store.accessToken, '');
      expect(store.refreshToken, '');
    });
  });

  group('AccountInfo.fromJson', () {
    test('解析登录响应的 account 字段', () {
      final info = AccountInfo.fromJson(const {
        'accountId': 'acct_9',
        'username': 'guest1',
        'role': 'guest',
        'actorId': 'actor_9',
      });

      expect(info.accountId, 'acct_9');
      expect(info.username, 'guest1');
      expect(info.role, 'guest');
      expect(info.actorId, 'actor_9');
    });
  });
}
