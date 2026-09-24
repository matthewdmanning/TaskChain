package com.taskchain.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.AppContainer
import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import com.taskchain.domain.model.UserPreferences
import com.taskchain.domain.home.projectTodayRoutines
import com.taskchain.domain.home.TodayProjection
import com.taskchain.domain.progress.ProgressSummary
import com.taskchain.domain.progress.projectProgress
import com.taskchain.domain.schedule.NextTriggerCalculator
import com.taskchain.reminder.toReminderRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    init {
        viewModelScope.launch { builtIns.value = container.builtInLibrary.load(container.now()) }
    }

    private companion object {
        const val HOME_REFRESH_MILLIS = 60_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Identifies a routine-builder field or persistence failure that the UI can explain. */
enum class BuilderValidationError {
    ROUTINE_NAME_REQUIRED,
    STEP_REQUIRED,
    STEP_NAME_REQUIRED,
    SELECTED_DAY_REQUIRED,
    SCHEDULE_DATE_REQUIRED,
    SCHEDULE_DATE_MUST_BE_FUTURE,
    REMINDER_MUST_BE_FUTURE,
    SCHEDULE_REMINDER_EXCLUSIVE,
    LEGACY_SETTINGS_CONFLICT,
    STEP_TIMER_MUST_BE_POSITIVE,
    SAVE_FAILED,
}

/** Editable state for one routine builder route. */
data class BuilderState(
    val title: String = "",
    val description: String = "",
    val steps: List<RoutineStep> = emptyList(),
    val pendingStepTitle: String = "",
    val pendingTimerSeconds: String = "",
    val deadlineEpochMillis: Long? = null,
    val reminderAtEpochMillis: Long? = null,
    val remindEveryMinutes: Int? = null,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val editingStepIndex: Int? = null,
    val scheduleEnabled: Boolean = false,
    val scheduleFrequency: ScheduleFrequency = ScheduleFrequency.DAILY,
    val scheduleHour: Int = 9,
    val scheduleMinute: Int = 0,
    val scheduleDaysOfWeek: Set<Int> = emptySet(),
    val scheduleOneTimeEpochMillis: Long? = null,
    val savedRoutineId: RoutineId? = null,
    val validationErrors: Set<BuilderValidationError> = emptySet(),
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
)

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
    if (state.steps.isEmpty()) errors += BuilderValidationError.STEP_REQUIRED

    state.editingStepIndex?.takeIf { it in state.steps.indices }?.let {
        if (state.pendingStepTitle.isBlank()) errors += BuilderValidationError.STEP_NAME_REQUIRED
        if (state.pendingTimerSeconds.isNotBlank() &&
            state.pendingTimerSeconds.trim().toLongOrNull()?.let { seconds -> seconds > 0 } != true
        ) errors += BuilderValidationError.STEP_TIMER_MUST_BE_POSITIVE
    }
    state.steps.forEach { step ->
        if (step.title.isBlank()) errors += BuilderValidationError.STEP_NAME_REQUIRED
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
    return errors
}

private fun BuilderState.hasConflictingLegacyRoutineSettings(): Boolean {
    if (steps.none {
        it.stackingAnchorStepId != null || it.deadlineEpochMillis != null || it.reminderAtEpochMillis != null ||
            it.schedule != null || it.remindEveryMinutes != null
        }
    ) return false
    val routineSchedule = if (scheduleEnabled) ScheduleRule(
        scheduleFrequency, scheduleHour, scheduleMinute, scheduleDaysOfWeek, scheduleOneTimeEpochMillis,
    ) else null
    val schedules = listOfNotNull(routineSchedule) + steps.mapNotNull { it.schedule }
    val deadlines = listOfNotNull(deadlineEpochMillis) + steps.mapNotNull { it.deadlineEpochMillis }
    val reminders = listOfNotNull(reminderAtEpochMillis) + steps.mapNotNull { it.reminderAtEpochMillis }
    val repeats = listOfNotNull(remindEveryMinutes) + steps.mapNotNull { it.remindEveryMinutes }
    val choiceCount = listOf(schedules, deadlines, reminders).count { it.isNotEmpty() }
    return schedules.distinct().size > 1 || deadlines.distinct().size > 1 ||
        reminders.distinct().size > 1 || repeats.distinct().size > 1 || choiceCount > 1 ||
        (repeats.isNotEmpty() && schedules.isEmpty())
}

private fun BuilderState.clearLegacyRoutineSettings(): BuilderState = copy(
    steps = steps.map {
        it.copy(
            deadlineEpochMillis = null,
            reminderAtEpochMillis = null,
            schedule = null,
            remindEveryMinutes = null,
        )
    },
)

internal fun RoutineTemplate.toBuilderState(): BuilderState {
    val legacySchedule = steps.firstNotNullOfOrNull { it.schedule }
    val migratedSchedule = schedule ?: legacySchedule
    val legacySettingsStep = steps.firstOrNull {
        it.schedule != null || it.deadlineEpochMillis != null || it.reminderAtEpochMillis != null ||
            it.remindEveryMinutes != null
    }
    val draft = BuilderState(
        title = title,
        description = description,
        steps = steps,
        deadlineEpochMillis = deadlineEpochMillis ?: steps.firstNotNullOfOrNull { it.deadlineEpochMillis },
        reminderAtEpochMillis = reminderAtEpochMillis ?: steps.firstNotNullOfOrNull { it.reminderAtEpochMillis },
        remindEveryMinutes = remindEveryMinutes ?: steps.firstNotNullOfOrNull { it.remindEveryMinutes },
        soundEnabled = legacySettingsStep?.soundEnabled ?: soundEnabled,
        vibrateEnabled = legacySettingsStep?.vibrateEnabled ?: vibrateEnabled,
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
        hasUnsavedChanges = false,
        isSaving = false,
    )

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

    /** Use this function when the expanded step title changes. */
    fun setPendingStepTitle(value: String) {
        val index = state.value.editingStepIndex?.takeIf { it in state.value.steps.indices } ?: return
        mutateDraft { draft ->
            val steps = draft.steps.toMutableList().apply { set(index, get(index).copy(title = value)) }
            draft.copy(steps = steps, pendingStepTitle = value)
        }
    }

    /** Use this function when the expanded step's optional duration changes. */
    fun setPendingTimerSeconds(value: String) {
        val index = state.value.editingStepIndex?.takeIf { it in state.value.steps.indices } ?: return
        val seconds = value.trim().toLongOrNull()?.takeIf { it > 0 }
        mutateDraft { draft ->
            val steps = draft.steps.toMutableList().apply { set(index, get(index).copy(timerSeconds = seconds)) }
            draft.copy(steps = steps, pendingTimerSeconds = value)
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

    /** Use this function when an existing draft step should be edited in place. */
    fun editStep(index: Int) {
        val draft = state.value
        val step = draft.steps.getOrNull(index) ?: return
        if (draft.editingStepIndex == index) return
        if (draft.editingStepIndex != null && draft.pendingTimerSeconds.isNotBlank() &&
            draft.pendingTimerSeconds.trim().toLongOrNull()?.let { it > 0 } != true
        ) return
        state.value = draft.copy(
            editingStepIndex = index,
            pendingStepTitle = step.title,
            pendingTimerSeconds = step.timerSeconds?.toString() ?: "",
        )
    }

    /** Use this function when Add Step should create and expand a task without requiring a prefilled name. */
    fun addStep(defaultTitle: String) {
        val draft = state.value
        if (draft.editingStepIndex != null && draft.pendingTimerSeconds.isNotBlank() &&
            draft.pendingTimerSeconds.trim().toLongOrNull()?.let { it > 0 } != true
        ) return
        val steps = draft.steps + RoutineStep(container.newStepId(), defaultTitle)
        val candidate = draft.copy(
            steps = steps,
            editingStepIndex = steps.lastIndex,
            pendingStepTitle = defaultTitle,
            pendingTimerSeconds = "",
            hasUnsavedChanges = true,
        )
        state.value = candidate.copy(
            validationErrors = candidate.validationErrors
                .minus(BuilderValidationError.SAVE_FAILED)
                .intersect(validateBuilderState(candidate, container.now())),
        )
    }

    /** Use this function when an ordered draft step moves by a list offset. */
    fun moveStep(index: Int, offset: Int) {
        val target = index + offset
        val steps = state.value.steps
        if (index !in steps.indices || target !in steps.indices) return
        val reordered = steps.toMutableList().apply { add(target, removeAt(index)) }
        val editingId = state.value.editingStepIndex?.let { steps.getOrNull(it)?.id }
        if (reordered == steps) return
        val candidate = state.value.copy(
            steps = reordered,
            editingStepIndex = editingId?.let { id -> reordered.indexOfFirst { it.id == id }.takeIf { it >= 0 } },
            hasUnsavedChanges = true,
        )
        state.value = candidate.copy(
            validationErrors = candidate.validationErrors.intersect(validateBuilderState(candidate, container.now())),
        )
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

    /** Use this function when removing one step from the current draft. */
    fun removeStep(index: Int) {
        val draft = state.value
        if (index !in draft.steps.indices) return
        val editingIndex = draft.editingStepIndex
        val removedEditingStep = editingIndex == index
        val candidate = draft.copy(
            steps = draft.steps.filterIndexed { itemIndex, _ -> itemIndex != index },
            editingStepIndex = when {
                removedEditingStep -> null
                editingIndex != null && editingIndex > index -> editingIndex - 1
                else -> editingIndex
            },
            pendingStepTitle = if (removedEditingStep) "" else draft.pendingStepTitle,
            pendingTimerSeconds = if (removedEditingStep) "" else draft.pendingTimerSeconds,
            hasUnsavedChanges = true,
        )
        state.value = candidate.copy(
            validationErrors = candidate.validationErrors.intersect(validateBuilderState(candidate, container.now())),
        )
    }

    /** Use this function when Save is pressed for a valid routine draft. */
    fun save() {
        if (state.value.isSaving) return
        val now = container.now()
        val initial = state.value
        val initialErrors = validateBuilderState(initial, now)
        if (initialErrors.isNotEmpty()) {
            state.value = initial.copy(validationErrors = initialErrors)
            return
        }
        val draft = state.value
        val errors = validateBuilderState(draft, now)
        if (errors.isNotEmpty()) {
            state.value = draft.copy(validationErrors = errors)
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
                    steps = draft.steps.map { step ->
                        step.copy(
                        title = step.title.trim(),
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
                )
                routine.requireRunnable()
                val trigger = routine.schedule?.let {
                    NextTriggerCalculator.nextTriggerEpochMillis(it, now)
                        ?: throw IllegalArgumentException("Routine schedule is not in the future")
                } ?: routine.reminderAtEpochMillis
                container.routines.save(routine)
                previous?.steps?.forEach { step -> container.reminders.cancel("step:${step.id.value}".hashCode()) }
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

    init {
        viewModelScope.launch {
            runMutex.withLock {
                val active = container.activeRun.observeActive().first()
                val run = active?.takeIf { it.status == RunStatus.ACTIVE } ?: startRun()
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

    /** Use this function when Complete is pressed for the displayed task. */
    fun complete() = transition { run, now -> container.runEngine.completeCurrent(run, now) }

    /** Use this function when Skip is pressed for the displayed task. */
    fun skip() = transition { run, now -> container.runEngine.skipCurrent(run, now) }

    /** Use this function when Back is pressed inside the runner. */
    fun back() = transition { run, now -> container.runEngine.back(run, now) }

    /** Use this function when a confirmation dialog should close without ending the run. */
    fun continueRun() = transition { run, now -> container.runEngine.continueRun(run, now) }

    /** Use this function when the user selects an unfinished task from the finish dialog. */
    fun selectStep(index: Int) = transition { run, now -> container.runEngine.selectStep(run, index, now) }

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
                    container.recoverTerminalRun()
                    state.value = RunnerState(nowEpochMillis = container.now(), finished = true)
                } catch (_: Exception) {
                    state.value = state.value.copy(historySaveFailed = true)
                }
            }
        }
    }

    /** Use this function on the runner clock cadence to update display and fire zero feedback once. */
    private suspend fun tick() = runMutex.withLock {
        val now = container.now()
        val run = state.value.run
        state.value = state.value.copy(nowEpochMillis = now)
        if (run != null && run.status == RunStatus.ACTIVE && container.runEngine.needsTimerFeedback(run, now)) {
            container.timerFeedback.fire(run.steps[run.currentStepIndex].source.soundEnabled, run.steps[run.currentStepIndex].source.vibrateEnabled)
            val acknowledged = container.runEngine.acknowledgeTimerFeedback(run, now)
            container.activeRun.saveActive(acknowledged)
            state.value = state.value.copy(run = acknowledged)
        }
    }

    /** Use this function to persist one ordinary active-run transition. */
    private fun transition(change: (RoutineRun, Long) -> RoutineRun) {
        val expected = state.value.run ?: return
        viewModelScope.launch {
            runMutex.withLock {
                val run = state.value.run ?: return@withLock
                if (run.status != RunStatus.ACTIVE || run.currentStepIndex != expected.currentStepIndex ||
                    run.finishConfirmationRequested != expected.finishConfirmationRequested ||
                    run.abortConfirmationRequested != expected.abortConfirmationRequested
                ) return@withLock
                val updated = change(run, container.now())
                container.activeRun.saveActive(updated)
                state.value = state.value.copy(run = updated)
            }
        }
    }

    /** Use this function to record a terminal run before clearing active-session storage. */
    private fun finish(change: (RoutineRun, Long) -> RoutineRun) {
        val expected = state.value.run ?: return
        viewModelScope.launch {
            runMutex.withLock {
                val run = state.value.run ?: return@withLock
                if (run.status != RunStatus.ACTIVE || run.currentStepIndex != expected.currentStepIndex ||
                    run.finishConfirmationRequested != expected.finishConfirmationRequested ||
                    run.abortConfirmationRequested != expected.abortConfirmationRequested
                ) return@withLock
                val terminal = change(run, container.now())
                container.activeRun.saveActive(terminal)
                state.value = state.value.copy(run = terminal)
                try {
                    container.completions.append(container.runEngine.toCompletionEvent(terminal))
                    container.activeRun.clearActive()
                } catch (_: Exception) {
                    state.value = state.value.copy(historySaveFailed = true)
                    return@withLock
                }
                if (terminal.status == RunStatus.COMPLETED) {
                    runCatching {
                        rescheduleRoutineReminderAfterCompletion(
                            run.routineId,
                            terminal.endedAtEpochMillis ?: container.now(),
                        )
                    }
                }
                state.value = RunnerState(nowEpochMillis = container.now(), finished = true)
            }
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

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
