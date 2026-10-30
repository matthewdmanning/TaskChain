package com.taskchain.features.progress.domain

import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.core.schedule.dayStartAtOrBeforeEpochMillis
import java.util.Calendar
import java.util.TimeZone

/** One local day of completion history for a seven-day trend; the day begins at the configured day-start hour. */
data class DailyCompletion(val dayStartEpochMillis: Long, val completedRuns: Int)

/** Derived progress metrics that are never stored as mutable counters. */
data class ProgressSummary(
    val completedRuns: Int,
    val abortedRuns: Int,
    val actualDurationMillis: Long,
    val taskAdherencePercent: Int,
    val skippedTaskPercent: Int,
    val lastSevenDays: List<DailyCompletion>,
)

/**
 * Use this function when Progress needs a summary derived from durable completion events.
 * Trend days begin at [dayStartHour] local time, the same day start Home uses.
 */
fun projectProgress(
    events: List<CompletionEvent>,
    nowEpochMillis: Long,
    dayStartHour: Int,
    timeZone: TimeZone = TimeZone.getDefault(),
): ProgressSummary {
    val tasks = events.flatMap(CompletionEvent::tasks)
    val completedEvents = events.filter { it.status == RunStatus.COMPLETED }
    val dayStarts = (6 downTo 0).map { daysAgo ->
        Calendar.getInstance(timeZone).apply {
            timeInMillis = nowEpochMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }.let { dayStartAtOrBeforeEpochMillis(it.timeInMillis, dayStartHour, timeZone) }
    }
    val completedByDay = completedEvents.groupingBy {
        dayStartAtOrBeforeEpochMillis(it.endedAtEpochMillis, dayStartHour, timeZone)
    }.eachCount()
    return ProgressSummary(
        completedRuns = completedEvents.size,
        abortedRuns = events.count { it.status == RunStatus.ABORTED },
        actualDurationMillis = tasks.sumOf { it.actualDurationMillis?.coerceAtLeast(0L) ?: 0L },
        taskAdherencePercent = if (tasks.isEmpty()) 0 else tasks.count { it.status == RunTaskStatus.COMPLETED } * 100 / tasks.size,
        skippedTaskPercent = if (tasks.isEmpty()) 0 else tasks.count { it.status == RunTaskStatus.SKIPPED } * 100 / tasks.size,
        lastSevenDays = dayStarts.map { DailyCompletion(it, completedByDay[it] ?: 0) },
    )
}
