// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, unused_import, invalid_annotation_target, unnecessary_import

import 'package:json_annotation/json_annotation.dart';

part 'approval_decision_request.g.dart';

@JsonSerializable()
class ApprovalDecisionRequest {
  const ApprovalDecisionRequest({
    required this.approved,
    this.whitelist = false,
    this.blacklist = false,
  });

  factory ApprovalDecisionRequest.fromJson(Map<String, Object?> json) =>
      _$ApprovalDecisionRequestFromJson(json);

  final bool approved;
  final bool whitelist;
  final bool blacklist;

  Map<String, Object?> toJson() => _$ApprovalDecisionRequestToJson(this);
}
