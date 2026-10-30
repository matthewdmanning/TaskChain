package com.taskchain.features.running.domain

import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineSubtask
import com.taskchain.core.model.RoutineSubtaskId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the persisted readiness delay applied when Complete advances to a new task. */
class RunReadinessTest {
    private val engine = RoutineRunEngine()

    /** Use this function to build a routine of untimed tasks, the first optionally split into two subtasks. */
    private fun routine(taskCount: Int = 2, firstHasSubtasks: Boolean = false) = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Routine",
        tasks = (0 until taskCount).map { index ->
            RoutineTask(
                RoutineTaskId("t$index"),
                "Task $index",
                subtasks = if (index == 0 && firstHasSubtasks) {
                    listOf(
                        RoutineSubtask(RoutineSubtaskId("a"), "A", 60),
                        RoutineSubtask(RoutineSubtaskId("b"), "B", 60),
                    )
                } else {
                    emptyList()
                },
            )
        },
    )

    /** Use this function to verify the delay equals the completion effect plus the five-second countdown. */
    @Test
    fun delayIsCompletionEffectPlusCountdown() {
        assertEquals(350L, RunReadiness.completionDurationMillis)
        assertEquals(5_000L, RunReadiness.taskReadyTransitionDurationMillis)
        assertEquals(5_350L, RunReadiness.taskStartDelayMillis)
        assertEquals(5_350L, RunReadiness.startDelayMillis(presented = true))
        assertEquals(0L, RunReadiness.startDelayMillis(presented = false))
    }

    /** Use this function to verify a presented completion persists the next task's later start. */
    @Test
    fun presentedCompletionDelaysNextTaskStart() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)

        val delayed = engine.completeCurrentWithReadiness(started, 5_000L, RunReadiness.startDelayMillis(presented = true))

        assertEquals(1, delayed.currentTaskIndex)
        assertEquals(5_000L + 5_350L, delayed.tasks[1].startedAtEpochMillis)
        assertTrue(RunReadiness.startsTaskReadyTransition(started, delayed))
    }

    /** Use this function to verify an unpresented completion starts the next task immediately. */
    @Test
    fun unpresentedCompletionStartsImmediately() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)

        val completed = engine.completeCurrentWithReadiness(started, 5_000L, RunReadiness.startDelayMillis(presented = false))

        assertEquals(engine.completeCurrent(started, 5_000L), completed)
        assertEquals(5_000L, completed.tasks[1].startedAtEpochMillis)
    }

    /** Use this function to verify finishing the run, or advancing a subtask, never adds a delay. */
    @Test
    fun finalTaskAndSubtaskAdvanceAreNotDelayed() {
        val single = engine.start(routine(taskCount = 1), RoutineRunId("run"), 0)
        val finished = engine.completeCurrentWithReadiness(single, 5_000L, RunReadiness.taskStartDelayMillis)
        assertEquals(RunStatus.COMPLETED, finished.status)
        assertEquals(engine.completeCurrent(single, 5_000L), finished)

        val withSubtasks = engine.start(routine(firstHasSubtasks = true), RoutineRunId("run"), 0)
        val advanced = engine.completeCurrentWithReadiness(withSubtasks, 5_000L, RunReadiness.taskStartDelayMillis)
        assertEquals(engine.completeCurrent(withSubtasks, 5_000L), advanced)
        assertFalse(RunReadiness.completedCurrentTask(withSubtasks, advanced))
    }

    /** Use this function to verify a next task that already ran keeps its prior active time and is not delayed again. */
    @Test
    fun previouslyStartedNextTaskIsNotDelayed() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val revisited = engine.previousTask(engine.skipCurrent(started, 1_000L), 2_000L)

        val completed = engine.completeCurrentWithReadiness(revisited, 3_000L, RunReadiness.taskStartDelayMillis)

        assertEquals(engine.completeCurrent(revisited, 3_000L), completed)
        assertEquals(2_000L, completed.tasks[1].startedAtEpochMillis)
    }

    /** Use this function to verify different runs never count as one completion. */
    @Test
    fun differentRunsDoNotMatch() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val other = engine.completeCurrent(started, 5_000L).copy(id = RoutineRunId("other"))

        assertFalse(RunReadiness.completedCurrentTask(started, other))
        assertFalse(RunReadiness.startsTaskReadyTransition(started, other))
    }
}
