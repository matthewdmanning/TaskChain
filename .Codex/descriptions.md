## `AppContainer` — app/src/main/java/com/taskchain/AppContainer.kt
Small application composition root that wires domain ports to local Android adapters.
Inputs: context: Context
Dependencies: `AndroidReminderScheduler`, `BuiltInRoutineLibrary`, `CompletionRepository`, `DataStoreUserPreferenceRepository`, `FileCompletionRepository`, `FileRoutineRepository`, `FileRoutineRunRepository`, `ReminderScheduler`, `RoutineRepository`, `RoutineRunEngine`, `RoutineRunRepository`, `TimerFeedback`, `UserPreferenceRepository`, `activeRun`.

## `newRoutineId` — app/src/main/java/com/taskchain/AppContainer.kt
Use this function when creating a new persistent routine identity offline.
Inputs: none.
Dependencies: `RoutineId`.

## `newStepId` — app/src/main/java/com/taskchain/AppContainer.kt
Use this function when creating a new routine-step identity offline.
Inputs: none.
Dependencies: `RoutineStepId`.

## `newRunId` — app/src/main/java/com/taskchain/AppContainer.kt
Use this function when starting a distinct routine execution offline.
Inputs: none.
Dependencies: `RoutineRunId`.

## `now` — app/src/main/java/com/taskchain/AppContainer.kt
Use this function wherever domain state needs wall-clock epoch milliseconds.
Inputs: none.
Dependencies: None.

## `MainActivity` — app/src/main/java/com/taskchain/MainActivity.kt
Hosts the single Compose activity and the local application composition root.
Inputs: none.
Dependencies: `AppContainer`.

## `onCreate` — app/src/main/java/com/taskchain/MainActivity.kt
Use this function when Android creates or recreates the app activity.
Inputs: savedInstanceState: Bundle?
Dependencies: `TaskChainApp`, `recoverTerminalRun`.

## `RoutineRepository` — app/src/main/java/com/taskchain/data/Repositories.kt
Storage boundary for reusable routine definitions.
Inputs: none.
Dependencies: None.

## `observeAll` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function when UI needs a live list of locally stored routines.
Inputs: none.
Dependencies: `RoutineTemplate`.

## `get` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function when opening one routine by stable identity.
Inputs: id: RoutineId
Dependencies: `RoutineId`, `RoutineTemplate`.

## `save` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function when creating or updating a routine definition.
Inputs: routine: RoutineTemplate
Dependencies: `RoutineTemplate`, `routine`.

## `delete` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function when explicitly deleting a routine definition.
Inputs: id: RoutineId
Dependencies: `RoutineId`.

## `RoutineRunRepository` — app/src/main/java/com/taskchain/data/Repositories.kt
Storage boundary for the one active run that must survive process death.
Inputs: none.
Dependencies: None.

## `observeActive` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function when runner UI needs the persisted active session.
Inputs: none.
Dependencies: `RoutineRun`.

## `saveActive` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function after every domain transition in an active run.
Inputs: run: RoutineRun
Dependencies: `RoutineRun`.

## `clearActive` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function after a terminal run has been safely added to history.
Inputs: none.
Dependencies: None.

## `CompletionRepository` — app/src/main/java/com/taskchain/data/Repositories.kt
Append-only storage boundary for progress history.
Inputs: none.
Dependencies: None.

## `append` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function exactly once when a run becomes completed or aborted.
Inputs: event: CompletionEvent
Dependencies: `CompletionEvent`.

## `UserPreferenceRepository` — app/src/main/java/com/taskchain/data/Repositories.kt
Persistence boundary for user-level appearance and timer behavior.
Inputs: none.
Dependencies: None.

## `observe` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function when UI or runner behavior needs current preferences.
Inputs: none.
Dependencies: `UserPreferences`.

## `setTheme` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function when the selected app theme changes.
Inputs: theme: String
Dependencies: None.

## `setContinueTimerPastZero` — app/src/main/java/com/taskchain/data/Repositories.kt
Use this function when overtime behavior is toggled.
Inputs: enabled: Boolean
Dependencies: None.

## `FileRoutineRepository` — app/src/main/java/com/taskchain/data/FileRepositories.kt
File-backed routine storage using one atomic JSON file per routine.
Inputs: private val directory: File, private val json: Json,
Dependencies: `RoutineRepository`.

## `readAll` — app/src/main/java/com/taskchain/data/FileRepositories.kt
Use this function to load all non-deleted routine files for an observation refresh.
Inputs: none.
Dependencies: `RoutineTemplate`, `read`.

## `read` — app/src/main/java/com/taskchain/data/FileRepositories.kt
Use this function when a missing local JSON file represents empty state.
Inputs: file: File
Dependencies: `RoutineTemplate`.

## `fileFor` — app/src/main/java/com/taskchain/data/FileRepositories.kt
Use this function to map stable routine identity to its private JSON file.
Inputs: id: RoutineId
Dependencies: `RoutineId`.

## `FileRoutineRunRepository` — app/src/main/java/com/taskchain/data/FileRepositories.kt
File-backed active-run storage with atomic replacement after every transition.
Inputs: private val file: File, private val json: Json,
Dependencies: `RoutineRunRepository`.

## `FileCompletionRepository` — app/src/main/java/com/taskchain/data/FileRepositories.kt
File-backed append-only completion history used by progress projections.
Inputs: private val file: File, private val json: Json,
Dependencies: `CompletionEvent`, `CompletionRepository`.

## `DataStoreUserPreferenceRepository` — app/src/main/java/com/taskchain/data/DataStoreUserPreferenceRepository.kt
DataStore-backed user preferences kept separate from device permission state.
Inputs: private val context: Context
Dependencies: `UserPreferenceRepository`.

## `BuiltInRoutineLibrary` — app/src/main/java/com/taskchain/data/BuiltInRoutineLibrary.kt
Reads predefined routines from assets without coupling the domain to asset JSON.
Inputs: private val context: Context, private val json: Json,
Dependencies: None.

## `load` — app/src/main/java/com/taskchain/data/BuiltInRoutineLibrary.kt
Use this function to seed or display the bundled routine library.
Inputs: nowEpochMillis: Long
Dependencies: `LibraryDto`, `RoutineTemplate`, `toDomain`.

## `LibraryDto` — app/src/main/java/com/taskchain/data/BuiltInRoutineLibrary.kt
Asset-only schema kept independent from persistent and domain representations.
Inputs: val formatVersion: Int, val routines: List<RoutineDto>
Dependencies: `RoutineDto`.

## `RoutineDto` — app/src/main/java/com/taskchain/data/BuiltInRoutineLibrary.kt
Asset-only representation of one predefined routine.
Inputs: val id: String, val title: String, val description: String, val steps: List<StepDto>,
Dependencies: `StepDto`.

## `StepDto` — app/src/main/java/com/taskchain/data/BuiltInRoutineLibrary.kt
Asset-only representation of one predefined step.
Inputs: val id: String, val title: String, val timerSeconds: Long? = null,
Dependencies: None.

## `toDomain` — app/src/main/java/com/taskchain/data/BuiltInRoutineLibrary.kt
Use this function when exposing predefined asset records to domain consumers.
Inputs: nowEpochMillis: Long
Dependencies: `EntityMetadata`, `RoutineId`, `RoutineTemplate`, `StepDto`.

## `RoutineId` — app/src/main/java/com/taskchain/domain/model/Models.kt
Stable routine identity that remains valid across storage adapters.
Inputs: val value: String
Dependencies: None.

