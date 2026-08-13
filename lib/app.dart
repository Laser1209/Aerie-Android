import 'package:flutter/material.dart';

/// Aerie 移动端根组件。
///
/// 承载全局主题与根路由。当前为接收端骨架（P-S1 T1.1），
/// 具体 feature 路由在 P-S2/P-S3 接入。
class AerieApp extends StatelessWidget {
  /// 创建 [AerieApp]。
  const AerieApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Aerie',
      theme: ThemeData(
        useMaterial3: true,
        // 伊塔风格软粉色主题（§3.2.4）。
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFFFC0CB),
        ),
      ),
      home: const _HomeScreen(),
    );
  }
}

class _HomeScreen extends StatelessWidget {
  const _HomeScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(child: Text('Aerie 接收端')),
    );
  }
}
