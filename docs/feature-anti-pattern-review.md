# Feature-design anti-pattern review (2026-10-30)

Scope: `app/src/main/java/com/taskchain/**`, `app/src/test/java/**`, `scripts/check-feature-boundaries.py`, against `architecture.md`/`CONTEXT.md`. Read-only review; no code changed. `check-feature-boundaries.py` passes (76 files, 18 owners, no cycles). Line numbers are as of commit `c3b077f`.

Severity: **H** = rule drift or divergent behavior likely; **M** = boundary erosion; **L** = hygiene.

## Summary

| Anti-pattern / smell | Verdict | Top items |
|---|---|---|
| Domino dependency | Main sources clean; tests and checker leak | 1.1, 1.2 |
| Clone and own | Main problem area | 2.1, 2.2, 2.3, 2.4 |
| Cross-feature envy (via root) | Present | 3.1, 3.2, 3.3 |
| God slice | Running UI layer | 4.1, 4.2 |
| Fake shared core | Minor; mostly justified by `architecture.md` | 5.1–5.4 |
| Leaky technical UI | Token bypass widespread | 6.1, 6.2 |
| Tramp data | Holder duplication | 7.1, 7.2 |
| Layered wolf | Only `keepscreenon` | 8.1 |

## 1. Domino dependency / import enforcement

- **1.1 H — tests cross owners; checker skips `app/src/test`** (`check-feature-boundaries.py:56` scans `app/src/main/java` only).
  - `test/.../core/model/SubtaskSerializationTest.kt:3` imports `features.Running.domain.RoutineRunEngine`.
  - `test/.../features/notifications/domain/RunAlertPolicyTest.kt:3-4`, `RunNotificationPolicyTest.kt:3-4` import root `notificationRunRules` and the Running engine.
  - Contradicts `docs/feature-refactor-plan.md:32` ("Tests follow the owner of their assertions").
  - Fix: scan test sources; move cross-owner tests to root `com.taskchain`, or use `NotificationRunRules` fakes.
- **1.2 M — char literal `'"'` defeats string stripping** (`check-feature-boundaries.py:80`). Reproduced in a scratch copy: a forbidden qualified reference after `'"'` passes. Fix: strip char literals before strings.
- **1.3 M — shared `R` resources unchecked** (`:84-85`). Cross-owner resources: `R.integer.reminder_snooze_minutes` (reminders, notifications), `R.string.resume_routine` (Running, scheduling), `start_routine`/`new_routine` (core.browsing, scheduling), `R.string.cancel` (authoring, scheduling). Fix: owner-prefixed resource names, or move shared values to Core-owned resource files.
- **1.4 L — no intra-owner layering rule.** No current violation (no Android imports in `*/domain`, `core/model`, `core/schedule`). Fix: forbid `android*`/`.ui` imports from `*/domain`.
- Clean: Core↔Core cycles, wildcard/aliased imports and qualified references are detected; no main-source Core→feature or feature→feature imports.

## 2. Clone and own

- **2.1 H — Builder copies Core routine rules.**
  - `features/authoring/ui/RoutineBuilderViewModel.kt:222-238` `BuilderState.hasConflictingLegacyRoutineSettings()` duplicates `core/model/RoutineValidation.kt:147-161` line for line.
  - `:241-250` `clearLegacyRoutineSettings` repeats `RoutineValidation.kt:135` `withoutLegacyRoutineSettings()`.
  - `save()` `:614-628` inlines `RoutineTemplate.normalizedForSave` (`RoutineValidation.kt:168`), which has no production caller.
  - `validateBuilderState` `:113-150` re-checks `validationIssues` (`RoutineValidation.kt:100-125`).
  - `ScheduleRule(...)` built three times (`:127`, `:228`, `:629`).
  - Fix: `BuilderState.toTemplate()`, then call `normalizedForSave`/`validationIssues()`; map Core issues to `BuilderValidationError`.
- **2.2 H — "current task is running" predicate (ACTIVE, no confirmation, PENDING, unpaused) written separately:**
  - `features/running/domain/RunFeedbackPolicy.kt:36-40`, `:51-53`
  - `features/running/ui/RoutineRunnerViewModel.kt:209-213`
  - `features/notifications/domain/RunAlertPolicy.kt:52`, `:79`
  - Variant: `RoutineRunEngine.kt:338-343` `requiresRunnerScreen` (also true during confirmation).
  - Fix: one `RoutineRunEngine.isTaskRunning(run)`, exposed via `NotificationRunRules`.
