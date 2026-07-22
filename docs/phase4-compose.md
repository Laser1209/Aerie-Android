# Phase 4 Compose 与 ViewModel 接线

Status: verified

Date: 2026-07-22

## Scope

This batch binds the authenticated chat repository to the Compose shell. The
screen is now driven by Room and connection state rather than placeholder text.
Foreground services, notifications, file transfer, approvals, and real-device
business acceptance remain separate gates.

## Implemented

- `AerieViewModel` observes the active non-preview account and switches every
  chat Flow by `accountId` when the session changes.
- The ViewModel starts and cancels the repository SSE lifecycle with the
  authenticated session; local preview never starts a network stream.
- Compose renders persisted messages, user/assistant alignment, connection
  state, pending offline confirmations, and safe error text.
- The send action delegates to the repository and preserves the repository's
  uncertain-delivery confirmation behavior.
- The task tab renders queued/running/cancel-requested/completed/failed and
  cancelled request states with cancel and retry actions.
- Settings exposes an explicit manual synchronization action for a signed-in
  remote session.
- Account-switch and send-action ViewModel tests verify that owner and guest
  flows do not share cached messages or request actions.

## Automated Evidence

The integrated command was:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --rerun-tasks --no-daemon
```

Result:

- 22 JVM tests, 0 failures, 0 errors, 0 skips.
- Debug APK: 65,786,538 bytes.
- Debug APK SHA-256:
  `C291939F9AAD7B59AD8CCB45B2D3A21D7049E4A9C0B6D0E78915FDD020A95B1A`.
- Android Lint: `No issues found`.

The target vivo phone was disconnected during this batch. No APK install,
credential entry, pairing-code generation, production database change, or
server configuration change was performed.

## Remaining Gate

- Add the foreground `dataSync` service and notification permission flow.
- Run Compose instrumented coverage on a connected device or emulator.
- Install this integrated APK with retention enabled and verify production
  history, one request, desktop/mobile sharing, and SSE recovery.
