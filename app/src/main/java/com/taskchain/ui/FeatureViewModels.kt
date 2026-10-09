package com.taskchain.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.AppContainer
import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineSubtaskId
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import com.taskchain.domain.model.SoundToken
import com.taskchain.domain.model.SoundSettings
import com.taskchain.domain.model.UserPreferences
import com.taskchain.domain.model.defaultSoundSettings
import com.taskchain.domain.home.projectTodayRoutines
import com.taskchain.domain.home.TodayProjection
import com.taskchain.domain.progress.ProgressSummary
import com.taskchain.domain.progress.projectProgress
import com.taskchain.domain.run.RunFeedbackEvent
import com.taskchain.domain.run.RunFeedbackPolicy
import com.taskchain.domain.schedule.NextTriggerCalculator
import com.taskchain.reminder.toReminderRequest
import com.taskchain.ui.designsystem.RunnerMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** UI state for the local and predefined routine lists. */
data class RoutinesState(
    val routines: List<RoutineTemplate> = emptyList(),
    val builtIns: List<RoutineTemplate> = emptyList(),
    val todayRoutines: TodayProjection = TodayProjection(emptyList(), emptyList(), emptyList()),
)

/** Coordinates routine lists while leaving routine rules in domain models. */
class RoutinesViewModel(private val container: AppContainer) : ViewModel() {
    private val builtIns = MutableStateFlow<List<RoutineTemplate>>(emptyList())
    private val clock = flow {
        while (true) {
            emit(container.now())
            delay(HOME_REFRESH_MILLIS)
        }
    }

