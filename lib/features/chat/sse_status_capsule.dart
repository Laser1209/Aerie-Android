import 'package:aerie_mobile/features/chat/sse_controller.dart';
import 'package:flutter/material.dart';

/// AppBar 实时连接状态胶囊（§3.2.6 / T2.2）。
///
/// 纯展示，按 [SseHealth] 渲染不同颜色与文案；仅在可测试、无网络依赖下
/// 渲染实时连接态。
class SseStatusCapsule extends StatelessWidget {
  /// 创建 [SseStatusCapsule]。
  const SseStatusCapsule({required this.health, super.key});

  /// 当前连接健康态。
  final SseHealth health;

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (health) {
      SseHealth.connected => ('实时在线', const Color(0xFF7BC59B)),
      SseHealth.reconnecting => ('重连中', const Color(0xFFE8B76F)),
      SseHealth.disconnected => ('未连接', const Color(0xFF8A7B82)),
    };
    return Container(
      margin: const EdgeInsets.only(right: 12),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 8,
            height: 8,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 6),
          Text(
            label,
            style: TextStyle(fontSize: 12, color: color),
          ),
        ],
      ),
    );
  }
}
