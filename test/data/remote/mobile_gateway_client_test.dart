import 'dart:typed_data';

import 'package:aerie_mobile/data/remote/api_result.dart';
import 'package:aerie_mobile/data/remote/auth_interceptor.dart';
import 'package:aerie_mobile/data/remote/mobile_gateway_client.dart';
import 'package:aerie_mobile/data/remote/token_provider.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

/// 可计数的 HTTP mock，第一次返回 401，之后返回 200。
class _FakeAdapter implements HttpClientAdapter {
  _FakeAdapter();

  int requestCount = 0;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requestCount++;
    final statusCode = requestCount == 1 ? 401 : 200;
    final body =
        statusCode == 200 ? '{"ok":true}' : '{"error":"auth_required"}';
    return ResponseBody.fromString(
      body,
      statusCode,
      headers: {
        Headers.contentTypeHeader: ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

class _FakeTokenProvider implements TokenProvider {
  String? _token = 'old-token';

  int refreshCount = 0;

  @override
  Future<String?> getAccessToken() async => _token;

  @override
  Future<void> setAccessToken(String token) async {
    _token = token;
  }

  @override
  Future<String?> refreshAccessToken() async {
    refreshCount++;
    return 'new-token';
  }
}

void main() {
  group('ApiResult', () {
    test('ApiSuccess 携带数据', () {
      const result = ApiSuccess<int>(42);

      expect(result.data, 42);
    });

    test('ApiFailure 携带结构化错误', () {
      const result = ApiFailure<void>(
        ApiError(code: 'auth_required', message: '需要登录', statusCode: 401),
      );

      expect(result.error.code, 'auth_required');
      expect(result.error.statusCode, 401);
    });
  });

  group('MobileGatewayClient', () {
    test('baseUrl 可注入覆盖', () {
      final client = MobileGatewayClient(
        tokenProvider: _FakeTokenProvider(),
        baseUrl: 'http://192.168.1.10:7891',
      );

      expect(client.dio.options.baseUrl, 'http://192.168.1.10:7891');
      expect(client.restClient, isNotNull);
    });
  });

  group('AuthInterceptor', () {
    test('401 触发单次刷新并重试', () async {
      final adapter = _FakeAdapter();
      final tokenProvider = _FakeTokenProvider();
      final dio = Dio(BaseOptions(baseUrl: 'http://test'));
      // 无法用 cascade：AuthInterceptor 构造需要引用已创建的 dio 实例
      // ignore: cascade_invocations
      dio.httpClientAdapter = adapter;
      dio.interceptors.add(
        AuthInterceptor(tokenProvider: tokenProvider, dio: dio),
      );

      final response = await dio.get<dynamic>('/api/mobile/v1/me');

      expect(response.statusCode, 200);
      expect(adapter.requestCount, 2);
      expect(tokenProvider.refreshCount, 1);
    });
  });
}
