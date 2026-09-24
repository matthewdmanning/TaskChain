# TaskChain punch list specification

This file records the product and architecture decisions established in prior design chats. Checkboxes are acceptance criteria, not a claim about current implementation status.

## App shell and navigation

- [ ] Home uses persistent tab navigation with labels **Home**, **Routines**, **Progress**, and **Settings**.
- [ ] Selecting Home displays the Today content.
- [ ] Each tab preserves its state when the user switches tabs.
- [ ] Routine Builder and Routine Runner are deeper routes outside the tab set.
- [ ] A run is one route; individual run steps are domain state, not Android navigation destinations.

## Routine authoring

- [ ] Users can build reusable routines from ordered tasks, habits, and goal-linked steps.
- [ ] A step may have an optional configured duration, deadline, reminder, and explicit habit-stacking anchor.
- [ ] Custom routines and predefined library routines are available from the Routines flow.
- [ ] Editing a routine never changes the snapshot held by a run already in progress or in history.

## Routine runner

- [ ] Starting a routine creates and persists a distinct run snapshot.
- [ ] The runner displays one primary step at a time.
- [ ] Each run step has a durable status: **Pending**, **Completed**, or **Skipped**.
- [ ] Complete records the step as completed and advances normally.
- [ ] Skip records the step as skipped and advances normally.
- [ ] Pressing Complete on a previously skipped step changes it directly to Completed; there is no separate Undo Skip action.
- [ ] An explicit Back control returns to the previous step while preserving all run state.
- [ ] Moving backward or forward never resets, duplicates, or silently recreates run state.
- [ ] The screen is fully driven by authoritative persisted run state.

## First-step back behavior

- [ ] Pressing Back on the first step opens an abort warning instead of exiting silently.
- [ ] The warning has exactly two actions: **Continue Run** and **Abort Run**.
- [ ] Continue Run closes the warning and preserves the active run.
- [ ] Abort Run ends the run as Aborted and exits the Runner.
- [ ] There is no separate Dismiss action.

## Final-step completion behavior

- [ ] Complete or Skip on the final step opens a Confirm Complete dialog instead of finalizing immediately.
- [ ] The dialog clearly lists every unfinished step, including Pending and Skipped steps.
- [ ] Each unfinished step in the dialog is actionable and lets the user jump directly to that step.
- [ ] Jumping to an unfinished step preserves the current run.
- [ ] Continue Run closes the dialog without finalizing.
- [ ] Confirm Complete finalizes and records the run even when unfinished steps remain.

## Timers and completion feedback

- [ ] A timed Pending step starts immediately when it first becomes active through forward navigation.
- [ ] Returning to an already completed or skipped step never restarts its timer or causes timer side effects.
- [ ] A completed step displays its persisted status and actual duration.
- [ ] Timer state is reconstructed from persisted timestamps rather than an in-memory counter.
- [ ] When a countdown reaches zero, audio and haptic feedback fire exactly once.
- [ ] With **Continue timer past zero** enabled, the timer continues in overtime until the step is completed or skipped.
- [ ] Overtime is displayed as elapsed time past zero and is included in actual duration.
- [ ] With **Continue timer past zero** disabled, feedback still fires once and the displayed countdown stops at zero.
- [ ] Actual duration is stored per step and remains distinct from the configured duration.

## Persistence and progress

- [ ] Active run state survives screen recreation and process death.
- [ ] Completed runs retain every step status, including Skipped.
- [ ] Completed and aborted runs record start time, end time, and the per-step actual durations.
- [ ] Completion history is append-only; statistics are derived from events rather than mutable counters.
- [ ] Progress can report completions, duration, adherence, skipped-step rate, and trends from local history.
- [ ] Persistence remains local and file-backed with no database or network requirement.

## Settings and themes

- [ ] Settings exposes **Continue timer past zero**, defaulting to enabled.
- [ ] Settings exposes app-wide theme selection and persists the choice.
- [ ] Theme choices are data-driven.
- [ ] A theme may optionally provide variants; the app must not assume every theme has both Light and Dark variants.
- [ ] UI text, colors, typography, shapes, spacing, dimensions, and motion values come from resources or design-system configuration rather than feature-level literals.

## Scheduling and reminders

- [ ] Scheduling supports one-time, daily, weekday, and selected-day recurrence without coupling the domain model to Android alarms.
- [ ] Routine reminders use local Android scheduling; inexact alarms are the default.
- [ ] Notification permission is requested when the user enables reminders, not automatically at first launch.
- [ ] Reminder notification channels and pending intents follow current Android requirements.

## Architecture boundaries

- [ ] Feature UI follows `Route -> ViewModel -> Screen` and renders state while sending user intent.
- [ ] The Routine Run domain module owns transitions, timing rules, invariants, completion, and abort behavior.
- [ ] ViewModels coordinate screens but do not duplicate domain behavior.
- [ ] Repository interfaces isolate domain and feature code from JSON files, DataStore, Android alarms, and notifications.
- [ ] Feature UI depends on the TaskChain design-system boundary; direct Material configuration remains isolated there.
- [ ] The initial product remains Android-only, offline-first, local-only, database-free, account-free, and telemetry-free.
- [ ] No speculative cloud, synchronization, authentication, or remote-backend implementation is required.

## Open decision

- [ ] **TBD:** After the user completes a previously skipped step, should the runner jump to the next unfinished step or continue the normal sequence?

Until decided, implementations must not treat either behavior as a settled product requirement.
