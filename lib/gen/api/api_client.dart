// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint, unused_import, invalid_annotation_target, unnecessary_import

import 'package:dio/dio.dart' hide Headers;
import 'package:retrofit/retrofit.dart';
import 'package:retrofit/error_logger.dart';

import '../models/approval_decision_request.dart';
import '../models/create_upload_request.dart';
import '../models/login_request.dart';
import '../models/refresh_request.dart';
import '../models/submit_request.dart';

part 'api_client.g.dart';

@RestApi()
abstract class ApiClient {
  factory ApiClient(Dio dio, {String? baseUrl}) = _ApiClient;

  /// Login.
  ///
  /// [body] - Name not received - field will be skipped.
  @POST('/api/mobile/v1/auth/login')
  Future<dynamic> loginApiMobileV1AuthLoginPost({
    @Body() required LoginRequest body,
  });

  /// Refresh.
  ///
  /// [body] - Name not received - field will be skipped.
  @POST('/api/mobile/v1/auth/refresh')
  Future<dynamic> refreshApiMobileV1AuthRefreshPost({
    @Body() required RefreshRequest body,
  });

  /// Logout
  @POST('/api/mobile/v1/auth/logout')
  Future<void> logoutApiMobileV1AuthLogoutPost({
    @Header('authorization') String? authorization,
  });

  /// Me
  @GET('/api/mobile/v1/me')
  Future<dynamic> meApiMobileV1MeGet({
    @Header('authorization') String? authorization,
  });

  /// Approvals
  @GET('/api/mobile/v1/approvals')
  Future<dynamic> approvalsApiMobileV1ApprovalsGet({
    @Header('authorization') String? authorization,
  });

  /// Approval Detail
  @GET('/api/mobile/v1/approvals/{approval_id}')
  Future<dynamic> approvalDetailApiMobileV1ApprovalsApprovalIdGet({
    @Path('approval_id') required String approvalId,
    @Header('authorization') String? authorization,
  });

  /// Approval Decision.
  ///
  /// [body] - Name not received - field will be skipped.
  @POST('/api/mobile/v1/approvals/{approval_id}/decision')
  Future<dynamic> approvalDecisionApiMobileV1ApprovalsApprovalIdDecisionPost({
    @Path('approval_id') required String approvalId,
    @Body() required ApprovalDecisionRequest body,
    @Header('authorization') String? authorization,
  });

  /// Owner Guests
  @GET('/api/mobile/v1/owner/guests')
  Future<dynamic> ownerGuestsApiMobileV1OwnerGuestsGet({
    @Header('authorization') String? authorization,
  });

  /// Owner Guest Messages
  @GET('/api/mobile/v1/owner/guests/{account_id}/messages')
  Future<dynamic> ownerGuestMessagesApiMobileV1OwnerGuestsAccountIdMessagesGet({
    @Path('account_id') required String accountId,
    @Header('authorization') String? authorization,
    @Query('limit') int? limit = 50,
  });

  /// Owner Audit
  @GET('/api/mobile/v1/owner/audit')
  Future<dynamic> ownerAuditApiMobileV1OwnerAuditGet({
    @Query('accountId') String? accountId,
    @Header('authorization') String? authorization,
    @Query('limit') int? limit = 50,
    @Query('offset') int? offset = 0,
  });

  /// Readonly Brief
  @GET('/api/mobile/v1/readonly/brief')
  Future<dynamic> readonlyBriefApiMobileV1ReadonlyBriefGet({
    @Header('authorization') String? authorization,
  });

  /// Readonly World
  @GET('/api/mobile/v1/readonly/world')
  Future<dynamic> readonlyWorldApiMobileV1ReadonlyWorldGet({
    @Header('authorization') String? authorization,
  });

  /// Readonly Memory
  @GET('/api/mobile/v1/readonly/memory')
  Future<dynamic> readonlyMemoryApiMobileV1ReadonlyMemoryGet({
    @Header('authorization') String? authorization,
  });

  /// Readonly Weather
  @GET('/api/mobile/v1/readonly/weather')
  Future<dynamic> readonlyWeatherApiMobileV1ReadonlyWeatherGet({
    @Header('authorization') String? authorization,
  });

  /// Devices
  @GET('/api/mobile/v1/devices')
  Future<dynamic> devicesApiMobileV1DevicesGet({
    @Header('authorization') String? authorization,
  });