## `RoutineStepId` — app/src/main/java/com/taskchain/domain/model/Models.kt
Stable identity for one step inside a routine snapshot.
Inputs: val value: String
Dependencies: None.

## `RoutineRunId` — app/src/main/java/com/taskchain/domain/model/Models.kt
Stable identity for one execution of a routine.
Inputs: val value: String
Dependencies: None.

## `EntityMetadata` — app/src/main/java/com/taskchain/domain/model/Models.kt
Metadata shared by persistent records without exposing file-storage details.
Inputs: val createdAtEpochMillis: Long, val updatedAtEpochMillis: Long, val revision: Long = 1, val deletedAtEpochMillis: Long? = null,
Dependencies: None.

## `RoutineStep` — app/src/main/java/com/taskchain/domain/model/Models.kt
A reusable task, habit, or goal-linked action within a routine.
Inputs: val id: RoutineStepId, val title: String, val timerSeconds: Long? = null, val stackingAnchorStepId: RoutineStepId? = null, val deadlineEpochMillis: Long? = null, val reminderAtEpochMillis: Long? = null, val schedule: ScheduleRule? = null, val remindEveryMinutes: Int? = null, val soundEnabled: Boolean = true, val vibrateEnabled: Boolean = true,
Dependencies: `RoutineStepId`, `ScheduleRule`, `schedule`.

## `RoutineTemplate` — app/src/main/java/com/taskchain/domain/model/Models.kt
A reusable ordered routine definition edited by the builder.
Inputs: val id: RoutineId, val metadata: EntityMetadata, val title: String, val description: String = , val steps: List<RoutineStep>, val schedule: ScheduleRule? = null,
Dependencies: `EntityMetadata`, `RoutineId`, `RoutineStep`, `ScheduleRule`, `schedule`.

## `requireRunnable` — app/src/main/java/com/taskchain/domain/model/Models.kt
Use this function before starting or saving a routine that must be executable.
Inputs: none.
Dependencies: `RoutineTemplate`, `ScheduleFrequency`, `schedule`, `step`.

## `ScheduleRule` — app/src/main/java/com/taskchain/domain/model/Models.kt
A local schedule definition that stays independent from Android alarms.
Inputs: val frequency: ScheduleFrequency, val localHour: Int, val localMinute: Int, val daysOfWeek: Set<Int> = emptySet(), val oneTimeEpochMillis: Long? = null,
Dependencies: `ScheduleFrequency`.

## `RoutineRunStep` — app/src/main/java/com/taskchain/domain/model/Models.kt
Per-run step state including timing and feedback state needed after process death.
Inputs: val source: RoutineStep, val status: RunStepStatus = RunStepStatus.PENDING, val startedAtEpochMillis: Long? = null, val finishedAtEpochMillis: Long? = null, val completedAtEpochMillis: Long? = null, val actualDurationMillis: Long? = null, val timerFeedbackAtEpochMillis: Long? = null,
Dependencies: `RoutineStep`, `RunStepStatus`.

## `RoutineRun` — app/src/main/java/com/taskchain/domain/model/Models.kt
Immutable snapshot of an active or completed routine execution.
Inputs: val id: RoutineRunId, val routineId: RoutineId, val routineTitle: String, val steps: List<RoutineRunStep>, val currentStepIndex: Int, val startedAtEpochMillis: Long, val endedAtEpochMillis: Long? = null, val status: RunStatus = RunStatus.ACTIVE, val finishConfirmationRequested: Boolean = false, val abortConfirmationRequested: Boolean = false, val confirmationStartedAtEpochMillis: Long? = null, val stepBeforeFinishConfirmation: RoutineRunStep? = null,
Dependencies: `RoutineId`, `RoutineRunId`, `RoutineRunStep`, `RunStatus`.

## `CompletionEvent` — app/src/main/java/com/taskchain/domain/model/Models.kt
Append-only history record used to derive progress without mutable counters.
Inputs: val runId: RoutineRunId, val routineId: RoutineId, val routineTitle: String, val startedAtEpochMillis: Long, val endedAtEpochMillis: Long, val status: RunStatus, val steps: List<RoutineRunStep>,
Dependencies: `RoutineId`, `RoutineRunId`, `RoutineRunStep`, `RunStatus`.

## `UserPreferences` — app/src/main/java/com/taskchain/domain/model/Models.kt
User-level behavior and appearance preferences that may roam in a future adapter.
Inputs: val selectedTheme: String = , val continueTimerPastZero: Boolean = true,
Dependencies: None.

## `RoutineRunEngine` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Owns all deterministic routine-run transitions so UI and persistence remain policy-free.
Inputs: routine: RoutineTemplate, runId: RoutineRunId, nowEpochMillis: Long
Dependencies: None.

## `start` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function when starting a fresh run from a validated routine snapshot.
Inputs: routine: RoutineTemplate, runId: RoutineRunId, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RoutineRunId`, `RoutineRunStep`, `RoutineTemplate`, `requireRunnable`, `routine`, `step`.

## `completeCurrent` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function when Complete is pressed for the currently displayed run step.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStepStatus`, `finishCurrent`.

## `skipCurrent` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function when Skip is pressed for the currently displayed run step.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStepStatus`, `finishCurrent`.

## `selectStep` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function to move to a specific step while preserving all recorded state.
Inputs: run: RoutineRun, index: Int, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStatus`, `RunStepStatus`.

## `back` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function when Back is pressed so the first step can request abort confirmation.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStatus`, `selectStep`.

## `continueRun` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function when a user cancels either run-ending confirmation dialog.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStatus`, `RunStepStatus`.

## `confirmComplete` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function after the finish dialog is confirmed, even if skipped tasks remain.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStatus`.

## `abort` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function after the abort dialog is confirmed.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStatus`.

## `unfinishedStepIndexes` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function to list tasks that must be surfaced by the finish dialog.
Inputs: run: RoutineRun
Dependencies: `RoutineRun`, `RunStepStatus`, `step`.

## `remainingMillis` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function to derive countdown or overtime from persisted timestamps.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStepStatus`, `step`.

## `needsTimerFeedback` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function before firing audio and haptics so zero feedback occurs only once.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStatus`, `RunStepStatus`, `remainingMillis`, `step`.

## `acknowledgeTimerFeedback` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function immediately after platform timer feedback succeeds.
Inputs: run: RoutineRun, nowEpochMillis: Long
Dependencies: `RoutineRun`, `RunStatus`, `step`.

## `toCompletionEvent` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function when persisting a terminal run to append-only progress history.
Inputs: run: RoutineRun
Dependencies: `CompletionEvent`, `RoutineRun`, `RunStatus`.

## `finishCurrent` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function to apply Complete or Skip and advance without duplicating transition rules.
Inputs: run: RoutineRun, nowEpochMillis: Long, status: RunStepStatus
Dependencies: `RoutineRun`, `RoutineRunStep`, `RunStatus`, `RunStepStatus`, `selectStep`.

## `replaceAt` — app/src/main/java/com/taskchain/domain/run/RoutineRunEngine.kt
Use this function to immutably replace one run step without a mutable collection.
Inputs: Uncertain: declaration source was not available.
Dependencies: Uncertain: direct project dependencies could not be confirmed from the available source.

## `ReminderRequest` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Platform-neutral request to prompt the user about one routine.
Inputs: val requestCode: Int, val routineId: String, val title: String, val triggerAtEpochMillis: Long, val schedule: ScheduleRule? = null, val stepId: String? = null, val remindEveryMinutes: Int? = null, val cycleStartEpochMillis: Long? = null, val soundEnabled: Boolean = true, val vibrateEnabled: Boolean = true,
Dependencies: `ScheduleRule`, `schedule`.

