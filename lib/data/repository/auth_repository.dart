// 状态机与 DTO 的类级注释已覆盖意图；不逐构造器补 doc 以免冗余。
// ignore_for_file: public_member_api_docs

import 'package:aerie_mobile/data/local/auth_store.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 认证仓库（§3.2.3 登录流程状态机，T1.4）。
///
/// 本节点实现登录状态机的状态流转与令牌落盘；真实登录 API 在 P-S2
/// 复用已生成的 `lib/gen` 客户端。当前用注入的 `loginCall` 回调驱动
/// 状态机，保证「状态机可测 + 存储可验证」，不虚构未接通的后端行为。

/// 账号元信息（登录响应 `account` 字段）。
class AccountInfo {
  /// 创建 [AccountInfo]。
  const AccountInfo({
    required this.accountId,
    required this.username,
    required this.role,
    required this.actorId,
  });

  /// 从登录响应 JSON 构造。
  factory AccountInfo.fromJson(Map<String, dynamic> json) => AccountInfo(
        accountId: json['accountId'] as String? ?? '',
        username: json['username'] as String? ?? '',
        role: json['role'] as String? ?? '',
        actorId: json['actorId'] as String? ?? '',
      );

  /// 账号 ID。
  final String accountId;

  /// 用户名。
  final String username;

  /// 角色（owner / guest）。
  final String role;

  /// 行为体 ID。
  final String actorId;
}

/// 登录响应。
class LoginResult {
  /// 创建 [LoginResult]。
  const LoginResult({
    required this.accessToken,
    required this.refreshToken,
    required this.account,
  });

  /// 访问令牌。
  final String accessToken;

  /// 刷新令牌。
  final String refreshToken;

  /// 账号元信息。
  final AccountInfo account;
}

/// 认证状态。
sealed class AuthState {
  const AuthState();
}

/// 初始态（尚未发起登录）。
class AuthUnknown extends AuthState {
  const AuthUnknown();
}

/// 登录中。
class AuthLoading extends AuthState {
  const AuthLoading();
}

/// 已登录。
class AuthAuthenticated extends AuthState {
  const AuthAuthenticated(this.account);

  /// 账号信息。
  final AccountInfo account;
}

/// 登录失败。
class AuthFailed extends AuthState {
  const AuthFailed(this.message);

  /// 失败说明。
  final String message;
}

/// 认证仓库（T1.4）。
class AuthRepository {
  /// 创建 [AuthRepository]。
  AuthRepository({required this.store, this.loginCall});

  final AuthTokenStore store;

  /// 登录回调（P-S2 绑定真实 `/auth/login`；测试注入 fake）。
  final Future<LoginResult> Function(
    String username,
    String password,
  )? loginCall;

  /// 当前认证状态。
  AuthState state = const AuthUnknown();

  /// 执行登录。
  ///
  /// 成功：写 access/refresh 令牌到存储，状态置 [AuthAuthenticated]。
  /// 失败：状态置 [AuthFailed]，不写任何令牌。
  Future<void> login(String username, String password) async {
    final call = loginCall;
    if (call == null) {
      state = const AuthFailed('login_unavailable');
      return;
    }
    state = const AuthLoading();
    try {
      final result = await call(username, password);
      await store.setAccessToken(result.accessToken);
      await store.setRefreshToken(result.refreshToken);
      state = AuthAuthenticated(result.account);
    } on Exception catch (_) {
      state = const AuthFailed('login_failed');
    }
  }

  /// 注销（清空令牌）。
  Future<void> logout() async {
    await store.setAccessToken('');
    await store.setRefreshToken('');
    state = const AuthUnknown();
  }
}

/// [AuthStore] 的 Riverpod Provider（实现 [AuthTokenStore] 与 TokenProvider）。
final authStoreProvider = Provider<AuthStore>((ref) => AuthStore());

/// [AuthRepository] 的 Riverpod Provider。
final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepository(store: ref.read(authStoreProvider)),
);