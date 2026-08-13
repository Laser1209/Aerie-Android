// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, unused_import, invalid_annotation_target, unnecessary_import

import 'package:json_annotation/json_annotation.dart';

part 'submit_request.g.dart';

@JsonSerializable()
class SubmitRequest {
  const SubmitRequest({
    required this.clientRequestId,
    this.fileIds,
    this.text = '',
    this.replyToId = 0,
  });

  factory SubmitRequest.fromJson(Map<String, Object?> json) =>
      _$SubmitRequestFromJson(json);

  final String clientRequestId;
  final String text;
  final List<String>? fileIds;
  final int replyToId;

  Map<String, Object?> toJson() => _$SubmitRequestToJson(this);
}