## `ReminderScheduler` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Android scheduling boundary kept outside routine and schedule domain models.
Inputs: request: ReminderRequest
Dependencies: None.

## `AndroidReminderScheduler` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
AlarmManager adapter for local, inexact routine reminders.
Inputs: private val context: Context
Dependencies: `ReminderScheduler`.

## `schedule` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Use this function when a domain reminder should become an Android alarm.
Inputs: request: ReminderRequest
Dependencies: `ReminderRequest`.

## `cancel` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Use this function when a domain reminder should no longer fire.
Inputs: requestCode: Int
Dependencies: None.

## `pendingIntent` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Use this function to create stable immutable alarm identity for schedule and cancel.
Inputs: request: ReminderRequest
Dependencies: `ReminderReceiver`, `ReminderRequest`, `schedule`.

## `ReminderReceiver` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Receives local alarms and displays the corresponding routine notification.
Inputs: none.
Dependencies: None.

## `onReceive` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Use this function when Android delivers a scheduled local routine alarm.
Inputs: context: Context, intent: Intent
Dependencies: `AndroidReminderScheduler`, `AppContainer`, `MainActivity`, `TimerFeedback`, `activeRun`, `completedSinceCycle`, `createChannel`, `finish`, `fire`, `observeActive`, `observeAll`, `rearm`.

## `createChannel` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Use this function before posting reminders on Android 8 or newer.
Inputs: context: Context
Dependencies: None.

## `TimerFeedback` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Provides one-shot audio and haptic feedback when a task timer reaches zero.
Inputs: private val context: Context
Dependencies: None.

## `fire` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Use this function exactly once when the domain reports an unacknowledged timer expiry.
Inputs: soundEnabled: Boolean = true, vibrateEnabled: Boolean = true
Dependencies: None.

## `RoutinesViewModel` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Coordinates routine lists while leaving routine rules in domain models.
Inputs: private val container: AppContainer
Dependencies: `AppContainer`, `RoutineTemplate`, `RoutinesState`, `activeRun`, `load`, `now`, `observeActive`, `observeAll`, `projectTodayRoutines`.

## `RoutineBuilderViewModel` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Coordinates a routine draft and persists only validated domain templates.
Inputs: private val container: AppContainer, private val routineId: RoutineId?,
Dependencies: `AppContainer`, `BuilderState`, `RoutineId`, `get`, `load`, `newRoutineId`, `now`, `routine`, `schedule`, `step`.

## `setTitle` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when the routine-name field changes.
Inputs: value: String
Dependencies: None.

## `setDescription` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when the routine-description field changes.
Inputs: value: String
Dependencies: None.

## `setPendingStepTitle` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when the add-step field changes.
Inputs: value: String
Dependencies: None.

## `setPendingTimerSeconds` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when the optional task-timer field changes.
Inputs: value: String
Dependencies: None.

## `addStep` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when adding a non-blank task to the current draft.
Inputs: defaultTitle: String
Dependencies: `RoutineStep`, `editStep`, `newStepId`, `updateStep`.

## `removeStep` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when removing one step from the current draft.
Inputs: index: Int
Dependencies: None.

## `save` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when Save is pressed for a valid routine draft.
Inputs: none.
Dependencies: `EntityMetadata`, `NextTriggerCalculator`, `ReminderRequest`, `RoutineTemplate`, `cancel`, `get`, `nextTriggerEpochMillis`, `now`, `requireRunnable`, `routine`, `schedule`, `step`, `updateStep`.

## `RoutineRunnerViewModel` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Coordinates runner UI and delegates every transition to the RoutineRunEngine.
Inputs: private val container: AppContainer, private val routineId: RoutineId,
Dependencies: `AppContainer`, `RoutineId`, `RunStatus`, `RunnerState`, `activeRun`, `now`, `observeActive`, `saveActive`, `startRun`, `tick`.

## `tick` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function on the runner clock cadence to update display and fire zero feedback once.
Inputs: none.
Dependencies: `RunStatus`, `acknowledgeTimerFeedback`, `activeRun`, `fire`, `needsTimerFeedback`, `now`, `saveActive`.

## `transition` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function to persist one ordinary active-run transition.
Inputs: change: (RoutineRun, Long) -> RoutineRun
Dependencies: `NextTriggerCalculator`, `ReminderRequest`, `RoutineRun`, `RunStatus`, `RunStepStatus`, `activeRun`, `cancel`, `nextTriggerAfterCompletionEpochMillis`, `now`, `saveActive`, `schedule`, `step`.

## `finish` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function to record a terminal run before clearing active-session storage.
Inputs: change: (RoutineRun, Long) -> RoutineRun
Dependencies: `RoutineRun`, `RunStatus`, `RunnerState`, `activeRun`, `append`, `clearActive`, `now`, `saveActive`, `toCompletionEvent`.

## `startRun` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when no compatible active run exists for the requested routine.
Inputs: none.
Dependencies: `ProgressSummary`, `RoutineRun`, `get`, `load`, `newRunId`, `now`, `routine`, `start`.

## `ProgressViewModel` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Projects local completion events into the small progress summary.
Inputs: container: AppContainer
Dependencies: `AppContainer`, `ProgressSummary`, `now`, `observeAll`, `projectProgress`.

## `SettingsViewModel` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Coordinates Settings UI with persisted user preferences.
Inputs: private val container: AppContainer
Dependencies: `AppContainer`, `UserPreferences`, `observe`.

## `TaskChainApp` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function as the Compose application entry point.
Inputs: container: AppContainer
Dependencies: `AppContainer`, `HomeShell`, `TaskChainTheme`, `RoutineBuilderRoute`, `RoutineId`, `RoutineRunnerRoute`, `UserPreferences`, `builderRoute`, `observe`, `onCreate`, `runnerRoute`.

## `HomeShell` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to render persistent tabs and preserve selected tab state.
Inputs: container: AppContainer, onCreate: () -> Unit, onEdit: (RoutineTemplate) -> Unit, onStart: (RoutineTemplate) -> Unit,
Dependencies: `AppContainer`, `HomeTab`, `ProgressRoute`, `ProgressViewModel`, `RoutineTemplate`, `RoutinesRoute`, `RoutinesViewModel`, `SettingsRoute`, `SettingsViewModel`, `TodayRoute`, `onCreate`.

## `TodayRoute` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to bind Today UI to the routine-list ViewModel.
Inputs: viewModel: RoutinesViewModel, padding: PaddingValues, listState: LazyListState, onStart: (RoutineTemplate) -> Unit
Dependencies: `RoutineList`, `RoutineTemplate`, `RoutinesViewModel`.

## `RoutinesRoute` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to bind the routine library UI to its ViewModel.
Inputs: viewModel: RoutinesViewModel, padding: PaddingValues, listState: LazyListState, onCreate: () -> Unit, onEdit: (RoutineTemplate) -> Unit, onStart: (RoutineTemplate) -> Unit,
Dependencies: `TaskChainDesignSystem`, `RoutineCard`, `RoutineTemplate`, `RoutinesViewModel`, `onCreate`, `routine`, `spacing`.

## `RoutineCard` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to render one routine consistently in Today and Routines.
Inputs: routine: RoutineTemplate, onEdit: (() -> Unit)?, onStart: () -> Unit
Dependencies: `TaskChainDesignSystem`, `RoutineTemplate`, `routine`, `spacing`.