    val state: StateFlow<RoutinesState> = combine(
        container.routines.observeAll(),
        builtIns,
        container.activeRun.observeActive(),
        container.completions.observeAll(),
        clock,
    ) { routines, library, activeRun, history, now ->
        val visibleBuiltIns = library.filterNot { builtIn -> routines.any { it.id == builtIn.id } }
        val homeSource = routines.ifEmpty { visibleBuiltIns }
        RoutinesState(
            routines = routines,
            builtIns = visibleBuiltIns,
            todayRoutines = projectTodayRoutines(homeSource, activeRun, history, now),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RoutinesState())

    /** The saved run that is still in progress, or null. */
    val activeRun: StateFlow<RoutineRun?> = container.activeRun.observeActive()
        .map { run -> run?.takeIf { it.status == RunStatus.ACTIVE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val mutableBlockedRoutine = MutableStateFlow<RoutineTemplate?>(null)

    /** The routine that cannot start because a different run is active; the screen explains the conflict while set. */
    val blockedRoutine: StateFlow<RoutineTemplate?> = mutableBlockedRoutine.asStateFlow()

    init {
        viewModelScope.launch { builtIns.value = container.builtInLibrary.load(container.now()) }
    }

    private val mutablePendingStart = MutableStateFlow<RoutineTemplate?>(null)

    /** The routine whose runner the screen opens next; the screen calls `startHandled` after it navigates. */
    val pendingStart: StateFlow<RoutineTemplate?> = mutablePendingStart.asStateFlow()

    /** Use this function when the user starts a routine. It sets either `pendingStart` or `blockedRoutine`. */
    fun requestStart(routine: RoutineTemplate) {
        val run = activeRun.value
        if (run != null && run.routineId != routine.id) {
            mutableBlockedRoutine.value = routine
        } else {
            mutablePendingStart.value = routine
        }
    }

    /** Use this function after the screen opens the runner for `pendingStart`. */
    fun startHandled() {
        mutablePendingStart.value = null
    }

    /** Use this function when the user closes the active-run conflict dialog. */
    fun dismissBlockedStart() {
        mutableBlockedRoutine.value = null
    }

    private companion object {
        const val HOME_REFRESH_MILLIS = 60_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Identifies a routine-builder field or persistence failure that the UI can explain. */
enum class BuilderValidationError {
    ROUTINE_NAME_REQUIRED,
    TASK_REQUIRED,
    TASK_NAME_REQUIRED,
    SELECTED_DAY_REQUIRED,
    SCHEDULE_DATE_REQUIRED,
    SCHEDULE_DATE_MUST_BE_FUTURE,
    REMINDER_MUST_BE_FUTURE,
    SCHEDULE_REMINDER_EXCLUSIVE,
    LEGACY_SETTINGS_CONFLICT,
    SUBTASK_TITLE_REQUIRED,
    SUBTASK_DURATION_INVALID,
    SUBTASK_TOTAL_TOO_LONG,
    SUBTASK_ID_DUPLICATED,
    SAVE_FAILED,
}

private const val DEFAULT_SUBTASK_DURATION_SECONDS = 60L
private const val MAX_SAFE_SUBTASK_DURATION_SECONDS = Long.MAX_VALUE / 1_000L

/** Timer of a new task, and the picker's start value for a task without a timer. */
internal const val DEFAULT_TASK_TIMER_SECONDS = 300L

/** Largest minute value that the task duration picker offers. */
internal const val TASK_TIMER_MAX_MINUTES = 1440

/** Use this function to convert a duration picker selection to a task timer. 0:00 means no timer; any other result is positive. */
internal fun taskTimerFromPicker(minutes: Int, seconds: Int): Long? =
    (minutes.toLong() * 60L + seconds).takeIf { it > 0L }

private val ROUTINE_SETTINGS_ERRORS = setOf(
    BuilderValidationError.SELECTED_DAY_REQUIRED,
    BuilderValidationError.SCHEDULE_DATE_REQUIRED,
    BuilderValidationError.SCHEDULE_DATE_MUST_BE_FUTURE,
    BuilderValidationError.REMINDER_MUST_BE_FUTURE,
    BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE,
    BuilderValidationError.LEGACY_SETTINGS_CONFLICT,
)

/** Identifies the builder section that the screen scrolls to after a failed save. */
sealed interface BuilderRevealTarget {
    data object RoutineName : BuilderRevealTarget
    data object RoutineSettings : BuilderRevealTarget
    data object TaskHeader : BuilderRevealTarget
    data class Task(val index: Int) : BuilderRevealTarget
}

/** Editable state for one routine builder route. */
data class BuilderState(
    val title: String = "",
    val description: String = "",
    val tasks: List<RoutineTask> = emptyList(),
    val pendingTaskTitle: String = "",
    val pendingSubtaskDurations: Map<String, String> = emptyMap(),
    val deadlineEpochMillis: Long? = null,
    val reminderAtEpochMillis: Long? = null,
    val remindEveryMinutes: Int? = null,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val soundSettings: SoundSettings = defaultSoundSettings(),
    val editingTaskIndex: Int? = null,
    val expandedTaskId: RoutineTaskId? = null,
    val editingNameTaskId: RoutineTaskId? = null,
    val scheduleEnabled: Boolean = false,
    val scheduleFrequency: ScheduleFrequency = ScheduleFrequency.DAILY,
    val scheduleHour: Int = 9,
    val scheduleMinute: Int = 0,
    val scheduleDaysOfWeek: Set<Int> = emptySet(),
    val scheduleOneTimeEpochMillis: Long? = null,
    val savedRoutineId: RoutineId? = null,
    val validationErrors: Set<BuilderValidationError> = emptySet(),
    val failedSaveCount: Int = 0,
    val revealTarget: BuilderRevealTarget? = null,
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
) {
    val invalidSubtaskDurationIds: Set<RoutineSubtaskId>
        get() = tasks.flatMap { task ->
            task.subtasks.mapNotNull { subtask ->
                val raw = pendingSubtaskDurations["${task.id.value}:${subtask.id.value}"]
                if (raw != null && raw.trim().toLongOrNull()?.let { it in 1L..MAX_SAFE_SUBTASK_DURATION_SECONDS } != true) subtask.id else null
            }
        }.toSet()
}

/**
 * Use this function to return field-specific errors for the current builder draft at a fixed time.
 * Inputs: `state` — the editable draft; `nowEpochMillis` — the fixed comparison clock.
 * Dependencies: `BuilderState`, `ScheduleRule`, and `ScheduleFrequency`.
 */
internal fun validateBuilderState(
    state: BuilderState,
    nowEpochMillis: Long,
): Set<BuilderValidationError> {
    val errors = mutableSetOf<BuilderValidationError>()
    if (state.title.isBlank()) errors += BuilderValidationError.ROUTINE_NAME_REQUIRED
    if (state.tasks.isEmpty()) errors += BuilderValidationError.TASK_REQUIRED

    state.editingTaskIndex?.takeIf { it in state.tasks.indices }?.let {
        if (state.pendingTaskTitle.isBlank()) errors += BuilderValidationError.TASK_NAME_REQUIRED
    }
    state.tasks.forEach { task ->
        if (task.title.isBlank()) errors += BuilderValidationError.TASK_NAME_REQUIRED
    }
    val schedule = if (state.scheduleEnabled) ScheduleRule(
        state.scheduleFrequency, state.scheduleHour, state.scheduleMinute,
        state.scheduleDaysOfWeek, state.scheduleOneTimeEpochMillis,
    ) else null
    schedule?.let {
        if (it.frequency == ScheduleFrequency.SELECTED_DAYS && it.daysOfWeek.isEmpty()) {
            errors += BuilderValidationError.SELECTED_DAY_REQUIRED
        }
        if (it.frequency == ScheduleFrequency.ONCE) {
            when (val date = it.oneTimeEpochMillis) {
                null -> errors += BuilderValidationError.SCHEDULE_DATE_REQUIRED
                else -> if (date <= nowEpochMillis) errors += BuilderValidationError.SCHEDULE_DATE_MUST_BE_FUTURE
            }
        }
    }
    if (state.reminderAtEpochMillis?.let { it <= nowEpochMillis } == true) errors += BuilderValidationError.REMINDER_MUST_BE_FUTURE
    if ((schedule != null && (state.deadlineEpochMillis != null || state.reminderAtEpochMillis != null)) ||
        (state.deadlineEpochMillis != null && state.reminderAtEpochMillis != null) ||
        (state.remindEveryMinutes != null && (schedule == null || state.remindEveryMinutes <= 0))
    ) {
        errors += BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE
    }
    if (state.hasConflictingLegacyRoutineSettings()) errors += BuilderValidationError.LEGACY_SETTINGS_CONFLICT
    state.tasks.forEach { errors += state.subtaskErrorsFor(it) }
    return errors
}

/**
 * Use this function to list the subtask errors that belong to one task, so the builder shows each message
 * only under the task that has the problem. Inputs: `task` — one task of the receiver draft.
 */
internal fun BuilderState.subtaskErrorsFor(task: RoutineTask): Set<BuilderValidationError> {
    val errors = mutableSetOf<BuilderValidationError>()
    val subtaskIdCounts = tasks.flatMap { it.subtasks }.groupingBy { it.id }.eachCount()
    val invalidDurationIds = invalidSubtaskDurationIds
    var subtaskTotal = 0L
    var subtaskDurationOverflow = false
    task.subtasks.forEach { subtask ->
        if (subtask.title.isBlank()) errors += BuilderValidationError.SUBTASK_TITLE_REQUIRED
        if ((subtaskIdCounts[subtask.id] ?: 0) > 1) errors += BuilderValidationError.SUBTASK_ID_DUPLICATED
        if (subtask.id in invalidDurationIds || subtask.durationSeconds <= 0L) {
            errors += BuilderValidationError.SUBTASK_DURATION_INVALID
        }
        if (subtask.durationSeconds > MAX_SAFE_SUBTASK_DURATION_SECONDS ||
            (subtask.durationSeconds > 0L && subtaskTotal > MAX_SAFE_SUBTASK_DURATION_SECONDS - subtask.durationSeconds)
        ) {
            subtaskDurationOverflow = true
        } else if (!subtaskDurationOverflow) {
            subtaskTotal += subtask.durationSeconds
        }
    }
    if (subtaskDurationOverflow) errors += BuilderValidationError.SUBTASK_TOTAL_TOO_LONG
    return errors
}

/** Use this function to list the shown errors that belong to the task at `index`, including its subtasks. */
internal fun BuilderState.taskErrorsFor(index: Int): Set<BuilderValidationError> {
    val task = tasks.getOrNull(index) ?: return emptySet()
    val errors = subtaskErrorsFor(task).toMutableSet()
    if (task.title.isBlank()) errors += BuilderValidationError.TASK_NAME_REQUIRED
    return errors intersect validationErrors
}

/** Use this function after a failed save to find the first task with a shown error. */
internal fun BuilderState.firstTaskIndexWithErrors(): Int? =
    tasks.indices.firstOrNull { taskErrorsFor(it).isNotEmpty() }

/** Use this function to expand `taskId` for editing. Returns null when the task is missing. */
internal fun BuilderState.expandingTask(taskId: RoutineTaskId): BuilderState? {
    val index = tasks.indexOfFirst { it.id == taskId }.takeIf { it >= 0 } ?: return null
    return copy(editingTaskIndex = index, pendingTaskTitle = tasks[index].title, expandedTaskId = taskId, editingNameTaskId = null)
}

/**
 * Use this function after a failed save. It expands the first task with errors, opens its name field when
 * the name is missing, and sets the section that the screen scrolls to.
 */
internal fun BuilderState.revealingFirstError(): BuilderState {
    val taskIndex = firstTaskIndexWithErrors()
    val revealed = taskIndex?.let { index ->
        val taskId = tasks[index].id
        expandingTask(taskId)?.copy(
            editingNameTaskId = taskId.takeIf { BuilderValidationError.TASK_NAME_REQUIRED in taskErrorsFor(index) },
        )
    } ?: this
    return revealed.copy(
        revealTarget = when {
            BuilderValidationError.ROUTINE_NAME_REQUIRED in validationErrors -> BuilderRevealTarget.RoutineName
            validationErrors.any { it in ROUTINE_SETTINGS_ERRORS } -> BuilderRevealTarget.RoutineSettings
            BuilderValidationError.TASK_REQUIRED in validationErrors -> BuilderRevealTarget.TaskHeader
            else -> taskIndex?.let(BuilderRevealTarget::Task)
        },
    )
}

private fun BuilderState.hasConflictingLegacyRoutineSettings(): Boolean {
    if (tasks.none {
        it.stackingAnchorTaskId != null || it.deadlineEpochMillis != null || it.reminderAtEpochMillis != null ||
            it.schedule != null || it.remindEveryMinutes != null
        }
    ) return false
    val routineSchedule = if (scheduleEnabled) ScheduleRule(
        scheduleFrequency, scheduleHour, scheduleMinute, scheduleDaysOfWeek, scheduleOneTimeEpochMillis,
    ) else null
    val schedules = listOfNotNull(routineSchedule) + tasks.mapNotNull { it.schedule }
    val deadlines = listOfNotNull(deadlineEpochMillis) + tasks.mapNotNull { it.deadlineEpochMillis }
    val reminders = listOfNotNull(reminderAtEpochMillis) + tasks.mapNotNull { it.reminderAtEpochMillis }
    val repeats = listOfNotNull(remindEveryMinutes) + tasks.mapNotNull { it.remindEveryMinutes }
    val choiceCount = listOf(schedules, deadlines, reminders).count { it.isNotEmpty() }
    return schedules.distinct().size > 1 || deadlines.distinct().size > 1 ||
        reminders.distinct().size > 1 || repeats.distinct().size > 1 || choiceCount > 1 ||
        (repeats.isNotEmpty() && schedules.isEmpty())
}

private fun BuilderState.clearLegacyRoutineSettings(): BuilderState = copy(
    tasks = tasks.map {
        it.copy(
            deadlineEpochMillis = null,
            reminderAtEpochMillis = null,
            schedule = null,
            remindEveryMinutes = null,
        )
    },
)

internal fun RoutineTemplate.toBuilderState(): BuilderState {
    val legacySchedule = tasks.firstNotNullOfOrNull { it.schedule }
    val migratedSchedule = schedule ?: legacySchedule
    val legacySettingsTask = tasks.firstOrNull {
        it.schedule != null || it.deadlineEpochMillis != null || it.reminderAtEpochMillis != null ||
            it.remindEveryMinutes != null
    }
    val draft = BuilderState(
        title = title,
        description = description,
        tasks = tasks,
        deadlineEpochMillis = deadlineEpochMillis ?: tasks.firstNotNullOfOrNull { it.deadlineEpochMillis },
        reminderAtEpochMillis = reminderAtEpochMillis ?: tasks.firstNotNullOfOrNull { it.reminderAtEpochMillis },
        remindEveryMinutes = remindEveryMinutes ?: tasks.firstNotNullOfOrNull { it.remindEveryMinutes },
        soundEnabled = legacySettingsTask?.soundEnabled ?: soundEnabled,
        vibrateEnabled = legacySettingsTask?.vibrateEnabled ?: vibrateEnabled,
        soundSettings = soundSettings,
        scheduleEnabled = migratedSchedule != null,
        scheduleFrequency = migratedSchedule?.frequency ?: ScheduleFrequency.DAILY,
        scheduleHour = migratedSchedule?.localHour ?: 9,
        scheduleMinute = migratedSchedule?.localMinute ?: 0,
        scheduleDaysOfWeek = migratedSchedule?.daysOfWeek ?: emptySet(),
        scheduleOneTimeEpochMillis = migratedSchedule?.oneTimeEpochMillis,
    )
    return draft.copy(
        validationErrors = if (draft.hasConflictingLegacyRoutineSettings()) {
            setOf(BuilderValidationError.LEGACY_SETTINGS_CONFLICT)
        } else {
            emptySet()
        },
    )
}

/** Coordinates a routine draft and persists only validated domain templates. */
class RoutineBuilderViewModel(
    private val container: AppContainer,
    private val routineId: RoutineId?,
) : ViewModel() {
    val state = MutableStateFlow(BuilderState())
    private val stableRoutineId = routineId ?: container.newRoutineId()

    /**
     * Use this function to apply a user draft mutation while preserving only still-relevant validation errors.
     * Inputs: `transform` — the state change requested by the user.
     * Dependencies: `BuilderState.editableContent`, `validateBuilderState`, and `container.now`.
     */
    private fun mutateDraft(transform: (BuilderState) -> BuilderState) {
        val before = state.value
        val candidate = transform(before)
        if (candidate.editableContent() == before.editableContent()) return
        val errors = candidate.validationErrors
            .minus(BuilderValidationError.SAVE_FAILED)
            .intersect(validateBuilderState(candidate, container.now()))
        state.value = candidate.copy(validationErrors = errors, hasUnsavedChanges = true)
    }

    /**
     * Use this function to compare draft fields without treating UI errors or save flags as edits.
     * Inputs: the receiver `BuilderState`.
     * Dependencies: `BuilderState.copy`.
     */
    private fun BuilderState.editableContent(): BuilderState = copy(
        validationErrors = emptySet(),
        failedSaveCount = 0,
        hasUnsavedChanges = false,
        isSaving = false,
    )

    /**
     * Use this function to add one editable subtask to a task with a stable identity.
     * The first subtask inherits the task's timer, so the task's total duration does not change.
     */
    fun addSubtask(taskId: RoutineTaskId) {
        mutateDraft { draft ->
            val tasks = draft.tasks.map { task ->
                if (task.id != taskId) task else task.copy(
                    timerSeconds = if (task.subtasks.isEmpty()) null else task.timerSeconds,
                    subtasks = task.subtasks + com.taskchain.domain.model.RoutineSubtask(
                        id = RoutineSubtaskId(UUID.randomUUID().toString()),
                        title = "",
                        durationSeconds = task.timerSeconds?.takeIf { task.subtasks.isEmpty() }
                            ?: DEFAULT_SUBTASK_DURATION_SECONDS,
                    ),
                )
            }
            draft.copy(tasks = tasks)
        }
    }

    /** Use this function when a subtask title changes; taskId scopes the subtask mutation to its owning task. */
    fun setSubtaskTitle(
        taskId: RoutineTaskId,
        subtaskId: RoutineSubtaskId,
        title: String,
    ) {
        mutateDraft { draft ->
            draft.copy(tasks = draft.tasks.map { task ->
                if (task.id != taskId) task else task.copy(subtasks = task.subtasks.map { subtask ->
                    if (subtask.id == subtaskId) subtask.copy(title = title) else subtask
                })
            })
        }
    }

    /** Use this function when a subtask duration field changes, retaining raw invalid input for visible validation. */
    fun setSubtaskDuration(
        taskId: RoutineTaskId,
        subtaskId: RoutineSubtaskId,
        value: String,
    ) {
        if (state.value.tasks.none { task -> task.id == taskId && task.subtasks.any { it.id == subtaskId } }) return
        mutateDraft { draft ->
            val seconds = value.trim().toLongOrNull()?.takeIf { it > 0L }
            val key = "${taskId.value}:${subtaskId.value}"
            draft.copy(
                tasks = draft.tasks.map { task ->
                    if (task.id != taskId || seconds == null) task else task.copy(subtasks = task.subtasks.map { subtask ->
                        if (subtask.id == subtaskId) subtask.copy(durationSeconds = seconds) else subtask
                    })
                },
                pendingSubtaskDurations = draft.pendingSubtaskDurations + (key to value),
            )
        }
    }

    /** Use this function when removing one subtask from its owning task. */
    fun removeSubtask(
        taskId: RoutineTaskId,
        subtaskId: RoutineSubtaskId,
    ) {
        mutateDraft { draft ->
            val key = "${taskId.value}:${subtaskId.value}"
            draft.copy(
                tasks = draft.tasks.map { task ->
                    if (task.id == taskId) task.copy(subtasks = task.subtasks.filterNot { it.id == subtaskId }) else task
                },
                pendingSubtaskDurations = draft.pendingSubtaskDurations - key,
            )
        }
    }

    /** Use this function to move a subtask within one task while preserving the task's other fields atomically. */
    fun moveSubtask(
        taskId: RoutineTaskId,
        subtaskId: RoutineSubtaskId,
        offset: Int,
    ) {
        if (offset == 0) return
        mutateDraft { draft ->
            draft.copy(tasks = draft.tasks.map { task ->
                if (task.id != taskId) task else {
                    val index = task.subtasks.indexOfFirst { it.id == subtaskId }
                    val target = index.toLong() + offset.toLong()
                    if (index < 0 || target < 0L || target >= task.subtasks.size) task else task.copy(
                        subtasks = task.subtasks.toMutableList().apply { add(target.toInt(), removeAt(index)) },
                    )
                }
            })
        }
    }

    init {
        if (routineId != null) viewModelScope.launch {
            val routine = container.routines.get(routineId)
                ?: container.builtInLibrary.load(container.now()).firstOrNull { it.id == routineId }
            routine?.let {
                state.value = routine.toBuilderState()
            }
        }
    }

    /** Use this function when the routine-name field changes. */
    fun setTitle(value: String) {
        mutateDraft { it.copy(title = value) }
    }

    /** Use this function when the routine-description field changes. */
    fun setDescription(value: String) {
        mutateDraft { it.copy(description = value) }
    }

    /** Use this function when the expanded task title changes. */
    fun setPendingTaskTitle(value: String) {
        val index = state.value.editingTaskIndex?.takeIf { it in state.value.tasks.indices } ?: return
        mutateDraft { draft ->
            val tasks = draft.tasks.toMutableList().apply { set(index, get(index).copy(title = value)) }
            draft.copy(tasks = tasks, pendingTaskTitle = value)
        }
    }

    /** Use this function when the duration picker returns a task timer; null means the task has no timer. */
    fun setTaskTimer(taskId: RoutineTaskId, timerSeconds: Long?) {
        mutateDraft { draft ->
            draft.copy(tasks = draft.tasks.map { if (it.id == taskId) it.copy(timerSeconds = timerSeconds) else it })
        }
    }

    /** Use this function when a routine deadline is selected. */
    fun setDeadline(value: Long?) {
        mutateDraft { draft ->
            val acknowledged = draft.clearLegacyRoutineSettings()
            acknowledged.copy(
                deadlineEpochMillis = value,
                scheduleEnabled = if (value != null) false else acknowledged.scheduleEnabled,
                reminderAtEpochMillis = if (value != null) null else acknowledged.reminderAtEpochMillis,
                remindEveryMinutes = if (value != null) null else acknowledged.remindEveryMinutes,
            )
        }
    }

    /** Use this function when a one-time routine Reminder is selected. */
    fun setReminder(value: Long?) {
        mutateDraft { draft ->
            val acknowledged = draft.clearLegacyRoutineSettings()
            acknowledged.copy(
                reminderAtEpochMillis = value,
                scheduleEnabled = if (value != null) false else acknowledged.scheduleEnabled,
                deadlineEpochMillis = if (value != null) null else acknowledged.deadlineEpochMillis,
                remindEveryMinutes = if (value != null) null else acknowledged.remindEveryMinutes,
            )
        }
    }

    /** Use this function when the routine's repeated prompt interval changes. */
    fun setRemindEveryMinutes(value: Int?) {
        mutateDraft { draft ->
            draft.clearLegacyRoutineSettings().copy(remindEveryMinutes = value?.takeIf { it > 0 })
        }
    }

    /** Use this function when audible routine Reminder feedback is enabled or disabled. */
    fun setSoundEnabled(value: Boolean) { mutateDraft { it.copy(soundEnabled = value) } }

    /** Use this function when haptic routine Reminder feedback is enabled or disabled. */
    fun setVibrateEnabled(value: Boolean) { mutateDraft { it.copy(vibrateEnabled = value) } }

    /** Use this function when the user expands a task. */
    fun expandTask(taskId: RoutineTaskId) {
        state.value = state.value.expandingTask(taskId) ?: return
    }

    /** Use this function when the user collapses a task. */
    fun collapseTask(taskId: RoutineTaskId) {
        if (state.value.expandedTaskId != taskId) return
        state.value = state.value.copy(expandedTaskId = null, editingNameTaskId = null)
    }

    /** Use this function when the user taps the title of the expanded task to rename it. */
    fun startTaskNameEdit(taskId: RoutineTaskId) {
        state.value = state.value.copy(editingNameTaskId = taskId)
    }

    /** Use this function when Add Task should create and expand a task without requiring a prefilled name. */
    fun addTask(defaultTitle: String) {
        val task = RoutineTask(container.newTaskId(), defaultTitle, timerSeconds = DEFAULT_TASK_TIMER_SECONDS)
        mutateDraft { draft ->
            val tasks = draft.tasks + task
            draft.copy(
                tasks = tasks,
                editingTaskIndex = tasks.lastIndex,
                expandedTaskId = task.id,
                pendingTaskTitle = defaultTitle,
            )
        }
    }

    /** Use this function when a draft task moves by a list offset. */
    fun moveTask(taskId: RoutineTaskId, offset: Int) {
        mutateDraft { draft ->
            val index = draft.tasks.indexOfFirst { it.id == taskId }
            val target = index + offset
            if (index < 0 || target !in draft.tasks.indices) return@mutateDraft draft
            val reordered = draft.tasks.toMutableList().apply { add(target, removeAt(index)) }
            val editingId = draft.editingTaskIndex?.let { draft.tasks.getOrNull(it)?.id }
            draft.copy(
                tasks = reordered,
                editingTaskIndex = editingId?.let { id -> reordered.indexOfFirst { it.id == id }.takeIf { it >= 0 } },
            )
        }
    }

    /** Use this function when routine scheduling is enabled or disabled. */
    fun setScheduleEnabled(value: Boolean) {
        mutateDraft { draft -> draft.clearLegacyRoutineSettings().copy(
            scheduleEnabled = value,
            deadlineEpochMillis = if (value) null else draft.deadlineEpochMillis,
            reminderAtEpochMillis = if (value) null else draft.reminderAtEpochMillis,
            remindEveryMinutes = if (value) draft.remindEveryMinutes else null,
        ) }
    }

    /** Use this function when a routine recurrence frequency changes. */
    fun setScheduleFrequency(value: ScheduleFrequency) {
        mutateDraft { it.clearLegacyRoutineSettings().copy(scheduleFrequency = value) }
    }

    /** Use this function when a routine schedule time changes. */
    fun setScheduleTime(hour: Int, minute: Int) {
        mutateDraft { it.clearLegacyRoutineSettings().copy(scheduleHour = hour, scheduleMinute = minute) }
    }

    /** Use this function when selected recurrence weekdays change. */
    fun setScheduleDays(value: Set<Int>) {
        mutateDraft { it.clearLegacyRoutineSettings().copy(scheduleDaysOfWeek = value) }
    }

    /** Use this function when a one-time schedule date changes. */
    fun setScheduleDate(value: Long?) {
        mutateDraft { it.clearLegacyRoutineSettings().copy(scheduleOneTimeEpochMillis = value) }
    }

    /** Use this function when removing one task from the current draft. */
    fun removeTask(taskId: RoutineTaskId) {
        mutateDraft { draft ->
            val index = draft.tasks.indexOfFirst { it.id == taskId }
            if (index < 0) return@mutateDraft draft
            val editingIndex = draft.editingTaskIndex
            val removedEditingTask = editingIndex == index
            draft.copy(
                tasks = draft.tasks.filterNot { it.id == taskId },
                pendingSubtaskDurations = draft.pendingSubtaskDurations.filterKeys { !it.startsWith("${taskId.value}:") },
                editingTaskIndex = when {
                    removedEditingTask -> null
                    editingIndex != null && editingIndex > index -> editingIndex - 1
                    else -> editingIndex
                },
                expandedTaskId = draft.expandedTaskId.takeIf { it != taskId },
                editingNameTaskId = draft.editingNameTaskId.takeIf { it != taskId },
                pendingTaskTitle = if (removedEditingTask) "" else draft.pendingTaskTitle,
            )
        }
    }

    /** Use this function when Save is pressed for a valid routine draft. */
    fun save() {
        if (state.value.isSaving) return
        val now = container.now()
        val draft = state.value
        val errors = validateBuilderState(draft, now)
        if (errors.isNotEmpty()) {
            state.value = draft.copy(validationErrors = errors, failedSaveCount = draft.failedSaveCount + 1)
                .revealingFirstError()
            return
        }
        state.value = draft.copy(validationErrors = emptySet(), isSaving = true)
        viewModelScope.launch {
            runCatching {
                val id = stableRoutineId
                val previous = container.routines.get(id)
                val routine = RoutineTemplate(
                    id = id,
                    metadata = previous?.metadata?.copy(updatedAtEpochMillis = now, revision = previous.metadata.revision + 1)
                        ?: EntityMetadata(now, now),
                    title = draft.title.trim(),
                    description = draft.description.trim(),
                    tasks = draft.tasks.map { task ->
                        task.copy(
                        title = task.title.trim(),
                        subtasks = task.subtasks.map { subtask -> subtask.copy(title = subtask.title.trim()) },
                        deadlineEpochMillis = null,
                        reminderAtEpochMillis = null,
                            schedule = null,
                            remindEveryMinutes = null,
                        )
                    },
                    schedule = if (draft.scheduleEnabled) ScheduleRule(
                        draft.scheduleFrequency,
                        draft.scheduleHour,
                        draft.scheduleMinute,
                        draft.scheduleDaysOfWeek,
                        draft.scheduleOneTimeEpochMillis,
                    ) else null,
                    deadlineEpochMillis = draft.deadlineEpochMillis,
                    reminderAtEpochMillis = draft.reminderAtEpochMillis,
                    remindEveryMinutes = draft.remindEveryMinutes,
                    soundEnabled = draft.soundEnabled,
                    vibrateEnabled = draft.vibrateEnabled,
                    soundSettings = draft.soundSettings,
                )
                routine.requireRunnable()
                val trigger = routine.schedule?.let {
                    NextTriggerCalculator.nextTriggerEpochMillis(it, now)
                        ?: throw IllegalArgumentException("Routine schedule is not in the future")
                } ?: routine.reminderAtEpochMillis
                container.routines.save(routine)
                previous?.tasks?.forEach { task -> container.reminders.cancel("step:${task.id.value}".hashCode()) }
                if (trigger != null) {
                    container.reminders.schedule(routine.toReminderRequest(trigger))
                }
                else container.reminders.cancel(routine.id.value.hashCode())
            }
                .onSuccess {
                    state.value = state.value.copy(
                        savedRoutineId = stableRoutineId,
                        validationErrors = emptySet(),
                        hasUnsavedChanges = false,
                        isSaving = false,
                    )
                }
                .onFailure {
                    state.value = state.value.copy(
                        validationErrors = setOf(BuilderValidationError.SAVE_FAILED),
                        isSaving = false,
                    )
                }
        }
    }
}

/** Runner UI state with persisted run state plus current wall-clock display time. */
data class RunnerState(
    val run: RoutineRun? = null,
    val nowEpochMillis: Long = 0,
    val finished: Boolean = false,
    val historySaveFailed: Boolean = false,
)

/** Coordinates runner UI and delegates every transition to the RoutineRunEngine. */
class RoutineRunnerViewModel(
    private val container: AppContainer,
    private val routineId: RoutineId,
) : ViewModel() {
    val state = MutableStateFlow(RunnerState(nowEpochMillis = container.now()))
    private val runMutex = Mutex()
    private var foreground = false
    private var foregroundSynchronized = false
    private var initialRunningFeedbackPending = false
    private var transitionsEnabled = true
    private var feedbackIntensity = 1f

    init {
        viewModelScope.launch {
            container.preferences.observe().collect { feedbackIntensity = it.vibrationIntensity }
        }
        viewModelScope.launch {
            runMutex.withLock {
                val active = container.activeRun.observeActive().first()
                val run = active?.takeIf { it.status == RunStatus.ACTIVE } ?: startRun()
                initialRunningFeedbackPending = active?.status != RunStatus.ACTIVE
                container.activeRun.saveActive(run)
                state.value = state.value.copy(run = run)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                tick()
            }
        }
    }

    /** Use this function when the runner route enters or leaves the foreground lifecycle state. */
    fun setForeground(active: Boolean) {
        if (foreground == active) return
        foreground = active
        foregroundSynchronized = false
    }

    /** Use this function when the route combines the user transition setting with the system animator gate. */
    fun setTransitionsEnabled(enabled: Boolean) {
        transitionsEnabled = enabled
    }

    /** Use this function when Complete is pressed for the displayed task. */
    fun complete() = transition { run, now ->
        val completed = container.runEngine.completeCurrent(run, now)
        val completedMain = completed.tasks.getOrNull(run.currentTaskIndex)?.status == RunTaskStatus.COMPLETED &&
            run.tasks.getOrNull(run.currentTaskIndex)?.status != RunTaskStatus.COMPLETED
        if (transitionsEnabled && foreground && completedMain && completed.currentTaskIndex != run.currentTaskIndex) {
            container.runEngine.prepareNextTask(
                completed,
                now,
                RunnerMotion.completionDurationMillis.toLong() + RunnerMotion.taskReadyTransitionDurationMillis,
            )
        } else {
            completed
        }
    }

    /** Use this function when Skip is pressed for the displayed task. */
    fun skip() = transition { run, now -> container.runEngine.skipCurrent(run, now) }

    /** Use this function when Back is pressed inside the runner. */
    fun back() = transition { run, now -> container.runEngine.back(run, now) }

    /** Use this function for a right swipe to revisit the next finished task; no inputs, dependency is RoutineRunEngine. */
    fun advanceToNextFinishedTask() = transition { run, now -> container.runEngine.advanceToNextFinishedTask(run, now) }

    /** Use this function when Pause is pressed for the displayed task. */
    fun pause() = transition { run, now -> container.runEngine.pauseCurrent(run, now) }

    /** Use this function when Resume is pressed for the displayed task. */
    fun resume() = transition { run, now -> container.runEngine.resumeCurrent(run, now) }

    /** Use this function when a confirmation dialog should close without ending the run. */
    fun continueRun() = transition { run, now -> container.runEngine.continueRun(run, now) }

    /** Use this function when the user selects an unfinished task from the finish dialog. */
    fun selectTask(index: Int) = transition { run, now -> container.runEngine.selectTask(run, index, now) }

    /** Use this function when the user confirms a completed run. */
    fun confirmComplete() = finish { run, now -> container.runEngine.confirmComplete(run, now) }

    /** Use this function when the user confirms an aborted run. */
    fun abort() = finish { run, now -> container.runEngine.abort(run, now) }

    /** Use this function if a terminal run remains on disk after history persistence fails. */
    fun retryHistorySave() {
        viewModelScope.launch {
            runMutex.withLock {
                val terminal = state.value.run ?: return@withLock
                if (terminal.status == RunStatus.ACTIVE) return@withLock
                try {
                    container.activeRun.saveActive(terminal)
                    container.recoverTerminalRun()
                    if (terminal.status == RunStatus.COMPLETED) {
                        runCatching {
                            rescheduleRoutineReminderAfterCompletion(
                                terminal.routineId,
                                terminal.endedAtEpochMillis ?: container.now(),
                            )
                        }
                    }
                    state.value = RunnerState(nowEpochMillis = container.now(), run = terminal, finished = true)
                } catch (_: Exception) {
                    state.value = state.value.copy(historySaveFailed = true)
                }
            }
        }
    }

    /** Use this function on the runner clock cadence to update display and fire zero feedback once. */
    private suspend fun tick() = runMutex.withLock {
        val now = container.now()
        state.value = state.value.copy(nowEpochMillis = now)
        var run = state.value.run ?: return@withLock
        if (foreground && !foregroundSynchronized) {
            val synchronized = RunFeedbackPolicy.skipMissedNudges(
                run,
                now,
                container.taskNudgeIntervalMillis,
            )
            if (synchronized != run) {
                try {
                    container.activeRun.saveActive(synchronized)
                } catch (_: Exception) {
                    return@withLock
                }
                state.value = state.value.copy(run = synchronized)
                run = synchronized
            }
            foregroundSynchronized = true
            if (initialRunningFeedbackPending) {
                RunFeedbackPolicy.stateEntryEvents(null, run).forEach { fireFeedback(run, it) }
                initialRunningFeedbackPending = false
            }
        }
        if (!foreground || run.status != RunStatus.ACTIVE || run.finishConfirmationRequested || run.abortConfirmationRequested) {
            return@withLock
        }
        val current = run.tasks.getOrNull(run.currentTaskIndex) ?: return@withLock
        if (current.status != RunTaskStatus.PENDING || current.pausedAtEpochMillis != null) return@withLock

        val nudge = RunFeedbackPolicy.evaluateNudge(
            run,
            now,
            container.taskNudgeIntervalMillis,
            emit = true,
        )
        if (nudge.run != run) {
            try {
                container.activeRun.saveActive(nudge.run)
            } catch (_: Exception) {
                return@withLock
            }
            state.value = state.value.copy(run = nudge.run)
            run = nudge.run
            if (nudge.shouldFire) {
                fireFeedback(run, RunFeedbackEvent(SoundToken.TaskNudge, run.currentTaskIndex))
            }
        }
        if (container.runEngine.needsTimerFeedback(run, now)) {
            val task = run.tasks[run.currentTaskIndex].source
            val acknowledged = container.runEngine.acknowledgeTimerFeedback(run, now)
            try {
                container.activeRun.saveActive(acknowledged)
            } catch (_: Exception) {
                return@withLock
            }
            state.value = state.value.copy(run = acknowledged)
            if (!foreground) return@withLock
            container.timerFeedback.fire(
                soundEnabled = run.routineSoundEnabled && task.soundEnabled,
                vibrateEnabled = run.routineVibrateEnabled && task.vibrateEnabled,
                soundSettings = run.soundSettings,
                intensity = feedbackIntensity,
            )
        }
    }

    /** Use this function to persist one ordinary active-run transition. */
    private fun transition(change: (RoutineRun, Long) -> RoutineRun) = withCurrentRun { run ->
        val updated = change(run, container.now())
        if (updated.status != RunStatus.ACTIVE) {
            if (persistTerminalRun(updated) && foreground) fireStateFeedback(run, updated)
        } else {
            container.activeRun.saveActive(updated)
            state.value = state.value.copy(run = updated)
            if (updated != run) initialRunningFeedbackPending = false
            if (foreground) fireStateFeedback(run, updated)
        }
    }

    /** Use this function to record a terminal run before clearing active-session storage. */
    private fun finish(change: (RoutineRun, Long) -> RoutineRun) = withCurrentRun { run ->
        persistTerminalRun(change(run, container.now()))
    }

    /**
     * Use this function to run `action` under the run lock only if the run still matches the snapshot the user
     * acted on. A stale action, for example a second tap after the task changed, does nothing.
     */
    private fun withCurrentRun(action: suspend (RoutineRun) -> Unit) {
        val expected = state.value.run ?: return
        viewModelScope.launch {
            runMutex.withLock {
                val run = state.value.run ?: return@withLock
                if (run.status != RunStatus.ACTIVE || run.currentTaskIndex != expected.currentTaskIndex ||
                    run.tasks.getOrNull(run.currentTaskIndex)?.activeSubtaskId !=
                    expected.tasks.getOrNull(expected.currentTaskIndex)?.activeSubtaskId ||
                    run.finishConfirmationRequested != expected.finishConfirmationRequested ||
                    run.abortConfirmationRequested != expected.abortConfirmationRequested
                ) return@withLock
                action(run)
            }
        }
    }

    /** Use this function to persist a terminal run once before history clear and completion navigation. */
    private suspend fun persistTerminalRun(terminal: RoutineRun): Boolean {
        try {
            container.activeRun.saveActive(terminal)
        } catch (_: Exception) {
            state.value = state.value.copy(run = terminal, historySaveFailed = true)
            return false
        }
        state.value = state.value.copy(run = terminal, historySaveFailed = false)
        try {
            container.completions.append(container.runEngine.toCompletionEvent(terminal))
            container.activeRun.clearActive()
        } catch (_: Exception) {
            state.value = state.value.copy(historySaveFailed = true)
            return false
        }
        if (terminal.status == RunStatus.COMPLETED) {
            runCatching {
                rescheduleRoutineReminderAfterCompletion(
                    terminal.routineId,
                    terminal.endedAtEpochMillis ?: container.now(),
                )
            }
        }
        state.value = RunnerState(nowEpochMillis = container.now(), run = terminal, finished = true)
        return true
    }

    /** Use this function to emit state-entry events after their run transition is durably saved. */
    private fun fireStateFeedback(previous: RoutineRun, updated: RoutineRun) {
        RunFeedbackPolicy.stateEntryEvents(previous, updated).forEach { event -> fireFeedback(updated, event) }
    }

    /** Use this function to pass one semantic event through independent sound and haptic gates. */
    private fun fireFeedback(run: RoutineRun, event: RunFeedbackEvent) {
        if (!foreground) return
        val task = run.tasks.getOrNull(event.taskIndex) ?: return
        val soundEnabled = run.routineSoundEnabled && task.source.soundEnabled && run.soundSettings[event.token].enabled
        val vibrateEnabled = event.token == SoundToken.TaskNudge &&
            run.routineVibrateEnabled && task.source.vibrateEnabled
        runCatching {
            container.taskFeedback.fire(
                token = event.token,
                soundEnabled = soundEnabled,
                vibrateEnabled = vibrateEnabled,
                soundSettings = run.soundSettings,
                intensity = feedbackIntensity,
            )
        }
    }

    /** Use this function after a completed run to resume only the routine's next recurrence. */
    private suspend fun rescheduleRoutineReminderAfterCompletion(routineId: RoutineId, completedAtEpochMillis: Long) {
        val routine = container.routines.get(routineId)
            ?: container.builtInLibrary.load(container.now()).firstOrNull { it.id == routineId }
        val schedule = routine?.schedule
        val next = schedule?.let {
            NextTriggerCalculator.nextTriggerAfterCompletionEpochMillis(it, completedAtEpochMillis)
        }
        if (routine == null || next == null) {
            container.reminders.cancel(routineId.value.hashCode())
            return
        }
        container.reminders.schedule(routine.toReminderRequest(next))
    }

    /** Use this function when no compatible active run exists for the requested routine. */
    // Run replacement and resume behavior is a pending decision; see "Open decision" in CONTEXT.md and docs/adr/0001-one-run-file-per-routine.md.
    private suspend fun startRun(): RoutineRun {
        val routine = container.routines.get(routineId)
            ?: container.builtInLibrary.load(container.now()).first { it.id == routineId }
        return container.runEngine.start(routine, container.newRunId(), container.now())
    }

    private companion object {
        const val TICK_MILLIS = 250L
    }
}

/** Projects local completion events into the small progress summary. */
private val EMPTY_PROGRESS = ProgressSummary(0, 0, 0, 0, 0, emptyList())
class ProgressViewModel(container: AppContainer) : ViewModel() {
    val state: StateFlow<ProgressSummary> = container.completions.observeAll()
        .map { events -> projectProgress(events, container.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EMPTY_PROGRESS)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Coordinates Settings UI with persisted user preferences. */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<UserPreferences> = container.preferences.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UserPreferences())

    /** Use this function when a supported theme is selected in Settings. */
    fun setTheme(theme: String) {
        viewModelScope.launch { container.preferences.setTheme(theme) }
    }

    /** Use this function when overtime behavior is toggled in Settings. */
    fun setContinuePastZero(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setContinueTimerPastZero(enabled) }
    }

    /** Use this function to set press vibration strength; input is 0..1 intensity, dependency is the preference repository. */
    fun setVibrationIntensity(intensity: Float) {
        viewModelScope.launch { container.preferences.setVibrationIntensity(intensity) }
    }

    /** Use this function to toggle task entry effects; input is the enabled gate, dependency is the preference repository. */
    fun setScreenTransitionsEnabled(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setScreenTransitionsEnabled(enabled) }
    }

    /** Use this function to opt into native run bubbles; input is the enabled gate, dependency is the preference repository. */
    fun setBubbleOnMinimize(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setBubbleOnMinimize(enabled) }
    }

    /** Use this function when subtask countdown visibility is toggled in Settings. */
    fun setShowSubtaskTimeRemaining(enabled: Boolean) {
        viewModelScope.launch { container.preferences.setShowSubtaskTimeRemaining(enabled) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
