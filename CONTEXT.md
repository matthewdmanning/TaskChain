# Domain context

- **Routine run:** an instance of the user completing or skipping steps. Completed runs are persisted locally, like routines. Aborting discards the run as though it never happened.

## Invariants

- A routine has a stable ID and at least one non-blank step before it can run.
- Completed or skipped steps never restart their timer when revisited.
- Timer state is derived from persisted timestamps, not an in-memory counter.
- Completing a skipped step changes it directly to completed.
- Completing the final step ends the run directly when no step remains unfinished. If any steps remain pending or skipped, finishing requests confirmation and shows the unfinished steps.
- Confirmation shows unfinished steps and lets the user jump to one before finalizing.
- Back on the first step requests abort confirmation.
- Aborting discards the active run and returns Home without creating completed or aborted history or changing the routine's scheduled-completion state. Settings changes are preserved.
- Actual duration is distinct from configured duration and is retained per run step.
- A routine's schedule cannot coexist with its deadline or one-time reminder.
- Sound and Vibrate are independently configurable for step-timer and routine-reminder feedback.

## Open decision

- After completing a previously skipped step, whether to jump to the next unfinished step or continue normal sequence remains TBD.