## `RoutineList` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to render a titled routine list with an empty state.
Inputs: routines: List<RoutineTemplate>, padding: PaddingValues, listState: LazyListState, emptyText: String, onStart: (RoutineTemplate) -> Unit,
Dependencies: `TaskChainDesignSystem`, `RoutineCard`, `RoutineTemplate`, `routine`, `spacing`.

## `RoutineBuilderRoute` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to bind an editable routine draft to builder fields and actions.
Inputs: container: AppContainer, routineId: RoutineId?, onClose: () -> Unit
Dependencies: `AppContainer`, `TaskChainDesignSystem`, `RoutineBuilderViewModel`, `RoutineId`, `ScheduleEditor`, `addStep`, `cancel`, `clearLegacyRoutineSchedule`, `editStep`, `moveStep`, `removeStep`, `save`, `setDescription`, `setPendingDeadline`, `setPendingReminder`, `setPendingSoundEnabled`, `setPendingStackingAnchor`, `setPendingStepTitle`, `setPendingTimerSeconds`, `setPendingVibrateEnabled`, `setTitle`, `showDateTimePicker`, `spacing`, `step`, `updateStep`.

## `RoutineRunnerRoute` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to bind persisted run state to the stateless runner screen.
Inputs: container: AppContainer, preferences: UserPreferences, routineId: RoutineId, onFinished: () -> Unit,
Dependencies: `AppContainer`, `TaskChainDesignSystem`, `RoutineId`, `RoutineRunnerViewModel`, `RunStatus`, `UserPreferences`, `abort`, `back`, `complete`, `confirmComplete`, `continueRun`, `remainingMillis`, `retryHistorySave`, `selectStep`, `skip`, `spacing`, `statusText`, `timerText`, `unfinishedStepIndexes`.

## `statusText` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to map durable step state to localized UI text.
Inputs: status: RunStepStatus
Dependencies: `RunStepStatus`.

## `timerText` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to localize countdown, overtime, elapsed, and untimed displays.
Inputs: remainingMillis: Long?, actualDurationMillis: Long?, continuePastZero: Boolean
Dependencies: `remainingMillis`.

## `ProgressRoute` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to bind completion-history projections to Progress UI.
Inputs: viewModel: ProgressViewModel, padding: PaddingValues
Dependencies: `TaskChainDesignSystem`, `ProgressViewModel`, `spacing`.

## `SettingsRoute` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to bind persisted appearance and timer behavior to Settings UI.
Inputs: viewModel: SettingsViewModel, padding: PaddingValues
Dependencies: `TaskChainDesignSystem`, `SettingsViewModel`, `ThemeCatalog`, `options`, `setContinuePastZero`, `setTheme`, `spacing`.

## `builderRoute` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function when navigating to a new or existing builder route.
Inputs: routineId: RoutineId?
Dependencies: `RoutineId`.

## `runnerRoute` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function when starting a routine by stable identity.
Inputs: routineId: RoutineId
Dependencies: `RoutineId`.

## `ThemeOption` — app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt
Theme choice exposed to settings without assuming every theme has every variant.
Inputs: val id: String, @StringRes val label: Int, val supportedModes: Set<ThemeMode>,
Dependencies: `ThemeMode`.

## `ThemeCatalog` — app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt
Central catalog consumed by settings and the app theme boundary.
Inputs: context: android.content.Context
Dependencies: None.

## `Spacing` — app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt
Semantic spacing tokens loaded from Android resources rather than feature literals.
Inputs: val small: androidx.compose.ui.unit.Dp, val medium: androidx.compose.ui.unit.Dp, val large: androidx.compose.ui.unit.Dp,
Dependencies: None.

## `TaskChainDesignSystem` — app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt
Provides configured spacing to feature UI through the design-system boundary.
Inputs: none.
Dependencies: None.

## `TaskChainTheme` — app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt
Use this function at the app boundary so feature UI never configures Material directly.
Inputs: selectedTheme: String, content: @Composable () -> Unit
Dependencies: `ThemeCatalog`, `ThemeMode`, `options`.

## `RoutineRunEngineTest` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Exercises the run invariants most likely to regress as runner UI evolves.
Inputs: timerSeconds: Long? = null
Dependencies: None.

## `preservesRunStateAcrossNavigationAndConfirmation` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Use this function to verify skipped state, final confirmation, and timer restoration together.
Inputs: none.
Dependencies: `EntityMetadata`, `RoutineId`, `RoutineRunEngine`, `RoutineRunId`, `RoutineStep`, `RoutineStepId`, `RoutineTemplate`, `RunStatus`, `RunStepStatus`, `back`, `completeCurrent`, `confirmComplete`, `routine`, `selectStep`, `skipCurrent`, `start`, `unfinishedStepIndexes`.

## `RoutineTemplateTest` — app/src/test/java/com/taskchain/domain/model/RoutineTemplateTest.kt
Guards routine authoring against invalid anchors, goal links, and schedules before persistence.
Inputs: none.
Dependencies: None.

## `rejectsInvalidStepLinksAndScheduleRules` — app/src/test/java/com/taskchain/domain/model/RoutineTemplateTest.kt
Use this function when checking that malformed step and schedule drafts cannot become runnable routines.
Inputs: none.
Dependencies: `EntityMetadata`, `RoutineId`, `RoutineStep`, `RoutineStepId`, `RoutineTemplate`, `ScheduleFrequency`, `ScheduleRule`, `requireRunnable`, `routine`, `schedule`.

## `DailyCompletion` — app/src/main/java/com/taskchain/domain/progress/ProgressSummary.kt
One local calendar day of completion history for a seven-day trend.
Inputs: val dayStartEpochMillis: Long, val completedRuns: Int
Dependencies: None.

## `ProgressSummary` — app/src/main/java/com/taskchain/domain/progress/ProgressSummary.kt
Derived progress metrics that are never stored as mutable counters.
Inputs: val completedRuns: Int, val abortedRuns: Int, val actualDurationMillis: Long, val stepAdherencePercent: Int, val skippedStepPercent: Int, val lastSevenDays: List<DailyCompletion>,
Dependencies: `DailyCompletion`.

## `projectProgress` — app/src/main/java/com/taskchain/domain/progress/ProgressSummary.kt
Use this function when Progress needs a summary derived from durable completion events.
Inputs: events: List<CompletionEvent>, nowEpochMillis: Long, timeZone: TimeZone = TimeZone.getDefault(),
Dependencies: `CompletionEvent`, `DailyCompletion`, `ProgressSummary`, `RunStatus`, `RunStepStatus`, `localDayStart`.

## `localDayStart` — app/src/main/java/com/taskchain/domain/progress/ProgressSummary.kt
Use this function when grouping an event into a local-day progress trend.
Inputs: timestamp: Long, timeZone: TimeZone
Dependencies: None.

## `ProgressSummaryTest` — app/src/test/java/com/taskchain/domain/progress/ProgressSummaryTest.kt
Checks that progress metrics come from events rather than stored totals.
Inputs: none.
Dependencies: None.

## `projectsHistoryWithoutMutableCounters` — app/src/test/java/com/taskchain/domain/progress/ProgressSummaryTest.kt
Use this function to verify duration, step rates, and seven-day counts from local history.
Inputs: none.
Dependencies: `CompletionEvent`, `RoutineId`, `RoutineRunId`, `RoutineRunStep`, `RoutineStep`, `RoutineStepId`, `RunStatus`, `RunStepStatus`, `now`, `projectProgress`.

