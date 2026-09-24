package com.taskchain.domain.home

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
import com.taskchain.domain.schedule.NextTriggerCalculator
import java.util.TimeZone

/** Categories for the routines displayed on Home. */
data class TodayProjection(
    val scheduled: List<RoutineTemplate>,
    val manual: List<RoutineTemplate>,
    val completed: List<RoutineTemplate>,
)

/** Categorizes every routine using its template schedule and the completion of its current occurrence. */
fun projectTodayRoutines(
    routines: List<RoutineTemplate>,
    activeRun: RoutineRun?,
    history: List<CompletionEvent>,
    nowEpochMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): TodayProjection {
    val scheduled = mutableListOf<RoutineTemplate>()
    val manual = mutableListOf<RoutineTemplate>()
    val completed = mutableListOf<RoutineTemplate>()

    routines.forEach { routine ->
        val schedule = routine.schedule
        if (schedule == null) {
            manual += routine
            return@forEach
        }

        val activeCompletedAt = activeRun?.takeIf {
            it.routineId == routine.id && it.status == RunStatus.COMPLETED
        }?.let { run ->
            run.endedAtEpochMillis ?: run.steps.asSequence()
                .filter { it.status == RunStepStatus.COMPLETED }
                .mapNotNull { it.completedAtEpochMillis ?: it.finishedAtEpochMillis }
                .maxOrNull()
        }
        val completedAt = listOfNotNull(
            activeCompletedAt,
            history.asSequence()
                .filter { it.routineId == routine.id && it.status == RunStatus.COMPLETED }
                .map { it.endedAtEpochMillis }
                .maxOrNull(),
        ).maxOrNull()

        val nextTrigger = completedAt?.let {
            NextTriggerCalculator.nextTriggerEpochMillis(schedule, it, timeZone)
        }
        if (completedAt != null && (nextTrigger == null || nowEpochMillis < nextTrigger)) {
            completed += routine
        } else {
            scheduled += routine
        }
    }

    return TodayProjection(scheduled, manual, completed)
}
