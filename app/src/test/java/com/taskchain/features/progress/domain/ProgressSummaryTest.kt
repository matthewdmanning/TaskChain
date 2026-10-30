package com.taskchain.features.progress.domain

import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineRunTask
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/** Checks that progress metrics come from events rather than stored totals. */
class ProgressSummaryTest {
    /** Use this function to verify duration, task rates, and seven-day counts from local history. */
    @Test
    fun projectsHistoryWithoutMutableCounters() {
        val now = 8L * 86_400_000L + 12L * 3_600_000L
        val source = RoutineTask(RoutineTaskId("task"), "Work")
        val completed = CompletionEvent(
            RoutineRunId("done"), RoutineId("routine"), "Morning", now - 10_000, now - 1_000,
            RunStatus.COMPLETED,
            listOf(
                RoutineRunTask(source, RunTaskStatus.COMPLETED, actualDurationMillis = 5_000),
                RoutineRunTask(source.copy(id = RoutineTaskId("skipped")), RunTaskStatus.SKIPPED, actualDurationMillis = 3_000),
            ),
        )
        val aborted = CompletionEvent(
            RoutineRunId("abort"), RoutineId("routine"), "Morning", now - 86_400_000, now - 86_400_000,
            RunStatus.ABORTED, listOf(RoutineRunTask(source, RunTaskStatus.PENDING)),
        )

        val summary = projectProgress(listOf(completed, aborted), now, 2, TimeZone.getTimeZone("UTC"))

        assertEquals(1, summary.completedRuns)
        assertEquals(1, summary.abortedRuns)
        assertEquals(8_000L, summary.actualDurationMillis)
        assertEquals(33, summary.taskAdherencePercent)
        assertEquals(33, summary.skippedTaskPercent)
        assertEquals(7, summary.lastSevenDays.size)
        assertEquals(1, summary.lastSevenDays.last().completedRuns)
    }

    /** Use this function to verify a 1 AM completion counts toward the previous day when the day starts at 2 AM. */
    @Test
    fun groupsCompletionBeforeDayStartIntoPreviousDay() {
        val utc = TimeZone.getTimeZone("UTC")
        val now = 8L * DAY + 12L * HOUR
        val oneAm = CompletionEvent(
            RoutineRunId("late"), RoutineId("routine"), "Evening", 8L * DAY, 8L * DAY + HOUR,
            RunStatus.COMPLETED, emptyList(),
        )
        val threeAm = oneAm.copy(runId = RoutineRunId("early"), endedAtEpochMillis = 8L * DAY + 3L * HOUR)

        val summary = projectProgress(listOf(oneAm, threeAm), now, 2, utc)

        assertEquals(8L * DAY + 2L * HOUR, summary.lastSevenDays[6].dayStartEpochMillis)
        assertEquals(1, summary.lastSevenDays[6].completedRuns)
        assertEquals(7L * DAY + 2L * HOUR, summary.lastSevenDays[5].dayStartEpochMillis)
        assertEquals(1, summary.lastSevenDays[5].completedRuns)
        assertEquals(2, projectProgress(listOf(oneAm, threeAm), now, 0, utc).lastSevenDays[6].completedRuns)
    }

    /** Use this function to verify that before the day start the newest trend day is still the previous date. */
    @Test
    fun trendEndsAtCurrentDayStartBeforeTwoAm() {
        val summary = projectProgress(emptyList(), 8L * DAY + HOUR, 2, TimeZone.getTimeZone("UTC"))

        assertEquals(7L * DAY + 2L * HOUR, summary.lastSevenDays.last().dayStartEpochMillis)
        assertEquals(1L * DAY + 2L * HOUR, summary.lastSevenDays.first().dayStartEpochMillis)
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24L * HOUR
    }
}