## `recoverTerminalSession` — app/src/main/java/com/taskchain/data/RunRecovery.kt
Use this function before showing the app after a crash interrupts terminal history persistence.
Inputs: activeRun: RoutineRunRepository, completions: CompletionRepository, engine: RoutineRunEngine,
Dependencies: `CompletionRepository`, `RoutineRunEngine`, `RoutineRunRepository`, `RunStatus`, `activeRun`, `append`, `clearActive`, `observeActive`, `toCompletionEvent`.

## `recoverTerminalRun` — app/src/main/java/com/taskchain/AppContainer.kt
Use this function before UI loads so an interrupted terminal run reaches append-only history.
Inputs: none.
Dependencies: `activeRun`, `recoverTerminalSession`.

## `RunRecoveryTest` — app/src/test/java/com/taskchain/data/RunRecoveryTest.kt
Checks recovery ordering at the active-run and append-only history boundary.
Inputs: none.
Dependencies: None.

## `recoversTerminalRunBeforeClearingIt` — app/src/test/java/com/taskchain/data/RunRecoveryTest.kt
Use this function to verify that a terminal session is appended before its active file is cleared.
Inputs: none.
Dependencies: `EntityMetadata`, `RoutineId`, `RoutineRun`, `RoutineRunEngine`, `RoutineRunId`, `RoutineRunRepository`, `RoutineStep`, `RoutineStepId`, `RoutineTemplate`, `completeCurrent`, `confirmComplete`, `routine`, `start`.

## `observeActive` — app/src/test/java/com/taskchain/data/RunRecoveryTest.kt
Use this function when the recovery check reads the fake active session.
Inputs: none.
Dependencies: `RoutineRun`.

## `saveActive` — app/src/test/java/com/taskchain/data/RunRecoveryTest.kt
Use this function if the recovery check needs to replace its fake active session.
Inputs: run: RoutineRun
Dependencies: `RoutineRun`.

## `clearActive` — app/src/test/java/com/taskchain/data/RunRecoveryTest.kt
Use this function after the fake history has accepted the terminal event.
Inputs: none.
Dependencies: `CompletionEvent`, `CompletionRepository`.

## `observeAll` — app/src/test/java/com/taskchain/data/RunRecoveryTest.kt
Use this function when a test consumer observes fake completion history.
Inputs: none.
Dependencies: `CompletionEvent`.

## `append` — app/src/test/java/com/taskchain/data/RunRecoveryTest.kt
Use this function to assert recovery appends before clearing the active session.
Inputs: event: CompletionEvent
Dependencies: `CompletionEvent`, `recoverTerminalSession`.

## `retryHistorySave` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when a terminal run could not yet be appended to durable history.
Inputs: none.
Dependencies: `RunStatus`, `RunnerState`, `now`, `recoverTerminalRun`.

## `NextTriggerCalculator` — app/src/main/java/com/taskchain/reminder/NextTriggerCalculator.kt
Calculates the next future local wall-clock occurrence for a saved schedule.
Inputs: rule: ScheduleRule, nowEpochMillis: Long, timeZone: TimeZone = TimeZone.getDefault(),
Dependencies: None.

## `nextTriggerEpochMillis` — app/src/main/java/com/taskchain/reminder/NextTriggerCalculator.kt
Use this function to convert a saved schedule into a future alarm timestamp.
Inputs: rule: ScheduleRule, nowEpochMillis: Long, timeZone: TimeZone = TimeZone.getDefault(),
Dependencies: `ScheduleFrequency`, `ScheduleRule`, `get`.

## `setPendingKind` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Choose task, habit, or goal behavior for a draft step.
Inputs: Uncertain: declaration source was not available.
Dependencies: Uncertain: direct project dependencies could not be confirmed from the available source.

## `setPendingDeadline` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Select an optional step deadline.
Inputs: value: Long?
Dependencies: None.

## `setPendingReminder` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Select or clear a one-time step reminder.
Inputs: value: Long?
Dependencies: None.

## `setPendingLinkedGoalId` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Enter the draft step's goal link.
Inputs: Uncertain: declaration source was not available.
Dependencies: Uncertain: direct project dependencies could not be confirmed from the available source.

## `setPendingStackingAnchor` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Choose an earlier step as a stack anchor.
Inputs: index: Int?
Dependencies: None.

## `editStep` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Load an existing step into editable draft fields.
Inputs: index: Int
Dependencies: `ScheduleFrequency`, `schedule`, `step`, `updateStep`.

## `moveStep` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Reorder steps while preserving valid anchor order.
Inputs: index: Int, offset: Int
Dependencies: `step`.

## `setScheduleEnabled` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Enable or disable a routine's local reminders.
Inputs: value: Boolean
Dependencies: None.

## `setScheduleFrequency` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Change a routine's recurrence pattern.
Inputs: value: ScheduleFrequency
Dependencies: `ScheduleFrequency`.

## `setScheduleTime` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Select a local reminder time.
Inputs: hour: Int, minute: Int
Dependencies: None.

## `setScheduleDays` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Change selected ISO weekdays for recurrence.
Inputs: value: Set<Int>
Dependencies: None.

## `setScheduleDate` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Select a one-time routine reminder.
Inputs: value: Long?
Dependencies: None.

## `ScheduleEditor` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Edit the optional local recurrence attached to a routine draft.
Inputs: state: BuilderState, viewModel: RoutineBuilderViewModel, spacing: Spacing
Dependencies: `BuilderState`, `RoutineBuilderViewModel`, `ScheduleFrequency`, `Spacing`, `cancel`, `dayLabel`, `frequencyLabel`, `setPendingRemindEveryMinutes`, `setScheduleDate`, `setScheduleDays`, `setScheduleEnabled`, `setScheduleFrequency`, `setScheduleTime`, `setTitle`, `showDateTimePicker`, `showTimePicker`, `spacing`.

## `frequencyLabel` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Map recurrence values to localized schedule labels.
Inputs: frequency: ScheduleFrequency
Dependencies: `ScheduleFrequency`.

## `dayLabel` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Show localized weekday names instead of storage integers.
Inputs: day: Int
Dependencies: None.

## `showDateTimePicker` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Select a deadline or reminder with native Android pickers.
Inputs: context: android.content.Context, onSelected: (Long) -> Unit
Dependencies: `get`.

## `showTimePicker` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Select a local schedule time with Android's native picker.
Inputs: context: android.content.Context, hour: Int, minute: Int, onSelected: (Int, Int) -> Unit
Dependencies: None.

## `ThemeMode` — app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt
Names the appearance variants a configured theme may support.
Inputs: context: android.content.Context
Dependencies: None.

## `options` — app/src/main/java/com/taskchain/ui/designsystem/TaskChainTheme.kt
Load configured appearance choices from Android resources.
Inputs: context: android.content.Context
Dependencies: `ThemeMode`, `ThemeOption`.

## `rearm` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Schedule the next recurring occurrence after alarm delivery.
Inputs: context: Context, intent: Intent, repeatNow: Boolean = true
Dependencies: `AndroidReminderScheduler`, `NextTriggerCalculator`, `ReminderRequest`, `ScheduleFrequency`, `ScheduleRule`, `nextRepeatedTriggerEpochMillis`, `nextTriggerEpochMillis`, `now`, `schedule`.

## `ReminderRescheduleReceiver` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Restore future saved alarms after reboot or local-time changes.
Inputs: none.
Dependencies: None.

