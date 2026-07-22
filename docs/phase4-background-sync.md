# Phase 4 Android Background Status Sync

Status: verified

Date: 2026-07-23

## Scope

This batch implements the master plan's non-real-time background status check
for an already authorized remote session. It does not replace SSE while the app
is active or send an offline command. The 2026-07-23 addendum records the final
Phase 4 real-device gate.

## Implemented Contract

- One unique WorkManager job named `aerie_periodic_status_sync`.
- A 15-minute periodic interval, which Android may defer according to battery
  and operating-system policy.
- A connected-network constraint and a five-minute exponential retry backoff.
- `ExistingPeriodicWorkPolicy.KEEP`, so repeated session emissions do not create
  duplicate periodic jobs.
- Scheduling after an existing or newly authenticated remote session becomes
  active.
- Cancellation on logout and when entering the Debug-only local preview.
- Worker restoration of the existing Keystore-backed session followed by the
  same account-scoped repository synchronization used by the foreground app.
- Room convergence for message history and active request state. A failed chat
  synchronization returns WorkManager `retry`.

## Safety Boundaries

- The worker does not open an SSE stream.
- The worker does not submit or confirm pending outbound messages.
- Interrupted sends remain `awaiting_confirmation` for an explicit user action.
- The worker does not start the foreground service and does not render message
  text, file names, credentials, or token values in a notification.
- A missing, revoked, or unreadable stored session completes without network
  chat work; logout remains authoritative and cancels the unique job.

## Automated Evidence

The first combined rerun exhausted native memory on the Windows build host.
The same gates were then run separately with one Gradle worker, a 1 GB heap,
and in-process Kotlin compilation; the user's Live2D process was left running.

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks --no-daemon --max-workers=1 `
  '-Dorg.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' `
  '-Pkotlin.compiler.execution.strategy=in-process'
```

Result: 33 JVM tests across 8 suites, with 0 failures, errors, or skips.

The Debug and Release build gates were run with the same conservative JVM
arguments:

```text
:app:assembleDebug   BUILD SUCCESSFUL
:app:lintDebug       BUILD SUCCESSFUL; No issues found
:app:assembleRelease BUILD SUCCESSFUL
:app:lintRelease     BUILD SUCCESSFUL; No issues found
```

Merged Debug and Release manifests contain WorkManager's initializer,
`SystemJobService`, `RescheduleReceiver`, `RECEIVE_BOOT_COMPLETED`, `INTERNET`,
and `ACCESS_NETWORK_STATE`. The app's existing `dataSync` foreground service
remains non-exported.

Artifacts:

- Debug APK: 65,953,442 bytes.
- Debug SHA-256:
  `461DB7F4128CE34C0479AA0F466806CD3F648A914D074BAAA759E10687F6E4F8`.
- Unsigned Release APK: 4,637,095 bytes.
- Unsigned Release SHA-256:
  `42D358EBD92DA9B61CD27C51EBF4BB5E0FA6D81AFAC62EB40F5BB441F1F8C8FB`.
- `apkanalyzer` reports package `top.etta.aerie`, version `0.1.0-dev`, min SDK
  28, target SDK 35, `debuggable=false`, and `usesCleartextTraffic=false`.
- All 542 compressed APK entries were checked. Sensitive file-name hits and
  fixed owner/test credential, private-key, JWT, OpenAI key, GitHub PAT, and
  credential-bearing URL content hits were all zero.

## Real-Device Closeout

The final retained-data install and background acceptance were completed on
the target vivo `V2516A` running Android 16:

- The installed main APK and local Debug APK have the same SHA-256:
  `E8CC4C7A591CB764E47028856DEA5161868714158AE2AA143664DB7168E4C08E`.
- Aerie private data remained `1754 KB` across the install and `1758 KB` after
  the final test run. The encrypted session, Room database, and WorkManager
  database remained present, and cold start restored the owner screen without
  another login.
- The temporary Android device-idle whitelist was removed. With only the vivo
  "allow background battery use" setting retained, request
  `req_6c3f7847104cda2e9e4fad9c5c5ebb71` completed while the app was on the
  launcher. The foreground monitor started at `02:37:48.583`, observed terminal
  state at `02:38:15.556`, then removed its service and notification.
- System logs contained no Aerie `fast_freezer` event. The active notification
  record count after completion was zero; the notification builder exposes only
  the product name and fixed status text.
- Room v3 held `1188` unique messages and exactly matched the server's ordered
  ID, `messageOrder`, and role sequence. The original seven-part response at
  orders `1850..1856` and the new 22-message turn at `1864..1885` were complete
  and contiguous.
- The installed and local Test APK SHA-256 values both equal
  `6DC07100764AC3D00410564873BF9FC2AACA3AE164A8CE437D532F01DECC16F5`.
  Manual instrumentation passed all 9 tests with no failures, errors, or skips.
- The production WorkManager database contains one
  `aerie_periodic_status_sync` row in `ENQUEUED` state, with a `900000ms`
  interval, `300000ms` retry backoff, and zero run attempts.

Final desktop evidence is 38/38 JVM tests, successful Debug and Release Lint,
54/54 related server tests, and 632/632 full server tests. Phase 4 is verified;
file transfer, approvals, Cloudflare Tunnel, and signed release remain later
phases.
