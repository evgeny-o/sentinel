# Implementation Plan: Android Sensor Node -> PC Bridge -> Existing Tracker/C2

**Date:** 2026-09-16  
**Status:** In Progress  
**Current milestone:** Android telemetry transport MVP  

## Problem
Build a new Android app that turns a Samsung S25 FE into a multimodal sensor node and connects it to the existing PC tracker/C2. The link should use USB as the primary transport, with Wi-Fi as fallback. The system should forward processed detections/telemetry by default and optionally support raw media streaming for remote analysis.

## Proposed Approach
Use a layered design:
- Android app performs on-device sensing, fusion, tracking, buffering, and transport management.
- A lightweight PC-side bridge terminates USB or Wi-Fi sessions from the phone, normalizes incoming data, and adapts it into the existing tracker/C2 input contract.
- Default operational mode sends compact telemetry/events only; optional media channels carry snapshots, clips, or live compressed streams when enabled.

## Architecture Outline
1. **Android sensing pipeline**
   - Audio trigger, dual-camera detection, target tracking, and sensor-fusion remain on-device.
   - Emit normalized target events, node health, calibration, and transport state updates.
   - Maintain an outbound queue with sequence IDs, timestamps, and delivery acknowledgements.

2. **Phone-to-PC transport**
   - Primary: USB connection using a direct local link model with framed messages and keepalives.
   - Fallback: Wi-Fi local-network transport using the same logical message schema.
   - Shared transport manager on Android selects active link, handles failover, and preserves queue integrity.

3. **PC bridge**
   - Receives framed messages from Android, validates schema/version, deduplicates by sequence ID, and persists a short rolling buffer.
   - Translates Android-native events into the existing tracker/C2 ingestion format.
   - Exposes optional media relay endpoints so snapshots or streams can be consumed without changing the telemetry path.

4. **Tracker/C2 integration**
   - Treat the phone as another sensor node with its own `node_id`, orientation, capabilities, and health state.
   - Map target class, azimuth, distance, confidence, trajectory, and source-camera fields into the tracker/C2 domain model.
   - Keep protocol translation isolated in the bridge so Android and tracker/C2 can evolve independently.

## Data Flow
1. Android detects or tracks a target.
2. Android packages an event envelope with metadata, target data, and delivery sequence.
3. Transport manager sends over USB if available; otherwise Wi-Fi.
4. PC bridge acknowledges receipt, stores a rolling cache, and transforms the event.
5. Bridge forwards normalized data into the existing tracker/C2.
6. If optional media is enabled, Android sends referenced snapshots/clips/streams on a parallel channel tied to the same event IDs.

## Contracts To Define
- **Telemetry event schema:** node identity, timestamps, class, confidence, track ID, camera source, azimuth, distance, velocity, audio SPL, and quality flags.
- **Health/status schema:** battery, thermals, camera availability, audio pipeline state, inference FPS, transport type, queue depth, and last error.
- **Ack/retry contract:** sequence IDs, bridge acknowledgements, resend window, duplicate suppression, and reconnect replay behavior.
- **Media contract:** snapshot or clip metadata, codec/format, reference to parent event ID, retention rules, and backpressure limits.

## Implementation Phases
1. **Architecture definition**
   - [x] Freeze system boundaries, message envelopes, transport responsibilities, and bridge ownership.

2. **Android MVP**
   - [x] Build sensing pipeline output contract, local queue, and telemetry-only transport.
   - [x] Support one active transport abstraction that can switch between USB and Wi-Fi.
   - [x] Add foreground-service lifecycle and health telemetry.
   - [ ] Validate on an Android device with ADB reverse and local Wi-Fi.

3. **PC bridge MVP**
   - [x] Implement TCP receiver, session-aware acknowledgements, schema validation, and duplicate suppression.
   - [x] Add optional UDP forwarding for the normalized envelope.
   - [ ] Implement the tracker/C2-specific field adapter after its input contract is available.

4. **Integration phase**
   - Register the phone as a sensor node in tracker/C2, map fields, and add health/connection visualization.

5. **Optional media phase**
   - Add snapshots first, then clips or live stream only if operationally needed.
   - Keep media optional and disable by default to protect latency and bandwidth.

6. **Hardening**
   - Reconnect handling, duplicate suppression, queue trimming, schema versioning, calibration workflow, and field validation.

