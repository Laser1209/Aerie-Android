import 'package:aerie_mobile/data/repository/auth_repository.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 登录屏（§3.2.4 / T1.4）。
///
/// 提供账号密码 + 配对码输入与登录态渲染。真实登录在 P-S2 接通后端；
/// 本节点聚焦登录流程状态机的 UI 骨架与可测试性。
class AuthScreen extends ConsumerStatefulWidget {
  /// 创建 [AuthScreen]。
  const AuthScreen({super.key});

  @override
  ConsumerState<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends ConsumerState<AuthScreen> {
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final repo = ref.read(authRepositoryProvider);
    await repo.login(
      _usernameController.text.trim(),
      _passwordController.text,
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(
      authRepositoryProvider.select((repo) => repo.state),
    );

    final errorText = switch (state) {
      AuthFailed(:final message) => message,
      _ => null,
    };

    return Scaffold(
      appBar: AppBar(title: const Text('Aerie 登录')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            TextField(
              controller: _usernameController,
              decoration: const InputDecoration(
                labelText: '用户名',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _passwordController,
              obscureText: true,
              decoration: const InputDecoration(
                labelText: '密码',
                border: OutlineInputBorder(),
              ),
            ),
            if (errorText != null) ...[
              const SizedBox(height: 16),
              Text(errorText, style: const TextStyle(color: Colors.red)),
            ],
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: state is AuthLoading ? null : _submit,
                child: state is AuthLoading
                    ? const CircularProgressIndicator()
                    : const Text('登录'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
