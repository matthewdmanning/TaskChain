package com.taskchain.ui

import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunStep
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.model.RunStepStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the completion transition gate used by the runner presentation. */
class RunPresentationTest {
    @Test
    fun completedStepTransitionOwnsOnePresentationHold() {
        val pending = run("run", listOf(RunStepStatus.PENDING, RunStepStatus.PENDING), 0)
        val advanced = pending.copy(
            currentStepIndex = 1,
            steps = pending.steps.mapIndexed { index, step ->
                if (index == 0) step.copy(status = RunStepStatus.COMPLETED, finishedAtEpochMillis = 2_000) else step
            },
        )

        assertTrue(shouldHoldCompletedPresentation(pending, advanced))
        assertTrue(shouldHoldTaskReadyTransition(pending, advanced))
        assertFalse(shouldHoldCompletedPresentation(pending, advanced.copy(id = RoutineRunId("other"))))
        assertFalse(shouldHoldCompletedPresentation(pending, advanced.copy(steps = advanced.steps.map { it.copy(status = RunStepStatus.SKIPPED) })))
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
    private fun run(id: String, statuses: List<RunStepStatus>, currentStepIndex: Int): RoutineRun =
        RoutineRun(
            id = RoutineRunId(id),
            routineId = RoutineId("routine"),
            routineTitle = "Routine",
            steps = statuses.mapIndexed { index, status ->
                RoutineRunStep(RoutineStep(RoutineStepId("step-$index"), "Step $index"), status = status)
            },
            currentStepIndex = currentStepIndex,
            startedAtEpochMillis = 1_000,
        )
}
