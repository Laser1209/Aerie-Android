/// 令牌提供者抽象（§3.2.1 鉴权拦截器依赖）。
///
/// 具体实现见 T1.3 的 `auth_store.dart`（flutter_secure_storage）。网络层
/// 仅依赖此抽象，便于单测注入 mock，实现 T1.2 / T1.3 并联开发。
abstract interface class TokenProvider {
  /// 读取当前访问令牌；无令牌时返回 null。
  Future<String?> getAccessToken();

  /// 持久化新的访问令牌。
  Future<void> setAccessToken(String token);

  /// 刷新访问令牌；刷新失败应抛出异常（由调用方映射为 ApiError）。
  Future<String?> refreshAccessToken();
}
