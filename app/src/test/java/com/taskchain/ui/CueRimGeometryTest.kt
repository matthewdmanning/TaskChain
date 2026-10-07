package com.taskchain.ui

import com.taskchain.domain.model.RoutineCue
import com.taskchain.domain.model.RoutineCueId
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies fixed planned rim anchors and actual-time overtime sweeps. */
class CueRimGeometryTest {
    /** Use this function to verify early advancement and overtime do not shift planned cue anchors.
     * Inputs: none. Dependencies: cue rim geometry and nested cue durations.
     */
    @Test
    fun rimsKeepPlannedAnchorsAndExtendPastAllowance() {
        val step = RoutineStep(RoutineStepId("main"), "Main", cues = listOf(
            RoutineCue(RoutineCueId("one"), "One", 180),
            RoutineCue(RoutineCueId("two"), "Two", 420),
        ))
        assertEquals(-90f, cueRimStartDegrees(step, 0), 0.001f)
        assertEquals(18f, cueRimStartDegrees(step, 1), 0.001f)
        assertEquals(72f, cueRimSweepDegrees(120_000, 600), 0.001f)
        assertTrue(cueRimSweepDegrees(480_000, 600) > cueRimSweepDegrees(420_000, 600))
    }
}
