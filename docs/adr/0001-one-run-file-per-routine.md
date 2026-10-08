---
status: accepted (not yet implemented)
---

# One run file per routine, with a stored expiry timestamp

The rules for Aborted and Unfinished runs in `CONTEXT.md` (one per routine, replaced on a new start, deleted after 18 hours) replace the single global `sessions/active-session.json`. We store each routine's non-completed Routine run at `sessions/<routineId>.json`, so the file name enforces one run per routine and a new start is a plain overwrite. An Aborted run stays in the same file with `status = ABORTED`; a completed run is appended to history and its file is deleted.

`RoutineRun` gets an explicit `lastStepChangeAtEpochMillis`, set by `RoutineRunEngine.start` and updated only in the Skip/Complete transition. The run repository treats an expired file as absent and deletes it when it reads it, so expiry needs no background job.

## Considered options

- **Derive the expiry time from step timestamps.** Rejected: `resumeCurrent` clears `finishedAtEpochMillis` and `completedAtEpochMillis` when a skipped step is reopened, so a derived value can move backward and expire a run early.
- **Scheduled cleanup (WorkManager or alarms).** Rejected: deleting on read keeps the rule in one testable module with an injected clock, and an expired file that is never read costs only a few kilobytes.

## Consequences

- Home, the run bubble, and run nudges currently assume a single run. Showing more than one routine's run at a time needs its own decision.
- The existing `active-session.json` must be migrated into the matching routine's file on first launch.
