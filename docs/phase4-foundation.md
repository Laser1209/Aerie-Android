# Phase 4 Android Foundation

Status: implementing

Date: 2026-07-22

## Scope

This batch establishes the buildable Android application foundation without
claiming that server authentication or persistent chat is available.

Implemented:

- Gradle Wrapper 8.11.1 with an official SHA-256 pinned distribution.
- Android Gradle Plugin 8.9.2 and Kotlin 2.1.20.
- Single `app` module with `top.etta.aerie` application ID.
- compile/target SDK 35 and min SDK 28.
- Jetpack Compose Material 3 application shell.
- Manual `AppContainer`, MVVM session state, and repository boundary.
- Login, password, pairing code, device name, and server URL inputs.
- Owner and guest debug previews that are disabled in release builds.
- Chat, task, file, and settings navigation surfaces.

Not yet claimed:

- Real account authentication or token persistence.
- Persistent chat, SSE, files, approvals, or background execution.
- APK installation or business-flow verification on the target phone.

## Evidence

- `gradlew.bat --version`: Gradle 8.11.1 on JDK 17.
- `gradlew.bat :app:assembleDebug --no-daemon`: passed.
- `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-daemon`: passed;
  5 tests, 0 failures, 0 errors, 0 skipped.
- `gradlew.bat :app:clean :app:testDebugUnitTest :app:assembleDebug
  :app:lintDebug --no-daemon`: passed; Android Lint reported `No issues found`.
- APK metadata: package `top.etta.aerie`, version `0.1.0-dev`, min SDK 28,
  target SDK 35, compile SDK 35.
- APK size: 65,393,180 bytes.
- APK SHA-256:
  `C10ECCDFFB0A6CF0E1B9A9666826DA22D8B41A937E22F56B2610C97AE795DF35`.
- ADB detected the only connected target as vivo `V2516A`, Android 16/API 36.
- `adb install -r` passed, then `top.etta.aerie/.MainActivity` completed a cold
  start and the application process remained running.
- Local gateway and real business-flow verification remain gated by server
  Phase 2/3.
