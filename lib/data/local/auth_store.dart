import 'package:aerie_mobile/data/remote/token_provider.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 认证仓库所依赖的令牌持久化抽象（§3.2.2，供 T1.4 注入与单测替换）。
abstract interface class AuthTokenStore {
  /// 写入访问令牌。
  Future<void> setAccessToken(String token);

  /// 写入刷新令牌。
  Future<void> setRefreshToken(String token);
}

/// 令牌存储（§3.2.2）。
///
/// 同时实现网络层 [TokenProvider] 与认证仓库 [AuthTokenStore]，令牌读写
/// 走 flutter_secure_storage（Android `allowBackup=false` 已排除云备份）；
/// 刷新通过注入的 onRefresh 回调委托给上层（auth_repository，T1.4）。
class AuthStore implements TokenProvider, AuthTokenStore {
  /// 创建 [AuthStore]。
  ///
  /// [onRefresh] 为空时刷新返回 null（401 重试直接放行失败）。
  AuthStore({
    FlutterSecureStorage? storage,
    Future<String?> Function()? onRefresh,
  }) : _storage = storage ?? const FlutterSecureStorage(),
       _onRefresh = onRefresh ?? _defaultRefresh;

  static const String _accessTokenKey = 'access_token';
  static const String _refreshTokenKey = 'refresh_token';

  final FlutterSecureStorage _storage;
  final Future<String?> Function() _onRefresh;

  @override
  Future<String?> getAccessToken() => _storage.read(key: _accessTokenKey);

  @override
  Future<void> setAccessToken(String token) =>
      _storage.write(key: _accessTokenKey, value: token);

  /// 持久化刷新令牌。
  @override
  Future<void> setRefreshToken(String token) =>
      _storage.write(key: _refreshTokenKey, value: token);

  /// 读取刷新令牌。
  Future<String?> getRefreshToken() => _storage.read(key: _refreshTokenKey);

  @override
  Future<String?> refreshAccessToken() => _onRefresh();

  static Future<String?> _defaultRefresh() async => null;
}