- **2.3 M — Sound/Vibrate gate expressions duplicated foreground vs background:** `features/notifications/platform/RunAlerts.kt:94-95,103-104` vs `RoutineRunnerViewModel.kt:245-246,352-354` (`run.routineSoundEnabled && task.soundEnabled && sounds[token].enabled`). Divergence breaks the `CONTEXT.md` mute-precedence invariant in one path only. Fix: pure gate function in `core/model`.
- **2.4 M — day boundary differs between Home and Progress.** `features/progress/domain/ProgressSummary.kt:48-54` uses midnight; `features/scheduling/domain/TodayProjection.kt:44-52` uses `dayStartHour` (default 2 AM). A 1 AM completion lands on different days. Decision needed: which boundary Progress should use. Fix: one day-start helper in `core/schedule`.
- **2.5 M — nudge timing computed twice:** `RunAlertPolicy` predicts the next nudge from `taskNudgeCount * nudgeIntervalMillis`; `RunFeedbackPolicy.kt:95-98` buckets with paused/confirmation-adjusted `effectiveNow`. Fix: `RunFeedbackPolicy.nextNudgeAt`, injected through `NotificationRunRules`.
- **2.6 M — "when did the run complete" derived twice:** `TodayProjection.kt:98-105` falls back to task timestamps; `core/schedule/ReminderPlanner.kt:97-99` does not. Fix: one `RoutineRun.completedAtOrNull()` in Core.
- **2.7 L — reminder-plan apply loop ×5:** `RoutineBuilderViewModel.kt:645-647`, `RoutineRunnerViewModel.kt:371-372`, `TodayViewModel.kt:78-79,89-90`, `features/reminders/ReminderScheduler.kt:167-168`. Fix: `ReminderScheduler.apply(plan)` in Core (5 owners; passes three-strike).
- **2.8 L — stale-step identity twice:** `RoutineRunnerViewModel.kt:293-297` vs `RunNotificationTarget` (`features/notifications/domain/RunNotificationPolicy.kt:17-21`). Fix: shared `RunStepKey` in `core/model`.

## 3. Cross-feature envy (routed through root)

- **3.1 H — Runner rule owned by Notifications, injected back into Running.** The `CONTEXT.md` nudge reminder limit lives in `RunAlertPolicy.remindersExhausted` (`RunAlertPolicy.kt:26`); `AppContainer.kt:112-113` binds it into `RunningDependencies`; used at `RoutineRunnerViewModel.kt:220`. Fix: move it to Running domain; expose to Notifications via `NotificationRunRules`.
- **3.2 H — active-run navigation rule in root.** `TaskChainApp.kt:179-191` combines `observeActive()` with `runEngine::requiresRunnerScreen` and force-navigates; `:193-203` applies a separate `RunStatus.ACTIVE` rule. `feature-folder-draft.md` M03 assigns active-run readiness to Running. Fix: Running exposes `Flow<RoutineId?>`; root only navigates.
- **3.3 M — root reaches into feature internals.** Root imports `domain.*` and `platform.*` directly (e.g. `RoutineRunEngine`, `RunFeedbackPolicy`, `RunAlertPolicy`, `RunNotification`, `AndroidReminderScheduler.EXTRA_*`). Only Running and Notifications have a `*Dependencies` entry. `NotificationIntegration.kt:16-54` holds untested adapter logic that feature tests import (1.1). Fix: one public entry file per feature; root never imports `domain.*`; move adapters to a tested root integration file.

## 4. God slice

- **4.1 M — `features/running/ui/RoutineRunnerViewModel.kt` (393 lines), six jobs:** run bootstrap/default sounds/reminder clearing (`:377-388`), tick clock (`:60-65`), foreground feedback (`:183-251`), visibility handoff (`:91-101`), external-save adoption (`:257-263`), terminal persistence + history + rescheduling (`:305-373`).
  - Rule leak: `complete()` `:112-122` decides readiness delay (a `CONTEXT.md` invariant); same check in `RunPresentation.kt:100-105`.
  - Reschedule try/catch duplicated (`:162-171`, `:329-338`).
  - Fix: `engine.completeCurrent(run, now, readinessDelay)`; extract a terminal-run writer shared with `RunRecovery.kt`.
