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
- A right swipe returns to the previous task without changing its status; on the first task it does nothing and never requests abort confirmation. A left swipe skips the current task.
- Double tap on the dial pauses or resumes the displayed task. A Pause or Play icon flashes at half-screen size to confirm the new state; single taps still reach the controls underneath.
- Timer state is derived from persisted timestamps, not an in-memory counter.
- Completing a skipped task changes it directly to completed.
- Completing the final task ends the run directly when no task remains unfinished. If any tasks remain pending or skipped, finishing requests confirmation and shows the unfinished tasks.
- Confirmation shows unfinished tasks and lets the user jump to one before finalizing.
- Back on the first task requests abort confirmation.
- No flow leaves a running run for another screen. While the run is active and its current task is pending and unpaused, or a confirmation is open, the app reopens the Run screen. Pausing, completing, or aborting lifts this.
- Aborting keeps the run as an Aborted run and returns Home without creating completed or aborted history or changing the routine's scheduled-completion state. Settings changes are preserved.
- Starting a new Routine run deletes that routine's Aborted or Unfinished run.
- An Aborted or Unfinished run is deleted 18 hours after its last Skip or Complete, or 18 hours after it started if no task was skipped or completed. Opening, closing, pausing, resuming, and aborting do not reset this time.
- Actual duration is distinct from configured duration and is retained per run task.
- A routine's schedule cannot coexist with its deadline or one-time reminder.
- Sound and Vibrate are independently configurable for task-timer and routine-reminder feedback.
- Skipping a scheduled routine moves its Home card to Completed with a Skipped flag and its reminder to the next recurrence; it adds no progress counts. Pressing the flag offers to undo the skip.
- Completed or skipped cards return to Scheduled at the day start (`home_day_start_hour` in `integers.xml`, default 2 AM local) of the day of their next occurrence.
- Repeated reminders stop two hours after the scheduled time.
- A delivered routine reminder stays in the notification shade, with the launcher dot, until the user dismisses it, snoozes it, or starts the routine. Dismiss ends that cycle's repeats and keeps the next regular occurrence. Snooze removes the notification and delivers the same reminder again after `reminder_snooze_minutes` (`integers.xml`, default 10); delivery after a snooze resumes repeats inside the two-hour window.
- While a run is active, one ongoing notification shows the active subtask's name and time left, or the main task's when no subtask is active. An untimed task counts up and a paused task shows its frozen time. Subtask time shows there regardless of the Settings subtask-time switch.
- The run notification's Skip and Complete apply the same engine transitions as the Run screen to the persisted run, without opening the app, and ignore a press built for an earlier step. On the last main task, and while a finish or abort confirmation is open, both buttons open the Run screen instead, because ending the run needs its dialog.
- The readiness countdown gives one short haptic pulse at each of 3, 2, and 1 when the routine and upcoming task allow vibration and haptic intensity is above zero. Get Ready does not pulse.
- A run snapshots its routine-level Sound and Vibrate gates and semantic sound settings. Routine mute takes precedence over task settings; missing or unplayable audio does not interrupt run behavior or enabled haptics.
- Task nudges use active task time and are suppressed while paused or in confirmation. The default cadence is one minute, including untimed and overtime tasks. With the Run screen visible the runner plays them; otherwise (app in background, screen off or locked) an exact alarm plays them and timer expiry. Missed nudges are never replayed on return.
- Nudges stop once the reminder limit has passed since the latest navigation, pause, resume, skip, or complete (or since the run started). The limit is the sum of all configured task times, fixed for the run; time remaining or passed in tasks does not change it. A run with no timed task uses 15 minutes (`run_untimed_reminder_limit_minutes`, `integers.xml`).
- A task can carry its own sound (`RoutineTask.soundUri`, a device sound). It replaces the nudge and timer-expiry sounds for that task and keeps the routine and task Sound gates. Settings can pick a default device sound per semantic token; a run applies it to every token the routine did not customize. No sound file is bundled, so an unset token plays the built-in tone, or nothing for tokens without one.
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
- Collapsed Builder task headers have no down-chevron; pressing the header expands the task. Expanded headers show a large filled up-chevron that collapses the task.
- `stackingAnchorTaskId` remains independent of subtask ownership. The pending per-routine run-storage migration is outside this integration.

## Open decisions

- After completing a previously skipped task, whether to jump to the next unfinished task or continue normal sequence remains TBD.
- Whether starting a routine that has an Unfinished run should prompt to resume instead of replacing it, and whether resuming an Aborted run is needed, waits for more app testing. Until then, a new run replaces the old one without prompting.
