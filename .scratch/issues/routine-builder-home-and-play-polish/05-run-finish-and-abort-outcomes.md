# 05 — Make run finish and abort outcomes match intent

**What to build:** When every step is completed, finish the run without a confirmation dialog and show a brief graphic reaction at the successful completion point. Keep the existing review and confirmation when any steps remain unfinished. Aborting means the run never happened: discard the active run and return Home without writing completed or aborted history or changing scheduled completion state. Preserve Settings changes. After abort, another run can be started.

**Blocked by:** None.

**Status:** ready-for-agent

- [ ] Completing the last remaining step finishes the run directly, without a confirmation dialog.
- [ ] A graphic reaction appears only after a successful clean completion and does not block continuing to Home.
- [ ] If any steps remain pending or skipped, the existing unfinished-step review and confirmation remain available.
- [ ] Abort returns to Home and removes the active run without recording a completion or aborted run.
- [ ] Aborting a scheduled routine does not mark it completed or change its schedule.
- [ ] A new routine run can be started after abort.
- [ ] Settings changes are preserved when a run is aborted.