## Current MVP Decisions
- Android package: `com.ptero.sentinel`.
- Minimum Android version: API 29; compile/target SDK: API 35.
- Implementation language: Kotlin.
- Wire format: newline-delimited JSON with a versioned envelope.
- USB MVP: TCP to device loopback through `adb reverse tcp:47500 tcp:47500`.
- Wi-Fi fallback: TCP to a configurable PC host on port `47500`.
- Delivery policy: bounded in-memory queue, monotonic sequence IDs, explicit acknowledgements, and oldest-message eviction under sustained overload.
- The same logical transport interface will support both links; protocol adaptation remains in the future PC bridge.

## Revision History
| Date | Change | By |
|------|--------|----|
| 2026-09-16 | Initial architecture plan created | Copilot |
| 2026-10-01 | Marked implementation in progress and fixed Android MVP decisions | Copilot |
| 2026-10-01 | Implemented telemetry contracts, bounded delivery, failover transport, foreground service, and PC bridge MVP | Copilot |
| 2026-10-01 | Installed Android/Gradle toolchain and completed the first successful debug build | Copilot |
| 2026-10-04 | User reports telemetry visible in emulator; added next steps for PC reception, reconnect behavior, and live status UI | Copilot |

## Key Design Decisions
- **USB primary, Wi-Fi fallback:** best for reliability and lower exposure while keeping operational flexibility.
- **Telemetry-first architecture:** protects latency and simplifies tracker/C2 integration.
- **Bridge-owned protocol adaptation:** avoids coupling Android internals to legacy PC ingestion details.
- **Media as side channel:** prevents raw media from blocking target telemetry.

## Notes and Considerations
- The repository currently contains only a stub app, so this plan assumes greenfield implementation for both Android app structure and PC bridge components.
- Before implementation, confirm the exact existing tracker/C2 input format, supported transport libraries on PC, and whether USB will be exposed as ADB port forwarding, USB tethering network, or another direct channel.
- Calibration data for camera orientation and phone mounting angle should be treated as first-class configuration because azimuth quality depends on installation geometry.
- Android SDK API 35, build-tools, platform-tools, JDK 17, and the Gradle 8.14.5 wrapper are installed. The debug app compiles and its unit tests pass; physical-device validation remains pending.
- Run the PC bridge with `python src/pc/sentinel_bridge.py`. For USB, also run `adb reverse tcp:47500 tcp:47500`. For tracker forwarding, pass `--tracker-host <host> --tracker-port <port>`.

## First local preview without a phone
1. Install Android Emulator and an API 35 system image in the IDE's SDK Manager (neither is currently installed). Create and start an API 35 virtual device through Device Manager. If IntelliJ does not expose these Android tools, use Android Studio to install and run the emulator, then open the same project there.
2. Select the running virtual device and run the Android app configuration to see the screen, which currently shows a status label and Start/Stop buttons. If the IDE does not offer an app run configuration, use `.\gradlew.bat assembleDebug`, then install and launch the APK with `adb install -r build\outputs\apk\debug\Sentinel-debug.apk`, followed by `adb shell am start -n com.ptero.sentinel/.MainActivity`.
3. For the service's health telemetry, run `python src\pc\sentinel_bridge.py` in a separate terminal, then `adb reverse tcp:47500 tcp:47500` while the emulator is running. Click Start in the app and allow notifications if prompted. The bridge terminal should log a `node_health` message about every 10 seconds. Click Stop to stop the service.
4. The emulator previews the UI and telemetry transport, not reliable physical camera, microphone, or device-specific sensor behavior. The app currently emits health telemetry only; the planned audio/video detection and tracking pipeline is not yet implemented. A real phone will be needed to validate those features later.

## Next steps after the emulator preview
**Updated:** 2026-10-04

1. Confirm end-to-end reception in the PC bridge terminal, not just service status on the emulated device. Look for increasing `node_health` sequence IDs about every 10 seconds.
2. Exercise the existing service: Start produces messages, Stop stops producing messages after any in-flight delivery, and pressing Android Home leaves the foreground service running. Stop the bridge temporarily, restart it, and confirm delivery resumes while the app service remains running.
3. Recommended next implementation milestone: extend the existing activity to display live connection state, active transport, queue depth, last successful acknowledgement, and last error. The current Running label only reflects the Start action; it does not prove PC connectivity. Keep this separate from detection features.
4. After transport behavior is understood, move to a small camera-preview milestone with permission handling, followed by physical-device validation before adding device-specific sensing or on-device inference.

Emulator telemetry has been reported by the user; PC reception, reconnect behavior, and physical-device operation are not yet recorded as validated. These are proposed next steps, not newly implemented features.
