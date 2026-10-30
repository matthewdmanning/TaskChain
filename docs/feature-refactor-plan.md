# Feature ownership refactor

Baseline: `d28a4df`. Branch: `refactor-feature-based-design`.

The supplied [feature-folder draft](../feature-folder-draft.md) defines the target ownership and dependency direction. This implementation preserves the current checkout's behavior, records, resources, and Android identities. It does not implement the pending per-routine storage ADR or add feature switches. The draft's unavailable-feature semantics remain future product requirements. Quick return is absent from this baseline; this refactor does not recreate it or add an empty feature.

## Target and dependencies

One `:app` module contains reusable `core` packages, eight current `features`, and executable root wiring. Root wiring may import both; Core imports no feature or root; features import Core and their own implementation. Feature UI retains Route -> ViewModel -> domain/repository direction. ViewModels remain legitimate coordinators.

| Current source | Destination |
| --- | --- |
| `domain/model` | `core/model` |
| `data` except run recovery | `core/data` |
| `domain/schedule` | `core/schedule` |
| Shared UI controls, sound selection, back preview | `core/ui` |
| `ui/designsystem` and press-feedback wrappers | `core/designsystem` |
| Collection UI and collection-only state from `ui/home` | `core/browsing` |
| Generic tab/route hosting | `core/navigation` |
| Startup ready/retry presentation and initialization host | `core/startup` |
| Reminder port, audio/haptic output, alarm primitive and launch contracts | `core/platform` |
| `ui/builder` | `features/authoring/ui` |
| `ui/settings` | `features/settings/ui` |
| `domain/home`, Today UI/state/actions | `features/scheduling/domain`, `features/scheduling/ui` |
| `domain/run`, run recovery | `features/running/domain`, `features/running` |
| `ui/runner` | `features/running/ui` |
| Run notification/alert adapters and their behavior | `features/notifications` (subsequent extraction; see `notifications-feature-integration.md`) |
| Routine alarm/delivery/rearm/restore behavior | `features/reminders` |
| `domain/progress`, `ui/progress` | `features/progress/domain`, `features/progress/ui` |
| App/Application/Activity construction and concrete route binding | Executable root |

Tests follow the owner of their assertions and the same import rules as main sources: owner-package tests fake a neighbor's injected contract. Root-package system tests may exercise multiple features.

## Mixed declarations and seam contracts

- `HomeShell` receives selected-tab state, a tab-change callback, and composable slots taking `PaddingValues`. It constructs no feature ViewModel or route. Collection state reads definitions/built-ins only. Shared duration formatting belongs to browsing.
- Scheduling owns Today projection, its sampled clock, skip/undo events and reminder effects. It reads shared stores directly; it does not depend on collection or Running ViewModels.
- Running owns pending-start/conflict state and the conflict dialog. Browsing emits selection; root binds it to Running and navigation.
- Authoring, Settings and Progress ViewModels receive their actual repositories, loader and clock/identity functions. Routes receive bounded inputs or ViewModel factories from each feature's entry file (see `architecture.md`, Feature entry files); no feature receives `AppContainer`.
- Running owns run coordination shared by the runner and Android entry points, including the same mutex, revision and visibility state. Common storage never imports the engine.
- Shared output includes `TimerFeedback`, because both routine reminders and Running use it. Common recurrence and reminder ports remain independent of either feature.
- Root supplies launch intents and registers platform entry dependencies. Existing receiver component names, request codes, action strings, extras, channel IDs and notification IDs stay stable so previously created Android operations remain addressable.
- Startup validates shared data and invokes an injected terminal-recovery action before editable content. Running supplies the recovery policy; Core owns only its ordering and ready/retry host.

These are functions, constructor inputs and existing repository interfaces, not a requirement for one interface per row. No forwarding use-case layer, event bus or new framework is needed.

## Delivery slices

1. Record the whole target and exact ownership; retain the supplied draft as baseline evidence.
2. Move coherent packages and matching tests, update imports, and verify mechanical equivalence. Mixed files temporarily retain their behavior until the next slices; this intermediate state is not the final architecture.
3. Independently split UI/collection/Scheduling/Running-entry seams and platform/recovery/run-runtime seams with disjoint file ownership. Root coordinates shared signatures and final assembly.
4. Review actual dependencies, update routing/reference docs, add one executable architecture check, run full verification, and commit the integrated result.

Commits should be self-contained where practical. A designated subagent makes reviewed commits, staging only explicitly assigned paths and preserving unrelated local settings. The primary agent owns integration and broad verification.

## Acceptance evidence

Before source edits, JDK 17.0.12 (including `javac` and `jlink`), the pinned Gradle 9.8.0 cache and published cyberpunkAndroid `v1.0.8` were verified. Baseline `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDeviceTestAndroidTest` passed.

## Implementation and verification results

The implementation retains one `:app` module with eight current features: Authoring, Settings, Scheduling, Running, Reminders, Progress, Keep screen on and Notifications (the last two were extracted after this refactor). Reusable Core owns models, repositories, schedule planning, collection browsing, generic navigation, controls, design-system configuration, startup ordering and platform contracts. Executable root binds concrete routes and dependencies. Scheduling owns Today actions, Running owns start conflicts and run coordination, and Core startup invokes injected recovery. There is no storage migration, feature-availability switch or Quick Return implementation in this change.

`app/src/main/java/com/taskchain/reminder/LegacyReceivers.kt` retains the five historical Android receiver identities: `com.taskchain.reminder.ReminderReceiver`, `com.taskchain.reminder.ReminderActionReceiver`, `com.taskchain.reminder.ReminderRescheduleReceiver`, `com.taskchain.reminder.RunActionReceiver` and `com.taskchain.reminder.RunAlertReceiver`. These adapters delegate to injected entry handlers. Pending-intent action strings, extras, request codes, channels and notification IDs remain unchanged.

