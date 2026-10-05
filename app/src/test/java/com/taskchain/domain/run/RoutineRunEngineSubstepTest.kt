package com.taskchain.domain.run

import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.model.RoutineStepRole
import com.taskchain.domain.model.RoutineTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Verifies that secondary steps reuse the ordinary persisted run-timer mechanics. */
class RoutineRunEngineSubstepTest {
    private val engine = RoutineRunEngine()

    /** A secondary timer advances and derives remaining time exactly like a main step. */
    @Test
    fun secondaryStepUsesExistingTimerMechanics() {
        val mainId = RoutineStepId("main")
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Routine",
            steps = listOf(
                RoutineStep(mainId, "Main", timerSeconds = 10),
                RoutineStep(
                    id = RoutineStepId("secondary"),
                    title = "Secondary",
                    timerSeconds = 5,
                    role = RoutineStepRole.SECONDARY,
                    parentStepId = mainId,
                ),
                RoutineStep(RoutineStepId("next"), "Next"),
            ),
        )

        val started = engine.start(routine, RoutineRunId("run"), nowEpochMillis = 1_000)
        val secondary = engine.completeCurrent(started, nowEpochMillis = 3_000)

        assertEquals(1, secondary.currentStepIndex)
        assertEquals(RoutineStepRole.SECONDARY, secondary.steps[1].source.role)
        assertEquals(3_000L, secondary.steps[1].startedAtEpochMillis)
        assertEquals(4_000L, engine.remainingMillis(secondary, nowEpochMillis = 4_000))

        val next = engine.completeCurrent(secondary, nowEpochMillis = 5_000)
        assertEquals(2, next.currentStepIndex)
        assertEquals(5_000L, next.steps[2].startedAtEpochMillis)
        assertEquals(2_000L, next.steps[1].actualDurationMillis)
    }

    /** A secondary step cannot be persisted without a valid preceding main parent. */
    @Test
    fun runnableRoutineRejectsOrphanSecondaryStep() {
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Routine",
            steps = listOf(
                RoutineStep(RoutineStepId("main"), "Main"),
                RoutineStep(
                    id = RoutineStepId("secondary"),
                    title = "Secondary",
                    role = RoutineStepRole.SECONDARY,
                    parentStepId = RoutineStepId("missing"),
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) { routine.requireRunnable() }
    }
}
