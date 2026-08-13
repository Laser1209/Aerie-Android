/// 网络请求结果（§3.2.1 错误映射：服务端稳定错误码 → Dart 类型）。
sealed class ApiResult<T> {
  /// 构造 [ApiResult]。
  const ApiResult();
}

/// 请求成功，携带数据。
class ApiSuccess<T> extends ApiResult<T> {
  /// 创建 [ApiSuccess]。
  const ApiSuccess(this.data);

  /// 响应数据。
  final T data;
}

/// 请求失败，携带结构化错误。
class ApiFailure<T> extends ApiResult<T> {
  /// 创建 [ApiFailure]。
  const ApiFailure(this.error);

  /// 结构化错误信息。
  final ApiError error;
}

/// 服务端稳定错误码映射。
class ApiError {
  /// 创建 [ApiError]。
  const ApiError({
    required this.code,
    required this.message,
    this.statusCode,
  });

  /// 服务端稳定错误码（如 `auth_required`、`chat_unavailable`）。
  final String code;

  /// 面向用户 / 调试的错误消息。
  final String message;

  /// HTTP 状态码（可选）。
  final int? statusCode;
}
