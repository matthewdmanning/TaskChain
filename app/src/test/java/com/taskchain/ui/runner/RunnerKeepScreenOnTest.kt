package com.taskchain.ui.runner

import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunTask
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RunStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies when the Run screen keeps the display awake. */
class RunnerKeepScreenOnTest {
    /** Use this function to verify the display stays on while a run is active.
     * Inputs: none. Dependencies: `runKeepsScreenOn`.
     */
    @Test
    fun activeRunKeepsScreenOn() {
        assertTrue(runKeepsScreenOn(run(RunStatus.ACTIVE)))
    }

    /** Use this function to verify a loading screen, and ended runs, let the display sleep.
     * Inputs: none. Dependencies: `runKeepsScreenOn`.
     */
    @Test
    fun loadingOrEndedRunAllowsScreenTimeout() {
        assertFalse(runKeepsScreenOn(null))
        assertFalse(runKeepsScreenOn(run(RunStatus.COMPLETED)))
        assertFalse(runKeepsScreenOn(run(RunStatus.ABORTED)))
    }

    /** Use this function to create the smallest run snapshot needed by the assertions. */
    private fun run(status: RunStatus): RoutineRun =
        RoutineRun(
            id = RoutineRunId("run"),
            routineId = RoutineId("routine"),
            routineTitle = "Routine",
            tasks = listOf(RoutineRunTask(RoutineTask(RoutineTaskId("task"), "Task"))),
            currentTaskIndex = 0,
            startedAtEpochMillis = 1_000,
            status = status,
        )
}
