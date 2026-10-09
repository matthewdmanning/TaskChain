# TaskChain UI → Function / Hook Trigger Reference

**Source:** [`matthewdmanning/TaskChain`, branch `feature/substeps`](https://github.com/matthewdmanning/TaskChain/tree/feature/substeps)  
**Pinned revision:** [`3b573f998b93088cf627b7daed91f544810427ca`](https://github.com/matthewdmanning/TaskChain/tree/3b573f998b93088cf627b7daed91f544810427ca)  
**Scope:** Every app-owned Compose component and the direct user-action callbacks, reactive hooks, rendering helpers, domain-facing calls and Android pickers they invoke. Includes substeps, the secondary-timer hook, and nonproduction Compose examples.  
**Evidence:** Static inspection of the pinned branch, not an instrumentation run. Library internals (`androidx.compose`, Material 3, `cyberpunkAndroid`) are not expanded into their implementation methods.

## How to read this reference

- **User event** — tap, long-press/drag, chip selection, typing, or Android picker confirmation.
- **Reactive hook** — Compose/lifecycle state observation or an effect triggered by a changed key/value.
- **Render / condition** — a composable or helper called during composition when a branch predicate is satisfied; this is **not** a user callback.
- **Deferred/system** — a coroutine tick, Android permission response, or Activity lifecycle event; a UI component may initiate the flow, but the named function is not called directly by the visual control.
- **Guard** — a predicate that can prevent the target from being called or prevent it from changing state. “No call” is documented when UI only updates `remember` state.

Source abbreviations (line numbers below refer to the pinned revision):

| Alias | Source file |
|---|---|
| **App** | [`app/src/main/java/com/taskchain/ui/TaskChainApp.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/ui/TaskChainApp.kt) |
| **Sub** | [`app/src/main/java/com/taskchain/ui/SubstepUi.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/ui/SubstepUi.kt) |
| **VM** | [`app/src/main/java/com/taskchain/ui/FeatureViewModels.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/ui/FeatureViewModels.kt) |
| **Engine** | [`app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt) |
| **Model** | [`app/src/main/java/com/taskchain/domain/model/Models.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/domain/model/Models.kt) |
| **Reminder** | [`app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt) |
| **Theme** | [`app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt) |
| **Path** | [`app/src/main/java/com/taskchain/ui/designsystem/RepeatedPath.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/ui/designsystem/RepeatedPath.kt) |
| **PathExamples** | [`app/src/main/java/com/taskchain/ui/designsystem/RepeatedPathExamples.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/ui/designsystem/RepeatedPathExamples.kt) |
| **Activity** | [`app/src/main/java/com/taskchain/MainActivity.kt`](https://github.com/matthewdmanning/TaskChain/blob/3b573f998b93088cf627b7daed91f544810427ca/app/src/main/java/com/taskchain/MainActivity.kt) |

## 1. Activity, app shell, navigation and themes

| UI component / hook | Function called | Trigger and precise condition | Type | Source |
|---|---|---|---|---|
| `MainActivity.onCreate` | `container.recoverTerminalRun()` → `setContent { TaskChainApp(container) }` | Android creates/recreates the sole Activity; UI is composed **after** recovery completes. | System | Activity 16–22 |
| `TaskChainApp` | `container.preferences.observe().collectAsStateWithLifecycle(UserPreferences())` | When root is composed; active preference emissions update `preferences` and recompose consumers. | Reactive hook | App 212–214 |
| `TaskChainApp` | `TaskChainTheme(preferences.selectedTheme)` | Initial composition and whenever the selected theme value changes. | Render | App 214; Theme 67–98 |
| `TaskChainApp` | `rememberNavController()`; `NavHost(startDestination = "home")` | App first renders and subsequently recomposes; `NavHost` displays whichever route is current. | Render / navigation | App 216–259 |
| `HomeShell` bottom `NavigationBarItem` | `onSelectedTabChange(tab)` → `selectedTab = tab` | A user taps **Home**, **Routines**, **Progress**, or **Settings**. This is a state switch inside `HomeShell`, **not** a `NavHost` destination change. | User event | App 217, 299–324 |
| `HomeShell` / Home and Routines tabs | `CyberAppearance(screen)` | `selectedTab == HOME` or `selectedTab == ROUTINES`. Progress and Settings render `screen()` without this extra wrapper. | Render / condition | App 350 |
| App navigation callback: create | `navController.navigate(builderRoute(null))` | Home empty-state Create, or Routines New Routine, invokes `onCreate`. | User event → navigation | App 224, 412, 520 |
| App navigation callback: edit | `navController.navigate(builderRoute(routine.id))` | Edit action on a `RoutineCard`; edit callback exists. | User event → navigation | App 225, 541, 595 |
| App navigation callback: start | `navController.navigate(runnerRoute(routine.id))` | Start action is permitted by `HomeShell.startOrExplain`. | User event → navigation | App 226, 286–292 |
| App navigation callback: resume | `navController.navigate(runnerRoute(id))` | Active run Resume button or conflict-dialog Resume action. | User event → navigation | App 227, 319, 335–336, 395 |
| Builder route save completion | `onSaved()` → set tab `ROUTINES`, `navController.popBackStack()` | `RoutineBuilderRoute` observes non-null `state.savedRoutineId` via `LaunchedEffect`. This happens after a successful `RoutineBuilderViewModel.save()`. | Reactive hook → navigation | App 243–246, 694; VM 501–573 |
| Builder route close | `onClose()` → `navController.popBackStack()` | User navigates back/discards and dirty-state guard permits closing (see §3). | User event → navigation | App 241–246, 695–698, 973 |
| Runner finished | `onFinished()` → `navController.popBackStack()` | `LaunchedEffect(state.finished)` sees `true` after terminal run persistence/recovery. | Reactive hook → navigation | App 259, 1209; VM 691–720 |
| `CyberAppearance` | `CyberTheme(...)` and `MaterialTheme(...)` | Its parent renders Home/Routines or Builder/Runner. This is a theme override in **App**, distinct from the global `TaskChainTheme`. | Render | App 178–208, 238, 254, 350 |
| `TaskChainTheme` | `ThemeCatalog.options(context)`, `isSystemInDarkTheme()`, `MaterialTheme(...)` | Theme wrapper composes; catalog is `remember(context)`-cached. System theme is consulted when selected mode requires it or falls back to it. | Render / remembered state | Theme 67–98 |
| `TaskChainDesignSystem.spacing()` | `dimensionResource(R.dimen.space_small/medium/large)` | Any consuming screen or composable asks for spacing during composition. | Render helper | Theme 57–62 |

## 2. Home / Today, Routines, and active-run conflict

| UI component | Function called | Trigger and precise condition | Type | Source |
|---|---|---|---|---|
| `HomeShell` | Construct `RoutinesViewModel`, `ProgressViewModel`, `SettingsViewModel` via `viewModel { ... }` | First composition in relevant ViewModel store; instances are retained for the shell. | Lifecycle / render | App 279–283 |
| `HomeShell` | `container.activeRun.observeActive().collectAsStateWithLifecycle(null)` | Shell starts collecting active run; emissions update Resume and conflicting-run handling. | Reactive hook | App 284 |
| `HomeShell.startOrExplain` | `onStart(routine)` | Start tapped when **no active run** exists or the active run's `routineId` equals the requested routine ID. | User event with guard | App 286–292 |
| `HomeShell.startOrExplain` | Set `blockedRoutine = routine`; **do not** invoke `onStart` | Start tapped for routine B while a different routine A has an active run. A conflict `AlertDialog` is rendered instead. | User event with guard | App 286–292, 325–346 |
| Active-run conflict `AlertDialog` — Resume | `onResume(run.routineId)` and clear `blockedRoutine` | User selects Resume while active run is still present and `status == ACTIVE`. | User event | App 325–338 |
| Active-run conflict `AlertDialog` — Keep browsing/dismiss | `blockedRoutine = null` | User dismisses or taps Keep Browsing; no navigation or run transition. | User event | App 325–344 |
| `TodayRoute` | `viewModel.state.collectAsStateWithLifecycle()` | Route composes; repository/clock-derived projection changes update sections. | Reactive hook | App 364; VM 44–72 |
| `TodayRoute` active-run card Resume | `onResume(run)` | Active run is non-null and user taps its Resume button. | Render guard + user event | App 379–401 |
| `TodayRoute` empty-state Create | `onCreate()` | All three projection buckets—`scheduled`, `manual`, `completed`—are empty, and user taps New Routine. | Render guard + user event | App 405–413 |
| `TodayRoute` sections | local `section(...)` rendering helper | Each composition renders Scheduled, Manual, Completed. When any section has no items, it renders a spacer; otherwise `CompactRoutineRow` for each routine. | Render | App 415–439 |
| `CompactRoutineRow` Start / play `CyberButton` | `onStart()` | Row has `isCompleted == false`; tap play icon. Completed rows show success icon **instead of** Start. | Render guard + user event | App 447–490 |
| `RoutinesRoute` | `viewModel.state.collectAsStateWithLifecycle()` | Route composes; routines/built-in lists refresh from ViewModel state. | Reactive hook | App 504; VM 44–72 |
| `RoutinesRoute` New Routine `CyberButton` | `onCreate()` | User taps the button, regardless of whether saved routines exist. | User event | App 512–529 |
| `RoutinesRoute` saved and starter lists | `RoutineCard(...)` | `state.routines`/`state.builtIns` nonempty respectively; one card per item. | Render / condition | App 531–555 |
| `RoutineCard` Edit `CyberButton` | `onEdit?.invoke()` | An edit callback was supplied (the list passes one), and the user taps Edit. Otherwise this button is not rendered. | Render guard + user event | App 592–605 |
| `RoutineCard` Start `CyberButton` | `onStart()` | User taps Start on a routine card. `HomeShell.startOrExplain` still enforces active-run conflict. | User event | App 607–619 |
| `RoutineCard` display helper | `formatRoutineItemCount(routine)`; `formatRoutineTotalTime(routine)` | Card is composed. Count includes **MAIN** steps only; total duration sums timed steps, including secondary steps. Time line is omitted when no step has a timer. | Render | App 579–589, 626–646 |
| `RoutinesViewModel.state` | `combine(routines.observeAll, builtIns, activeRun.observeActive, completions.observeAll, clock)` → `projectTodayRoutines` | Initial ViewModel subscription and any input emission; clock emits every 60 seconds while flow is collecting. | Deferred/reactive, not a click | VM 44–72 |
| `RoutineList` (**currently unmounted**) | `onCreate()` or `RoutineCard(... onStart)` | Only if another caller explicitly mounts this helper; none in the production `NavHost` at this commit. Within it, Create is visible for empty list and Start per routine. | Declared, no current runtime trigger | App 649–674 |

## 3. Routine builder: routine fields, main-step cards, save/discard

**Important:** In this revision, the parent is a regular editable `Card`, **not** `CyberAccordion`. `editingStepIndex == index` controls the edit panel and the presence of `SubstepEditor`.

| UI component | Function called | Trigger and precise condition | Type | Source |
|---|---|---|---|---|
| `RoutineBuilderRoute` | `viewModel(key = "builder-${routineId?.value}") { RoutineBuilderViewModel(...) }` | Builder navigated to, with ID for editing or null for creation. Existing ID loads stored routine, falling back to built-in template. | Lifecycle / render | App 679–688; VM 230–270 |
| `RoutineBuilderRoute` | `viewModel.state.collectAsStateWithLifecycle()` | Builder composes; edits, validation errors, and saving state recompose controls. | Reactive hook | App 688 |
| Name `OutlinedTextField` | `viewModel.setTitle(newText)` | Every field value change; ViewModel tracks dirty state and remaining applicable validation errors. | User event | App 724–735; VM 273–276 |
| Description `OutlinedTextField` | `viewModel.setDescription(newText)` | Every value change. | User event | App 736–743; VM 278–281 |
| Routine settings card | `ScheduleEditor(state, viewModel, spacing)` | Builder renders; schedule edit functions described in §4. | Render | App 744–757 |
| Sound setting `LabeledSwitchRow` | `viewModel.setSoundEnabled(checked)` | User toggles sound row. | User event | App 755; VM 383–384 |
| Vibration setting `LabeledSwitchRow` | `viewModel.setVibrateEnabled(checked)` | User toggles vibration row. | User event | App 756; VM 386–387 |
| Step list `LazyColumn.items` | Render a main-step `Card` | Step has `step.role == RoutineStepRole.MAIN`; SECONDARY records are **not** top-level cards. | Render guard | App 768–775 |
| Main-step Edit `IconButton` | `viewModel.editStep(index)` | User taps Edit on a main-step card. ViewModel ignores invalid index, non-main step, and a repeated request for same editing index; it may also reject switching away from an invalid pending timer. | User event with VM guard | App 857–863; VM 389–402 |
| Main-step title `OutlinedTextField` | `viewModel.setPendingStepTitle(newText)` | **Only** when `state.editingStepIndex == index`; user changes visible title field. This updates the actual `steps[index].title` in draft. | Render guard + user event | App 864–876; VM 283–289 |
| Main-step Duration `OutlinedButton` | `showDurationPicker(context, pendingTimerSeconds, viewModel::setPendingTimerSeconds)` | Only when this main step is in editing mode, and user taps Duration. | User event → platform dialog | App 877–884, 1065–1099 |
| Native duration picker positive `OK` | `viewModel.setPendingTimerSeconds(selectedSecondsString)` | User confirms minutes+seconds. If total is zero, helper passes `""` (= no timer); Cancel/dismiss does not call it. | Android callback | App 1065–1099; VM 292–299 |
| Main step edited body | `SubstepEditor(parentIndex, parent, steps, viewModel, onPickDuration)` | `state.editingStepIndex == index`; substep rows are shown directly beneath that main step. | Render guard | App 864–896 |
| Main-step Remove `TextButton` | `viewModel.removeStep(index)` | User taps Remove Step in the edited panel. ViewModel removes the MAIN record **and all SECONDARY records with matching `parentStepId`**. | User event | App 895; VM 478–498 |
| Main-step drag gesture | `viewModel.moveStep(index, +1 or -1)` | User long-presses and drags a main-step card until `abs(dragOffset) >= DRAG_REORDER_THRESHOLD_PX` (48px); sign determines direction. ViewModel moves the main step together with its substeps. | Pointer event with threshold | App 812–825; VM 425–445 |
| Main-step accessibility reorder | `viewModel.moveStep(index, -1/+1)` | Accessibility custom Move Up/Down is invoked; only presented if a preceding/following **MAIN** step exists. | Accessibility event | App 803–810; VM 425–445 |
| Add Step `CyberButton` | `viewModel.addStep(newTaskTitle)` | User taps Add Step. `newTaskTitle` is localized default text. VM can refuse if currently pending duration text is invalid. Added step has `role = MAIN`. | User event with VM guard | App 902–914; VM 404–422 |
| Save `CyberButton` | `viewModel.save()` → `validateBuilderState(...)` | User taps Save; button disabled while `state.isSaving`. Validation must pass; then VM saves the routine and schedules/cancels a reminder. Failures expose `SAVE_FAILED`, not a navigation change. | User event / validation | App 949–960; VM 501–573 |
| Save-success effect | `onSaved()` | `state.savedRoutineId != null`; selected tab becomes Routines and builder pops. | Reactive hook | App 694, 243–246 |
| Discard button or system Back | `requestClose()` | Discard button tapped or `BackHandler` fired; if `state.hasUnsavedChanges`, set `showDiscardDialog = true`, **otherwise** call `onClose()` immediately. | User event + guard | App 693–698, 941–946 |
| Discard confirmation `AlertDialog` | `onClose()` | Dialog exists (`showDiscardDialog == true`) and user presses **Discard changes**. | User event | App 967–980 |
| Discard dialog Keep editing / dismissal | `showDiscardDialog = false` | User presses Keep editing or dismisses dialog; no ViewModel mutation or navigation. | User event | App 967–984 |
| Builder errors / field support text | `validateBuilderState(...)` indirectly via VM | Text appears when associated `BuilderValidationError` is present. Guard examples: empty routine name, no MAIN step, empty step title, missing selected days, invalid timer, or persistence failure. | Render conditional on state | App 729–734, 759–764, 871–875, 884–885, 919–935; VM 123–164 |

### Builder save path

```text
Save CyberButton tap
 → RoutineBuilderViewModel.save()
 → validateBuilderState(BuilderState, now)
 → [valid] construct RoutineTemplate with MAIN and SECONDARY steps
 → RoutineRepository.save(routine)
 → ReminderScheduler.schedule(...) or cancel(...)
 → BuilderState.savedRoutineId = stableRoutineId
 → LaunchedEffect(savedRoutineId) calls onSaved()
 → switch HomeShell tab to Routines + pop builder route
```

If validation fails, the routine and navigation remain unchanged and error state is rendered. The `save()` method also validates the persisted routine by calling `requireRunnable()`.

## 4. Schedule editor, switch/weekday helpers, and Android dialogs

| UI component | Function called | Trigger and precise condition | Type | Source |
|---|---|---|---|---|
| Repeating `LabeledSwitchRow` | `viewModel.setScheduleEnabled(enabled)` | User toggles off, or toggles on with notification permission already granted, or Android is below API 33. | User event / permission guard | App 997–1000; VM 448–456 |
| Repeating switch on Android 13+ | `permissionLauncher.launch(POST_NOTIFICATIONS)` | User toggles on and runtime notification permission is not granted. This invokes the OS prompt instead of enabling immediately. | User event → OS | App 993–1000 |
| Permission response | `viewModel.setScheduleEnabled(granted)` | OS permission flow returns `true`/`false`. | System callback | App 993–995 |
| Frequency buttons | `viewModel.setScheduleFrequency(frequency)` | Schedule enabled and user taps Daily, Weekdays or Selected Days. Current selection uses `Button`, alternatives `OutlinedButton`; both call the method. | Render guard + user event | App 1002–1012; VM 458–461 |
| Frequency button labels | `frequencyLabel(frequency)` → `stringResource(...)` | For each rendered choice during composition. The helper understands `ONCE` too, but `ONCE` is **not** in this control's clickable frequency list. | Render helper | App 1003–1011, 1150–1160 |
| `WeekdaySelector` | `onDayToggle(day)` → `viewModel.setScheduleDays(newSet)` | Only when schedule enabled **and** frequency is `SELECTED_DAYS`; user taps one of seven `FilterChip`s. Adds/removes the day in the current set. | Render guard + user event | App 1014–1023, 1117–1129; VM 468–470 |
| Weekday chip label | `dayLabel(day)` → `stringResource(...)` | Each `FilterChip` is rendered. Days are 1–7, Monday–Sunday. | Render helper | App 1122–1128, 1161–1172 |
| Schedule time `TextButton` | `showTimePicker(context, scheduleHour, scheduleMinute, viewModel::setScheduleTime)` | Schedule enabled and user taps displayed time. | User event → Android dialog | App 1026–1035, 1185–1195 |
| Schedule `TimePickerDialog` confirmation | `viewModel.setScheduleTime(hour, minute)` | User picks a clock time and confirms; cancelling does not invoke callback. | OS dialog callback | App 1185–1195; VM 463–466 |
| Schedule time text | `formatScheduleTime(context, hour, minute)` | Schedule time button rendered; uses device 12/24-hour time preference. | Render helper | App 1030–1034, 1138–1147 |
| Remind Every `LabeledSwitchRow` | `viewModel.setRemindEveryMinutes(defaultMinutes or null)` | Schedule enabled, user switches repeated reminder on/off. Enabling loads `R.integer.authoring_default_repeat_minutes`; disabling sets `null`. | User event | App 1037–1043; VM 376–381 |
| Reminder interval `TextButton` | Show native `NumberPicker` dialog | Rendered only when `state.remindEveryMinutes != null`; tap opens picker from 1 to 1440 minutes. | Render guard + user event | App 1044–1057 |
| Reminder interval dialog `OK` | `viewModel.setRemindEveryMinutes(picker.value)` | User confirms interval selection. Cancel/dismiss makes no VM call. | OS dialog callback | App 1055; VM 376–381 |
| `LabeledSwitchRow` shared component | `onCheckedChange(newBoolean)` | User toggles anywhere on merged semantic row. Internal Material `Switch` has `onCheckedChange = null` to avoid a second handler. Used for builder sound/vibrate/repeat/remind and Settings overtime. | User event | App 1101–1114 |
| `showDurationPicker` shared dialog | `onSelected(secondsString)` | User confirms selected minutes+seconds. Both main-step and secondary-step duration controls inject callbacks; helper does not itself choose which ViewModel method to invoke. | OS dialog callback | App 1065–1099 |
| `formatDateTime` and `showDateTimePicker` | **No active call site found in the branch UI** | These helpers are declared but not currently connected to a rendered control; document existence, not an invented trigger. | Unmounted helper | App 1133–1137, 1175–1183 |

## 5. SubstepEditor: each substep control and trigger

**Parent gate:** `SubstepEditor` only composes inside a main step's editing panel (`editingStepIndex == parent index`). It derives displayed children by filtering the flat `steps` list for `role == SECONDARY && parentStepId == parent.id`. Local pending-entry values are keyed with `remember(parent.id)`.

| UI element / hook | Function called | Trigger and precise condition | Type | Source |
|---|---|---|---|---|
| `SubstepEditor` children filter | `steps.mapIndexedNotNull { ... }` | Component composes; for each child matching both role and parent ID, one editable substep row is rendered. | Render | Sub 44–48 |
| Local pending values | `remember(parent.id) { mutableStateOf("") }` for `newTitle` and `newTimerSeconds` | Component enters composition for a parent; local drafts persist across recompositions for same parent key and reset on parent identity change. | Remembered state | Sub 49–50 |
| Existing substep title `OutlinedTextField` | `viewModel.setSubstepTitle(index, newText)` | User types into an existing secondary row. VM **rejects blank value** (`value.isBlank()`), so a fully cleared field is not persisted. | User event + VM guard | Sub 76–82; VM 324–331 |
| Existing substep duration `OutlinedButton` | `onPickDuration(substep.timerSeconds) { viewModel.setSubstepTimerSeconds(index, value) }` | User taps duration in an existing row; parent supplies `showDurationPicker`. VM accepts blank to remove timer or a positive number, rejects invalid nonblank input. | User event → OS dialog | Sub 83–93; App 890–893; VM 333–340 |
| Existing substep Remove `TextButton` (×) | `viewModel.removeSubstep(index)` | User taps the row's ×. VM verifies the selected record is SECONDARY before deleting it. | User event + guard | Sub 94–97; VM 343–348 |
| New substep name `OutlinedTextField` | `newTitle = newValue` (**no ViewModel call**) | User types in placeholder **“Create a new substep”**. This updates only local remembered draft state until Plus is pressed. | User event / local state | Sub 127–133 |
| New substep duration `OutlinedButton` | `onPickDuration(newTimerSeconds.toLongOrNull()) { newTimerSeconds = value }` | User taps candidate duration. Native picker `OK` updates **local** `newTimerSeconds`; no persisted secondary record exists yet. | User event → local state | Sub 135–146 |
| Circled-plus `IconButton` | `viewModel.addSubstep(parentIndex, newTitle, newTimerSeconds)` | User taps plus. VM requires parent to be MAIN, nonblank trimmed title, and either blank or positive seconds. Inserts after the parent's existing substeps, links `parentStepId`, marks `SECONDARY`. | User event + VM guard | Sub 105–125; VM 302–322 |
| Plus button post-click state | `newTitle = ""`; `newTimerSeconds = ""` | UI clears both local entry values **when `newTitle.isNotBlank()`**, regardless of whether the VM actually accepted the duration. This is a possible lost-input edge case for malformed duration strings. | Local state after call | Sub 114–119 |
| Existing-row duration text | `"%d:%02d".format(seconds / 60, seconds % 60)` | Each existing row renders; null timer displays `0:00`. | Render helper | Sub 88–92 |
| New-entry duration text | Same `"%d:%02d".format(...)` | Entry row renders or local timer value changes; blank/null displays `0:00`. | Render helper | Sub 142–145 |
| Substep icon and sizing | `CyberIcon(CyberIcons.Plus)` plus `MaterialTheme` secondary typography | Each existing row uses small circled plus; entry row has a circled plus `IconButton`. No domain function is called by icon rendering alone. | Render | Sub 54–75, 105–126 |

### Substep creation path

```text
Edit MAIN card
 → editingStepIndex == main index
 → SubstepEditor displayed
 → user types title [local `newTitle` only]
 → optional duration picker [local `newTimerSeconds` only]
 → tap circled Plus
 → RoutineBuilderViewModel.addSubstep(parentIndex, title, seconds)
 → [valid] insert `RoutineStep(role=SECONDARY, parentStepId=main.id)`
 → BuilderState.steps emits
 → SubstepEditor re-renders matching child row
 → Save routine to persist the draft
```

**UI distinction:** This revision has **no `CyberAccordion`** and no independent navigation destination for substeps.

## 6. Routine runner: buttons, dialogs, presentation and timers

| UI component / hook | Function called | Trigger and precise condition | Type | Source |
|---|---|---|---|---|
| `RoutineRunnerRoute` | `viewModel(key = "runner-${routineId.value}") { RoutineRunnerViewModel(...) }` | Runner navigation begins or recreates for given routine ID. VM initialization resumes any active run (regardless of requested ID) or starts one, then persists it. | Lifecycle / render | App 1198–1208; VM 589–604 |
| `RoutineRunnerRoute` | `state.collectAsStateWithLifecycle()` | Runner composes; changes in current run / wall clock / finished / history-save-error recompose the UI. | Reactive hook | App 1208 |
| Runner loading view | `CircularProgressIndicator()` | `state.run == null` (initial asynchronous run load). Early return prevents step UI from composing. | Render guard | App 1210–1219 |
| Runner finish effect | `onFinished()` | `LaunchedEffect(state.finished)` and `state.finished == true`, after VM completes terminal processing. | Reactive hook | App 1209; VM 691–720 |
| Android system Back | `viewModel.continueRun()` | `run.status == ACTIVE` and either finish or abort confirmation is requested. | User event / BackHandler | App 1222–1228 |
| Android system Back | `viewModel.back()` | `run.status == ACTIVE` and no finish/abort confirmation is open. If current step is first, engine requests abort confirmation. | User event / BackHandler | App 1222–1228; Engine 109–140 |
| Parent/main title resolution | Find parent via `current.source.parentStepId` | Current step has `role == SECONDARY`; fallback to current source if parent lookup fails. Main title always renders. | Render conditional | App 1230–1239 |
| Secondary subtitle | Compose accent-colored `Text(secondaryTitle)` | `current.source.role == SECONDARY`; shown below main title. For MAIN steps no secondary title is rendered. | Render conditional | App 1239, 1309–1317 |
| Step title entrance | `Animatable.animateTo(1f, tween(RunnerMotion.durationMillis, easing))` | `LaunchedEffect(run.currentStepIndex)` runs on current-step-index change; animates main title and dial alpha. | Reactive hook | App 1244–1247; `RunnerMotion.kt` 8–15 |
| Remaining time | `container.runEngine.remainingMillis(run, state.nowEpochMillis)` | Every runner recomposition with a loaded run; current step can be MAIN or SECONDARY. | Render-time domain calculation | App 1240–1242; Engine 181–191 |
| Timer string | `formatRunnerTimer(remaining, actualDuration, preferences.continueTimerPastZero)` | `current.source.timerSeconds != null`. Preferences determine clamping at zero versus overtime prefix. | Render helper | App 1249–1254, 1609–1621 |
| Dial rendering gate | `RunCountdownDial(timerString, countdownProgress(...), remaining, dialDiameter)` | `timerString.isNotEmpty()`; **untimed** current steps have no dial. Uses **current** step's timer, including for SECONDARY. | Render conditional | App 1319–1334 |
| Complete `CyberButton` | `viewModel.complete()` → `RoutineRunEngine.completeCurrent(...)` | User taps Complete while runner's step UI is visible. No explicit `enabled = run.status == ACTIVE` on this control; VM transition guard rejects stale/inactive runs. | User event + VM guard | App 1336–1353; VM 613–615 |
| Main-step status `FlowRow` | Filter `run.steps` to `role == MAIN`; render pending/success/skipped marker | Runner composes; markers are display-only (not navigation buttons), even while a secondary step is current. | Render conditional | App 1353–1414 |
| Back `CyberButton` | `viewModel.back()` → `RoutineRunEngine.back(...)` | `run.status == ACTIVE`, `historySaveFailed == false`, and user taps Back. | User event | App 1421–1441; VM 619–621 |
| Pause / Resume `CyberButton` | `viewModel.pause()` or `viewModel.resume()` | Active run and no history-save-error; if current `pausedAtEpochMillis == null`, button invokes pause; otherwise resume. | User event / condition | App 1442–1457; VM 622–627 |
| Skip `CyberButton` | `viewModel.skip()` → `RoutineRunEngine.skipCurrent(...)` | Active run and no history-save-error; user taps Skip. | User event | App 1458–1466; VM 616–618 |
| History-save failure button | `viewModel.retryHistorySave()` | `state.historySaveFailed == true`; normal Back/Pause/Skip row is replaced by Retry. | Render guard + user event | App 1421–1425; VM 640–652 |
| Finish confirmation `AlertDialog` | `viewModel.selectStep(index)` | `run.status == ACTIVE && run.finishConfirmationRequested`; user taps an unfinished step text button. | User event / dialog guard | App 1475–1494; VM 631–633 |
| Finish confirmation — Confirm | `viewModel.confirmComplete()` → `RoutineRunEngine.confirmComplete(...)` | Same dialog gate; user taps Confirm Complete. VM writes terminal run/history and then sets finished. | User event | App 1488–1490; VM 634–636 |
| Finish confirmation — Continue/dismiss | `viewModel.continueRun()` | Same dialog gate; user taps Continue or dismisses. | User event | App 1477–1494; VM 628–630 |
| Abort confirmation — Abort | `viewModel.abort()` → `RoutineRunEngine.abort(...)` | `run.status == ACTIVE && run.abortConfirmationRequested`; user taps Abort Run. | User event | App 1496–1504; VM 637–639 |
| Abort confirmation — Continue/dismiss | `viewModel.continueRun()` | Same abort dialog gate; user taps Continue or dismisses. | User event | App 1498–1503; VM 628–630 |
| Terminal processing | `container.completions.append(runEngine.toCompletionEvent(terminal))`, `activeRun.clearActive()` | VM `finish` successfully transitions to terminal state and persists active snapshot. On failure, sets `historySaveFailed`; retry path available. A completed run may reschedule a recurrence. | Deferred result of user event | VM 691–720 |

### Runner ViewModel transition guards

All button-driven `transition(...)` and `finish(...)` methods check that `run.status == ACTIVE` and that the currently stored step index and finish/abort confirmation flags still match the snapshot captured at event dispatch. A callback may therefore be **invoked** but yield **no state change** if the UI event is stale. (`VM` 674–709)

### Countdown display behavior (not tap handlers)

| Composable/helper | Called / updated when | Condition / effect | Source |
|---|---|---|---|
| `countdownProgress(timerSeconds, remainingMillis)` | Timer-enabled runner display recomposes. | Returns `0` for null/nonpositive timer or null remaining time; otherwise clamps elapsed fraction to `[0, 1]`. | App 1328, 1600–1606 |
| `RunCountdownDial` `animateFloatAsState` | New `progress` changes. | Animates dial's elapsed tick sweep. | App 1509–1517 |
| `RunCountdownDial` `animateColorAsState` | Remaining time crosses warning/urgent/overtime thresholds. | Overtime at `remainingMillis <= 0`; urgent when remaining ratio <= 0.15; warning when <= 0.35 but not urgent. | App 1516–1535 |
| `RunCountdownDial` `rememberInfiniteTransition` | Dial composes, with urgent state affecting alpha values. | Pulsing glow effect is visually relevant in urgent/overtime state. | App 1537–1552 |
| `CyberSectorRim` | Dial renders. | Sector rim around timer. | App 1561–1568 |
| `CyberDialTicks` (base and clipped elapsed) | Dial renders / progress changes. | Two tick layers; elapsed layer clipped to sweep angle. | App 1570–1590 |
| `GlowingText` | Dial renders / formatted timer changes. | Shows timer characters. The dial is presentation-only: it does **not** itself fire sound or modify run state. | App 1592–1598 |

## 7. Secondary-timer feedback: ViewModel-driven hook

**This sound does not originate from a Compose control.** The runner ViewModel ticker is created when `RoutineRunnerViewModel` is initialized and runs every 250 milliseconds; each tick updates `nowEpochMillis` and then checks timer-expiry eligibility.

| Caller / condition | Function called | Guard and effect | Source |
|---|---|---|---|
| `RoutineRunnerViewModel.init` | Coroutine loop `delay(TICK_MILLIS); tick()` | Starts with ViewModel initialization. `TICK_MILLIS = 250`. | VM 597–611, 746–749 |
| `tick()` | `runEngine.needsTimerFeedback(run, now)` | Only when a run exists and its status is `ACTIVE`. Engine requires current step `PENDING`, timer set, feedback not previously acknowledged, and remaining millis <= 0. | VM 656–662; Engine 193–202 |
| Eligible expiry, secondary step | `container.timerFeedback.fireSecondary(source.soundEnabled, source.vibrateEnabled)` | `source.role == RoutineStepRole.SECONDARY` and expiry predicate passes. Uses `ToneGenerator.TONE_PROP_ACK`. | VM 662–665; Reminder 357–360 |
| Eligible expiry, main step | `container.timerFeedback.fire(source.soundEnabled, source.vibrateEnabled)` | Eligible expiry and current step is not SECONDARY. Uses `ToneGenerator.TONE_PROP_BEEP`. | VM 665–667; Reminder 352–355 |
| After eligible feedback hook | `runEngine.acknowledgeTimerFeedback(...)` and `activeRun.saveActive(...)` | Acknowledges the same step's timer feedback and persists the updated run to suppress repeats. | VM 668–670; Engine 204–212 |
| `TimerFeedback.fireTone` | Android `ToneGenerator.startTone(...)` and vibration path | Sound starts only if `soundEnabled`; vibration only if `vibrateEnabled`. Main/secondary use the same haptic helper with different tone types. | Reminder 362–380 |

```text
Runner ViewModel ticker (250ms)
 → needsTimerFeedback(run, now)?
    ├─ false: update display time only
    └─ true: inspect current.source.role
          ├─ MAIN      → TimerFeedback.fire()          → TONE_PROP_BEEP
          └─ SECONDARY → TimerFeedback.fireSecondary() → TONE_PROP_ACK
       → acknowledgeTimerFeedback()
       → save active snapshot
       → UI observes new state
```

**Persistence qualification:** The code invokes feedback *before* attempting to persist the acknowledgment. “Once” is enforced by the persisted `timerFeedbackAtEpochMillis` field on success; failures can affect that guarantee. The caller does not distinguish foreground/background while the ViewModel remains active.

## 8. Progress and Settings

| UI component | Function called | Trigger and precise condition | Type | Source |
|---|---|---|---|---|
| `ProgressRoute` | `ProgressViewModel.state.collectAsStateWithLifecycle()` | User selects Progress tab and route composes; completion-history emissions re-render completed/aborted counts, durations, adherence, skipped percentage, seven-day trend. | Reactive hook | App 1624–1643; VM 753–759 |
| `ProgressViewModel` | `container.completions.observeAll().map { projectProgress(events, now) }` | Completion repository emits and StateFlow is subscribed; not a click. | Deferred/reactive | VM 753–757 |
| `SettingsRoute` | `SettingsViewModel.state.collectAsStateWithLifecycle()` | User selects Settings tab; preference emissions update selected theme/overtime switch. | Reactive hook | App 1646–1650; VM 764–766 |
| `SettingsRoute` theme choice `Button` / `OutlinedButton` | `viewModel.setTheme(option.id)` | User taps any theme choice from `ThemeCatalog.options(context)`. Current option shows filled button; alternatives outlined. Both invoke same method. | User event | App 1658–1665; VM 768–771 |
| `SettingsRoute` Continue Past Zero `LabeledSwitchRow` | `viewModel.setContinuePastZero(enabled)` | User toggles overtime behavior. VM persists via `container.preferences.setContinueTimerPastZero`. Runner formatting observes pref through root `TaskChainApp`. | User event | App 1668–1674; VM 773–776 |
| `SettingsRoute` options | `ThemeCatalog.options(context)` | Settings screen composes; IDs/labels/modes supplied by resource arrays. | Render helper | App 1659; Theme 27–46 |

## 9. Design-system render helpers and isolated previews

These are included because they are app-owned composables, even though they do not appear in production navigation on this commit.

| UI component | Function called | Trigger and precise condition | Type | Source |
|---|---|---|---|---|
| `ParallelChevronPreview` | `ParallelChevronExample()` | Compose tooling renders the preview; **no production route call** found. | Preview-only | PathExamples 71–77 |
| `ParallelChevronExample` | `anglePath(...)`, `integerResourceValue(...)`, `translatedPlacements(...)`, `RepeatedPath(...)` | Preview/example composes; resource values determine stroke, count, offsets, gradient. | Render | PathExamples 16–40; Path 31–36, 79–109 |
| `RadialChevronPreview` | `RadialChevronExample()` | Compose tooling renders this preview. | Preview-only | PathExamples 79–84 |
| `RadialChevronExample` | `anglePath(...)`, `integerResourceValue(...)`, `radialPlacements(...)`, `RepeatedPath(...)` | Preview/example composes; resource values determine radius, count, sweep and gradient. | Render | PathExamples 44–63; Path 47–70, 79–109 |
| `RepeatedPath` | Compose `Canvas` + `withTransform(...)` + `drawPath(...)` | Whenever either example renders; once per `PathPlacement`, using provided path and brush. | Render | Path 79–105 |
| `TaskChainDesignSystem.spacing` | `dimensionResource` (3 semantic spaces) | Any screen/helper that calls `spacing()`; does not itself initiate events. | Render | Theme 57–62 |
| `RunnerMotion` | Read `titleStartSize`, `titleEndSize`, offsets, duration, easing | Runner title animation runs after step index changes. This is a config object, **not** a composable or event hook. | Config read | App 1244–1247, 1290–1307; `RunnerMotion.kt` 8–15 |

## 10. High-level callback routes

| User intent | Immediate callback | State owner / domain action | Visible result |
|---|---|---|---|
| Create routine | Home/Routines `onCreate()` | `NavController` → Builder → `RoutineBuilderViewModel` | Builder route opens. |
| Edit routine | `RoutineCard.onEdit()` | `NavController` → Builder → load by routine ID | Existing draft fields appear. |
| Start routine | Row/Card `onStart()` | `HomeShell.startOrExplain()` → `NavController` → runner VM | Runner opens or active-run conflict dialog blocks it. |
| Resume active routine | Home active-card `onResume()` | `NavController` → runner VM | Existing run resumes rendering. |
| Change routine title | `setTitle(newText)` | `RoutineBuilderViewModel.mutateDraft` | Title field and validation state update. |
| Change main-step title | `setPendingStepTitle(newText)` | `RoutineBuilderViewModel.mutateDraft` | Main-card header updates. |
| Add substep | `addSubstep(index, title, timer)` | `RoutineBuilderViewModel.mutateDraft` | Secondary row appears below edited main step. |
| Change substep duration | `onPickDuration(... callback)` | Picker OK → `setSubstepTimerSeconds` | Saved-draft duration text updates. |
| Reorder main step | `moveStep(index, ±1)` | Builder VM moves main-plus-children group | Main cards and their children reorder. |
| Save routine | `save()` | Validation → routine repository / scheduler | Builder exits on success. |
| Complete current step | `complete()` | `RoutineRunEngine.completeCurrent` | Next/current step state changes; finish confirmation may appear. |
| Pause timer | `pause()` | `RoutineRunEngine.pauseCurrent` | Pause action becomes Resume; timer stays paused. |
| Skip step | `skip()` | `RoutineRunEngine.skipCurrent` | Step status updates; next step/dialog may appear. |
| Finish run | `confirmComplete()` | Engine → completion repository | Runner exits when finished state publishes. |
| Abort run | `abort()` | Engine → completion repository | Runner exits when finished state publishes; **this branch records an ABORTED completion event**. |
| Expire secondary timer | **No UI click** | ViewModel ticker → `TimerFeedback.fireSecondary` | Secondary timer sound/haptic if enabled. |
| Change theme | `setTheme(option.id)` | Settings VM → DataStore | App recomposes with selected theme. |
| Change overtime display | `setContinuePastZero(enabled)` | Settings VM → DataStore | Timer digits clamp or continue below zero. |

## 11. Important qualifications / possible follow-ups

1. **Function invocation is different from mutation.** The builder and runner ViewModels apply guards; a button's callback can run yet produce no state change (invalid data, stale run, same selection).
2. **Substeps have one logical state owner.** Pending new-entry text/duration live locally in `SubstepEditor` until Plus is tapped. Existing substep edits mutate `BuilderState.steps` immediately. Persistence occurs only on routine Save.
3. **Substep draft-clearing risk.** The UI clears local entry text if the title is nonblank even when `addSubstep()` rejects malformed duration text. It would be safer for the ViewModel to return success or expose validation state before clearing. This is an observation, not an asserted runtime failure.
4. **Existing substep titles cannot be emptied.** `setSubstepTitle()` returns when `value.isBlank()`, while the UI provides a normal text field and separate remove button.
5. **Timer feedback is ViewModel-driven.** Audio is not invoked by `RunCountdownDial`; expiry detection is a 250ms ViewModel ticker and the current step's role determines the tone.
6. **Only MAIN steps appear in runner status circles.** Secondary steps participate in the run and timer but do not create standalone circle markers.
7. **Old helper declarations are not active controls.** `RoutineList`, `formatDateTime`, and `showDateTimePicker` have no production caller in this branch. Design previews are isolated from `NavHost`.
8. **This is the inspected branch, not merged `main`.** No claims about current behavior on other branches are implied. No application source files were modified to produce this documentation.

---

**Related diagram:** `TaskChain-UI-Wiring-feature-substeps.md` (navigation and component-composition wiring).