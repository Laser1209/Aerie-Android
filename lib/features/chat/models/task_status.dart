/// 消息任务状态（§3.2.4 任务状态徽章的取值）。
///
/// 用于气泡上叠加「发送中 / 已取消 / 重试 / 失败 / 完成」等状态。
enum TaskStatus {
  /// 发送中。
  sending,

  /// 下载中。
  downloading,

  /// 已取消。
  canceled,

  /// 失败（可重试）。
  failed,

  /// 已完成（隐藏徽章）。
  completed,
}
