package com.taskchain.domain.progress

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunTask
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
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

        val summary = projectProgress(listOf(completed, aborted), now, TimeZone.getTimeZone("UTC"))

        assertEquals(1, summary.completedRuns)
        assertEquals(1, summary.abortedRuns)
        assertEquals(8_000L, summary.actualDurationMillis)
        assertEquals(33, summary.taskAdherencePercent)
        assertEquals(33, summary.skippedTaskPercent)
        assertEquals(7, summary.lastSevenDays.size)
        assertEquals(1, summary.lastSevenDays.last().completedRuns)
    }
}
