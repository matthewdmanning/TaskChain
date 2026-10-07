# Integrate the feature branches through MVVM alignment

## Subsequent user refinements (2026-10-07)

These override the earlier cue-list presentation and builder controls: show only the current cue; remove instructional prose; reorder cues by dragging with handles at the far left; put main-task handles at the far left too; show collapsed duration values without "Duration:" and hide unset durations. Increase other text sizes while preserving timer, routine-name and Home category-header sizes.

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

## Agreed task and cue behavior

These decisions supersede the earlier proposal that secondary steps execute as independent tasks:

- A main task stays active while its **ordered cues** progress. Cues appear beneath its title in larger text; only main tasks appear as bottom icons, next-up tasks and overall task-progress entries.
- **Complete** advances the active cue. Completing the last cue completes the main task and advances to the next main task. Tasks without cues retain current Complete behavior.
- **Skip** skips the main task. Cues have no Skip action or independent skipped status. Preserve existing right-swipe navigation to completed/skipped main tasks.
- Skipping or navigating away freezes the main timer and cue timing. Returning displays the saved state; explicit resume reopens a skipped task without resetting its active cue.
- Store cue definitions within the main task and cue execution state within its run snapshot. Persist active cue identity, elapsed timing and advancement markers. Do not treat cues as additional entries in the run's task sequence.
- Cue durations partition the main duration. For tasks with cues, derive the total duration from positive cue durations; tasks without cues retain existing timed/untimed behavior.
- Preserve the main dial's **time-remaining** behavior. Give each cue an outer rim of increasing radius, beginning at its proportional start angle and growing according to time actually spent on that cue. Preserve completed rims and glowing advancement markers.
- Keep rim colors unchanged during overtime. Generate sequential colors from a configurable design-system semantic seed.
- Set the entire dial interior to **Warning** when main elapsed time exceeds the active cue's cumulative planned end. Early cue completion carries saved time forward.
- Play a distinct semantic sound when Complete advances a cue, through existing sound/mute/haptic gates. No automatic cue advancement.
- Add **Show substep time remaining** to Settings, default off. When enabled, show the active cue's allowance minus its active elapsed time; hiding it does not change timing.
- Preserve `stackingAnchorStepId` independently. It does not automatically become cue parenthood.

Update `CONTEXT.md`, architecture guidance and regression tests to record these agreed semantics. Keep ADR-0001's pending run-storage migration outside this integration.

## Feature alignment order and orchestration

1. **Substeps:** adapt the branch's authoring capability into nested cues and durable cue state; replace its independent-step execution assumptions.
2. **Step accordions:** integrate cue editing into main's existing accordion builder, preserving stable-ID reorder, title editing, duration controls and theme behavior.
3. **Frictionless gestures:** retain the consolidated detector and current intent semantics; resolve dependency configuration without restoring older behavior.
4. **Collapsible nested runner:** render cue lists within the Run screen and preserve active-cue visibility/state during collapse.
5. **Next-up preview:** fix the undefined function call and preview main tasks only.
6. **Dynamic progress ring:** extend the existing dial with proportional cue rims, markers and Warning behavior.
7. **Micro-animations:** coordinate main-task progress and transient feedback with existing presentation. Cue advancement must not trigger main-task completion/readiness effects.

The primary agent owns dependency decisions, integration commits and full verification. Delegate bounded source tracing and implementation slices with explicit ownership; review every resulting diff. Gesture and accordion alignment may require little new functionality because main already incorporates them.

## Acceptance and verification

- Establish a baseline build before edits. Use the checked-in wrapper and complete **JDK 17**, as enforced by the current helper.
- After each aligned feature, run compilation, JVM tests, lint and assembly before accepting its merge.
- Add focused tests for cue ordering, duration totals, Complete/Skip behavior, frozen restoration, process recreation, immutable snapshots, advancement markers and once-only feedback.
- Test the example: a 10-minute task with 3- and 7-minute cues, advancing cue 1 at minute 2. The main clock stays unchanged; cue 2 remains manually active; the interior becomes Warning after main minute 10.
- Verify on device: main-only icons, cue text/rims, collapse, gestures, bubble navigation, font scaling, animation settings, background/resume and final-task confirmation.
- Preserve old records with backward-compatible defaults: tasks without cue fields behave unchanged. Test template, active-run and history serialization fixtures.
- Stop on repeated infrastructure failures. A stopped subagent resumes only after primary review supplies a corrected command, following the updated hook rule.
- Finish with a validated integration branch and a per-feature handoff. Merging that result into `main` is a separate action.
