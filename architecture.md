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
- `domain/run` owns run transitions, timing, skip/complete rules, confirmation, feedback eligibility, and invariants.
- `data` exposes repository interfaces and local JSON/DataStore implementations.
- `reminder` adapts domain reminder requests to `AlarmManager` and notifications, and resolves semantic run feedback to local Android audio and haptic output.
- `ui/designsystem` is the only UI package that directly configures Material 3.
- `ui/<feature>` follows `Route -> ViewModel -> Screen`; screens render state and emit intent.

## MVVM ownership

The feature ViewModel is the cornerstone of frontend architecture. Routes construct and observe it, screens render its state and emit user intent, and the ViewModel coordinates domain behavior, repository interfaces, and lifecycle-aware UI state. Direct ViewModel calls into domain modules and repository interfaces are expected; they are not, by themselves, evidence that another orchestration seam is needed.

Introduce a deeper module only when behavior is reused outside one ViewModel, expresses a domain invariant, or is duplicated or difficult to verify through the ViewModel. Do not add use-case or lifecycle pass-through modules solely to move coordination out of a ViewModel.

## Reminder entry points

Feature ViewModels initiate Reminder scheduling and cancellation in response to routine authoring and Routine run outcomes. Android broadcast receivers handle background delivery and rearming because those entry points exist outside the frontend lifecycle and cannot route through a ViewModel.

Both entry points reuse the same platform-neutral recurrence and Reminder mapping policy. Keep recurrence calculations in `domain/schedule` and Android intent, `AlarmManager`, notification, audio, and haptic behavior in `reminder`. Do not add another interface around Reminder delivery unless behavior genuinely varies across a second adapter.

The initial app is Android-only, offline-only, and database-free. Repository interfaces keep the file format and Android APIs outside domain behavior without adding speculative cloud, sync, authentication, or backup implementations.

## UI library dependency

cyberpunkAndroid supplies UI components, theme tokens, and effects through the published `com.github.matthewdmanning:cyberpunkAndroid:v1.0.8` dependency. TaskChain has no `third_party` source checkout, submodule, or Gradle project inclusion for it.

## Runner feature composition

The Run route owns ViewModel creation, lifecycle-aware observation, navigation, and completion/readiness presentation. `RoutineRunnerScreen` receives immutable state, derived timer values, and callbacks; nested cues and runner visuals compose within that screen. No runner feature observes repository emissions to infer navigation or starts an independent polling clock.

`RoutineRunEngine` owns cue advancement, frozen elapsed time, and behind-schedule policy. Cue definitions live inside `RoutineStep`; persisted active identity and advancement markers live inside `RoutineRunStep`. All run sequence indexes and completion/progress summaries refer to main tasks. The existing ViewModel persists transitions before semantic feedback dispatch, including `CueAdvanced`.

The Screen displays only the current cue. Cue rims, next-up main tasks, segmented main progress and transient feedback use that same run snapshot and sampled time. Gesture detection and drag feedback stay in Compose; they emit existing ViewModel intents and preserve accessibility actions. Font-sensitive action rows wrap, and shared typography is configured at the theme boundary.