## `onReceive` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Restore routine and step reminders after system time events.
Inputs: context: Context, intent: Intent
Dependencies: `AppContainer`, `NextTriggerCalculator`, `ReminderRequest`, `finish`, `nextTriggerEpochMillis`, `now`, `observeAll`, `routine`, `schedule`, `step`.

## `NextTriggerCalculatorTest` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Check recurrence boundaries without Android framework dependencies.
Inputs: none.
Dependencies: None.

## `dailyRollsToTomorrowAfterTodaysTime` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Verify daily recurrence crosses a month boundary.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`, `now`.

## `selectedDaysUseIsoMondayThroughSunday` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Verify ISO weekday mapping for selected days.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`, `now`.

## `onceInThePastHasNoTrigger` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Verify an expired one-time rule has no future alarm.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`, `now`.

## `springForwardUsesTheNextValidLocalTime` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Verify recurrence handles a missing daylight-saving time.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`, `now`.

## `invalidTimeOrEmptySelectionHasNoTrigger` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Verify malformed or empty recurrence has no alarm.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`, `now`.

## `routine` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Provide a small valid routine for run transition checks.
Inputs: timerSeconds: Long? = null
Dependencies: `EntityMetadata`, `RoutineId`, `RoutineStep`, `RoutineStepId`, `RoutineTemplate`.

## `transitionsRequireAnActiveRun` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Verify terminal runs reject further transitions.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `completeCurrent`, `confirmComplete`, `routine`, `selectStep`, `start`.

## `rejectsInvalidStepIndex` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Verify direct selection rejects out-of-range indexes.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `routine`, `selectStep`, `start`.

## `completingSkippedStepChangesOnlyItsStatus` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Verify revisiting a skipped step preserves timing.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `RunStepStatus`, `completeCurrent`, `routine`, `selectStep`, `skipCurrent`, `start`, `step`.

## `timerFeedbackIsAcknowledgedOnce` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Verify idempotent zero-feedback acknowledgement.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `acknowledgeTimerFeedback`, `needsTimerFeedback`, `routine`, `start`.

## `abortConfirmationControlsTerminalTransition` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Verify abort requires explicit confirmation.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `RunStatus`, `abort`, `back`, `routine`, `start`.

## `repeatedSkipCannotDemoteCompletedFinalStep` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Verify repeated Skip preserves completion and reopens confirmation.
Inputs: Uncertain: declaration source was not available.
Dependencies: Uncertain: direct project dependencies could not be confirmed from the available source.

## `terminalConfirmationProducesAccurateEvent` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Verify terminal event status and timestamps.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `RunStatus`, `completeCurrent`, `confirmComplete`, `routine`, `selectStep`, `start`, `toCompletionEvent`.

## `setPendingRemindEveryMinutes` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when a scheduled task's repeated prompt interval changes.
Inputs: value: Int?
Dependencies: None.

## `setPendingSoundEnabled` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when a task's audible notifications are enabled or disabled.
Inputs: value: Boolean
Dependencies: None.

## `setPendingVibrateEnabled` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when a task's haptic notifications are enabled or disabled.
Inputs: value: Boolean
Dependencies: None.

## `addStep` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when Add Step should create and expand a task without requiring a prefilled name.
Inputs: defaultTitle: String
Dependencies: `RoutineStep`, `ScheduleRule`, `editStep`, `moveStep`, `newStepId`, `routine`, `schedule`, `setScheduleEnabled`, `step`, `updateStep`.

## `updateStep` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when the expanded task's edited fields should replace its draft step.
Inputs: none.
Dependencies: `ScheduleRule`, `schedule`, `step`.

## `clearLegacyRoutineSchedule` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when the user removes an older routine-level alarm that cannot be migrated to a task.
Inputs: none.
Dependencies: None.

## `nextRepeatedTriggerEpochMillis` — app/src/main/java/com/taskchain/reminder/NextTriggerCalculator.kt
Use this function when a scheduled task repeats prompts but must yield to its next regular occurrence.
Inputs: rule: ScheduleRule, deliveredAtEpochMillis: Long, minutes: Int, timeZone: TimeZone = TimeZone.getDefault(),
Dependencies: `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`.

## `repeatedPromptYieldsToNextOccurrence` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Use this function to verify a repeated prompt yields to the next regular task occurrence.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextRepeatedTriggerEpochMillis`.

## `nextTriggerAfterCompletionEpochMillis` — app/src/main/java/com/taskchain/reminder/NextTriggerCalculator.kt
Use this function after completing a scheduled task to start recurrence on a later local calendar day.
Inputs: rule: ScheduleRule, completedAtEpochMillis: Long, timeZone: TimeZone = TimeZone.getDefault(),
Dependencies: `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`.

## `projectTodayRoutines` — app/src/main/java/com/taskchain/domain/home/TodayProjection.kt
Use this function when Home needs only the scheduled occurrences actionable on the current local date.
Inputs: routines: List<RoutineTemplate>, activeRun: RoutineRun?, history: List<CompletionEvent>, nowEpochMillis: Long, timeZone: TimeZone = TimeZone.getDefault(),
Dependencies: `CompletionEvent`, `RoutineRun`, `RoutineTemplate`, `RunStepStatus`, `ScheduleFrequency`, `activeRun`, `get`, `routine`, `schedule`, `step`.

## `completedSinceCycle` — app/src/main/java/com/taskchain/reminder/ReminderScheduler.kt
Use this function when a repeated task alert must honor a completion persisted in an active run or history.
Inputs: routineId: String, stepId: String, cycleStartEpochMillis: Long, activeRun: RoutineRun?, history: List<CompletionEvent>,
Dependencies: `CompletionEvent`, `RoutineRun`, `RunStepStatus`, `activeRun`, `step`.

## `ReminderRepeatTest` — app/src/test/java/com/taskchain/reminder/ReminderRepeatTest.kt
Verifies that repeated alerts stop only for the task's current scheduled cycle.
Inputs: none.
Dependencies: None.

## `completedTaskStopsOnlyItsCurrentCycle` — app/src/test/java/com/taskchain/reminder/ReminderRepeatTest.kt
Use this function to verify active and historical completions stop repeats without hiding a new occurrence.
Inputs: none.
Dependencies: `CompletionEvent`, `RoutineId`, `RoutineRun`, `RoutineRunId`, `RoutineRunStep`, `RoutineStep`, `RoutineStepId`, `RunStatus`, `RunStepStatus`, `completedSinceCycle`, `step`.

## `ScheduleFrequency` — app/src/main/java/com/taskchain/domain/model/Models.kt
Names the recurrence modes supported by routine and task schedules.
Inputs: val frequency: ScheduleFrequency, val localHour: Int, val localMinute: Int, val daysOfWeek: Set<Int> = emptySet(), val oneTimeEpochMillis: Long? = null,
Dependencies: None.

## `RunStepStatus` — app/src/main/java/com/taskchain/domain/model/Models.kt
Names the durable states of a step within a routine run.
Inputs: val source: RoutineStep, val status: RunStepStatus = RunStepStatus.PENDING, val startedAtEpochMillis: Long? = null, val finishedAtEpochMillis: Long? = null, val completedAtEpochMillis: Long? = null, val actualDurationMillis: Long? = null, val timerFeedbackAtEpochMillis: Long? = null,
Dependencies: None.

## `RunStatus` — app/src/main/java/com/taskchain/domain/model/Models.kt
Names the durable lifecycle states of a routine run.
Inputs: val source: RoutineStep, val status: RunStepStatus = RunStepStatus.PENDING, val startedAtEpochMillis: Long? = null, val finishedAtEpochMillis: Long? = null, val completedAtEpochMillis: Long? = null, val actualDurationMillis: Long? = null, val timerFeedbackAtEpochMillis: Long? = null,
Dependencies: None.

