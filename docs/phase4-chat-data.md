# Phase 4 Persistent Chat Data Layer

Status: implementing

Data-layer subsection: verified

Date: 2026-07-22

## Scope

This batch implements the Android persistence and request contracts needed
before the chat and task UI can use the production mobile gateway. It does not
claim that Compose or SSE integration is complete.

## Implemented

- `aerie_mobile_chat.db` Room v1 with account-scoped message, request, pending
  outbound, and synchronization cursor tables.
- Composite account/entity keys and account-filtered DAO queries so cached owner
  and guest records cannot appear in another account's flows.
- A `sending` outbound record becomes `awaiting_confirmation` after interrupted
  process recovery. No background path automatically resubmits it.
- Retrofit DTOs and methods for paged messages, request submission, request
  status, cancellation, and retry.
- Shared use of the authenticated session's server URL, in-memory Access Token,
  and existing one-time 401 refresh executor.
- Initial message synchronization from the newest page toward older history,
  incremental synchronization after the latest cursor, and invalid-cursor
  recovery from a fresh server snapshot.
- Server convergence for locally known queued, running, and cancel-requested
  requests.
- A locally generated `clientRequestId` is persisted before submission. An
  uncertain delivery result requires user confirmation and reuses the same ID,
  preserving server idempotency.
- Request cancellation and retry responses are persisted into the same
  account-scoped request timeline.

## Automated Evidence

The fully rerun local command was:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --rerun-tasks --no-daemon
```

Result:

- 55 tasks executed successfully in 3 minutes 39 seconds.
- 15 JVM tests, 0 failures, 0 errors, 0 skips.
- Four new MockWebServer/repository tests cover full history pagination,
  Authorization, active-request convergence, uncertain delivery, fixed
  `clientRequestId`, cancel/retry, and cross-account rejection.
- Android Lint: `No issues found`.
- Debug APK: 65,639,082 bytes.
- Debug APK SHA-256:
  `065CFF55D19C331FCA9E2D7F206DC09FD5A9749FF6B1F71AAD2C07E52C51D679`.

## Device Room Evidence

The vivo `V2516A` Android 16 device executed two in-memory Room instrumented
tests:

- `messagesRequestsAndCursorsRemainAccountScoped`
- `interruptedSendRequiresManualConfirmationAfterRestart`

Final JUnit evidence reports 2 tests, 0 failures, 0 errors, and 0 skips. The UTP
log also records both tests as `PASSED`.

The outer Gradle command timed out after 364 seconds during test-run cleanup.
Before that timeout, UTP had completed the tests and applied its default
`uninstall_after_test=true` behavior to both APKs. This removed the installed
App and its local session data. It did not alter the computer's Aerie database,
mobile authentication database, account, or server process. The next integrated
device run must use the APK-retention option and a newly generated pairing code.

## Remaining Gate

- Bind Room flows and repository actions to the authenticated ViewModel.
- Render actual messages, pending confirmations, and request state in Compose.
- Add filtered SSE with persisted `Last-Event-ID`, 1/2/4/8/30-second jittered
  reconnect, and database reconciliation after reconnect.
- Install the integrated APK and verify production history, one real request,
  desktop/mobile timeline sharing, process restart, and SSE on the target phone.
