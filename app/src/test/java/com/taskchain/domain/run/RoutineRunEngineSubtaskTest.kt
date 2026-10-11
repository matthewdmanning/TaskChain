package com.taskchain.domain.run

import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineSubtask
import com.taskchain.domain.model.RoutineSubtaskId
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import com.taskchain.domain.model.SoundToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards nested subtask timing, manual advancement, and immutable run snapshots. */
class RoutineRunEngineSubtaskTest {
    /** Use this function to build the ten-minute main task used by subtask timing checks. */
    private fun routine(): RoutineTemplate = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Routine",
        tasks = listOf(
            RoutineTask(
                id = RoutineTaskId("main"),
                title = "Main",
                subtasks = listOf(
                    RoutineSubtask(RoutineSubtaskId("subtask-1"), "First", 180),
                    RoutineSubtask(RoutineSubtaskId("subtask-2"), "Second", 420),
                ),
            ),
            RoutineTask(RoutineTaskId("next"), "Next"),
        ),
    )

    /** Use this function to verify manual subtask advancement leaves the main task active and preserves its clock. */
    @Test
    fun advancingSubtaskKeepsMainTaskActiveAndUsesMainElapsedTime() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val advanced = engine.completeCurrent(started, 2 * 60_000L)

        assertEquals(RunStatus.ACTIVE, advanced.status)
        assertEquals(0, advanced.currentTaskIndex)
        assertEquals(0L, advanced.tasks.first().startedAtEpochMillis)
        assertEquals(RoutineSubtaskId("subtask-2"), advanced.tasks.first().activeSubtaskId)
        assertEquals(2 * 60_000L, advanced.tasks.first().subtaskAdvancements.single().elapsedMillis)
        assertEquals(2 * 60_000L, engine.elapsedMillis(advanced, 2 * 60_000L))
        assertEquals(420_000L, engine.subtaskRemainingMillis(advanced, 2 * 60_000L))
        assertFalse(engine.isBehindSubtaskSchedule(advanced, 10 * 60_000L))
        assertTrue(engine.isBehindSubtaskSchedule(advanced, 10 * 60_000L + 1L))
    }

    /** Use this function to verify subtask and main clocks freeze through skip, navigation, and explicit resume. */
    @Test
    fun skippedSubtaskTaskRestoresFrozenMainAndSubtaskState() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val advanced = engine.completeCurrent(started, 2 * 60_000L)
        val paused = engine.pauseCurrent(advanced, 3 * 60_000L)
        val skipped = engine.skipCurrent(paused, 4 * 60_000L)
        val revisited = engine.back(skipped, 10 * 60_000L)
        val resumed = engine.resumeCurrent(revisited, 20 * 60_000L)

        assertEquals(RunTaskStatus.SKIPPED, revisited.tasks.first().status)
        assertEquals(RoutineSubtaskId("subtask-2"), revisited.tasks.first().activeSubtaskId)
        assertEquals(3 * 60_000L, revisited.tasks.first().actualDurationMillis)
        assertEquals(RunTaskStatus.PENDING, resumed.tasks.first().status)
        assertEquals(RoutineSubtaskId("subtask-2"), resumed.tasks.first().activeSubtaskId)
        assertEquals(3 * 60_000L, engine.elapsedMillis(resumed, 20 * 60_000L))
        assertEquals(2 * 60_000L, engine.subtaskElapsedMillis(resumed, RoutineSubtaskId("subtask-1"), 20 * 60_000L))
        assertEquals(60_000L, engine.subtaskElapsedMillis(resumed, RoutineSubtaskId("subtask-2"), 20 * 60_000L))
    }

    /** Use this function to verify the last subtask completes its main task and requests normal final confirmation. */
    @Test
    fun completingLastSubtaskCompletesMainTask() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val first = engine.completeCurrent(started, 60_000L)
        val second = engine.completeCurrent(first, 120_000L)

        assertEquals(RunTaskStatus.COMPLETED, second.tasks.first().status)
        assertEquals(2, second.tasks.first().subtaskAdvancements.size)
        assertEquals(RoutineSubtaskId("subtask-2"), second.tasks.first().subtaskAdvancements.last().subtaskId)
        assertTrue(second.tasks.first().activeSubtaskId == null)
        assertEquals(1, second.currentTaskIndex)
    }

    /** Use this function to verify a completed subtask task still requests confirmation when an earlier task is unfinished. */
    @Test
    fun completingLastSubtaskUsesMainFinalConfirmation() {
        val engine = RoutineRunEngine()
        val subtaskTask = routine().tasks.first().copy(id = RoutineTaskId("subtask-main"))
        val started = engine.start(
            routine().copy(tasks = listOf(RoutineTask(RoutineTaskId("pending"), "Pending"), subtaskTask)),
            RoutineRunId("run"),
            0,
        )
        val onSubtaskTask = engine.selectTask(started, 1, 1_000L)
        val first = engine.completeCurrent(onSubtaskTask, 2_000L)
        val requested = engine.completeCurrent(first, 3_000L)

        assertEquals(RunStatus.ACTIVE, requested.status)
        assertTrue(requested.finishConfirmationRequested)
        assertEquals(listOf(0), engine.unfinishedTaskIndexes(requested))
        assertEquals(RunStatus.COMPLETED, engine.confirmComplete(requested, 4_000L).status)
    }

    /** Use this function to verify Complete advances a skipped subtask while preserving its frozen main state until Resume. */
    @Test
    fun completingSkippedSubtaskPreservesFrozenStateUntilResume() {
        val engine = RoutineRunEngine()
        val threeSubtaskTask = routine().tasks.first().copy(
            subtasks = routine().tasks.first().subtasks + RoutineSubtask(RoutineSubtaskId("subtask-3"), "Third", 60),
        )
        val started = engine.start(
            routine().copy(tasks = listOf(threeSubtaskTask, routine().tasks[1])),
            RoutineRunId("run"),
            0,
        )
        val firstSubtask = engine.completeCurrent(started, 60_000L)
        assertEquals(0L, engine.subtaskElapsedMillis(firstSubtask, RoutineSubtaskId("subtask-3"), 60_000L))
        val skipped = engine.skipCurrent(firstSubtask, 120_000L)
        val revisited = engine.back(skipped, 180_000L)
        val secondSubtask = engine.completeCurrent(revisited, 180_000L)
        val resumed = engine.resumeCurrent(secondSubtask, 600_000L)

        assertEquals(RunTaskStatus.SKIPPED, secondSubtask.tasks.first().status)
        assertEquals(RoutineSubtaskId("subtask-3"), secondSubtask.tasks.first().activeSubtaskId)
        assertEquals(2, secondSubtask.tasks.first().subtaskAdvancements.size)
        assertEquals(120_000L, secondSubtask.tasks.first().actualDurationMillis)
        assertEquals(RunTaskStatus.PENDING, resumed.tasks.first().status)
        assertEquals(RoutineSubtaskId("subtask-3"), resumed.tasks.first().activeSubtaskId)
    }

    /** Use this function to verify cancelling final-subtask confirmation restores the last subtask for a later Complete. */
    @Test
    fun continuingFinalSubtaskConfirmationRestoresActiveSubtaskSnapshot() {
        val engine = RoutineRunEngine()
        val subtaskTask = routine().tasks.first().copy(id = RoutineTaskId("subtask-main"))
        val started = engine.start(
            routine().copy(tasks = listOf(RoutineTask(RoutineTaskId("pending"), "Pending"), subtaskTask)),
            RoutineRunId("run"),
            0,
        )
        val onSubtaskTask = engine.selectTask(started, 1, 1_000L)
        val first = engine.completeCurrent(onSubtaskTask, 2_000L)
        val requested = engine.completeCurrent(first, 3_000L)
        val continued = engine.continueRun(requested, 10_000L)
        val completedSubtask = engine.completeCurrent(continued, 11_000L)

        assertEquals(RoutineSubtaskId("subtask-2"), continued.tasks.last().activeSubtaskId)
        assertEquals(1, continued.tasks.last().subtaskAdvancements.size)
        assertEquals(2, completedSubtask.tasks.last().subtaskAdvancements.size)
        assertTrue(completedSubtask.finishConfirmationRequested)
    }

    /** Use this function to verify subtask feedback is emitted once without pretending a nonfinal subtask completed the task. */
    @Test
    fun subtaskAdvancementFeedbackIsOnceOnlyAndFinalSubtaskAlsoAdvances() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val advanced = engine.completeCurrent(started, 60_000L)
        val feedback = RunFeedbackPolicy.stateEntryEvents(started, advanced)

        assertEquals(listOf(RunFeedbackEvent(SoundToken.SubtaskAdvanced, 0)), feedback)
        assertTrue(RunFeedbackPolicy.stateEntryEvents(advanced, advanced).isEmpty())

        val single = routine().copy(tasks = listOf(routine().tasks.first()))
        val singleStarted = engine.start(single, RoutineRunId("single"), 0)
        val singleAdvanced = engine.completeCurrent(singleStarted, 60_000L)
        val singleCompleted = engine.completeCurrent(singleAdvanced, 120_000L)

        assertEquals(
            listOf(
                RunFeedbackEvent(SoundToken.SubtaskAdvanced, 0),
                RunFeedbackEvent(SoundToken.TaskCompleted, 0),
            ),
            RunFeedbackPolicy.stateEntryEvents(singleAdvanced, singleCompleted),
        )
    }

    /** Use this function to verify subtask definitions are copied into a run instead of sharing mutable caller state. */
    @Test
    fun startDeepCopiesSubtaskDefinitions() {
        val subtasks = mutableListOf(RoutineSubtask(RoutineSubtaskId("subtask"), "Original", 60))
        val source = routine().copy(tasks = listOf(routine().tasks.first().copy(subtasks = subtasks)))
        val run = RoutineRunEngine().start(source, RoutineRunId("run"), 0)

        subtasks[0] = subtasks[0].copy(title = "Edited")

        assertNotSame(source.tasks.first().subtasks, run.tasks.first().source.subtasks)
        assertEquals("Original", run.tasks.first().source.subtasks.single().title)
    }
}
