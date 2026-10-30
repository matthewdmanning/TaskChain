package com.taskchain.features.scheduling

import com.taskchain.core.data.BuiltInRoutineLibrary
import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.features.scheduling.ui.TodayViewModel

/**
 * Public entry of the Scheduling feature: the bounded inputs of the Today list and its ViewModel factory.
 * [dayStartHour] is `home_day_start_hour`, which decides the day of every completion and skip.
 */
class SchedulingDependencies(
    private val routines: RoutineRepository,
    private val activeRun: RoutineRunRepository,
    private val completions: CompletionRepository,
    private val builtInLibrary: BuiltInRoutineLibrary,
    private val reminders: ReminderScheduler,
    private val now: () -> Long,
    private val dayStartHour: Int,
    private val newRunId: () -> RoutineRunId,
) {
    /** Use this function as the root ViewModel factory for the Today tab. */
    fun todayViewModel(): TodayViewModel =
        TodayViewModel(routines, activeRun, completions, builtInLibrary, reminders, now, dayStartHour, newRunId)
}
