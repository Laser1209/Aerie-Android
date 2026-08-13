import 'package:aerie_mobile/app.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// 应用入口。
void main() {
  runApp(const ProviderScope(child: AerieApp()));
}
