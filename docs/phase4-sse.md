# Phase 4 Android SSE 客户端

Status: verified

Date: 2026-07-23

## Scope

This batch adds the Android side of the frozen mobile event contract. Compose,
foreground-service, and real-device chat acceptance are now closed; file
transfer remains a later phase.

## Implemented

- OkHttp SSE connection with `Authorization: Bearer` and persisted
  `Last-Event-ID` request headers.
- Standard `id`, `event`, and `data` frame handling; heartbeat comments are
  handled by OkHttp and are not surfaced as application events.
- A strict client event allowlist for `stream.open`, `message.created`,
  `request.updated`, `approval.pending`, and `file.updated`; unknown event
  types do not advance the cursor.
- `message.created` and `request.updated` frames are merged into the existing
  account-scoped Room store. Approval and file frames are acknowledged by the
  cursor until their feature-specific stores are implemented.
- `message.created` carries the server `messageOrder`; the Room message query
  uses it as the primary display order so equal timestamps and random IDs
  cannot reorder a multi-segment assistant response.
- Event cursors only move forward by the numeric `evt_` sequence. Duplicate or
  out-of-order frames cannot move a stored cursor backwards.
- A reconnect loop that re-synchronizes messages and active requests after a
  disconnect, retries a single expired access token through the existing
  refresh mutex, and uses `1/2/4/8/30` second base delays with bounded jitter.
- Connection state is exposed as `Idle`, `Connecting`, `Connected`,
  `Reconnecting`, or `Offline` for the future ViewModel surface.

## Automated Evidence

The targeted command was:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "top.etta.aerie.data.remote.MobileEventStreamTest" --tests "top.etta.aerie.data.chat.SseReconnectBackoffTest" --tests "top.etta.aerie.data.chat.NetworkChatRepositoryTest" --rerun-tasks --no-daemon
```

Result: 9 targeted JVM tests passed with 0 failures, errors, or skips.

Coverage includes:

- SSE authorization and `Last-Event-ID` headers.
- Heartbeat comment handling and event frame parsing.
- 401 response classification without copying response content into an error.
- Backoff schedule and jitter bounds.
- MockWebServer disconnect, Room-store event application, and reconnect with
  the persisted `evt_1` cursor.

The final retained-data APK restored the real owner session. A new request was
submitted, the app was backgrounded, and Room converged to the server's complete
1188-message ordered collection. The active request reached terminal state,
the foreground monitor stopped, and the next cold start remained authenticated.
The final device suite passed 9 tests with no failures, errors, or skips. No
Cloudflare state was changed; Tunnel work remains Phase 7.
