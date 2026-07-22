# Phase 4 Persistent Chat Data Layer

Status: implementing

Data-layer subsection: verified

Date: 2026-07-22

## Scope

This batch implements the Android persistence and request contracts used by the
chat and task UI and the production mobile gateway. Real production business
acceptance remains a separate gate.

## Implemented

- `aerie_mobile_chat.db` Room v2 with account-scoped message, request, pending
  outbound, and synchronization cursor tables.
- `messageOrder` is carried from the mobile API through the domain model and
  Room entity; the DAO sorts by it before the display-only timestamp and ID
  fallbacks. Same-timestamp user messages and assistant segments therefore
  retain the server order.
- The v1 to v2 migration preserves cached messages, initializes a temporary
  local order from the old row order, replaces the message index, and clears
  synchronization cursors so the next authenticated sync refreshes all rows
  with server order values.
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

- 55 tasks executed successfully in 2 minutes 31 seconds.
- 27 JVM tests, 0 failures, 0 errors, 0 skips.
- Same-timestamp ordering, Room migration, and MockWebServer/repository tests cover full history pagination,
  Authorization, active-request convergence, uncertain delivery, fixed
  `clientRequestId`, cancel/retry, and cross-account rejection.
- Android Lint: `No issues found`.
- Debug APK: 65,937,058 bytes.
- Debug APK SHA-256:
  `49ECB0238C92F996F7B5DF026FB878AB087D49654EF77DB96E029352BFD84C7C`.

## Device Room Evidence

The vivo `V2516A` Android 16 device executed the instrumented test suite with
the APKs retained:

- `messagesRequestsAndCursorsRemainAccountScoped`
- `interruptedSendRequiresManualConfirmationAfterRestart`
- `messagesUseServerOrderWhenCreatedAtIsIdentical`
- `migrationToV2PreservesMessagesAndForcesResync`
- `appShellStartsAndDisplaysTheProductName`

Final JUnit evidence reports 5 tests, 0 failures, 0 errors, and 0 skips. The
manual `am instrument` output records all five tests as `PASSED`; the APKs were
not uninstalled.

The current installed app has an empty secure-session preference file after the
test/build cycle and therefore opens at the login screen. No password, pairing
code, access token, or refresh token was read or entered by the test process.

## Remaining Gate

- Re-authenticate the owner through the phone's secure keyboard.
- Install/retain the integrated APK and verify production history, including a
  same-timestamp multi-segment response displayed as user then all assistant
  segments.
- Verify one real request, desktop/mobile timeline sharing, process restart,
  SSE recovery, and a captured foreground notification for a long-running task.
