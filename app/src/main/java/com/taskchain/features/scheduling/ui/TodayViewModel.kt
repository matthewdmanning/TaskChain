package com.taskchain.features.scheduling.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.core.data.BuiltInRoutineLibrary
import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import com.taskchain.core.schedule.ReminderPlanner
import com.taskchain.features.scheduling.domain.TodayProjection
import com.taskchain.features.scheduling.domain.projectTodayRoutines
import com.taskchain.features.scheduling.domain.skipEvent
import com.taskchain.features.scheduling.domain.skipUndoneEvent
import com.taskchain.core.platform.ReminderScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** State projected for the Today occurrence list and its active-run card. */
data class TodayState(
    val projection: TodayProjection = TodayProjection(emptyList(), emptyList(), emptyList()),
    val activeRun: RoutineRun? = null,
)

/** Coordinates Today occurrence projection, skip/undo markers, and reminder effects. */
class TodayViewModel(
    private val routines: RoutineRepository,
    private val activeRuns: RoutineRunRepository,
    private val completions: CompletionRepository,
    private val builtInLibrary: BuiltInRoutineLibrary,
    private val reminders: ReminderScheduler,
    private val now: () -> Long,
    private val dayStartHour: Int,
    private val newRunId: () -> com.taskchain.core.model.RoutineRunId,
) : ViewModel() {
    private val builtIns = MutableStateFlow<List<RoutineTemplate>>(emptyList())
    private val clock = flow {
        while (true) {
            emit(now())
            delay(HOME_REFRESH_MILLIS)
        }
    }

    val state: StateFlow<TodayState> = combine(
        routines.observeAll(),
        builtIns,
        activeRuns.observeActive(),
        completions.observeAll(),
        clock,
    ) { definitions, library, activeRun, history, timestamp ->
        val visibleBuiltIns = library.filterNot { builtIn -> definitions.any { it.id == builtIn.id } }
        val source = definitions.ifEmpty { visibleBuiltIns }
        TodayState(
            projection = projectTodayRoutines(source, activeRun, history, timestamp, dayStartHour),
            activeRun = activeRun.takeIf { it?.status == RunStatus.ACTIVE },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TodayState())

    init {
        viewModelScope.launch { builtIns.value = builtInLibrary.load(now()) }
    }

    /** Use this function when the user skips the current occurrence of a scheduled routine. */
    fun skip(routine: RoutineTemplate) {
        viewModelScope.launch {
            val timestamp = now()
            completions.append(skipEvent(routine, newRunId(), timestamp))
            val plan = ReminderPlanner.afterCompletion(routine, routine.id, timestamp)
            plan.cancelRequestCodes.forEach(reminders::cancel)
            plan.requests.forEach(reminders::schedule)
        }
    }

    /** Use this function when the user undoes a skipped occurrence. */
    fun undoSkip(routine: RoutineTemplate) {
        viewModelScope.launch {
            val timestamp = now()
            completions.append(skipUndoneEvent(routine, newRunId(), timestamp))
            val plan = ReminderPlanner.forRestore(routine, timestamp)
            plan.cancelRequestCodes.forEach(reminders::cancel)
            plan.requests.forEach(reminders::schedule)
        }
    }

    private companion object {
        const val HOME_REFRESH_MILLIS = 60_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
