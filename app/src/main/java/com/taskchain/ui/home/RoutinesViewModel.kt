package com.taskchain.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.AppContainer
import com.taskchain.domain.home.TodayProjection
import com.taskchain.domain.home.projectTodayRoutines
import com.taskchain.domain.home.skipEvent
import com.taskchain.domain.schedule.ReminderPlanner
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    /** Use this function when the user skips the routine's current occurrence; the next recurrence is scheduled. */
    fun skip(routine: RoutineTemplate) {
        viewModelScope.launch {
            val now = container.now()
            container.completions.append(skipEvent(routine, container.newRunId(), now))
            val plan = ReminderPlanner.afterCompletion(routine, routine.id, now)
            plan.cancelRequestCodes.forEach(container.reminders::cancel)
            plan.requests.forEach(container.reminders::schedule)
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
