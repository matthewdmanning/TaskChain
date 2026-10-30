package com.taskchain.features.running.domain

import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineSubtask
import com.taskchain.core.model.RoutineSubtaskId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the Run screen's system Back decision and derived timer values, owned by the engine. */
class RunScreenRulesTest {
    private val engine = RoutineRunEngine()

    /** Use this function to build a routine whose first task has 3- and 7-minute subtasks and a 60 s second task. */
    private fun routine() = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Routine",
        tasks = listOf(
            RoutineTask(
                RoutineTaskId("main"),
                "Main",
                subtasks = listOf(
                    RoutineSubtask(RoutineSubtaskId("first"), "First", 180),
                    RoutineSubtask(RoutineSubtaskId("second"), "Second", 420),
                ),
            ),
            RoutineTask(RoutineTaskId("last"), "Last", timerSeconds = 60),
        ),
    )

    /** Use this function to verify system Back on the first task requests abort confirmation, like Back. */
    @Test
    fun systemBackOnFirstTaskRequestsAbort() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)

        val backed = engine.systemBack(started, 1_000L)

        assertEquals(engine.back(started, 1_000L), backed)
        assertTrue(backed.abortConfirmationRequested)
    }

    /** Use this function to verify system Back on a later task returns to the previous task. */
    @Test
    fun systemBackOnLaterTaskReturnsToPrevious() {
        val second = engine.skipCurrent(engine.start(routine(), RoutineRunId("run"), 0), 1_000L)

        val backed = engine.systemBack(second, 2_000L)

        assertEquals(engine.back(second, 2_000L), backed)
        assertEquals(0, backed.currentTaskIndex)
    }

    /** Use this function to verify system Back closes an open abort or finish confirmation and continues the run. */
    @Test
    fun systemBackClosesOpenConfirmation() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val aborting = engine.back(started, 1_000L)
        val closedAbort = engine.systemBack(aborting, 2_000L)
        assertEquals(engine.continueRun(aborting, 2_000L), closedAbort)
        assertFalse(closedAbort.abortConfirmationRequested)

        // Skipping the first task leaves it unfinished, so completing the last task opens the finish confirmation.
        val finishing = engine.completeCurrent(engine.skipCurrent(started, 1_000L), 2_000L)
        assertTrue(finishing.finishConfirmationRequested)
        val closedFinish = engine.systemBack(finishing, 3_000L)
        assertEquals(engine.continueRun(finishing, 3_000L), closedFinish)
        assertFalse(closedFinish.finishConfirmationRequested)
    }

    /** Use this function to verify derived timing matches the individual engine clocks for one sample. */
    @Test
    fun timingMatchesEngineClocks() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val advanced = engine.completeCurrent(started, 120_000L)
        val now = 610_000L

        val timing = engine.timing(advanced, now)

        assertEquals(engine.remainingMillis(advanced, now), timing.remainingMillis)
        assertEquals(engine.subtaskRemainingMillis(advanced, now), timing.subtaskRemainingMillis)
        assertEquals(listOf(120_000L, 490_000L), timing.subtaskElapsedMillis)
        assertTrue(timing.behindSubtaskSchedule)
        assertEquals(listOf(0, 1), timing.unfinishedTaskIndexes)
    }

    /** Use this function to verify the placeholder timing shown before a run loads. */
    @Test
    fun noneHasNoTimers() {
        assertEquals(RunTiming(null, null, emptyList(), behindSubtaskSchedule = false, unfinishedTaskIndexes = emptyList()), RunTiming.None)
    }
}
