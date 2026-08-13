// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, unused_import, invalid_annotation_target, unnecessary_import

import 'package:json_annotation/json_annotation.dart';

part 'login_request.g.dart';

@JsonSerializable()
class LoginRequest {
  const LoginRequest({
    required this.username,
    required this.password,
    required this.deviceName,
    required this.pairingCode,
    this.publicKey,
  });

  factory LoginRequest.fromJson(Map<String, Object?> json) =>
      _$LoginRequestFromJson(json);

  final String username;
  final String password;
  final String deviceName;
  final String pairingCode;
  final String? publicKey;

  Map<String, Object?> toJson() => _$LoginRequestToJson(this);
}