## `RoutinesState` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Carries routine-library, built-in, and Today projections to the list UI.
Inputs: val routines: List<RoutineTemplate> = emptyList(), val builtIns: List<RoutineTemplate> = emptyList(), val todayRoutines: List<RoutineTemplate> = emptyList(),
Dependencies: `RoutineTemplate`.

## `BuilderState` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Carries the editable routine and step fields used by the builder UI.
Inputs: val title: String = , val description: String = , val steps: List<RoutineStep> = emptyList(), val pendingStepTitle: String = , val pendingTimerSeconds: String = , val pendingDeadlineEpochMillis: Long? = null, val pendingReminderAtEpochMillis: Long? = null, val pendingRemindEveryMinutes: Int? = null, val pendingSoundEnabled: Boolean = true, val pendingVibrateEnabled: Boolean = true, val pendingStackingAnchorIndex: Int? = null, val editingStepIndex: Int? = null, val scheduleEnabled: Boolean = false, val scheduleFrequency: ScheduleFrequency = ScheduleFrequency.DAILY, val scheduleHour: Int = 9, val scheduleMinute: Int = 0, val scheduleDaysOfWeek: Set<Int> = emptySet(), val scheduleOneTimeEpochMillis: Long? = null, val legacyRoutineSchedule: ScheduleRule? = null, val savedRoutineId: RoutineId? = null, val saveError: Boolean = false,
Dependencies: `RoutineId`, `RoutineStep`, `ScheduleFrequency`, `ScheduleRule`.

## `RunnerState` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Carries persisted runner state, display time, terminal status, and history-save failure state.
Inputs: val run: RoutineRun? = null, val nowEpochMillis: Long = 0, val finished: Boolean = false, val historySaveFailed: Boolean = false,
Dependencies: `RoutineRun`.

## `complete` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when the runner user completes the displayed task.
Inputs: none.
Dependencies: `completeCurrent`, `now`, `transition`.

## `skip` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when the runner user skips the displayed task.
Inputs: none.
Dependencies: `now`, `skipCurrent`, `transition`.

## `setContinuePastZero` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function when Settings changes whether timers continue past zero.
Inputs: enabled: Boolean
Dependencies: `setContinueTimerPastZero`.

## `HomeTab` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Represents one persistent top-level tab in the Compose home shell.
Inputs: @param:StringRes val label: Int
Dependencies: None.

## `TodayProjectionTest` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Verifies that Home exposes only currently actionable routine occurrences.
Inputs: none.
Dependencies: `local`, `now`.

## `keepsRoutinesWithoutScheduledSteps` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to verify unscheduled routines remain available on Home.
Inputs: none.
Dependencies: `now`, `projectTodayRoutines`, `routine`, `step`.

## `projectsRecurringFrequenciesForToday` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to verify daily, weekday, and selected-day schedules use the current local weekday.
Inputs: none.
Dependencies: `ScheduleFrequency`, `now`, `projectTodayRoutines`, `routine`, `step`.

## `excludesPriorOneTimeAndSelectedDayOccurrences` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to verify stale one-time and selected-day occurrences are excluded.
Inputs: none.
Dependencies: `RoutineId`, `ScheduleFrequency`, `local`, `now`, `projectTodayRoutines`, `routine`, `step`.

## `projectsOneTimeOccurrenceUsingInjectedTimeZone` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to verify one-time occurrence dates use the supplied time zone.
Inputs: none.
Dependencies: `RoutineId`, `ScheduleFrequency`, `local`, `projectTodayRoutines`, `routine`, `step`.

## `keepsRoutineUntilEveryScheduledStepIsCompleted` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to verify one completed scheduled step does not hide its routine while another remains actionable.
Inputs: none.
Dependencies: `RoutineId`, `ScheduleFrequency`, `completion`, `now`, `projectTodayRoutines`, `routine`, `step`.

## `olderCompletionDoesNotHideToday` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to verify a prior-day completion does not hide today’s recurring occurrence.
Inputs: none.
Dependencies: `ScheduleFrequency`, `completion`, `local`, `now`, `projectTodayRoutines`, `routine`, `step`.

## `activeRunCompletionCountsImmediately` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to verify a completed step in the active run immediately hides today’s occurrence.
Inputs: none.
Dependencies: `RoutineId`, `ScheduleFrequency`, `activeRun`, `now`, `projectTodayRoutines`, `routine`, `step`.

## `skippedActiveRunStepDoesNotHideToday` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to verify a skipped active-run step remains actionable.
Inputs: none.
Dependencies: `RoutineRunStep`, `RunStepStatus`, `ScheduleFrequency`, `activeRun`, `now`, `projectTodayRoutines`, `routine`, `step`.

## `step` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to build a test step with an optional schedule.
Inputs: id: String, frequency: ScheduleFrequency? = null, days: Set<Int> = emptySet(), oneTime: Long? = null,
Dependencies: `RoutineStep`, `RoutineStepId`, `ScheduleFrequency`, `ScheduleRule`, `schedule`.

## `completion` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to represent a completed historical step for projection tests.
Inputs: routine: RoutineTemplate, step: RoutineStep, completedAt: Long
Dependencies: `CompletionEvent`, `RoutineRunId`, `RoutineRunStep`, `RoutineStep`, `RoutineTemplate`, `RunStatus`, `RunStepStatus`, `routine`, `step`.

## `activeRun` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to represent an active run whose step has completed today.
Inputs: routine: RoutineTemplate, step: RoutineStep
Dependencies: `RoutineRun`, `RoutineRunId`, `RoutineRunStep`, `RoutineStep`, `RoutineTemplate`, `RunStepStatus`, `now`, `routine`, `step`.

## `local` — app/src/test/java/com/taskchain/domain/home/TodayProjectionTest.kt
Use this function to parse deterministic local wall-clock timestamps for projection tests.
Inputs: value: String
Dependencies: None.

## `cancelledFinalCompleteRestoresPendingBeforeSkip` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Use this function to verify cancelling final-step completion restores pending state before a later skip.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `RunStepStatus`, `completeCurrent`, `continueRun`, `routine`, `selectStep`, `skipCurrent`, `start`.

## `cancellingFinalUntimedSkipRestoresPendingWithoutStartingTiming` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Use this function to verify cancelling an untimed final-step skip restores pending state without timing data.
Inputs: none.
Dependencies: `EntityMetadata`, `RoutineId`, `RoutineRunEngine`, `RoutineRunId`, `RoutineStep`, `RoutineStepId`, `RoutineTemplate`, `RunStepStatus`, `continueRun`, `remainingMillis`, `routine`, `skipCurrent`, `start`, `step`.

## `cancellingFinalCompleteRestoresTimedStepBeforeDialogPause` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Use this function to verify cancelling final completion restores the timed step before the confirmation pause.
Inputs: none.
Dependencies: `EntityMetadata`, `RoutineId`, `RoutineRunEngine`, `RoutineRunId`, `RoutineStep`, `RoutineStepId`, `RoutineTemplate`, `RunStepStatus`, `completeCurrent`, `continueRun`, `remainingMillis`, `routine`, `start`, `step`.

## `abortConfirmationPausesPendingTimer` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Use this function to verify abort confirmation pauses a pending timer without losing its prior state.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `back`, `continueRun`, `remainingMillis`, `routine`, `start`.

