# Domain context

This is the source of truth for TaskChain's domain vocabulary, behavior invariants, and open decisions. `architecture.md` owns package boundaries and technical architecture; `docs/agents/domain.md` only routes readers to these sources.

- **Routine run:** an instance of the user completing or skipping steps. Completed runs are persisted locally, like routines. Each routine has at most one Routine run that is not completed.
- **Aborted run:** a Routine run the user ended before finishing. It is kept temporarily and never enters history. Restoring an Aborted run is a possible future feature, not current behavior.
- **Unfinished run:** a Routine run that is neither completed nor aborted.

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
- Aborting keeps the run as an Aborted run and returns Home without creating completed or aborted history or changing the routine's scheduled-completion state. Settings changes are preserved.
- Starting a new Routine run deletes that routine's Aborted or Unfinished run.
- An Aborted or Unfinished run is deleted 18 hours after its last Skip or Complete, or 18 hours after it started if no step was skipped or completed. Opening, closing, pausing, resuming, and aborting do not reset this time.
- Actual duration is distinct from configured duration and is retained per run step.
- A routine's schedule cannot coexist with its deadline or one-time reminder.
- Sound and Vibrate are independently configurable for step-timer and routine-reminder feedback.
- A run snapshots its routine-level Sound and Vibrate gates and semantic sound settings. Routine mute takes precedence over step settings; missing or unplayable audio does not interrupt run behavior or enabled haptics.
- Task nudges use active task time and are suppressed while paused, in confirmation, or outside the foreground Run screen. The default cadence is one minute, including untimed and overtime tasks, without replaying missed background nudges.
- Completion effects delay presentation only; completion timestamps and run transitions are persisted immediately.
- With task transitions enabled, a newly started next task waits for the completion effect and readiness countdown before its active timer begins; the start timestamp is persisted so recreation retains that delay.

## Open decision

- After completing a previously skipped step, whether to jump to the next unfinished step or continue normal sequence remains TBD.
- Whether starting a routine that has an Unfinished run should prompt to resume instead of replacing it, and whether resuming an Aborted run is needed, waits for more app testing. Until then, a new run replaces the old one without prompting.
