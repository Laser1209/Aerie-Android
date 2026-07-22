# Aerie Android

Native Android companion client for Aerie.

## Status

The Phase 4 Android foundation is under active development. The project now
contains a reproducible Gradle 8.11.1 wrapper, a Kotlin and Compose app module,
manual dependency injection, MVVM session state, login and pairing UI, and a
debug-only local preview shell. Real authentication and chat integration remain
gated by the server-side Phase 2 and Phase 3 contracts.

## Build

Prerequisites:

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools 35.0.0

From this repository:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

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
