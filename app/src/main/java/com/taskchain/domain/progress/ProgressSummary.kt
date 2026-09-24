package com.taskchain.domain.progress

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
import java.util.Calendar
import java.util.TimeZone

/** One local calendar day of completion history for a seven-day trend. */
data class DailyCompletion(val dayStartEpochMillis: Long, val completedRuns: Int)

/** Derived progress metrics that are never stored as mutable counters. */
data class ProgressSummary(
    val completedRuns: Int,
    val abortedRuns: Int,
    val actualDurationMillis: Long,
    val stepAdherencePercent: Int,
    val skippedStepPercent: Int,
    val lastSevenDays: List<DailyCompletion>,
)

/** Use this function when Progress needs a summary derived from durable completion events. */
fun projectProgress(
    events: List<CompletionEvent>,
    nowEpochMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): ProgressSummary {
    val steps = events.flatMap(CompletionEvent::steps)
    val completedEvents = events.filter { it.status == RunStatus.COMPLETED }
    val dayStarts = (6 downTo 0).map { daysAgo ->
        Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }.let { localDayStart(it.timeInMillis, timeZone) }
    }
    val completedByDay = completedEvents.groupingBy { localDayStart(it.endedAtEpochMillis, timeZone) }.eachCount()
    return ProgressSummary(
        completedRuns = completedEvents.size,
        abortedRuns = events.count { it.status == RunStatus.ABORTED },
        actualDurationMillis = steps.sumOf { it.actualDurationMillis?.coerceAtLeast(0L) ?: 0L },
        stepAdherencePercent = if (steps.isEmpty()) 0 else steps.count { it.status == RunStepStatus.COMPLETED } * 100 / steps.size,
        skippedStepPercent = if (steps.isEmpty()) 0 else steps.count { it.status == RunStepStatus.SKIPPED } * 100 / steps.size,
        lastSevenDays = dayStarts.map { DailyCompletion(it, completedByDay[it] ?: 0) },
    )
}

/** Use this function when grouping an event into a local-day progress trend. */
private fun localDayStart(timestamp: Long, timeZone: TimeZone): Long = Calendar.getInstance(timeZone).apply {
    timeInMillis = timestamp
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
