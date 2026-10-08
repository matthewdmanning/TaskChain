package com.taskchain.ui

import com.taskchain.domain.model.RoutineSubtask
import com.taskchain.domain.model.RoutineSubtaskId
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies fixed planned rim anchors and actual-time overtime sweeps. */
class SubtaskRimGeometryTest {
    /** Use this function to verify early advancement and overtime do not shift planned subtask anchors.
     * Inputs: none. Dependencies: subtask rim geometry and nested subtask durations.
     */
    @Test
    fun rimsKeepPlannedAnchorsAndExtendPastAllowance() {
        val task = RoutineTask(RoutineTaskId("main"), "Main", subtasks = listOf(
            RoutineSubtask(RoutineSubtaskId("one"), "One", 180),
            RoutineSubtask(RoutineSubtaskId("two"), "Two", 420),
        ))
        assertEquals(-90f, subtaskRimStartDegrees(task, 0), 0.001f)
        assertEquals(18f, subtaskRimStartDegrees(task, 1), 0.001f)
        assertEquals(72f, subtaskRimSweepDegrees(120_000, 600), 0.001f)
        assertTrue(subtaskRimSweepDegrees(480_000, 600) > subtaskRimSweepDegrees(420_000, 600))
    }
}