## `skippingPendingFinalStepRequestsConfirmationWithAllUnfinishedTasks` — app/src/test/java/com/taskchain/domain/run/RoutineRunEngineTest.kt
Use this function to verify skipping the pending final step requests confirmation and reports all unfinished tasks.
Inputs: none.
Dependencies: `RoutineRunEngine`, `RoutineRunId`, `RunStepStatus`, `routine`, `selectStep`, `skipCurrent`, `start`, `unfinishedStepIndexes`.

## `futureOnceUsesStoredEpoch` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Use this function to verify a future one-time schedule returns its stored epoch timestamp.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`, `now`.

## `weekdaysSkipWeekend` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Use this function to verify weekday recurrence skips weekend dates.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`, `now`.

## `selectedSundayWrapsToFollowingWeek` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Use this function to verify selected Sunday recurrence wraps to the following week.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerEpochMillis`, `now`.

## `completionBeforeDailyScheduleMovesToTomorrow` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Use this function to verify completion before a daily schedule moves its next trigger to tomorrow.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerAfterCompletionEpochMillis`.

## `completionBeforeWeekdayScheduleMovesToMonday` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Use this function to verify completion before a weekday schedule moves its next trigger to Monday.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerAfterCompletionEpochMillis`.

## `completionBeforeSelectedDayMovesToNextSelectedDay` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Use this function to verify completion before a selected-day schedule advances to the next selected day.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerAfterCompletionEpochMillis`.

## `completionOfOnceHasNoTrigger` — app/src/test/java/com/taskchain/reminder/NextTriggerCalculatorTest.kt
Use this function to verify a completed one-time schedule produces no next trigger.
Inputs: none.
Dependencies: `NextTriggerCalculator`, `ScheduleFrequency`, `ScheduleRule`, `nextTriggerAfterCompletionEpochMillis`.

## `BuilderValidationError` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Identifies a routine-builder field or persistence failure that the UI can explain.
Constructor inputs: none.
Dependencies: `BuilderState`, `RoutineBuilderViewModel`.

## `validateBuilderState` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function to return field-specific errors for the current builder draft at a fixed time.
Inputs: `state` — the editable draft; `nowEpochMillis` — the fixed comparison clock.
Dependencies: `BuilderState`, `BuilderValidationError`, `ScheduleFrequency`, `ScheduleRule`.

## `mutateDraft` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function to apply a user draft mutation while preserving only still-relevant validation errors.
Inputs: `transform` — the state change requested by the user.
Dependencies: `BuilderState`, `editableContent`, `validateBuilderState`, `AppContainer.now`.

## `editableContent` — app/src/main/java/com/taskchain/ui/FeatureViewModels.kt
Use this function to compare draft fields without treating UI errors or save flags as edits.
Inputs: receiver `BuilderState` — the state being normalized for comparison.
Dependencies: `BuilderState.copy`.

## `BuilderValidationTest` — app/src/test/java/com/taskchain/ui/BuilderValidationTest.kt
Verifies deterministic routine-builder validation without Android or persistence setup.
Constructor inputs: none.
Dependencies: `BuilderState`, `BuilderValidationError`, `validateBuilderState`.

## `step` — app/src/test/java/com/taskchain/ui/BuilderValidationTest.kt
Use this function to create the smallest valid step for validation scenarios.
Inputs: `title` — the step title and stable test identity.
Dependencies: `RoutineStep`, `RoutineStepId`.

## `reportsAllFieldSpecificErrors` — app/src/test/java/com/taskchain/ui/BuilderValidationTest.kt
Use this function to verify every typed builder validation error and strict time boundary.
Inputs: none.
Dependencies: `BuilderState`, `BuilderValidationError`, `validateBuilderState`.

## `acceptsFutureScheduleAndReminderValues` — app/src/test/java/com/taskchain/ui/BuilderValidationTest.kt
Use this function to verify future selected-day, one-time, and reminder values pass validation.
Inputs: none.
Dependencies: `BuilderState`, `ScheduleRule`, `validateBuilderState`.

## `reportsPendingStepNameWithoutChangingInput` — app/src/test/java/com/taskchain/ui/BuilderValidationTest.kt
Use this function to verify invalid pending input is reported without rewriting the draft.
Inputs: none.
Dependencies: `BuilderState`, `BuilderValidationError`, `validateBuilderState`.

## `LabeledSwitchRow` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function when a labeled setting should expose one accessible switch target for its whole row.
Inputs: `label` — visible and spoken label; `checked` — current state; `onCheckedChange` — state-change callback.
Dependencies: `Row`, `Switch`, `Role.Switch`.

## `WeekdaySelector` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to render selectable weekdays with visible and semantic selected state.
Inputs: `selectedDays` — selected weekday numbers; `onDayToggle` — selected day callback.
Dependencies: `FilterChip`, `dayLabel`, `TaskChainDesignSystem`.

## `formatDateTime` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to format a selected device date and time for builder controls.
Inputs: `context` — Android locale and clock-format source; `epochMillis` — selected instant.
Dependencies: `DateFormat`, `Date`.

## `formatScheduleTime` — app/src/main/java/com/taskchain/ui/TaskChainApp.kt
Use this function to format a recurring schedule time with the device 12/24-hour preference.
Inputs: `context` — Android clock-format source; `hour` — local hour; `minute` — local minute.
Dependencies: `Calendar`, `DateFormat`, `stringResource`.

## `TaskChainComponentTest` — app/src/androidTest/java/com/taskchain/ui/TaskChainComponentTest.kt
Verifies accessible semantics and state transitions of reusable Compose controls.
Constructor inputs: none.
Dependencies: `LabeledSwitchRow`, `WeekdaySelector`, `RoutineList`, `TaskChainTheme`.

## `labeledSwitchRow_mergesLabelAndTogglesExactlyOncePerClick` — app/src/androidTest/java/com/taskchain/ui/TaskChainComponentTest.kt
Use this function to verify a labeled switch exposes one merged stateful target and toggles once per click.
Inputs: none.
Dependencies: `LabeledSwitchRow`, Compose semantics assertions.

## `weekdaySelector_eachDayCanBeSelected` — app/src/androidTest/java/com/taskchain/ui/TaskChainComponentTest.kt
Use this function to verify every weekday chip exposes and updates selected semantics.
Inputs: none.
Dependencies: `WeekdaySelector`, Compose selection assertions.

## `routineList_emptyStateShowsCreateAction` — app/src/androidTest/java/com/taskchain/ui/TaskChainComponentTest.kt
Use this function to verify the empty routine list exposes a creation action that fires once.
Inputs: none.
Dependencies: `RoutineList`, `rememberLazyListState`, Compose click assertions.

## `MainActivityTest` — app/src/androidTest/java/com/taskchain/MainActivityTest.kt
Exercises the published navigation and builder flows through the real MainActivity composition.
Constructor inputs: none.
Dependencies: `MainActivity`, Compose Android test rule.

## `builderCancelAndBack_offerDiscardChoicesAndKeepDraftInput` — app/src/androidTest/java/com/taskchain/MainActivityTest.kt
Use this function to verify Cancel and Android Back offer discard choices without losing a draft.
Inputs: none.
Dependencies: `MainActivity`, `BackHandler`, Compose text actions.

## `routines_keepCardTextInertAndExposeSeparateEditAndStartActions` — app/src/androidTest/java/com/taskchain/MainActivityTest.kt
Use this function to verify routine-card text is inert while Edit and Start remain separate actions.
Inputs: none.
Dependencies: `MainActivity`, routine cards, runner navigation.
