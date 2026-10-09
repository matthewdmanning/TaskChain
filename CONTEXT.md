# Domain context

This is the source of truth for TaskChain's domain vocabulary, behavior invariants, and open decisions. `architecture.md` owns package boundaries and technical architecture; `docs/agents/domain.md` only routes readers to these sources.

- **Routine run:** an instance of the user completing or skipping tasks. Completed runs are persisted locally, like routines. Each routine has at most one Routine run that is not completed.
- **Aborted run:** a Routine run the user ended before finishing. It is kept temporarily and never enters history. Restoring an Aborted run is a possible future feature, not current behavior.
- **Unfinished run:** a Routine run that is neither completed nor aborted.

## Invariants

- A routine has a stable ID and at least one non-blank task before it can run.
- tasks inside a run are domain state, not Android navigation destinations.
- The active run is persisted so screen recreation does not reset progress or timers.
- Editing a routine never changes an existing run because the run owns a snapshot of its tasks.
- Completed or skipped tasks never restart their timer when revisited.
- Explicitly resuming a skipped task reopens it as pending and retains its prior active duration, including when it was paused before being skipped.
- A right swipe returns to the previous task without changing its status; on the first task it does nothing and never requests abort confirmation. A left swipe skips the current task. A double tap on the dial pauses or resumes.
- Timer state is derived from persisted timestamps, not an in-memory counter.
- Completing a skipped task changes it directly to completed.
- Completing the final task ends the run directly when no task remains unfinished. If any tasks remain pending or skipped, finishing requests confirmation and shows the unfinished tasks.
- Confirmation shows unfinished tasks and lets the user jump to one before finalizing.
- Back on the first task requests abort confirmation.
- Aborting keeps the run as an Aborted run and returns Home without creating completed or aborted history or changing the routine's scheduled-completion state. Settings changes are preserved.
- Starting a new Routine run deletes that routine's Aborted or Unfinished run.
- An Aborted or Unfinished run is deleted 18 hours after its last Skip or Complete, or 18 hours after it started if no task was skipped or completed. Opening, closing, pausing, resuming, and aborting do not reset this time.
- Actual duration is distinct from configured duration and is retained per run task.
- A routine's schedule cannot coexist with its deadline or one-time reminder.
- Sound and Vibrate are independently configurable for task-timer and routine-reminder feedback.
- A run snapshots its routine-level Sound and Vibrate gates and semantic sound settings. Routine mute takes precedence over task settings; missing or unplayable audio does not interrupt run behavior or enabled haptics.
- Task nudges use active task time and are suppressed while paused, in confirmation, or outside the foreground Run screen. The default cadence is one minute, including untimed and overtime tasks, without replaying missed background nudges.
- Completion effects delay presentation only; completion timestamps and run transitions are persisted immediately.
- With task transitions enabled, a newly started next task waits for the completion effect and readiness countdown before its active timer begins; the start timestamp is persisted so recreation retains that delay.

## Ordered subtasks within a main task

- A subtask is a piece of its owning main task, with a stable identity, title, and positive duration. Subtask durations partition the main task's total duration; a task without subtasks retains its optional timer.
- A run snapshots subtask definitions and advancement markers inside each main task. Subtasks never become separate run tasks, bottom icons, next-up tasks, or overall-progress entries.
- Complete manually advances the active subtask. The last subtask completes the main task and advances to the next main task. Expiry never advances a subtask.
- Skip skips the main task and retains its active subtask. Navigation, pause, skip, and confirmation freeze subtask timing through the existing persisted main-task clock. Explicit Resume reopens a skipped task without resetting its subtasks.
- Completing a subtask on a revisited skipped task retains its frozen skipped state until Resume or completion of its last subtask.
- Each advancement records cumulative main active time and its wall-clock timestamp. Actual subtask duration is the difference between adjacent advancement markers; the active subtask uses the main clock after the previous marker.
- The main dial keeps showing main time remaining. Subtask rims have increasing radii, proportional start angles, stable sequential colors from the design-system seed, and actual-time sweep that can extend beyond the subtask's planned end. Advancement leaves a glowing marker.
- The entire dial interior becomes Warning when main active time exceeds the active subtask's cumulative planned end. With 3- and 7-minute subtasks and first-subtask advancement at minute 2, the second subtask becomes behind schedule after main minute 10. Rim colors stay unchanged.
- Subtask advancement emits a distinct semantic sound through the existing routine/task sound gates. It does not trigger main-task completion or readiness effects.
- Settings can show active subtask time remaining (allowance minus actual subtask time), default off. Visibility never affects timing.
- The runner shows only the current subtask beneath the main title. Completed subtasks remain represented by durable dial rims and markers.
- Builder drag handles sit at the far left. Collapsed task headers show only a set duration value, without a label. The UI uses controls and visual state rather than instructional prose.
- `stackingAnchorTaskId` remains independent of subtask ownership. The pending per-routine run-storage migration is outside this integration.

## Open decisions

- After completing a previously skipped task, whether to jump to the next unfinished task or continue normal sequence remains TBD.
- Whether starting a routine that has an Unfinished run should prompt to resume instead of replacing it, and whether resuming an Aborted run is needed, waits for more app testing. Until then, a new run replaces the old one without prompting.
