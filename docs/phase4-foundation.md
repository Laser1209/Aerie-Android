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
- Retrofit/kotlinx.serialization authentication DTO and API contracts.
- Stable server-error mapping and an authorized 401 refresh/retry executor.
- Mutex-protected Refresh Token rotation for concurrent requests.
- Access Token memory-only storage and AES-GCM Android Keystore protection for
  the Refresh Token ciphertext stored in DataStore.
- Release HTTPS enforcement; Debug cleartext is limited by client validation to
  localhost, `127.0.0.1`, and emulator `10.0.2.2` test endpoints.

Not yet claimed:

- Successful production account authentication; the owner account is not yet
  provisioned on the server.
- Persistent chat, SSE, files, approvals, or background execution.
- Keystore encryption round-trip with a real issued Refresh Token.
- Authenticated business-flow verification on the target phone.

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

## Authentication Client Evidence

- `gradlew.bat :app:clean :app:testDebugUnitTest :app:assembleDebug
  :app:lintDebug --no-daemon`: passed.
- 11 JVM tests passed with 0 failures/errors/skips. MockWebServer covers login,
  stable errors, one-time mutex refresh under 12 concurrent callers, 401 refresh
  and retry, and local logout while the backend is unreachable.
- Android Lint: `No issues found`.
- Installed authentication-batch APK size: 65,610,280 bytes.
- Installed authentication-batch APK SHA-256:
  `677818D2F07B78C4937D707330D06D40E1EE6471903712FFDD10BC7F9E7E81C1`.
- `adb install -r` passed on vivo `V2516A`, Android 16/API 36. The updated APK
  cold-started in 1.905 seconds, remained running, and produced no Aerie fatal
  startup logs.
- Startup verifies that the empty-session DataStore/Keystore restoration path
  does not crash. Token encryption with a real issued token remains part of the
  production login gate.

## Release Build Evidence

- The first Release attempt exhausted native JVM memory during R8 with a 3 GB
  heap and unrestricted worker concurrency; this was a build-host resource
  failure, not a Kotlin, Android, or R8 compilation error.
- The reproducible Gradle baseline now uses a 1.5 GB heap, a 512 MB Metaspace
  cap, two workers, and disabled parallel project execution for this 16 GB
  Windows build host.
- `gradlew.bat :app:assembleRelease :app:lintRelease --no-daemon`: passed in
  2 minutes 40 seconds; Release Lint reported `No issues found`.
- A subsequent clean Debug and Release verification completed all 108 requested
  tasks in 52 seconds with both Lint variants reporting `No issues found` and
  all 11 JVM tests passing.
- The merged Release manifest contains `android:usesCleartextTraffic="false"`.
- Latest clean Debug APK size: 65,491,626 bytes; SHA-256:
  `2A9F60F8990648CE12A1CACC4226DDBCE92FAD4EAFB0974F18426CEE17E80FB3`.
- Unsigned Release APK size: 4,486,789 bytes.
- Unsigned Release APK SHA-256:
  `D24B613C6B6576FDC1FF8785EA39B524A4FEFF1A0471A7DF601B20F2F1440542`.
- This artifact is not a distributable release. Signing-key creation and signed
  APK installation remain deferred until the release phase.
