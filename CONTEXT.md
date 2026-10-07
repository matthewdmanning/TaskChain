# Domain context

This is the source of truth for TaskChain's domain vocabulary, behavior invariants, and open decisions. `architecture.md` owns package boundaries and technical architecture; `docs/agents/domain.md` only routes readers to these sources.

- **Routine run:** an instance of the user completing or skipping steps. Completed runs are persisted locally, like routines. Aborting discards the run as though it never happened.

## Invariants

- A routine has a stable ID and at least one non-blank step before it can run.
- Steps inside a run are domain state, not Android navigation destinations.
- The active run is persisted so screen recreation does not reset progress or timers.
- Editing a routine never changes an existing run because the run owns a snapshot of its steps.
- Completed or skipped steps never restart their timer when revisited.
- Explicitly resuming a skipped step reopens it as pending and retains its prior active duration, including when it was paused before being skipped.
- A right swipe visits the next completed or skipped step without changing its status; when no such step exists, it does nothing.
- Timer state is derived from persisted timestamps, not an in-memory counter.
- Completing a skipped step changes it directly to completed.
- Completing the final step ends the run directly when no step remains unfinished. If any steps remain pending or skipped, finishing requests confirmation and shows the unfinished steps.
- Confirmation shows unfinished steps and lets the user jump to one before finalizing.
- Back on the first step requests abort confirmation.
- Aborting discards the active run and returns Home without creating completed or aborted history or changing the routine's scheduled-completion state. Settings changes are preserved.
- Actual duration is distinct from configured duration and is retained per run step.
- A routine's schedule cannot coexist with its deadline or one-time reminder.
- Sound and Vibrate are independently configurable for step-timer and routine-reminder feedback.
- A run snapshots its routine-level Sound and Vibrate gates and semantic sound settings. Routine mute takes precedence over step settings; missing or unplayable audio does not interrupt run behavior or enabled haptics.
- Task nudges use active task time and are suppressed while paused, in confirmation, or outside the foreground Run screen. The default cadence is one minute, including untimed and overtime tasks, without replaying missed background nudges.
- Completion effects delay presentation only; completion timestamps and run transitions are persisted immediately.
- With task transitions enabled, a newly started next task waits for the completion effect and readiness countdown before its active timer begins; the start timestamp is persisted so recreation retains that delay.

## Ordered cues within a main task

- A cue is a piece of its owning main task, with a stable identity, title, and positive duration. Cue durations partition the main task's total duration; a task without cues retains its optional timer.
- A run snapshots cue definitions and advancement markers inside each main task. Cues never become separate run steps, bottom icons, next-up tasks, or overall-progress entries.
- Complete manually advances the active cue. The last cue completes the main task and advances to the next main task. Expiry never advances a cue.
- Skip skips the main task and retains its active cue. Navigation, pause, skip, and confirmation freeze cue timing through the existing persisted main-task clock. Explicit Resume reopens a skipped task without resetting its cues.
- Completing a cue on a revisited skipped task retains its frozen skipped state until Resume or completion of its last cue.
- Each advancement records cumulative main active time and its wall-clock timestamp. Actual cue duration is the difference between adjacent advancement markers; the active cue uses the main clock after the previous marker.
- The main dial keeps showing main time remaining. Cue rims have increasing radii, proportional start angles, stable sequential colors from the design-system seed, and actual-time sweep that can extend beyond the cue's planned end. Advancement leaves a glowing marker.
- The entire dial interior becomes Warning when main active time exceeds the active cue's cumulative planned end. With 3- and 7-minute cues and first-cue advancement at minute 2, the second cue becomes behind schedule after main minute 10. Rim colors stay unchanged.
- Cue advancement emits a distinct semantic sound through the existing routine/task sound gates. It does not trigger main-task completion or readiness effects.
- Settings can show active substep time remaining (allowance minus actual cue time), default off. Visibility never affects timing.
- `stackingAnchorStepId` remains independent of cue ownership. The pending per-routine run-storage migration is outside this integration.

## Open decisions

- After completing a previously skipped step, whether to jump to the next unfinished step or continue normal sequence remains TBD.
