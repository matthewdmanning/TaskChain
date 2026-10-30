package com.taskchain.features.authoring

import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.features.authoring.ui.RoutineBuilderViewModel

/**
 * Public entry of the Authoring feature: the bounded inputs of the routine builder and its ViewModel factory.
 * Root wires it once and passes [builderViewModel] to the builder route.
 */
class AuthoringDependencies(
    private val routines: RoutineRepository,
    private val loadBuiltIns: suspend (Long) -> List<RoutineTemplate>,
    private val reminders: ReminderScheduler,
    private val now: () -> Long,
    private val newRoutineId: () -> RoutineId,
    private val newTaskId: () -> RoutineTaskId,
) {
    /** Use this function to create the builder ViewModel for a new routine (null) or an existing one. */
    fun builderViewModel(routineId: RoutineId?): RoutineBuilderViewModel =
        RoutineBuilderViewModel(routines, loadBuiltIns, reminders, now, newRoutineId, newTaskId, routineId)
}
