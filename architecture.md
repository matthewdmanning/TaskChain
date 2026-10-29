# TaskChain architecture

TaskChain uses one Android application module with feature ownership and acyclic source dependencies. The [implementation plan](docs/feature-refactor-plan.md) maps the [supplied draft](feature-folder-draft.md) to the current checkout; the target below guides the restructure.

```text
Executable root -> features -> reusable Core
       |                         ^
       +-------------------------+

Inside a feature: Route -> ViewModel -> domain rules / Core repositories
```

- `core/model` defines routines, immutable run snapshots, events, preferences, sound settings and shared validity.
- `core/data` owns repository contracts, built-in loading and local JSON/DataStore durability. It imports no execution engine.
- `core/schedule` owns recurrence and shared reminder planning.
- `core/browsing`, `core/navigation`, `core/ui` and `core/designsystem` own collection display, injected-content hosting, shared controls and provider/theme configuration. Core constructs no feature routes or ViewModels.
- `core/platform` owns reminder/output ports, shared audio/haptic output and platform contracts. `core/startup` validates data and invokes injected recovery.
- `features/authoring`, `features/settings`, `features/scheduling`, `features/execution`, `features/reminders` and `features/progress` own current user workflows. Feature internals retain UI, pure domain and Android adapter separation.
- Executable root wiring constructs both features and Core. Neither features nor reusable Core imports `AppContainer`, `MainActivity`, `TaskChainApplication` or concrete root route binding.

Collection browsing belongs to Core; occurrence projection/skip/undo belong to Scheduling. Execution owns start conflicts and all run transitions. Settings edits preferences while consumers apply them; Progress reads shared history while other features retain their writers. Features receive bounded dependencies rather than the root container.

Quick return is absent from the current baseline and is not recreated. This change adds no feature switches, Gradle modules or storage migration. Root Android receiver adapters retain their historical component names for pending-intent compatibility; root supplies launch intents through Core contracts. Action strings, extras, request/channel/notification IDs remain stable.

## MVVM ownership

The feature ViewModel is the cornerstone of frontend architecture. Routes construct and observe it, screens render its state and emit user intent, and the ViewModel coordinates domain behavior, repository interfaces, and lifecycle-aware UI state. Direct ViewModel calls into domain modules and repository interfaces are expected; they are not, by themselves, evidence that another orchestration seam is needed.

Introduce a deeper module only when behavior is reused outside one ViewModel, expresses a domain invariant, or is duplicated or difficult to verify through the ViewModel. Do not add use-case or lifecycle pass-through modules solely to move coordination out of a ViewModel.

## Reminder entry points

Feature ViewModels initiate Reminder scheduling and cancellation in response to routine authoring and Routine run outcomes. Android broadcast receivers handle background delivery and rearming because those entry points exist outside the frontend lifecycle and cannot route through a ViewModel.

Both entry points reuse the same platform-neutral recurrence and Reminder mapping policy. Keep recurrence calculations in `core/schedule` and Android intent, `AlarmManager`, notification, audio, and haptic behavior in `features/reminders` and shared output in `core/platform`. Do not add another interface around Reminder delivery unless behavior genuinely varies across a second adapter.

The ongoing run notification follows the same split. `features/execution/domain/RunNotificationPolicy` derives its content and applies Skip and Complete through `RoutineRunEngine`. `features/execution/platform/RunNotification` maps that content to Android, and `RunActionReceiver` applies a button press to the persisted run. The receiver and the runner ViewModel share one Execution-owned run mutex; the receiver increments the shared run revision after saving, and a live ViewModel adopts the saved run before its next read. The ViewModel never infers navigation from this signal. Routine Reminder notifications carry Snooze and Dismiss through `ReminderActionReceiver`, which uses the same recurrence mapping as delivery.

Run alerts follow the same split. `features/execution/domain/RunAlertPolicy` decides when the next nudge or timer expiry is due, which alerts a firing delivers, and when nudges stop. `features/execution/platform/RunAlerts` keeps one exact alarm for that time only while the Run screen is hidden (shared runner visibility), and `RunAlertReceiver` plays what is due under the shared run mutex. The runner ViewModel owns the same alerts while the screen is visible, so each alert plays once.

The initial app is Android-only, offline-only, and database-free. Repository interfaces keep the file format and Android APIs outside domain behavior without adding speculative cloud, sync, authentication, or backup implementations.

## Local data durability

JSON repositories use versioned records, coordinated file access and atomic replacement.
Preferences use one process-wide DataStore delegate. Startup validates saved data before
exposing editable screens and offers Retry on failure. Instrumentation uses a separate
`com.taskchain.testapp` sandbox. See [On-device data lifecycle](docs/on-device-data.md)
for schema migration, update/reboot behavior, snapshots and testing commands.

## UI library dependency

cyberpunkAndroid supplies UI components, theme tokens, and effects through the published `com.github.matthewdmanning:cyberpunkAndroid:v1.0.8` dependency. TaskChain has no `third_party` source checkout, submodule, or Gradle project inclusion for it.

## Runner feature composition

The Run route owns ViewModel creation, lifecycle-aware observation, navigation, and completion/readiness presentation. `RoutineRunnerScreen` receives immutable state, derived timer values, and callbacks; nested subtasks and runner visuals compose within that screen. No runner feature observes repository emissions to infer navigation or starts an independent polling clock.

`RoutineRunEngine` owns subtask advancement, frozen elapsed time, and behind-schedule policy. Subtask definitions live inside `RoutineTask`; persisted active identity and advancement markers live inside `RoutineRunTask`. All run sequence indexes and completion/progress summaries refer to main tasks. The existing ViewModel persists transitions before semantic feedback dispatch, including `subtaskAdvanced`.

The Screen displays only the current subtask. Subtask rims, next-up main tasks, segmented main progress and transient feedback use that same run snapshot and sampled time. Gesture detection and drag feedback stay in Compose (`features/execution/ui/RunnerGestures.kt` for detection and state, `features/execution/ui/RunnerGestureCues.kt` for visuals); they emit existing ViewModel intents and preserve accessibility actions. Gesture distances and glow radii are screen-relative semantic tokens in `core/designsystem/RunnerGestureTokens.kt`, not borrowed library sizes. Font-sensitive action rows wrap, and shared typography is configured at the theme boundary.
