# TaskChain architecture

TaskChain uses one Android application module with strict package boundaries:

```text
UI routes/screens -> feature ViewModels -> domain services -> repository interfaces
                                              ^                    |
                                              |                    v
                                      Android adapters <- file/DataStore storage
```

- `domain/model` defines routines, immutable run snapshots, completion events, routine scheduling and reminder settings, and step timer settings.
- `domain/schedule` owns platform-neutral recurrence and next-trigger calculation.
- `domain/run` owns run transitions, timing, skip/complete rules, confirmation, and invariants.
- `data` exposes repository interfaces and local JSON/DataStore implementations.
- `reminder` adapts domain reminder requests to `AlarmManager` and notifications.
- `ui/designsystem` is the only UI package that directly configures Material 3.
- `ui/<feature>` follows `Route -> ViewModel -> Screen`; screens render state and emit intent.

## MVVM ownership

The feature ViewModel is the cornerstone of frontend architecture. Routes construct and observe it, screens render its state and emit user intent, and the ViewModel coordinates domain behavior, repository interfaces, and lifecycle-aware UI state. Direct ViewModel calls into domain modules and repository interfaces are expected; they are not, by themselves, evidence that another orchestration seam is needed.

Introduce a deeper module only when behavior is reused outside one ViewModel, expresses a domain invariant, or is duplicated or difficult to verify through the ViewModel. Do not add use-case or lifecycle pass-through modules solely to move coordination out of a ViewModel.

## Reminder entry points

Feature ViewModels initiate Reminder scheduling and cancellation in response to routine authoring and Routine run outcomes. Android broadcast receivers handle background delivery and rearming because those entry points exist outside the frontend lifecycle and cannot route through a ViewModel.

Both entry points reuse the same platform-neutral recurrence and Reminder mapping policy. Keep recurrence calculations in `domain/schedule` and Android intent, `AlarmManager`, notification, audio, and haptic behavior in `reminder`. Do not add another interface around Reminder delivery unless behavior genuinely varies across a second adapter.

Steps inside a run are domain state, not Android navigation destinations. The active run is persisted so screen recreation does not reset progress or timers. Editing a routine never changes an existing run because a run owns a snapshot of its steps.

The initial app is Android-only, offline-only, and database-free. Repository interfaces keep the file format and Android APIs outside domain behavior without adding speculative cloud, sync, authentication, or backup implementations.
