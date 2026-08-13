import 'package:aerie_mobile/data/repository/file_repository.dart';
import 'package:aerie_mobile/features/chat/models/task_status.dart';
import 'package:aerie_mobile/features/files/transfer_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 文件仓库注入点（须在应用根 override，接入真实 `Dio`）。
final fileRepositoryProvider =
    Provider<FileRepository>((ref) => throw StateError(
          'fileRepositoryProvider 未在应用装配点注入',
        ));

/// 文件屏（§3.2.5 / T2.3）。
///
/// 上传入口（file_picker）与传输进度卡；真实文件选取/worker 在 P-S4 接入，
/// 本节点提供骨架与传输卡渲染入口。
class FileScreen extends ConsumerWidget {
  /// 创建 [FileScreen]。
  const FileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('文件')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          FilledButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.upload_file),
            label: const Text('选择文件上传'),
            style: FilledButton.styleFrom(
              backgroundColor: const Color(0xFFF5A3B7),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(999),
              ),
            ),
          ),
          const SizedBox(height: 16),
          const TransferCard(
            fileName: '下载示例.txt',
            progress: 0,
            status: TaskStatus.completed,
          ),
        ],
      ),
    );
  }
}
