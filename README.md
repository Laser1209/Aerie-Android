# Aerie Android

Native Android companion client for Aerie.

## Status

The Phase 4 Android foundation is under active development. The project now
contains a reproducible Gradle 8.11.1 wrapper, a Kotlin and Compose app module,
manual dependency injection, MVVM session state, login and pairing UI, and a
debug-only local preview shell. The authentication client now implements the
frozen Phase 2 login, refresh, logout, stable-error, and secure token contracts.
Production owner login, one-time pairing, Keystore-backed session restoration,
and Refresh Token rotation have passed on the target vivo phone. The persistent
chat batches now include account-scoped Room v2 storage, paged history,
server-authoritative `messageOrder` sorting for same-timestamp replies,
idempotent request submission, manual offline confirmation, status/cancel/retry
contracts, filtered SSE reconnect with a persisted event cursor, and a
Compose/ViewModel chat and task surface. Foreground service and notification
scaffolding is present; the integrated real-device business flow remains the
next Phase 3/4 gate.

Implementation evidence:

- [`docs/phase4-foundation.md`](docs/phase4-foundation.md)
- [`docs/phase4-chat-data.md`](docs/phase4-chat-data.md)
- [`docs/phase4-sse.md`](docs/phase4-sse.md)
- [`docs/phase4-compose.md`](docs/phase4-compose.md)

## Build

Prerequisites:

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.0

From this repository:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Release compilation and lint verification can be run without creating signing
credentials:

```powershell
.\gradlew.bat :app:assembleRelease :app:lintRelease --no-daemon
```

The resulting `app-release-unsigned.apk` is verification evidence only. A
fixed private signing key will be created during the release phase and must
never be committed.

## Repository Boundary

- This repository contains only Android client code, Gradle configuration,
  Android resources, client tests, and client CI.
- The Python mobile gateway, desktop application, server configuration, and
  server-side tests remain in the Aerie server repository.
- The Android client will use the narrow mobile gateway API. It must never
  connect to the local management API on port `7890`.
- Public connectivity is planned through `https://aerie.etta.top`; Cloudflare
  Tunnel setup is a later server-side phase and is not configured here.

## Source Of Truth

Cross-repository architecture, security constraints, API contracts, ports,
and delivery gates are controlled by:

`E:\Agent_reply\documents\Android\Aerie_Android_Companion_Master_Plan.md`

Any client contract copy added to this repository must be synchronized with
that master plan before implementation changes are made.
