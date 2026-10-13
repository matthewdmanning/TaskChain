package com.taskchain.domain.home

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import com.taskchain.domain.schedule.NextTriggerCalculator
import java.util.TimeZone

/** Categories for the routines displayed on Home. */
data class TodayProjection(
    val scheduled: List<RoutineTemplate>,
    val manual: List<RoutineTemplate>,
    val completed: List<RoutineTemplate>,
    val skipped: List<RoutineTemplate> = emptyList(),
)

/** Use this function when the user skips a routine's current occurrence; it records no tasks. */
fun skipEvent(routine: RoutineTemplate, runId: RoutineRunId, nowEpochMillis: Long) = CompletionEvent(
    runId = runId,
    routineId = routine.id,
    routineTitle = routine.title,
    startedAtEpochMillis = nowEpochMillis,
    endedAtEpochMillis = nowEpochMillis,
    status = RunStatus.SKIPPED,
    tasks = emptyList(),
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
    val skipped = mutableListOf<RoutineTemplate>()

    routines.forEach { routine ->
        val schedule = routine.schedule
        if (schedule == null) {
            manual += routine
            return@forEach
        }

        val activeCompletedAt = activeRun?.takeIf {
            it.routineId == routine.id && it.status == RunStatus.COMPLETED
        }?.let { run ->
            run.endedAtEpochMillis ?: run.tasks.asSequence()
                .filter { it.status == RunTaskStatus.COMPLETED }
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
        val skippedAt = history.asSequence()
            .filter { it.routineId == routine.id && it.status == RunStatus.SKIPPED }
            .map { it.endedAtEpochMillis }
            .maxOrNull()

        val handledAt = listOfNotNull(completedAt, skippedAt).maxOrNull()
        val nextTrigger = handledAt?.let {
            NextTriggerCalculator.nextTriggerEpochMillis(schedule, it, timeZone)
        }
        if (handledAt != null && (nextTrigger == null || nowEpochMillis < nextTrigger)) {
            if (skippedAt == handledAt) skipped += routine else completed += routine
        } else {
            scheduled += routine
        }
    }

    return TodayProjection(scheduled, manual, completed, skipped)
}
