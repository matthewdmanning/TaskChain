# 2026-10-06 punchlist verification map

This note maps implementation scope and records the root agent's build, test, review, and installation evidence below. Full visual and interaction acceptance remains pending; source review alone does not prove an item complete.

| Item | Source-review coverage | Evidence status |
| --- | --- | --- |
| 1 | `ui/TaskChainApp.kt`, `ui/designsystem/RunnerMotion.kt` radial sweep and tick timing | Source reviewed; runtime and visual proof pending |
| 2 | `ui/TaskChainApp.kt`, `ui/designsystem/TaskChainTheme.kt` timer/error color tokens | Source reviewed; visual proof pending |
| 3 | `ui/TaskChainApp.kt` dial tick and rim dimensions | Source reviewed; visual proof pending |
| 4 | `ui/TaskChainApp.kt` progression and completed-rim color state | Source reviewed; visual proof pending |
| 5 | `ui/TaskChainApp.kt`, `ui/designsystem/RunnerMotion.kt` pause pulse timing | Source reviewed; timing proof pending |
| 6 | `ui/TaskChainApp.kt` sweep and pulse clipping bounds | Source reviewed; visual proof pending |
| 7 | `domain/run/RoutineRunEngine.kt`, `ui/FeatureViewModels.kt` skipped-step resume path | Source reviewed; focused regression test and runtime proof pending |
| 8 | `ui/RunnerGestures.kt`, `ui/FeatureViewModels.kt` right-swipe advance behavior | Source reviewed; gesture test/device proof pending |
| 9 | `ui/TaskChainApp.kt`, `ui/designsystem/RunnerMotion.kt` runner timer typography | Source reviewed; visual proof pending |
| 10 | `ui/TaskChainApp.kt`, `ui/designsystem/TaskChainTheme.kt` abort dialog hierarchy and error action styling | Source reviewed; visual proof pending |
| 11 | `reminder/TaskFeedback.kt`, `ui/TaskChainApp.kt` button-press haptics seam | Source reviewed; device haptic proof pending |
| 12 | `ui/TaskChainApp.kt` routine-builder accordion hit targets and edit affordance | Source reviewed; Compose/device proof pending |
| 13 | `ui/TaskChainApp.kt` duration display icon/value | Source reviewed; visual proof pending |
| 14 | `ui/TaskChainApp.kt` duration editor interaction | Source reviewed; Compose/device proof pending |
| 15 | `ui/TaskChainApp.kt` collapsed/expanded duration placement | Source reviewed; visual proof pending |
| 16 | `ui/TaskChainApp.kt` routines-tab new-routine placement | Source reviewed; visual proof pending |
| 17 | `ui/TaskChainApp.kt`, `ui/designsystem/TaskChainTheme.kt` page/header semantic sizing | Source reviewed; visual proof pending |
| 18 | `ui/TaskChainApp.kt`, `res/values/strings.xml` routines header removal | Source reviewed; UI proof pending |
| 19 | `ui/TaskChainApp.kt`, `ui/designsystem/TaskChainTheme.kt` card/background contrast, border, and glow | Source reviewed; visual proof pending |
| 20 | `ui/TaskChainApp.kt`, `ui/designsystem/TaskChainTheme.kt` minimum text sizing and system-scale behavior | Source reviewed; accessibility and device proof pending |
| 20 transition | `ui/TaskReadyTransition.kt`, `ui/designsystem/RunnerMotion.kt`, `ui/RunPresentation.kt` completion-to-next-task sequence | Source reviewed; the requested five-second sequence coexists with the existing 350 ms completion effect; timing interaction and visual proof pending |
| 21 | `reminder/RunBubble.kt`, `MainActivity.kt`, `AndroidManifest.xml`, `res/values/settings_punch_strings.xml` native minimize bubble | Source reviewed; Android version/permission/channel constraints and device proof pending |
| 22 | `domain/model/Models.kt`, `data/DataStoreUserPreferenceRepository.kt`, `data/Repositories.kt` persisted vibration intensity, screen transitions, and bubble preference | Source reviewed; persistence/UI tests and device proof pending |
| 22 font limit | Settings/UI font-scale upper-limit behavior | No runtime evidence recorded; root verification pending |
| Wishlist 2 | `ui/TaskChainApp.kt`, `ui/designsystem/TaskChainTheme.kt` Scheduled/Manual/Completed header gradient treatment | Source reviewed; visual proof pending |
| Wishlist 1 | `ui/TaskChainApp.kt` tapered Complete button with cutouts and a downward success-color gradient | Source reviewed; visual proof pending |

## Timing note

`RunnerMotion.taskReadyTransitionDurationMillis` is currently 5,000 ms, while `completionDurationMillis` is 350 ms. Source review confirms both values are present; it does not prove that the effects compose correctly over the requested transition.

## Bubble constraints

The implementation uses Android's notification bubble path only. It requires Android 10+ bubble support, posted-notification permission where applicable, enabled app and channel notifications, global bubbles enabled, a long-lived conversation shortcut, and a user-allowed bubble channel. The activity is resizeable and uses `documentLaunchMode="always"`; Android 12+ uses a mutable bubble launch `PendingIntent`. Unsupported or denied conditions are reported as unavailable and do not create overlays or background services.

## Verification state

- Root verification on 2026-10-06, JDK 17: `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` passed. All 70 JVM tests passed; lint reported zero errors and 91 warnings.
- The root reviewer corrected the Android 12 bubble API guard, forwarded the bubble intent into the active runner, made the custom accordion header toggle expansion, and reserved a 48dp drag target outside the name editor. Lint also exposed and verified corrections to bubble notification permission handling and the shortcut API guard.
- USB device `2A151FDH200HY4`: the final `com.taskchain` debug APK was updated using `adb -d install -r`; installation returned Success. No uninstall or data clearing was performed.
- Instrumentation on the isolated `com.taskchain.dragcheck` package failed before the drag assertion because Espresso could not resolve `InputManager.getInstance` on this device. This does not establish whether dragging passes. Full visual, haptic, bubble, and gesture acceptance checks remain pending.
- Screenshot evidence is not attached. The item-level source-review entries above describe implementation scope, not completed device acceptance.
