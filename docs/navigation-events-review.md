# Navigation events review

**Reviewed:** 2026-10-11  
**Source revision:** `918115b` (`architecture-refactor`)  
**Method:** static source review; no code, tests, or device behavior changed or exercised.

## Result

TaskChain currently uses Navigation Compose 2.10.2 with a small `Home → Builder/Runner` graph. Its back-stack ownership and screen callbacks are easy to follow. Navigation 3 is not in use, and the current graph does not need Navigation 3 features such as multiple retained top-level back stacks or adaptive multi-pane destinations. A migration is not recommended from this review alone.

Two areas merit follow-up: custom back handlers do not expose predictive-gesture progress, and the open-active-run launch flag can replay after Activity recreation when a runner entry is restored.

## Action-index freshness

The obsolete action-index snapshots were removed during feature-refactor cleanup; they were not used as evidence for this review. For current ownership and navigation wiring, see [architecture.md](../architecture.md) and [TaskChainApp.kt](../app/src/main/java/com/taskchain/TaskChainApp.kt).

## Event map

| Event | Current behavior | Evidence |
| --- | --- | --- |
| Home tab selection | Changes `selectedTab`, saved with `rememberSaveable`; tabs are not independent navigation destinations or back stacks. | `TaskChainApp.kt:53-64`; `HomeScreen.kt:93-176` |
| Create or edit a routine | Pushes the builder route, optionally with a routine ID. | `TaskChainApp.kt:54-78` |
| Save or close the builder | Pops to Home. Android Back and the in-screen cancel action share the unsaved-change check; discard is confirmed in an `AlertDialog`. | `TaskChainApp.kt:78-85`; `RoutineBuilderRoute.kt:143-146,482-500` |
| Start or resume a run | Pushes the runner route for the routine ID. | `TaskChainApp.kt:60-64,87-96` |
| Android Back in the runner | While active, forwards to the run engine's back transition; if an abort/finish confirmation is open, dismisses it through `continueRun`. Completion hold intentionally consumes Back. | `RoutineRunnerRoute.kt:231-239`; `RoutineRunnerViewModel.kt:95-108` |
| Runner completion | Pops the runner after finish. The runner's visible Back control uses the same runner back intent as Android Back. | `TaskChainApp.kt:87-96`; `RoutineRunnerRoute.kt:169-186` |
| Open the active run from the run notification | The notification PendingIntent sets `EXTRA_OPEN_ACTIVE_RUN`; `MainActivity` passes the flag into `TaskChainApp`, which navigates to the active routine in a `LaunchedEffect`. | `RunNotification.kt` (`openActiveRun`); `MainActivity.kt`; `TaskChainApp.kt:45-50` |
| Back from Home | No app-level handler is installed on Home; the root navigation/activity behavior applies. | `TaskChainApp.kt:54-64` |

## Findings

### P2 — Custom Back handlers do not provide predictive progress

The builder and runner each use `androidx.activity.compose.BackHandler` (`RoutineBuilderRoute.kt:146`; `RoutineRunnerRoute.kt:231`). These handlers intentionally replace ordinary stack-pop behavior with a discard prompt or run transition. They do not observe gesture progress or cancellation, so users cannot preview those custom outcomes while swiping back. The runner handler also consumes Back during the completion hold.

The app already meets the platform setup prerequisite (`compileSdk = 37`, `targetSdk = 36`; `app/build.gradle.kts:22-30`) and uses Navigation Compose 2.10.2 (`gradle/libs.versions.toml:7,24`). Navigation Compose supports predictive back for normal stack pops, but the two custom handlers own these events. Follow the Navigation Event skill for those custom flows: give each active handler its own `NavigationEventState`, preserve its current completion behavior, and use progress/cancellation only for a meaningful preview that resets cleanly when the gesture is cancelled. Keep the default NavHost behavior for routes that do not need custom interception.

**Validation needed:** exercise slow-start, complete, and cancelled edge swipes on the builder and active runner, including an open discard/abort/finish confirmation. Existing instrumentation tests cover builder Back and runner's visible Back button, but not predictive gesture progress or runner hardware/system Back (`MainActivityTest.kt:99-166`).

### P2 — Open-active-run request can replay after Activity recreation

`TaskChainApp` navigates whenever `openActiveRun` is true (`TaskChainApp.kt:46-50`). The value comes directly from the Activity's launch Intent (`MainActivity.kt:27-30`), while `rememberNavController()` restores its navigation state (`TaskChainApp.kt:45`). If the Activity is recreated after the runner entry has been restored, the still-true Intent extra can make the effect push a second runner entry.

Treat this as a one-shot navigation request that is acknowledged/consumed, or check the restored current destination before navigating. Add a recreation or process-restore test that launches from the run notification, restores the Activity, and verifies there is only one runner entry.

The run notification's `PendingIntent` uses `SINGLE_TOP | CLEAR_TOP` (`RunNotification.kt`, `openActiveRun`), but the Activity declares `documentLaunchMode="always"` (`AndroidManifest.xml:15-23`). Android documents `always` as creating a new document task for each launch, so the absence of `onNewIntent` is not itself a confirmed routing defect in this configuration. Recheck that assumption if the document launch mode or Intent flags change.

## Practices that currently fit

- The Activity relies on `ComponentActivity`'s built-in dispatcher owner; it does not create or override a dispatcher (`MainActivity.kt:16-18`).
- There are no nested navigation hosts, tab-owned back stacks, or manually provided dispatcher owners. The standard `AlertDialog` is allowed to use its normal dialog dispatch scope.
- Builder and runner each have one unified handler, so the current code avoids multiple active handlers for one navigation state.
- Navigation 3 is not used. The current app has one root stack and stateful tabs; adopting Navigation 3 only for its back API would add a migration without a demonstrated need.

## References

- [Navigation Event: handle back gestures and predictive animations](https://developer.android.com/guide/navigation/navigation-event/handle-back)
- [Compose predictive back setup and Navigation Compose support](https://developer.android.com/develop/ui/compose/system/predictive-back-setup)
- [Navigation 3 basics](https://developer.android.com/guide/navigation/navigation-3/basics)
- [Navigation 3 migration guide](https://developer.android.com/guide/navigation/navigation-3/migration-guide)
- [Android activity manifest and `documentLaunchMode`](https://developer.android.com/guide/topics/manifest/activity-element.html)
