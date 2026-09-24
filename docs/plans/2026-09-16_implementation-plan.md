# TaskChain Vertical-Slice Implementation Plan

This plan records the reset criteria closed during the September 2026 implementation pass while preserving the domain terms and invariants in `CONTEXT.md` and the package boundaries in `architecture.md`. The tree already contained implementations for much of the requested behavior, so Round 1 focused on demonstrated gaps and the smallest regression checks for non-trivial logic.

## Guardrails

- Keep the app Android-only, offline-only, file-backed, account-free, telemetry-free, and database-free.
- Keep run rules in `domain/run`; ViewModels coordinate state and screens render it.
- Keep Android alarm, notification, audio, and vibration APIs inside `reminder`.
- Keep UI values in resources or `ui/designsystem`; add no dependency for native Android or Kotlin behavior.
- Do not decide navigation after completing a previously skipped task. That product decision remains open.
- Do not claim automated verification unless Gradle completes. Record user-confirmed physical-device evidence separately.
- Each slice owns the files listed below. A task must not edit a file owned by another slice.

## Round 1

### Slice 1: Author and save one task-based routine

**User outcome:** A user can add a task, expand it, edit its timer and mutually exclusive schedule/deadline/reminder fields, choose independent Sound and Vibrate settings, collapse it to the `[M m SS s]` summary, and save it without Habit, Goal, kind-selection, or Goal Link controls.

**Owned files:**

- `app/src/main/java/com/taskchain/domain/model/Models.kt`
- `app/src/main/java/com/taskchain/ui/FeatureViewModels.kt`
- `app/src/main/java/com/taskchain/ui/TaskChainApp.kt`
- `app/src/main/res/values/authoring_strings.xml`
- `app/src/test/java/com/taskchain/domain/model/RoutineTemplateTest.kt`

**Tasks:**

1. Trace add, edit, save, reload, and legacy JSON decoding through the owned files.
2. Correct only punch-list gaps; reuse the existing model and native pickers.
3. Add or tighten one focused model regression test only if non-trivial validation changes.

**Success criteria:**

- Add Step creates and expands a task.
- Timer length is stored on `RoutineStep` and rendered as `[M m SS s]` when collapsed.
- A task schedule cannot coexist with its deadline or one-time reminder.
- Remind Every is available only for scheduled tasks and uses a scrolling minute selector below Time.
- Sound and Vibrate persist independently per task.
- No task-kind or goal-link authoring control remains.
- Home, Builder, and Runner content stays in safe drawing insets and top-level headers are centered.
- The Home/Routines flow has no `Built-in routines` label.

### Slice 2: Deliver and restore local task prompts

**User outcome:** A saved task prompt arrives locally, follows its recurrence, repeat interval, Sound, and Vibrate settings, and is restored after reboot or time changes.

**Owned files:**

- `app/src/main/java/com/taskchain/reminder/NextTriggerCalculator.kt`
- `app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt`
- `app/src/test/java/com/taskchain/reminder/ReminderRepeatTest.kt`

**Tasks:**

1. Trace initial delivery, repeat rearming, completion-based stopping, next recurrence, and boot/time restoration.
2. Correct only failures against the reset criteria; keep alarms inexact and local.
3. Add one focused JVM regression test for each changed scheduling rule.

**Success criteria:**

- One-time, daily, weekday, and selected-day tasks calculate a future trigger correctly.
- Repeat prompts yield to the next regular occurrence and stop for the completed occurrence.
- Delivery honors Sound and Vibrate independently without relying on channel defaults.
- Stale alarms are cancellable and persisted schedules are restored after supported system broadcasts.
- Notification permission is not requested at launch.

### Slice 3: Preserve runner timing and completion evidence

**User outcome:** Scheduled-task settings also govern timer feedback while persisted run timing, skip/completion state, and confirmation rules remain intact.

**Owned files:**

- `app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt`
- `app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt`

**Tasks:**

1. Recheck timer expiry, persisted acknowledgement, revisit behavior, final confirmation, and completing a skipped task.
2. Correct only domain-rule regressions; do not select a policy for the open post-skip navigation decision.
3. Add one focused regression test for each changed transition.

**Success criteria:**

- Timer feedback is requested once from persisted timestamps.
- Completed or skipped steps do not restart timing when revisited.
- Completing a skipped task changes it directly to Completed and retains actual duration.
- Complete or Skip on the final task requests confirmation and exposes unfinished tasks.
- Back on the first task requests abort confirmation.

## Integration and verification

1. Review every agent diff against its owned files and reject unrelated cleanup or cross-slice edits.
2. Run focused tests, then `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug` with a complete JDK 17.
3. Confirm no slice changed the unresolved skipped-task navigation policy.
4. Commit the reviewed plan and code locally with a short imperative subject.

## Evidence snapshot

- **Earlier physical-device evidence:** User confirmed that evidence applied only to the first punchlist, not to this Round 1 implementation.
- **Current physical-device run:** The updated debug APK installed on a Pixel 7 on 2026-09-16. All five physical-device findings passed. For the final untimed task, Skip followed by a delayed Cancel restored `Pending` / `No timer` and removed the skipped state. For a timed final task, Cancel shifted its start timestamp by the confirmation interval and resumed the countdown without charging that interval. A completed daily task disappeared from Home immediately and remained absent after an app restart; Android scheduled its next alarm for the following local day rather than accumulating a same-day occurrence.
- **Round 1 changes:** Added the required brackets to collapsed task durations, restored the pre-confirmation task state when Cancel is pressed, paused task timing during finish and abort confirmations, projected Home from only the current local day's actionable occurrences, and advanced completed recurrence alarms to a later local day.
- **Automated checks:** `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug` passed on 2026-09-16 with the installed JDK 17. The final unit suite was rerun after the last regression test was added.
- **Latest UI-fix checks:** `:app:assembleDebug` and `:app:testDebugUnitTest` passed after the physical-device findings were addressed.
- **Device-test cleanup:** Three isolated test routines were removed, their temporary history and alarm were cleared, and the pre-test active session and history were restored before leaving the app open on Home.
- **Open decision:** Navigation after completing a previously skipped task remains TBD and is not an acceptance claim.
