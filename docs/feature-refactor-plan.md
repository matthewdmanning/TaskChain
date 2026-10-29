# Feature ownership refactor

Baseline: `d28a4df`. Branch: `refactor-feature-based-design`.

The supplied [feature-folder draft](../feature-folder-draft.md) defines the target ownership and dependency direction. This implementation preserves the current checkout's behavior, records, resources, and Android identities. It does not implement the pending per-routine storage ADR or add feature switches. The draft's unavailable-feature semantics remain future product requirements. Quick return is absent from this baseline; this refactor does not recreate it or add an empty feature.

## Target and dependencies

One `:app` module contains reusable `core` packages, six current `features`, and executable root wiring. Root wiring may import both; Core imports no feature or root; features import Core and their own implementation. Feature UI retains Route -> ViewModel -> domain/repository direction. ViewModels remain legitimate coordinators.

| Current source | Destination |
| --- | --- |
| `domain/model` | `core/model` |
| `data` except run recovery | `core/data` |
| `domain/schedule` | `core/schedule` |
| Shared UI controls, feedback, sound selection | `core/ui` |
| `ui/designsystem` | `core/designsystem` |
| Collection UI and collection-only state from `ui/home` | `core/browsing` |
| Generic tab/route hosting | `core/navigation` |
| Startup ready/retry presentation and initialization host | `core/startup` |
| Reminder port, audio/haptic output, alarm primitive and launch contracts | `core/platform` |
| `ui/builder` | `features/authoring/ui` |
| `ui/settings` | `features/settings/ui` |
| `domain/home`, Today UI/state/actions | `features/scheduling/domain`, `features/scheduling/ui` |
| `domain/run`, run recovery | `features/execution/domain`, `features/execution` |
| `ui/runner` | `features/execution/ui` |
| Run notification/alert adapters and their behavior | `features/execution/platform` |
| Routine alarm/delivery/rearm/restore behavior | `features/reminders` |
| `domain/progress`, `ui/progress` | `features/progress/domain`, `features/progress/ui` |
| App/Application/Activity construction and concrete route binding | Executable root |

Tests follow the owner of their assertions. Root integration tests may exercise multiple features.

## Mixed declarations and seam contracts

- `HomeShell` receives selected-tab state, a tab-change callback, and composable slots taking `PaddingValues`. It constructs no feature ViewModel or route. Collection state reads definitions/built-ins only. Shared duration formatting belongs to browsing.
- Scheduling owns Today projection, its sampled clock, skip/undo events and reminder effects. It reads shared stores directly; it does not depend on collection or execution ViewModels.
- Execution owns pending-start/conflict state and the conflict dialog. Browsing emits selection; root binds it to Execution and navigation.
- Authoring, Settings and Progress ViewModels receive their actual repositories, loader and clock/identity functions. Routes receive bounded inputs or ViewModel factories; no feature receives `AppContainer`.
- Execution owns run coordination shared by the runner and Android entry points, including the same mutex, revision and visibility state. Common storage never imports the engine.
- Shared output includes `TimerFeedback`, because both routine reminders and Execution use it. Common recurrence and reminder ports remain independent of either feature.
- Root supplies launch intents and registers platform entry dependencies. Existing receiver component names, request codes, action strings, extras, channel IDs and notification IDs stay stable so previously created Android operations remain addressable.
- Startup validates shared data and invokes an injected terminal-recovery action before editable content. Execution supplies the recovery policy; Core owns only its ordering and ready/retry host.

These are functions, constructor inputs and existing repository interfaces, not a requirement for one interface per row. No forwarding use-case layer, event bus or new framework is needed.

## Delivery slices

1. Record the whole target and exact ownership; retain the supplied draft as baseline evidence.
2. Move coherent packages and matching tests, update imports, and verify mechanical equivalence. Mixed files temporarily retain their behavior until the next slices; this intermediate state is not the final architecture.
3. Independently split UI/collection/Scheduling/Execution-entry seams and platform/recovery/run-runtime seams with disjoint file ownership. Root coordinates shared signatures and final assembly.
4. Review actual dependencies, update routing/reference docs, add one executable architecture check, run full verification, and commit the integrated result.

Commits should be self-contained where practical. A designated subagent makes reviewed commits, staging only explicitly assigned paths and preserving unrelated local settings. The primary agent owns integration and broad verification.

## Acceptance evidence

Before source edits, JDK 17.0.12 (including `javac` and `jlink`), the pinned Gradle 9.8.0 cache and published cyberpunkAndroid `v1.0.8` were verified. Baseline `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDeviceTestAndroidTest` passed.

Final checks: executable source-boundary/cycle check; JDK 17 JVM tests; Android lint; debug APK; disposable device-test APK and instrumentation APK compilation. Add focused checks for newly exposed entry/startup seams without changing engine or persistence behavior. Device tests, when a device is available, use only `com.taskchain.testapp`; inspect APK IDs before installation. Builds do not establish device UX or background delivery behavior.
