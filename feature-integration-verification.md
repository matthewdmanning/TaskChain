# Feature integration verification

Goal: implement [the agreed plan](feature-integration-plan.md) while retaining Route → ViewModel → Screen.

## Baseline

- GitHub integration verified `main` at `ae2d52ef7ab5382d43c0d1ec1f8c7443cba7dd32` and all seven feature tips on 2026-10-07; local refs match.
- Isolated worktree: `.scratch/feature-integration`; integration branch: `refactor-feature-integration`.
- Original checkout, original feature refs, and `main` are preserved. No branches are published.
- Runtime: complete JDK 17; checked-in Gradle 9.8.0; shared wrapper distribution cache exists.
- Context7 official Compose documentation confirms lifecycle-aware ViewModel state collection and plain-state/callback screens: https://developer.android.com/develop/ui/compose/state-hoisting
- Baseline checks: compilation, JVM tests, lint, assembly (result recorded below when complete).

## Alignment policy

Each feature merges its original history into an alignment branch created from the latest accepted integration tip. Older overlapping implementations are replaced with the current MVVM implementation. The original branch remains unchanged. Full compile, JVM tests, lint, and assembly gate every accepted feature merge.

## Device acceptance

Device validation remains separate from build and JVM evidence. Required flows: cue authoring, main-only icons, Complete/Skip, collapse, cue rims/Warning, gestures, bubble navigation, scaling, animation gates, background/resume, and final-task confirmation.
Baseline outcome: all four checks passed (2026-10-07). Existing warnings remain; no device proof claimed.

Foundation cleanup: removed .gitmodules, the empty third_party/cyberpunkAndroid gitlink, and its stale local Git configuration. No Gradle files reference third_party; the published v1.0.8 library dependency remains. Renormalized the Windows wrapper under existing .gitattributes (line endings only). Compilation, JVM tests, lint, and assembly passed after cleanup.

## Substeps alignment

Original tip: 3b573f9. Content conflicts: ReminderScheduler.kt, FeatureViewModels.kt, TaskChainApp.kt. Replaced flat independent secondary tasks with nested cue definitions and persisted main-clock advancement markers. Current reminder and runner behavior retained. Added MVVM Screen boundary, cue authoring, preference persistence, semantic sound, and backward-compatible JSON tests. Primary review corrected future-cue timing, skipped-task advancement, final-confirmation rollback, readiness/double-action guards, and duration summaries. Gate: compile, 84 JVM tests (zero failures/errors), lint, assembly passed. Device checks remain pending until all visual features are aligned.

## Step accordions alignment

Original tip: 2b705c3. Conflicts: TaskChainApp.kt, TaskChainTheme.kt, deleted third_party gitlink. Current stable-ID accordion/reorder, title editing, native duration pickers, typography, and nested cue editing already implement the feature; retained them and retained submodule deletion. Gate: compile, all 84 JVM tests, lint, assembly passed. An initial premature build encountered unresolved merge markers; corrected resolution and reran the complete gate successfully.

## Frictionless gestures alignment

Original tip: 8e430bc. Conflicts: app/build.gradle.kts, RunnerGestures.kt (add/add), TaskChainApp.kt, build.gradle.kts, settings.gradle.kts. Retained main's consolidated detector, current next-finished/Skip/Pause/Resume intents and completion guards, JDK 17 configuration, and published library dependency. No Gradle third_party wiring restored. Gate: compile, all 84 JVM tests, lint, assembly passed (unchanged code checks reused by Gradle).

## Collapsible nested runner alignment

Original tip: 7c7eec1. Conflict: MainActivity.kt. Removed repository-driven feature host and legacy stacking overlay; composed cue list beneath the main title. Collapse changes presentation only and always keeps the active cue visible, with saveable expansion state. Bubble navigation/current activity retained. Gate: compile, all 84 JVM tests, lint, assembly passed.

## Next-up preview alignment

Original tip: 4cdcc13. Conflict: MainActivity.kt. Replaced undefined host overlay call with RunnerNextUpPreview inside the current Screen, reading only its displayed run snapshot. Retained resource-configurable two-task preview and pending main-task filtering. Removed repository host and duplicated progress overlay; progress is reserved for the micro-animation stage. Gate: compile, all 84 JVM tests, lint, assembly passed.

## Dynamic progress ring alignment

