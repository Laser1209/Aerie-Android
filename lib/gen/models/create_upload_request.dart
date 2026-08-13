// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, unused_import, invalid_annotation_target, unnecessary_import

import 'package:json_annotation/json_annotation.dart';

part 'create_upload_request.g.dart';

@JsonSerializable()
class CreateUploadRequest {
  const CreateUploadRequest({
    required this.clientUploadId,
    required this.fileName,
    required this.size,
    required this.sha256,
    required this.mimeType,
    this.directoryGrantId,
  });

  factory CreateUploadRequest.fromJson(Map<String, Object?> json) =>
      _$CreateUploadRequestFromJson(json);

  final String clientUploadId;
  final String fileName;
  final int size;
  final String sha256;
  final String mimeType;
  final String? directoryGrantId;

  Map<String, Object?> toJson() => _$CreateUploadRequestToJson(this);
}