- **4.2 M — `features/running/ui/RoutineRunnerRoute.kt` (821 lines)** calls the engine directly for timing (`:196-206`), decides Back (`:176-182`), repeats paused-or-skipped checks (`:288,529,537,541`) and hosts the countdown dial (`:615-776`). Fix: derived times in `RunnerState`, Back decision in engine, dial in its own file.
- **4.3 M — `RoutineBuilderViewModel.kt` (665 lines):** mostly 2.1; removing duplication leaves draft mutation.
- **4.4 L — `RunningEntryViewModel.kt:66-92`** holds a Composable dialog; `status == ACTIVE` checked three times. Fix: move dialog to a UI file.
- Not a God class: `RoutineRunEngine` (492 lines) holds only transitions, clocks and acknowledgement, per `architecture.md`.

## 5. Fake shared core (three-strike rule)

Counts are distinct non-Core owners naming the declaration.

- **5.1 M — dead Core code:** `core/designsystem/RepeatedPath.kt`, `RepeatedPathExamples.kt` have no use outside each other. Fix: delete, or move to cyberpunkAndroid.
- **5.2 M — single-feature helpers in Core:**
  - `routineDurationParts` (`core/browsing/HomeScreen.kt:153`) used only by authoring. Move to `features/authoring/ui`.
  - `completedSinceCycle`/`completedRoutineSinceCycle` (`core/schedule/ReminderPlanner.kt:64,89`), `afterDelivery` (`:208`) and the two-hour repeat window used only by reminders. Move to `features/reminders`, keeping shared planning in Core.
- **5.3 M — run timing in design system:** `RunnerMotion.taskReadyTransitionDurationMillis` (`core/designsystem/RunnerMotion.kt:19`) sets the persisted start delay (`RoutineRunnerViewModel.kt:118`). Fix: move run-delay constants to `features/running/domain`; keep animation values in designsystem.
- **5.4 L — naming:** `TimerFeedback` lives in `core/platform/ReminderPlatform.kt:37`. Move to its own file.
- Justified single-owner Core (documented in `architecture.md`): `core/browsing`, `core/navigation`, `core/startup`, `KeepScreenOnEffect`, `RunnerGestureTokens`.
- Healthy shared Core: repository interfaces (5 owners each), `RunStatus` (6), `ReminderScheduler` port (5), `NextTriggerCalculator`, `RoutineValidation` invariants.

## 6. Leaky technical UI

- **6.1 M — features bypass semantic tokens.** `TaskChainTheme.kt:87` promises tokens "without direct Cyberpunk theme access", yet direct `CyberTheme.`/`CyberPrimitives.` uses: `RoutineRunnerRoute` 63, `RoutineBuilderRoute` 33, `TodayRoute` 24, `BuilderSubtaskEditor` 10. A library token change reaches features unmediated. Fix: add missing tokens to `TaskChainTokens`; migrate per screen.
- **6.2 M — mixed typography:** `MaterialTheme.typography` beside `CyberTheme.typography` (`SettingsRoute.kt:54,66,97,103`; `RoutineRunnerRoute.kt:353,429,595-605`; `RunnerSubtaskContent.kt:40,48`). Fix: semantic text styles at the theme boundary.
- **6.3 L — raw values:** dp literals (`RoutineRunnerRoute.kt:410,593,686,816`; `RunnerNextUpPreview.kt:28`; `RunnerSubtaskDial.kt:33`); `Color.Black` (`RunnerGestureCues.kt:71`); hardcoded strings in Core (`HomeScreen.kt:142,144,150`).
- Clean: no shared ViewModels or state across features; no `Color(0x…)` in features.

## 7. Tramp data

- **7.1 M — intervals passed standalone and inside the holder:** `RunAlerts.kt:26-27` and `NotificationDependencies.kt:25-26` both carry `nudgeIntervalMillis`/`untimedIdleLimitMillis`; `RunAlerts.kt:82-83` passes them again. Fix: one config owner.
- **7.2 L — `NotificationNudge` re-wraps `NudgeDecision` field for field** (`NotificationIntegration.kt:25-28`). Acceptable under the no-cross-import rule; drop if `NudgeDecision` moves to Core.
- **7.3 L — `RunningDependencies` exposes writable `Mutex`, `MutableStateFlow` revision and `@Volatile var runnerVisible`** (`:18-38`), read by Notifications via root (`AppContainer.kt:123`). Fix: expose read-only views.
- **7.4 M — `AppContainer` near god object:** ~12 public members; `TaskChainApp(container)` builds 8 ViewModels positionally (e.g. `TodayViewModel`, 8 args, `TaskChainApp.kt:80-91`). Contradicts `feature-refactor-plan.md:39` ("ViewModel factories"). Fix: per-feature factory, as Builder does (`TaskChainApp.kt:144`).