  /// Delete Device
  @DELETE('/api/mobile/v1/devices/{device_id}')
  Future<void> deleteDeviceApiMobileV1DevicesDeviceIdDelete({
    @Path('device_id') required String deviceId,
    @Header('authorization') String? authorization,
  });

  /// Create Upload.
  ///
  /// [body] - Name not received - field will be skipped.
  @POST('/api/mobile/v1/files/uploads')
  Future<dynamic> createUploadApiMobileV1FilesUploadsPost({
    @Body() required CreateUploadRequest body,
    @Header('authorization') String? authorization,
  });

  /// Get Upload
  @GET('/api/mobile/v1/files/uploads/{upload_id}')
  Future<dynamic> getUploadApiMobileV1FilesUploadsUploadIdGet({
    @Path('upload_id') required String uploadId,
    @Header('authorization') String? authorization,
  });

  /// Cancel Upload
  @DELETE('/api/mobile/v1/files/uploads/{upload_id}')
  Future<void> cancelUploadApiMobileV1FilesUploadsUploadIdDelete({
    @Path('upload_id') required String uploadId,
    @Header('authorization') String? authorization,
  });

  /// Put Upload Part
  @PUT('/api/mobile/v1/files/uploads/{upload_id}/parts/{part_number}')
  Future<void> putUploadPartApiMobileV1FilesUploadsUploadIdPartsPartNumberPut({
    @Path('upload_id') required String uploadId,
    @Path('part_number') required int partNumber,
    @Header('X-Part-SHA256') String? xPartSha256,
    @Header('authorization') String? authorization,
  });

  /// Complete Upload
  @POST('/api/mobile/v1/files/uploads/{upload_id}/complete')
  Future<dynamic> completeUploadApiMobileV1FilesUploadsUploadIdCompletePost({
    @Path('upload_id') required String uploadId,
    @Header('authorization') String? authorization,
  });

  /// List Files
  @GET('/api/mobile/v1/files')
  Future<dynamic> listFilesApiMobileV1FilesGet({
    @Query('limit') int? limit = 50,
    @Query('beforeId') String? beforeId,
    @Header('authorization') String? authorization,
  });

  /// Get File
  @GET('/api/mobile/v1/files/{file_id}')
  Future<dynamic> getFileApiMobileV1FilesFileIdGet({
    @Path('file_id') required String fileId,
    @Header('authorization') String? authorization,
  });

  /// Download File
  @GET('/api/mobile/v1/files/{file_id}/content')
  Future<void> downloadFileApiMobileV1FilesFileIdContentGet({
    @Path('file_id') required String fileId,
    @Header('Range') String? range,
    @Header('authorization') String? authorization,
  });

  /// Messages
  @GET('/api/mobile/v1/messages')
  Future<dynamic> messagesApiMobileV1MessagesGet({
    @Query('limit') int? limit = 50,
    @Query('beforeId') String? beforeId,
    @Query('afterId') String? afterId,
    @Header('authorization') String? authorization,
  });

  /// Submit Request.
  ///
  /// [body] - Name not received - field will be skipped.
  @POST('/api/mobile/v1/requests')
  Future<dynamic> submitRequestApiMobileV1RequestsPost({
    @Body() required SubmitRequest body,
    @Header('authorization') String? authorization,
  });

  /// Get Request
  @GET('/api/mobile/v1/requests/{request_id}')
  Future<dynamic> getRequestApiMobileV1RequestsRequestIdGet({
    @Path('request_id') required String requestId,
    @Header('authorization') String? authorization,
  });

  /// Cancel Request
  @POST('/api/mobile/v1/requests/{request_id}/cancel')
  Future<dynamic> cancelRequestApiMobileV1RequestsRequestIdCancelPost({
    @Path('request_id') required String requestId,
    @Header('authorization') String? authorization,
  });

  /// Retry Request
  @POST('/api/mobile/v1/requests/{request_id}/retry')
  Future<dynamic> retryRequestApiMobileV1RequestsRequestIdRetryPost({
    @Path('request_id') required String requestId,
    @Header('authorization') String? authorization,
  });

  /// Events
  @GET('/api/mobile/v1/events')
  Future<void> eventsApiMobileV1EventsGet({
    @Header('Last-Event-ID') String? lastEventId,
    @Header('authorization') String? authorization,
  });
}