Original tip: 47f22a4. Conflict: MainActivity.kt. Removed independently polling halo/feature host. Existing main countdown is wrapped in fixed-angle, increasing-radius cue rims computed from the same sampled time and persisted markers. Stable palette is generated from the design-system Info seed. Advancement markers glow; overtime extends sweeps without changing rim colors; behind-schedule state fills the entire main interior with Warning. Added proportional/overtime geometry checks and accessible cue state descriptions. Gate: compile, all 85 JVM tests, lint, assembly passed. Pixel 7 installation succeeded; app data backed up before device checks.

## Micro-animation alignment and final UI refinements

Original tip: 4cd3b49. Conflict: MainActivity.kt. Removed the repository-driven feature host and independently polling overlay. RunnerMainProgress uses the displayed main-task snapshot, existing sampled time, and completion/readiness hold, foreground, user-transition and system-animation gates. Cue advancement leaves main-task progress unchanged. Initial gate: compile, all 85 JVM tests, lint and assembly passed.

Subsequent user instructions supersede the earlier collapsible list: show only the current cue, keep completed rims/markers, remove instructional prose, use leftmost drag handles for cues and main tasks, show only set compact durations on collapsed headers, and enlarge other text while retaining timer/routine-name/Home-category sizes. Cue reordering retains stable identities and associated durations; accessibility move actions remain available. Primary device review also replaced the nonfunctional native-view gesture listener with Compose pointer observation and made the runner action row wrap for enlarged text. The final source gate is recorded below after completion.

## Physical-device evidence (2026-10-07)

Pixel 7, density 420, authored a temporary two-main-task routine using the UI, with Prepare/180 seconds and Focus/420 seconds in the first task. Tested at default font scale 1.0 and original scale 1.5. Artifacts remain ignored under `.scratch/device-verification/`.

- Cue authoring derives 10:00, preserves values on save, and blocks malformed raw duration input. Native drag-and-drop reordered the two cues in both directions while retaining their associated durations.
- Main-task handles are leftmost; collapsed timed header displays `10:00`, untimed header has no duration. Larger shared text and current-cue-only display were inspected on device.
- Complete changes Prepare to Focus without changing main index, main-only icons or `0 of 2` progress. Completing the last cue advances the main task with existing readiness behavior.
- Skip advances the main task. Back restores Focus and its advancement marker. A skipped main elapsed value of 46,773 ms remained unchanged after process termination/relaunch; explicit Resume reopened it without resetting the cue.
- Long press pauses/resumes; left swipe skips; right swipe navigates to a finished main task. Normal Complete/button taps and vertical scrolling still work.
- Completing the last main task with an earlier skipped task presents final confirmation. Selecting the unfinished task returns to its saved cue; right swipe then reaches the completed task.
- Settings initially hid cue remaining time, then showed it when enabled. A controlled test-run fixture with first advancement at minute 2 and main active time 10:10 rendered main overtime +0:10, cue overtime +1:10, whole-interior Warning, unchanged cue colors and durable marker. This rendering fixture supplements the domain test; it is not a ten-minute real-time device run.
- The bubble's `open_active_run` activity extra opened the persisted run directly. Native bubble delivery/expansion was not exercised because the feature remains opted out; notification/bubble permission settings were preserved.
- System animator scale 0 allowed last-cue completion to start the next main task immediately (767 ms relative to the rounded device-second sample), without readiness delay. Restored the original absent animator setting afterwards.
- Sound eligibility and once-only dispatch pass JVM tests; physical audibility and haptic strength were not measured.

Temporary routine/session/history data and the cue-time preference are restored during final cleanup. Font scale is restored to the original 1.5. No existing user routine or history record is intentionally replaced.

A focused device check remains at `.scratch/device-verification/gesture-check.ps1` (requires the controlled integration test run). It verifies two long presses toggle pause/resume exactly once and retain main/cue identity; it passed after waiting for the UI hierarchy to be ready. Enlarged-font review also reserves full switch width and uses larger single-line navigation labels. Original 13 history events match the pre-test backup semantically after removing the temporary test event.

Final source gate passed: compileDebugKotlin, testDebugUnitTest (85 tests, zero failures/errors), lintDebug and assembleDebug, using JDK 17 and pinned Gradle 9.8.0. Installed the exact final APK successfully. Native hierarchy confirms all navigation labels remain single-line at font scale 1.5; long Settings labels wrap while switches retain their full width; runner actions wrap with readable labels and all remain scroll-accessible.

Device cleanup completed: removed only the identified temporary routine/session and its one history event. Both original routines and all 13 original history events match the backup semantically. Cue-time preference is back off, font scale is 1.5 and animator setting is absent, matching the original settings. Temporary device-side fixture/capture files were removed; ignored local evidence and backup remain available.