## 8. Layered wolf

- **8.1 L — `features/keepscreenon`:** `domain/` holds one one-line rule (`RunKeepScreenOnPolicy.kt:13-14`) plus three tiny `ui/` files. Fix: flatten to one file; keep the test.
- **8.2 L — `features/reminders` is the inverse:** one 222-line file mixing actions, Android scheduler and handlers, against `architecture.md` "UI, pure domain and Android adapter separation". Fix: split only if it grows.
- Proportional: `notifications/domain` vs `platform`, `progress/domain`, `scheduling/domain` (pure, tested).

## 9. Documentation drift

- `docs/feature-refactor-plan.md:9,62` say six features; there are eight. `:72` records 67 files/16 owners; now 76/18.
- `architecture.md:29` says Routes construct their ViewModel; root builds Today, Routines, Progress, Settings, KeepScreenOn and Entry ViewModels (`TaskChainApp.kt:77-103`).
- `architecture.md:25` omits that the checker skips tests.

## Recommended order

1. 2.1 (Builder duplication) — pure deletion toward Core calls; existing validation tests cover it.
2. 2.2 + 3.1 — one running predicate and reminder-limit rule in Running domain.
3. 1.1 + 1.2 — extend checker to tests; fix char-literal stripping; relocate three tests.
4. 2.3, 2.4 (needs a decision on Progress day start), 2.6.
5. 3.2, 3.3, 7.4 — feature entry files and factories; Running-owned active-run flow.
6. 6.1/6.2 token migration, screen by screen.

Each behavior change needs a focused JVM regression test (AGENTS.md).

## Remediation status (2026-10-30)

Execution was renamed to Running (`features/running`). All **M** items, plus 1.1, 2.1 and 3.2 (prerequisites), are implemented; remaining **L** items are open unless listed. Not compiled: the session had no JDK 17 or Android SDK. Run `:app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug :app:assembleDeviceTestAndroidTest`.

| Item | Change |
|---|---|
| 1.1 | Checker scans `test`/`androidTest`; owner tests use fakes (`FakeNotificationRunRules`); real-engine assertions moved to root `NotificationIntegrationTest`; AGENTS.md test-type table |
| 1.2 | Char literals stripped before reference scan |
| 1.3 | `RESOURCE_OWNERS` rule; cross-owner strings moved to `strings.xml`; `settings_strings.xml` added; `punch_strings.xml` removed. Names unchanged |
| 2.1/4.3 | Builder maps `toTemplate()` and calls Core `validationIssues`, `normalizedForSave`, legacy-settings rules |
| 2.3 | `core/model/FeedbackGates` shared by runner and background alerts |
| 2.4 | 2 AM day start (`core/schedule/DayStart.kt`) for Home, Progress and completion-based next reminders |
| 2.5 | `RunFeedbackPolicy.nextNudgeAt` via `NotificationRunRules` |
| 2.6 | `core/model/RunTimestamps.completedAtOrNull` |
| 3.2 | `RunningDependencies.runnerScreenRoutineIds`; root only navigates |
| 3.3 | Entry file per feature; checker forbids root use of `features.*.domain`/`platform` |
| 4.1 | `completeCurrentWithReadiness`, `TerminalRunWriter` (shared with recovery) |
| 4.2 | `RunTiming`, `systemBack`, `RunCountdownDial.kt` |
| 5.1 | `RepeatedPath*` and their resources deleted |
| 5.2 | `routineDurationParts` → authoring; reminder repeat rules → `features/reminders/ReminderRepeatRules.kt` |
| 5.3 | `features/running/domain/RunReadiness` |
| 6.1/6.2/6.3 (runner) | `TaskChainDesignSystem` Space/Sizes/Strokes/Dial/Colors/Shapes/TextStyles; checker forbids `CyberTheme.`, `CyberPrimitives.`, `MaterialTheme.typography` in features |
| 7.1 | `RunAlerts` sole owner of cadence/limit |
| 7.4 | Feature `*Dependencies` factories; `AppContainer` internals private |

Deliberate behavior changes:
- Progress trend days start at 2 AM.
- A completion between midnight and 2 AM schedules that night's occurrence. A routine scheduled before 2 AM, completed after 2 AM, skips the next calendar night's occurrence (it belongs to the handled day).
- First terminal write skips a duplicate history append, matching retry/recovery.
