// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'create_upload_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CreateUploadRequest _$CreateUploadRequestFromJson(Map<String, dynamic> json) =>
    CreateUploadRequest(
      clientUploadId: json['clientUploadId'] as String,
      fileName: json['fileName'] as String,
      size: (json['size'] as num).toInt(),
      sha256: json['sha256'] as String,
      mimeType: json['mimeType'] as String,
      directoryGrantId: json['directoryGrantId'] as String?,
    );

Map<String, dynamic> _$CreateUploadRequestToJson(
  CreateUploadRequest instance,
) => <String, dynamic>{
  'clientUploadId': instance.clientUploadId,
  'fileName': instance.fileName,
  'size': instance.size,
  'sha256': instance.sha256,
  'mimeType': instance.mimeType,
  'directoryGrantId': instance.directoryGrantId,
};
