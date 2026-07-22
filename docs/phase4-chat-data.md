# Phase 4 Persistent Chat Data Layer

Status: verified

Data-layer subsection: verified

Date: 2026-07-23

## Scope

This batch implements the Android persistence and request contracts used by the
chat and task UI and the production mobile gateway. The final production
business acceptance is recorded below.

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

At that earlier test/build checkpoint, the installed app had an empty
secure-session preference file and opened at the login screen. No password,
pairing code, access token, or refresh token was read or entered by the test
process. The later authenticated closeout is recorded below.

## Real-Device Closeout

- The final retained-data APK restored the existing owner session and upgraded
  the production cache to Room v3 without clearing messages.
- The server and Room each contain 1188 unique messages. Missing and extra IDs,
  `messageOrder` mismatches, and role mismatches are all zero; the complete
  ordered ID sequences are equal.
- The reported seven-part same-timestamp reply remains contiguous at
  `messageOrder=1850..1856`. A new background turn contains one user message
  and 21 assistant messages at `1864..1885`, all present on the phone.
- The new request reached `completed` in Room while the app remained on the
  launcher. Reopening the app was not required to repair the collection.
- v1-to-v3 and v2-to-v3 migration tests, ordering, account isolation, and
  interrupted-send recovery passed in the 9-test device suite.
