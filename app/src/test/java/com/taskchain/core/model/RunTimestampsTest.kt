package com.taskchain.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifies the single derivation of when a run task or Routine run completed. */
class RunTimestampsTest {
    /** Use this function to verify a completed task prefers its completion time and falls back to its finish time. */
    @Test
    fun completedTaskPrefersCompletionThenFinishTime() {
        assertEquals(30L, task(RunTaskStatus.COMPLETED, finishedAt = 20, completedAt = 30).completedAtOrNull())
        assertEquals(20L, task(RunTaskStatus.COMPLETED, finishedAt = 20).completedAtOrNull())
        assertNull(task(RunTaskStatus.COMPLETED).completedAtOrNull())
    }

    /** Use this function to verify skipped and pending tasks never report a completion time. */
    @Test
    fun unfinishedTaskHasNoCompletionTime() {
        assertNull(task(RunTaskStatus.SKIPPED, finishedAt = 20).completedAtOrNull())
        assertNull(task(RunTaskStatus.PENDING, finishedAt = 20, completedAt = 30).completedAtOrNull())
    }

    /** Use this function to verify a completed run prefers its end time over task timestamps. */
    @Test
    fun completedRunPrefersEndTime() {
        val tasks = listOf(task(RunTaskStatus.COMPLETED, completedAt = 90))
        assertEquals(50L, run(RunStatus.COMPLETED, tasks, endedAt = 50).completedAtOrNull())
    }

    /** Use this function to verify a completed run without an end time uses its latest completed task only. */
    @Test
    fun completedRunWithoutEndTimeUsesLatestCompletedTask() {
        val tasks = listOf(
            task(RunTaskStatus.COMPLETED, completedAt = 30),
            task(RunTaskStatus.COMPLETED, finishedAt = 40),
            task(RunTaskStatus.SKIPPED, finishedAt = 70),
        )
        assertEquals(40L, run(RunStatus.COMPLETED, tasks).completedAtOrNull())
        assertNull(run(RunStatus.COMPLETED, listOf(task(RunTaskStatus.SKIPPED, finishedAt = 70))).completedAtOrNull())
    }

    /** Use this function to verify active and aborted runs never report a completion time. */
    @Test
    fun unfinishedRunHasNoCompletionTime() {
        val tasks = listOf(task(RunTaskStatus.COMPLETED, completedAt = 30))
        assertNull(run(RunStatus.ACTIVE, tasks, endedAt = 50).completedAtOrNull())
        assertNull(run(RunStatus.ABORTED, tasks, endedAt = 50).completedAtOrNull())
    }

    /** Use this function to build a run task with the given status and optional finish/completion timestamps. */
    private fun task(status: RunTaskStatus, finishedAt: Long? = null, completedAt: Long? = null) = RoutineRunTask(
        source = RoutineTask(RoutineTaskId("task"), "Work"),
        status = status,
        finishedAtEpochMillis = finishedAt,
        completedAtEpochMillis = completedAt,
    )

    /** Use this function to build a run with the given status, tasks and optional end timestamp. */
    private fun run(status: RunStatus, tasks: List<RoutineRunTask>, endedAt: Long? = null) = RoutineRun(
        id = RoutineRunId("run"),
        routineId = RoutineId("routine"),
        routineTitle = "Morning",
        tasks = tasks,
        currentTaskIndex = 0,
        startedAtEpochMillis = 0,
        endedAtEpochMillis = endedAt,
        status = status,
    )
}
