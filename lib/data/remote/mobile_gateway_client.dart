import 'package:aerie_mobile/data/remote/auth_interceptor.dart';
import 'package:aerie_mobile/data/remote/token_provider.dart';
import 'package:aerie_mobile/gen/rest_client.dart';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

/// 移动网关客户端（§3.2.1）。
///
/// 封装 OpenAPI 生成的 [RestClient]，负责 Dio 实例、鉴权拦截器与
/// baseUrl 策略（Debug 走 `http://127.0.0.1:7891`，Release 走
/// `https://aerie.etta.top`）。
class MobileGatewayClient {
  /// 创建 [MobileGatewayClient]。
  ///
  /// [baseUrl] 为空时按构建模式自动选择；真机 Debug 需传入电脑局域网 IP
  /// 或经 `adb reverse tcp:7891` 映射后使用默认值。
  factory MobileGatewayClient({
    required TokenProvider tokenProvider,
    String? baseUrl,
  }) {
    final resolvedBaseUrl = baseUrl ?? defaultBaseUrl();
    final dio = Dio(BaseOptions(baseUrl: resolvedBaseUrl));
    dio.interceptors.add(
      AuthInterceptor(tokenProvider: tokenProvider, dio: dio),
    );
    return MobileGatewayClient._(dio, RestClient(dio));
  }

  MobileGatewayClient._(this._dio, this._restClient);

  final Dio _dio;
  final RestClient _restClient;

  /// OpenAPI 生成的 REST 客户端。
  RestClient get restClient => _restClient;

  /// Dio 实例（供 SSE 流式 / 下载等直接使用）。
  Dio get dio => _dio;

  /// 按构建模式返回默认 baseUrl。
  static String defaultBaseUrl() {
    if (kDebugMode) {
      return 'http://127.0.0.1:7891';
    }
    return 'https://aerie.etta.top';
  }
}
