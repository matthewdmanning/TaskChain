package com.taskchain.domain.home

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import java.util.Calendar
import java.util.TimeZone

/** Categories for the routines displayed on Home; `skipped` flags the members of `completed` that were skipped. */
data class TodayProjection(
    val scheduled: List<RoutineTemplate>,
    val manual: List<RoutineTemplate>,
    val completed: List<RoutineTemplate>,
    val skipped: Set<RoutineId> = emptySet(),
)

/** Use this function when the user skips a routine's current occurrence; it records no tasks. */
fun skipEvent(routine: RoutineTemplate, runId: RoutineRunId, nowEpochMillis: Long) =
    markerEvent(routine, runId, nowEpochMillis, RunStatus.SKIPPED)

/** Use this function when the user undoes an earlier skip; the newer marker cancels the older skip. */
fun skipUndoneEvent(routine: RoutineTemplate, runId: RoutineRunId, nowEpochMillis: Long) =
    markerEvent(routine, runId, nowEpochMillis, RunStatus.SKIP_UNDONE)

/** Use this function to build a task-less history event that only marks a Home state change. */
private fun markerEvent(routine: RoutineTemplate, runId: RoutineRunId, nowEpochMillis: Long, status: RunStatus) =
    CompletionEvent(
        runId = runId,
        routineId = routine.id,
        routineTitle = routine.title,
        startedAtEpochMillis = nowEpochMillis,
        endedAtEpochMillis = nowEpochMillis,
        status = status,
        tasks = emptyList(),
    )

/**
 * Use this function to find the first local day start after [epochMillis]; a day starts at [dayStartHour].
 * Handled routines return to Scheduled at that instant.
 */
fun nextDayStartEpochMillis(epochMillis: Long, dayStartHour: Int, timeZone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(timeZone).apply {
        timeInMillis = epochMillis
        add(Calendar.HOUR_OF_DAY, -dayStartHour)
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, dayStartHour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/**
 * Categorizes every routine by schedule and by whether its current day was completed or skipped.
 * A completed or skipped routine stays in Completed until the next day start ([dayStartHour]).
 */
fun projectTodayRoutines(
    routines: List<RoutineTemplate>,
    activeRun: RoutineRun?,
    history: List<CompletionEvent>,
    nowEpochMillis: Long,
    dayStartHour: Int,
    timeZone: TimeZone = TimeZone.getDefault(),
): TodayProjection {
    val scheduled = mutableListOf<RoutineTemplate>()
    val manual = mutableListOf<RoutineTemplate>()
    val completed = mutableListOf<RoutineTemplate>()
    val skipped = mutableSetOf<RoutineId>()

    routines.forEach { routine ->
        if (routine.schedule == null) {
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
        val events = history.filter { it.routineId == routine.id }
        fun latest(status: RunStatus) = events.filter { it.status == status }.maxOfOrNull { it.endedAtEpochMillis }
        val completedAt = listOfNotNull(activeCompletedAt, latest(RunStatus.COMPLETED)).maxOrNull()
        val skippedAt = latest(RunStatus.SKIPPED)
            ?.takeIf { skip -> latest(RunStatus.SKIP_UNDONE)?.let { it < skip } ?: true }

        val handledAt = listOfNotNull(completedAt, skippedAt).maxOrNull()
        if (handledAt != null && nowEpochMillis < nextDayStartEpochMillis(handledAt, dayStartHour, timeZone)) {
            completed += routine
            if (skippedAt == handledAt) skipped += routine.id
        } else {
            scheduled += routine
        }
    }

    return TodayProjection(scheduled, manual, completed, skipped)
}
