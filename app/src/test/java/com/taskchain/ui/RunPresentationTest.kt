package com.taskchain.ui

import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineSubtask
import com.taskchain.domain.model.RoutineSubtaskId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.run.RoutineRunEngine
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunTask
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RunTaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the completion transition gate used by the runner presentation. */
class RunPresentationTest {
    /** Use this function to verify subtask advancement does not start a main-task visual hold.
     * Inputs: none. Dependencies: RoutineRunEngine and the two existing presentation gates.
     */
    @Test
    fun subtaskAdvancementDoesNotHoldCompletionOrReadiness() {
        val engine = RoutineRunEngine()
        val template = RoutineTemplate(
            RoutineId("routine"), EntityMetadata(0, 0), "Routine",
            tasks = listOf(RoutineTask(RoutineTaskId("main"), "Main", subtasks = listOf(
                RoutineSubtask(RoutineSubtaskId("first"), "First", 180),
                RoutineSubtask(RoutineSubtaskId("second"), "Second", 420),
            ))),
        )
        val before = engine.start(template, RoutineRunId("run"), 0)
        val after = engine.completeCurrent(before, 120_000)
        assertFalse(shouldHoldCompletedPresentation(before, after))
        assertFalse(shouldHoldTaskReadyTransition(before, after))
    }

    @Test
    fun completedTaskTransitionOwnsOnePresentationHold() {
        val pending = run("run", listOf(RunTaskStatus.PENDING, RunTaskStatus.PENDING), 0)
        val advanced = pending.copy(
            currentTaskIndex = 1,
            tasks = pending.tasks.mapIndexed { index, task ->
                if (index == 0) task.copy(status = RunTaskStatus.COMPLETED, finishedAtEpochMillis = 2_000) else task
            },
        )

        assertTrue(shouldHoldCompletedPresentation(pending, advanced))
        assertTrue(shouldHoldTaskReadyTransition(pending, advanced))
        assertFalse(shouldHoldCompletedPresentation(pending, advanced.copy(id = RoutineRunId("other"))))
        assertFalse(shouldHoldCompletedPresentation(pending, advanced.copy(tasks = advanced.tasks.map { it.copy(status = RunTaskStatus.SKIPPED) })))
    }

    /** Use this function to verify the five-second task-ready labels stay on their specified boundaries.
     * Inputs: none.
     * Dependencies: `taskReadyPhase`.
     */
    @Test
    fun taskReadyPhase_usesGetReadyThenThreeTwoOne() {
        assertEquals(TaskReadyPhase.GET_READY, taskReadyPhase(0L))
        assertEquals(TaskReadyPhase.GET_READY, taskReadyPhase(1_999L))
        assertEquals(TaskReadyPhase.THREE, taskReadyPhase(2_000L))
        assertEquals(TaskReadyPhase.TWO, taskReadyPhase(3_000L))
        assertEquals(TaskReadyPhase.ONE, taskReadyPhase(4_000L))
        assertNull(taskReadyPhase(5_000L))
    }

    /** Use this function to create the smallest run snapshots needed by the transition assertions. */
    private fun run(id: String, statuses: List<RunTaskStatus>, currentTaskIndex: Int): RoutineRun =
        RoutineRun(
            id = RoutineRunId(id),
            routineId = RoutineId("routine"),
            routineTitle = "Routine",
            tasks = statuses.mapIndexed { index, status ->
                RoutineRunTask(RoutineTask(RoutineTaskId("task-$index"), "Task $index"), status = status)
            },
            currentTaskIndex = currentTaskIndex,
            startedAtEpochMillis = 1_000,
        )
}
