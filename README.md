# Aerie Android

Native Android companion client for Aerie.

## Status

The repository is initialized for the Aerie v2 Android companion. No Android
application source has been created yet. The Kotlin and Compose application is
scheduled for Phase 4 only after the server-side account, identity, and
persistent-chat gates have passed.

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
