// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'submit_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

SubmitRequest _$SubmitRequestFromJson(Map<String, dynamic> json) =>
    SubmitRequest(
      clientRequestId: json['clientRequestId'] as String,
      fileIds: (json['fileIds'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
      text: json['text'] as String? ?? '',
      replyToId: (json['replyToId'] as num?)?.toInt() ?? 0,
    );

Map<String, dynamic> _$SubmitRequestToJson(SubmitRequest instance) =>
    <String, dynamic>{
      'clientRequestId': instance.clientRequestId,
      'text': instance.text,
      'fileIds': instance.fileIds,
      'replyToId': instance.replyToId,
    };
