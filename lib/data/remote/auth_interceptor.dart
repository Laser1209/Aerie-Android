import 'package:aerie_mobile/data/remote/token_provider.dart';
import 'package:dio/dio.dart';

/// 鉴权拦截器（§3.2.1：401 单次刷新互斥重试）。
///
/// - 请求前注入 `authorization: Bearer <token>`。
/// - 收到 401 时，通过 [tokenProvider] 刷新令牌并重试原请求一次。
/// - 通过 [_refreshInFlight] 保证并发 401 只触发一次刷新。
class AuthInterceptor extends Interceptor {
  /// 创建 [AuthInterceptor]。
  AuthInterceptor({
    required this.tokenProvider,
    required this.dio,
  });

  /// 令牌提供者（抽象，具体实现见 auth_store）。
  final TokenProvider tokenProvider;

  /// 用于重试原请求的 Dio 实例。
  final Dio dio;

  Future<String?>? _refreshInFlight;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    final token = await tokenProvider.getAccessToken();
    if (token != null && token.isNotEmpty) {
      options.headers['authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final alreadyRetried = err.requestOptions.extra['_retried'] == true;
    if (err.response?.statusCode != 401 || alreadyRetried) {
      handler.next(err);
      return;
    }

    final request = err.requestOptions;
    final refreshFuture =
        _refreshInFlight ??= tokenProvider.refreshAccessToken();
    try {
      final newToken = await refreshFuture;
      if (newToken != null && newToken.isNotEmpty) {
        await tokenProvider.setAccessToken(newToken);
        request.headers['authorization'] = 'Bearer $newToken';
        request.extra['_retried'] = true;
        final response = await dio.fetch<dynamic>(request);
        handler.resolve(response);
        return;
      }
      handler.next(err);
    } on Exception {
      handler.next(err);
    } finally {
      _refreshInFlight = null;
    }
  }
}
