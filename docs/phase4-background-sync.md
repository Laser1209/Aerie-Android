# Phase 4 Android Background Status Sync

Status: verified

Date: 2026-07-22

## Scope

This batch implements the master plan's non-real-time background status check
for an already authorized remote session. It does not replace SSE while the app
is active, send an offline command, or complete the Phase 4 real-device gate.

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

## Remaining Real-Device Gate

No ADB, installation, credential entry, or device-session inspection occurred
in this batch because the phone was intentionally disconnected. The next
real-device batch must verify owner login, Room v2 full convergence, all seven
segments of the reported long response in `messageOrder`, foreground
notification privacy during a long task, and the persisted WorkManager job.
