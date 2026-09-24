# TaskChain Implementation Plan

This plan records the reset criteria used for the September 2026 implementation pass. Keep the app single-module, offline, file-backed, account-free, and telemetry-free. Code, automated checks, and device checks are distinct evidence.

## 1. Update the task model

- Store schedule, optional repeat-minute interval, Sound, and Vibrate per task. Keep timer length on each step.
- Enforce schedule-versus-deadline/reminder exclusivity when saving. Preserve IDs and existing run snapshots when editing.
- Load older saved routines and bundled defaults without losing task titles, timers, or schedules.

## 2. Rebuild the builder layout

- Center top-level headers and apply safe-area padding on Home, Routines, Progress, Settings, Builder, and Runner.
- Remove the built-in label, kind selector, and goal-link UI. Make Add Step create a task and place timer editing in its expanded step.
- Put the expandable step list under Schedule. Show name left and `[M m SS s]` timer length right; let taps open field editing.
- Provide task schedule, one-time deadline/reminder, repeat-minute wheel, and independent Sound/Vibrate switches.

## 3. Wire local feedback and reminders

- Schedule task alarms from saved task settings, restore them after boot/time changes, and cancel stale alarms after edits.
- At delivery, honor Sound and Vibrate separately. Repeat only when enabled and stop repeats according to the resolved product rule.
- Request notification permission only when enabling notifications.

## 4. Verify and hand off

- Run focused model/reminder JVM tests, `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug`.
- On a device, inspect all safe areas, builder add/edit/expand behavior, permission flow, schedule delivery, repeat stopping, and audio/haptic combinations.
- Record code presence, automated verification, and device verification separately; do not treat source review alone as device evidence.

## Open decision

Navigation after completing a previously skipped step remains TBD. It is outside this reset scope.

## Execution snapshot (2026-09-15)

- **Code present:** Safe-area-aware, centered headers; the built-in label removed; task-only steps; Add Step and expandable task editing; task-level schedule/deadline exclusivity; Sound/Vibrate settings; native minute selector; legacy routine-schedule preservation; task alarms, repeat rearming, completion-based stopping, and timer feedback wiring. Completion timestamps are distinct from retained skip timing.
- **Assumption:** Repeat prompts stop when the task is completed, then restart at a recurring task's next occurrence. The user may choose a different stopping rule.
- **Automated checks:** `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` all passed after integration.
- **Device checks:** Not yet performed. Safe areas, builder interactions, permission prompts, alarm timing, and audio/haptic combinations need on-device proof.
- **Timing limit:** Android's inexact alarms can be delayed; the selected minute interval can request an earliest next alert, not guarantee precise wall-clock delivery. See [Android alarm guidance](https://developer.android.com/develop/background-work/services/alarms).
