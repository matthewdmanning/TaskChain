package com.taskchain.features.running.domain

import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RunStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that Running entry blocks only a different active routine. */
class RunningEntryPolicyTest {
    /** Use this test when guarding start eligibility for absent, same, and different active runs. */
    @Test
    fun startIsBlocked_onlyDifferentActiveRoutine() {
        val activeRoutine = RoutineId("active")
        val otherRoutine = RoutineId("other")
        val activeRun = RoutineRun(
            id = RoutineRunId("run"),
            routineId = activeRoutine,
            routineTitle = "Active",
            tasks = emptyList(),
            currentTaskIndex = 0,
            startedAtEpochMillis = 1L,
        )
        assertFalse(startIsBlocked(null, otherRoutine))
        assertFalse(startIsBlocked(activeRun, activeRoutine))
        assertTrue(startIsBlocked(activeRun, otherRoutine))
        assertFalse(startIsBlocked(activeRun.copy(status = RunStatus.COMPLETED), otherRoutine))
    }
}
