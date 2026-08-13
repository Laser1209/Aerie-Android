// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'approval_decision_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApprovalDecisionRequest _$ApprovalDecisionRequestFromJson(
  Map<String, dynamic> json,
) => ApprovalDecisionRequest(
  approved: json['approved'] as bool,
  whitelist: json['whitelist'] as bool? ?? false,
  blacklist: json['blacklist'] as bool? ?? false,
);

Map<String, dynamic> _$ApprovalDecisionRequestToJson(
  ApprovalDecisionRequest instance,
) => <String, dynamic>{
  'approved': instance.approved,
  'whitelist': instance.whitelist,
  'blacklist': instance.blacklist,
};
