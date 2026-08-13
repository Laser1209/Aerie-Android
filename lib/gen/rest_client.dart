// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, unused_import, invalid_annotation_target, unnecessary_import

import 'package:dio/dio.dart' hide Headers;

import 'api/api_client.dart';

/// Aerie Mobile Gateway `vv1`
class RestClient {
  RestClient(
    Dio dio, {
    String? baseUrl,
  }) : _dio = dio,
       _baseUrl = baseUrl;

  final Dio _dio;
  final String? _baseUrl;

  static String get version => 'v1';

  ApiClient? _api;

  ApiClient get api => _api ??= ApiClient(_dio, baseUrl: _baseUrl);
}
