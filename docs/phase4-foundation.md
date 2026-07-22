# Phase 4 Android Foundation

Status: verified

Date: 2026-07-23

## Scope

This document began with the buildable Android application foundation and now
records verified production authentication plus the final Phase 4 real-device
chat, recovery, and background-execution gate.

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

Deferred beyond Phase 4:

- File transfer, mobile approvals, Cloudflare Tunnel, signed distribution, and
  QQ-disabled end-to-end release acceptance.

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
- Local authentication is now verified; chat and request business-flow
  verification remain gated by the unfinished Phase 3/4 client integration.

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
- Startup first verified that the empty-session DataStore/Keystore restoration
  path does not crash. The real issued-token round trip is recorded below.

## Real Device Authentication Evidence

- ADB detected one target, vivo `V2516A` on Android 16/API 36, and established
  `tcp:7891 -> tcp:7891` reverse. The Debug client used only
  `http://127.0.0.1:7891`; port `7890` was not exposed to the phone.
- Production owner login succeeded with a password entered through the phone's
  secure keyboard and a locally displayed one-time pairing code. No credential
  or token value was copied into documentation, logs, or Git.
- The server recorded one active device, one active Refresh Token, a consumed
  pairing session, and `auth.login success` after the first login.
- After `adb shell am force-stop top.etta.aerie`, a cold start completed in
  1.236 seconds and restored the owner screen without another login. The server
  then held two rows in one Refresh Token family: the old row was revoked and
  linked to its replacement, while exactly one new row remained active. This
  verifies Keystore encryption, DataStore persistence, Keystore decryption,
  and server rotation with a real issued Refresh Token.
- OriginOS intentionally blocked screenshots while its secure keyboard was
  active. After it closed, the owner identity and navigation were confirmed by
  screenshot and UI hierarchy. A scan of 65 post-restart App log lines found
  zero password, pairing-code, token, Authorization, or Bearer key names.
- Server mobile identity/API/gateway regression: 36 tests passed, including
  pairing-code single-use behavior. Android
  `:app:testDebugUnitTest --rerun-tasks` executed all 11 JVM tests with zero
  failures, errors, or skips.
- At that authentication checkpoint, the evidence covered session recovery
  only; persistent history, request status, and SSE were closed by the later
  Phase 4 evidence below.

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

## Ordering and Integrated-Test Addendum

The persistent chat implementation has since advanced to Room v2. The server
now supplies `messageOrder` in both history items and `message.created` events;
the client uses it as the primary display order for same-timestamp replies.
The v1 to v2 migration preserves cached messages and clears synchronization
cursors so the next authenticated sync repopulates authoritative order values.

The final Debug APK is 66,122,092 bytes with SHA-256
`E8CC4C7A591CB764E47028856DEA5161868714158AE2AA143664DB7168E4C08E`.
It was installed over the retained application on vivo `V2516A`; private data
and the Keystore-backed owner session survived, cold start restored the owner
screen, and the process logged no fatal exception or credential/token keyword.

## Phase 4 Closeout

- Android JVM tests: 38 passed, 0 failures/errors/skips.
- Device instrumentation: 9 passed, 0 failures/errors/skips.
- Debug and Release Lint: successful with no reported issue.
- Room v3 and the server contain the same 1188-message ordered ID sequence,
  including the original seven-part reply and a new 22-message turn.
- After removal of the standard device-idle whitelist, the vivo OEM
  background-battery permission alone allowed a real request to finish in the
  background; the status-only foreground monitor then removed its service and
  notification without a `fast_freezer` event.
- One `aerie_periodic_status_sync` job remains `ENQUEUED` at a 15-minute
  interval. Phase 4 is verified; later-phase capabilities are not claimed.
