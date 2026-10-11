# Integrate the feature branches through MVVM alignment

## Subsequent user refinements (2026-10-07)

These override the earlier subtask-list presentation and builder controls: show only the current subtask; remove instructional prose; reorder subtasks by dragging with handles at the far left; put main-task handles at the far left too; show collapsed duration values without "Duration:" and hide unset durations. Increase other text sizes while preserving timer, routine-name and Home category-header sizes.

## Integration workspace and workflow

- Create local branch `refactor-feature-integration` from verified current `main`, in a separate worktree at `C:\GitHub\TaskChain\.scratch\feature-integration`.
- Preserve the current checkout, its unpushed commit, temporary assessment, and original feature branches. Do not publish branches or change `main` during integration.
- For each feature, create `refactor-align-<feature>` from the latest accepted integration tip. Merge its original branch there, resolve conflicts, adapt the implementation, and validate before merging it into the integration branch.
- Handle one feature at a time. Each accepted integration gets a separate merge commit and verification record. Do not accumulate unresolved or unverified features.
- Refresh the conflict assessment against edited tips; its original counts remain historical evidence.

## Shared MVVM foundation

- Retain **Route → ViewModel → Screen**. The Route owns navigation/lifecycle; the ViewModel owns UI state, user actions, domain coordination and persistence; the Screen renders state and emits intent.
- Extract the existing Run route/screen only where needed to give features distinct composition locations. Preserve bubble navigation and current behavior during this extraction.
- Use the existing runner snapshot, sampled time, foreground state and completion/readiness presentation. Remove repository-emission-based feature hosts and independent overlay polling during alignment.
- Keep transitions and timing rules in the Run engine. Extend existing semantic feedback and Android adapters; introduce no additional orchestration controller or generic feature framework.

## Agreed task and subtask behavior

These decisions supersede the earlier proposal that secondary tasks execute as independent tasks:

- A main task stays active while its **ordered subtasks** progress. Subtasks appear beneath its title in larger text; only main tasks appear as bottom icons, next-up tasks and overall task-progress entries.
- **Complete** advances the active subtask. Completing the last subtask completes the main task and advances to the next main task. Tasks without subtasks retain current Complete behavior.
- **Skip** skips the main task. Subtasks have no Skip action or independent skipped status. Preserve existing right-swipe navigation to completed/skipped main tasks.
- Skipping or navigating away freezes the main timer and subtask timing. Returning displays the saved state; explicit resume reopens a skipped task without resetting its active subtask.
- Store subtask definitions within the main task and subtask execution state within its run snapshot. Persist active subtask identity, elapsed timing and advancement markers. Do not treat subtasks as additional entries in the run's task sequence.
- Subtask durations partition the main duration. For tasks with subtasks, derive the total duration from positive subtask durations; tasks without subtasks retain existing timed/untimed behavior.
- Preserve the main dial's **time-remaining** behavior. Give each subtask an outer rim of increasing radius, beginning at its proportional start angle and growing according to time actually spent on that subtask. Preserve completed rims and glowing advancement markers.
- Keep rim colors unchanged during overtime. Generate sequential colors from a configurable design-system semantic seed.
- Set the entire dial interior to **Warning** when main elapsed time exceeds the active subtask's cumulative planned end. Early subtask completion carries saved time forward.
- Play a distinct semantic sound when Complete advances a subtask, through existing sound/mute/haptic gates. No automatic subtask advancement.
- Add **Show subtask time remaining** to Settings, default off. When enabled, show the active subtask's allowance minus its active elapsed time; hiding it does not change timing.
- Preserve `stackingAnchorTaskId` independently. It does not automatically become subtask parenthood.

Update `CONTEXT.md`, architecture guidance and regression tests to record these agreed semantics. Keep ADR-0001's pending run-storage migration outside this integration.

## Feature alignment order and orchestration

1. **Subtasks:** adapt the branch's authoring capability into nested subtasks and durable subtask state; replace its independent-step execution assumptions.
2. **Step accordions:** integrate subtask editing into main's existing accordion builder, preserving stable-ID reorder, title editing, duration controls and theme behavior.
3. **Frictionless gestures:** retain the consolidated detector and current intent semantics; resolve dependency configuration without restoring older behavior.
4. **Collapsible nested runner:** render subtask lists within the Run screen and preserve active-subtask visibility/state during collapse.
5. **Next-up preview:** fix the undefined function call and preview main tasks only.
6. **Dynamic progress ring:** extend the existing dial with proportional subtask rims, markers and Warning behavior.
7. **Micro-animations:** coordinate main-task progress and transient feedback with existing presentation. Subtask advancement must not trigger main-task completion/readiness effects.

The primary agent owns dependency decisions, integration commits and full verification. Delegate bounded source tracing and implementation slices with explicit ownership; review every resulting diff. Gesture and accordion alignment may require little new functionality because main already incorporates them.

## Acceptance and verification

- Establish a baseline build before edits. Use the checked-in wrapper and complete **JDK 17**, as enforced by the current helper.
- After each aligned feature, run compilation, JVM tests, lint and assembly before accepting its merge.
- Add focused tests for subtask ordering, duration totals, Complete/Skip behavior, frozen restoration, process recreation, immutable snapshots, advancement markers and once-only feedback.
- Test the example: a 10-minute task with 3- and 7-minute subtasks, advancing subtask 1 at minute 2. The main clock stays unchanged; subtask 2 remains manually active; the interior becomes Warning after main minute 10.
- Verify on device: main-only icons, subtask text/rims, collapse, gestures, bubble navigation, font scaling, animation settings, background/resume and final-task confirmation.
- Preserve old records with backward-compatible defaults: tasks without subtask fields behave unchanged. Test template, active-run and history serialization fixtures.
- Stop on repeated infrastructure failures. A stopped subagent resumes only after primary review supplies a corrected command, following the updated hook rule.
- Finish with a validated integration branch and a per-feature handoff. Merging that result into `main` is a separate action.