`python scripts/check-feature-boundaries.py` runs its own assertions and checks Kotlin package paths, project imports and explicit qualified references in `main`, `test` and `androidTest`. It rejects Core-to-feature/root and feature-to-other-feature/root dependencies (tests included; root-package tests may cross features), root main references to feature `domain`/`platform` packages, project wildcard imports, unsupported legacy packages and cycles between owners. Generated `R` and `BuildConfig` references are shared, except `R` references to feature-owned value resource files, which only their owner may use. The latest run passed for 137 Kotlin files (86 main, 41 test, 10 androidTest) and 18 owners with no cycles. Owners are Core areas or whole features, so this is an owner-level source check: it does not resolve symbols or establish intra-owner layering, callback wiring, reflection or runtime behavior.

| Checkpoint | Evidence |
| --- | --- |
| Baseline | Full JVM tests, lint, debug APK and disposable instrumentation APK build passed before source edits. |
| Mechanical move | Commit `2e9fcf3`; 151 JVM tests and instrumentation Kotlin compilation passed. Body-equivalence checks preserved executable bodies except qualified references, plus comments and literals. |
| Integrated source boundaries | The check passed for 67 main Kotlin files and 16 owners, with no owner cycles, after moving the pure start-policy helper into Running domain. |
| Preserved Android/storage inputs | Manifest, packaged assets, resources and build inputs are byte-equivalent to baseline `d28a4df`. |
| Integrated build | After restoring the original inline permission guard, `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDeviceTest :app:assembleDeviceTestAndroidTest --console=plain` passed under JDK 17, including 153 JVM tests and all three APK builds. |
| Device instrumentation | Pixel 7 / Android 17: 21 tests, 15 passed and 6 failed. An unlocked, untouched `d28a4df` baseline reproduced the same six failing tests. The final refactor rerun with fresh screenshot artifacts reproduced the same assertions and screenshot difference. |

The baseline comparison identifies pre-existing device-test failures; it does not establish a fully passing device suite or end-to-end alarm delivery. Existing failing assertions, app behavior and the screenshot reference were retained. Only the disposable `com.taskchain.testapp` and `com.taskchain.testapp.test` packages were used for installation and fixture cleanup; the regular `com.taskchain` app and data were untouched. Temporary device settings were restored after each completed run.

| Historical device failure | Observed baseline-matched result | Follow-up identified at that checkpoint |
| --- | --- | --- |
| `MainActivityTest.builder_savesRoutineOwnedSettingsAndReturnsToRoutines` | `Scheduled` selector absent. | Review builder completion readiness and the assertion's destination selector. |
| `MainActivityTest.builder_dragHandlesReorderCollapsedAndExpandedTasks` | `CyberAccordionChevron` selector absent. | Review current accordion semantics and the drag test's selectors/readiness. |
| `OpenActiveRunNavigationTest.openActiveRunRequestAfterRecreation_doesNotLeaveDuplicateRunnerEntry` | Ten-second timeout while the active-run navigation lock applies. | Align navigation readiness/assertions with the persisted active-run invariant. |
| `ScreenshotTest.routinesTab_matchesReference` | A focused baseline capture after test-artifact cleanup reproduced exactly 474,182 differing pixels (18.294058%), matching the refactor result. | Review screenshot capture configuration and reference compatibility. |
| `SystemBackNavigationTest.homeSystemBack_leavesTheAppForeground` | Test accesses an already destroyed Activity. | Review lifecycle-safe foreground observation after system Back. |
| `TaskChainComponentTest.weekdaySelector_eachDayCanBeSelected` | `Sun` is offscreen. | Review the test's scrolling/visibility handling for weekday selectors. |

Local XML evidence is saved under `.scratch/feature-refactor-integrated-device-results`, `.scratch/feature-refactor-baseline-device-results` and `.scratch/feature-refactor-baseline-screenshot-result`. These are local verification artifacts, not committed fixtures. Rerun the boundary check if later source edits change ownership.

Cleanup removed five obsolete feature-integration snapshots and moved the integration test from `com.taskchain.ui.TaskChainComponentTest` to `com.taskchain.TaskChainComponentTest`. Earlier reports retain the former name and the baseline-matched 15/21 result as historical evidence; test behavior and expectations are unchanged. After cleanup, `:app:compileDeviceTestAndroidTestKotlin --console=plain` passed under JDK 17.

## Completed device-test follow-up, 2026-10-29

The six historical failures above are resolved. Case-insensitive headings, scrolling to weekday and drag targets, lifecycle-safe Back observation, and pausing the restored run before expecting Home correct the stale test assumptions. The builder save test now exercises the current Sound/Vibrate controls and checks persisted routine and task values. Builder headers omit the collapsed down-chevron and use a larger filled up-chevron when expanded; tests use task headers and the `Collapse task` action rather than library-internal labels.

The user approved the current Routines screenshot. Its reference was refreshed at the recorded font scale of 1.15 without increasing tolerance. The historical difference is accepted as a font-scale change rather than an outstanding app defect.

Final physical Pixel 7 / Android 17 verification passed all 21 device tests, plus a second clean screenshot-only run. All 153 JVM tests passed; lint completed with 0 errors, 111 warnings and 7 hints; feature boundaries and diff whitespace checks passed. This establishes the tested UI and navigation behavior, not end-to-end alarm audibility or reboot/process-death recovery. See the [committed verification record and screenshots](testing/evidence/2026-10-29/README.md).
