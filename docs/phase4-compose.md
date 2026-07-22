# Phase 4 Compose 与 ViewModel 接线

Status: verified

Date: 2026-07-23

## Scope

This batch binds the authenticated chat repository to the Compose shell. The
screen is driven by Room and connection state rather than placeholder text.
File transfer and approvals remain later phases; foreground notification and
real-device chat acceptance are now closed below.

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

- 27 JVM tests, 0 failures, 0 errors, 0 skips.
- Debug APK: 65,937,058 bytes.
- Debug APK SHA-256:
  `49ECB0238C92F996F7B5DF026FB878AB087D49654EF77DB96E029352BFD84C7C`.
- Android Lint: `No issues found`.

The target vivo phone executed the Compose and Room instrumented suite: 5 tests
passed with 0 failures, errors, or skips. The current app opens at login because
the secure-session preference file is empty; no credential or token was entered
by automation.

## Real-Device Closeout

- The retained owner session opened directly on the main Compose shell after
  the final APK install; the login form was absent and owner navigation was
  present.
- A real request was submitted from the Compose input, the app was returned to
  the launcher, and the server plus Room converged to the same 1188-message
  timeline without UI intervention.
- The foreground notification appeared while work was active, contained only
  product/status text, and had no active `NotificationRecord` after terminal
  state.
- The final cold-start UI test and all other instrumented tests passed 9/9.
