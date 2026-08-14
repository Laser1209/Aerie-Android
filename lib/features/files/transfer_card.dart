import 'package:aerie_mobile/features/chat/models/task_status.dart';
import 'package:flutter/material.dart';

/// 文件传输进度卡（§3.2.5 / T2.3）。
///
/// 展示文件名、传输方向、进度条与状态微章。纯展示，便于 Widget 测试。
class TransferCard extends StatelessWidget {
  /// 创建 [TransferCard]。
  const TransferCard({
    required this.fileName,
    required this.progress,
    required this.status,
    super.key,
  });

  /// 文件名。
  final String fileName;

  /// 传输进度（0.0~1.0）。
  final double progress;

  /// 状态（uploading/downloading/failed/canceled/completed）。
  final TaskStatus status;

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (status) {
      TaskStatus.sending => ('发送中', const Color(0xFF7FA8D9)),
      TaskStatus.downloading => ('下载中', const Color(0xFF7FA8D9)),
      TaskStatus.failed => ('失败', const Color(0xFFE87C7C)),
      TaskStatus.canceled => ('已取消', const Color(0xFF8A7B82)),
      TaskStatus.completed => ('已完成', const Color(0xFF7BC59B)),
    };
    final clamped = progress.clamp(0, 1).toDouble();
    final percent = (clamped * 100).round();
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFF0E4E4)),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.insert_drive_file,
            size: 28,
            color: Color(0xFF7FA8D9),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        fileName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF3D2F35),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      label,
                      style: TextStyle(fontSize: 12, color: color),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                ClipRRect(
                  borderRadius: BorderRadius.circular(999),
                  child: LinearProgressIndicator(
                    value: clamped,
                    minHeight: 5,
                    color: const Color(0xFFF5A3B7),
                    backgroundColor: const Color(0xFFFCE4EC),
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  '$percent%',
                  style: const TextStyle(
                    fontSize: 11,
                    color: Color(0xFF8A7B82),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}