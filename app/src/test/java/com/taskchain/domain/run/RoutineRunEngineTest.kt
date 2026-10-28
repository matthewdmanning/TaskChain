package com.taskchain.domain.run

import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import com.taskchain.domain.model.SoundSetting
import com.taskchain.domain.model.SoundSettings
import com.taskchain.domain.model.SoundToken
import com.taskchain.domain.model.defaultSoundSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises the run invariants most likely to regress as runner UI evolves. */
class RoutineRunEngineTest {
    /** Use this function to supply a small valid routine for transition tests. */
    private fun routine(timerSeconds: Long? = null): RoutineTemplate = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Morning",
        tasks = listOf(
            RoutineTask(RoutineTaskId("one"), "One", timerSeconds = timerSeconds),
            RoutineTask(RoutineTaskId("two"), "Two"),
        ),
    )

    /** Use this function to verify a confirmation inside an explicit pause is excluded exactly once. */
    @Test
    fun confirmationInsidePauseDoesNotDoubleCountPausedTime() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 120), RoutineRunId("run"), 1_000)
        val paused = engine.pauseCurrent(started, 21_000)
        val confirming = engine.back(paused, 32_000)
        assertEquals(100_000L, engine.remainingMillis(confirming, 42_000))
        val continued = engine.continueRun(confirming, 42_000)
        val resumed = engine.resumeCurrent(continued, 61_000)

        assertEquals(41_000L, resumed.tasks.first().startedAtEpochMillis)
        assertEquals(100_000L, engine.remainingMillis(resumed, 61_000))
    }

    /** Use this function to verify skipped state, final confirmation, and timer restoration together. */
    @Test
    fun preservesRunStateAcrossNavigationAndConfirmation() {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Morning",
            tasks = listOf(
                RoutineTask(RoutineTaskId("one"), "One", timerSeconds = 10),
                RoutineTask(RoutineTaskId("two"), "Two"),
            ),
        )

        val started = engine.start(routine, RoutineRunId("run"), nowEpochMillis = 1_000)
        val skipped = engine.skipCurrent(started, nowEpochMillis = 4_000)
        val back = engine.back(skipped, nowEpochMillis = 5_000)
        val revisited = engine.selectTask(back, index = 1, nowEpochMillis = 6_000)
        val requested = engine.completeCurrent(revisited, nowEpochMillis = 7_000)

        assertEquals(RunTaskStatus.SKIPPED, back.tasks.first().status)
        assertEquals(3_000L, back.tasks.first().actualDurationMillis)
        assertFalse(back.tasks.first().startedAtEpochMillis == 5_000L)
        assertTrue(requested.finishConfirmationRequested)
        assertEquals(listOf(0), engine.unfinishedTaskIndexes(requested))
        assertEquals(RunStatus.COMPLETED, engine.confirmComplete(requested, 8_000).status)
    }

    /** Use this function to verify that terminal runs reject further transitions. */
    @Test(expected = IllegalArgumentException::class)
    fun transitionsRequireAnActiveRun() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)
        val requested = engine.completeCurrent(engine.selectTask(started, 1, 2_000), 2_000)
        val completed = engine.confirmComplete(requested, 3_000)

        engine.selectTask(completed, 0, 4_000)
    }

    /** Use this function to verify that selecting an out-of-range task fails before mutation. */
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidTaskIndex() {
        RoutineRunEngine().selectTask(
            RoutineRunEngine().start(routine(), RoutineRunId("run"), 1_000),
            2,
            2_000,
        )
    }

    /** Use this function to verify that completing a skipped task preserves its recorded timing. */
    @Test
    fun completingSkippedTaskChangesOnlyItsStatus() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 10), RoutineRunId("run"), 1_000)
        val skipped = engine.skipCurrent(started, 4_000)
        val revisited = engine.selectTask(skipped, 0, 9_000)
        val completed = engine.completeCurrent(revisited, 10_000)
        val task = completed.tasks.first()

        assertEquals(RunTaskStatus.COMPLETED, task.status)
        assertEquals(1_000L, task.startedAtEpochMillis)
        assertEquals(4_000L, task.finishedAtEpochMillis)
        assertEquals(10_000L, task.completedAtEpochMillis)
        assertEquals(3_000L, task.actualDurationMillis)
    }

    /** Use this function to verify explicit pause and resume preserve elapsed timer time. */
    @Test
    fun pauseAndResumeExcludePausedTime() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 10), RoutineRunId("run"), 1_000)

        val paused = engine.pauseCurrent(started, 4_000)
        assertEquals(4_000L, paused.tasks.first().pausedAtEpochMillis)
        assertEquals(7_000L, engine.remainingMillis(paused, 9_000))

        val resumed = engine.resumeCurrent(paused, 9_000)
        assertEquals(null, resumed.tasks.first().pausedAtEpochMillis)
        assertEquals(6_000L, resumed.tasks.first().startedAtEpochMillis)
        assertEquals(6_000L, engine.remainingMillis(resumed, 10_000))
    }

    /**
     * Use this function to verify that explicitly resuming a skipped paused task preserves elapsed time.
     * Inputs: none; the test builds an active run with a paused then skipped current task.
     * Dependencies: `RoutineRunEngine`, `routine`, and JUnit assertions.
     */
    @Test
    fun explicitlyResumingSkippedPausedTaskRestoresPendingTimer() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 10), RoutineRunId("run"), 1_000)
        val paused = engine.pauseCurrent(started, 4_000)
        val skipped = engine.skipCurrent(paused, 5_000)
        val revisited = engine.back(skipped, 6_000)
        val resumed = engine.resumeCurrent(revisited, 9_000)

        assertEquals(RunTaskStatus.SKIPPED, revisited.tasks.first().status)
        assertEquals(1_000L, revisited.tasks.first().startedAtEpochMillis)
        assertEquals(RunTaskStatus.PENDING, resumed.tasks.first().status)
        assertEquals(6_000L, resumed.tasks.first().startedAtEpochMillis)
        assertEquals(null, resumed.tasks.first().pausedAtEpochMillis)
        assertEquals(7_000L, engine.remainingMillis(resumed, 9_000))
    }

    /**
     * Use this function to verify right-swipe navigation selects only the next finished task.
     * Inputs: none; the test builds active runs with completed and skipped candidates after the current task.
     * Dependencies: `RoutineRunEngine`, `routine`, and JUnit assertions.
     */
    @Test
    fun advancesToNextCompletedOrSkippedTaskOnly() {
        val engine = RoutineRunEngine()
        listOf(RunTaskStatus.COMPLETED, RunTaskStatus.SKIPPED).forEach { finishedStatus ->
            val startedRun = engine.start(routine(), RoutineRunId("run-$finishedStatus"), 1_000)
            val started = startedRun.copy(
                tasks = startedRun.tasks.mapIndexed { index, task ->
                    if (index == 1) task.copy(status = finishedStatus) else task
                },
            )
            val advanced = engine.advanceToNextFinishedTask(started, 2_000)

            assertEquals(1, advanced.currentTaskIndex)
            assertEquals(finishedStatus, advanced.tasks[1].status)
            assertEquals(advanced, engine.advanceToNextFinishedTask(advanced, 3_000))
        }
    }

    /** Use this function to verify that a previous-task swipe revisits without changing status and never aborts. */
    @Test
    fun previousTaskRevisitsWithoutChangingStatusAndStaysOnFirstTask() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 10), RoutineRunId("run"), 1_000)
        val completed = engine.completeCurrent(started, 2_000)

        val revisited = engine.previousTask(completed, 3_000)

        assertEquals(0, revisited.currentTaskIndex)
        assertEquals(RunTaskStatus.COMPLETED, revisited.tasks[0].status)
        assertEquals(started, engine.previousTask(started, 2_000))
        assertEquals(false, engine.previousTask(started, 2_000).abortConfirmationRequested)
    }

    /**
     * Use this function to verify that a newly entered timer waits for the presentation delay without changing persisted completion time.
     * Inputs: none; the test builds a two-task timed run and completes the first task.
     * Dependencies: `RoutineRunEngine`, `RoutineTemplate`, and JUnit assertions.
     */
    @Test
    fun preparedNextTaskStartsAfterReadyTransition() {
        val engine = RoutineRunEngine()
        val baseRoutine = routine(timerSeconds = 10)
        val timedRoutine = baseRoutine.copy(tasks = baseRoutine.tasks.map { it.copy(timerSeconds = 10) })
        val started = engine.start(timedRoutine, RoutineRunId("run"), 1_000)
        val completed = engine.completeCurrent(started, 2_000)
        val prepared = engine.prepareNextTask(completed, 2_000, 5_350)

        assertEquals(2_000L, prepared.tasks.first().finishedAtEpochMillis)
        assertEquals(7_350L, prepared.tasks.last().startedAtEpochMillis)
        assertEquals(10_000L, engine.remainingMillis(prepared, 2_000))
        assertEquals(10_000L, engine.remainingMillis(prepared, 7_350))
        assertEquals(prepared, engine.prepareNextTask(prepared, 3_000, 5_350))
    }

    /** Use this function to verify that zero-time feedback is acknowledged idempotently. */
    @Test
    fun timerFeedbackIsAcknowledgedOnce() {
        val engine = RoutineRunEngine()
        val run = engine.start(routine(timerSeconds = 1), RoutineRunId("run"), 1_000)

        assertTrue(engine.needsTimerFeedback(run, 2_000))
        val acknowledged = engine.acknowledgeTimerFeedback(run, 2_000)
        val repeated = engine.acknowledgeTimerFeedback(acknowledged, 3_000)

        assertFalse(engine.needsTimerFeedback(repeated, 3_000))
        assertEquals(2_000L, repeated.tasks.first().timerFeedbackAtEpochMillis)
    }

    /** Use this function to verify active runs retain sound policy after the reusable routine is edited. */
    @Test
    fun snapshotsRoutineSoundPolicyAtStart() {
        val settings = SoundSettings(
            entries = mapOf(SoundToken.TaskRunning to SoundSetting(enabled = true, assetPath = "custom/running.ogg")),
        )
        val source = routine().copy(soundEnabled = false, vibrateEnabled = false, soundSettings = settings)
        val started = RoutineRunEngine().start(source, RoutineRunId("run"), 1_000)
        val edited = source.copy(soundEnabled = true, soundSettings = defaultSoundSettings())

        assertFalse(started.routineSoundEnabled)
        assertFalse(started.routineVibrateEnabled)
        assertEquals(settings, started.soundSettings)
        assertTrue(edited.soundEnabled)
        assertEquals(settings, started.soundSettings)
    }

    /** Use this function to verify that abort confirmation is explicit and mutually exclusive. */
    @Test
    fun abortConfirmationControlsTerminalTransition() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)

        val rejected = runCatching { engine.abort(started, 2_000) }
        assertTrue(rejected.isFailure)

        val requested = engine.back(started, 2_000)
        assertTrue(requested.abortConfirmationRequested)
        assertFalse(requested.finishConfirmationRequested)
        assertEquals(RunStatus.ABORTED, engine.abort(requested, 3_000).status)
    }

    /** Use this function to verify repeated actions preserve completed status and reopen final confirmation. */
    @Test
    fun cancelledFinalCompleteRestoresPendingBeforeSkip() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)
        val finalTask = engine.selectTask(started, 1, 2_000)
        val firstRequest = engine.completeCurrent(finalTask, 3_000)
        val continued = engine.continueRun(firstRequest, 3_500)
        val secondRequest = engine.skipCurrent(continued, 4_000)

        assertEquals(RunTaskStatus.SKIPPED, secondRequest.tasks.last().status)
        assertTrue(secondRequest.finishConfirmationRequested)
        assertEquals(4_000L, secondRequest.tasks.last().finishedAtEpochMillis)
    }

    /** Use this function to verify cancelling final Skip restores an untimed pending task without timing it. */
    @Test
    fun cancellingFinalUntimedSkipRestoresPendingWithoutStartingTiming() {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Morning",
            tasks = listOf(RoutineTask(RoutineTaskId("one"), "One")),
        )
        val started = engine.start(routine, RoutineRunId("run"), 1_000)

        val requested = engine.skipCurrent(started, 2_000)
        val continued = engine.continueRun(requested, 9_000)
        val task = continued.tasks.single()

        assertEquals(RunTaskStatus.PENDING, task.status)
        assertEquals(8_000L, task.startedAtEpochMillis)
        assertEquals(null, task.finishedAtEpochMillis)
        assertEquals(null, task.actualDurationMillis)
        assertEquals(null, engine.remainingMillis(continued, 9_000))
    }

    /** Use this function to verify completing a timed final task ends the run without confirmation. */
    @Test
    fun completingFinalTimedTaskEndsImmediately() {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Morning",
            tasks = listOf(RoutineTask(RoutineTaskId("one"), "One", timerSeconds = 10)),
        )
        val started = engine.start(routine, RoutineRunId("run"), 1_000)

        val completed = engine.completeCurrent(started, 5_000)
        val task = completed.tasks.single()

        assertEquals(RunStatus.COMPLETED, completed.status)
        assertFalse(completed.finishConfirmationRequested)
        assertEquals(RunTaskStatus.COMPLETED, task.status)
        assertEquals(5_000L, task.finishedAtEpochMillis)
        assertEquals(4_000L, task.actualDurationMillis)
    }

    /** Use this function to verify a pending timer freezes while an abort dialog is open. */
    @Test
    fun abortConfirmationPausesPendingTimer() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 10), RoutineRunId("run"), 1_000)

        val requested = engine.back(started, 5_000)

        assertEquals(6_000L, engine.remainingMillis(requested, 9_000))
        val continued = engine.continueRun(requested, 9_000)
        assertEquals(5_000L, continued.tasks.first().startedAtEpochMillis)
        assertEquals(6_000L, engine.remainingMillis(continued, 9_000))
    }

    /** Use this function to verify final Skip confirms and exposes pending and skipped tasks. */
    @Test
    fun skippingPendingFinalTaskRequestsConfirmationWithAllUnfinishedTasks() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)
        val finalTask = engine.selectTask(started, 1, 2_000)
        val requested = engine.skipCurrent(finalTask, 3_000)

        assertTrue(requested.finishConfirmationRequested)
        assertEquals(listOf(0, 1), engine.unfinishedTaskIndexes(requested))
        assertEquals(RunTaskStatus.PENDING, requested.tasks.first().status)
        assertEquals(RunTaskStatus.SKIPPED, requested.tasks.last().status)
    }

    /** Use this function to verify final completion keeps its timestamp without a confirmation or animation delay. */
    @Test
    fun completingFinalTaskWithoutUnfinishedTasksEndsImmediately() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)
        val next = engine.completeCurrent(started, 2_000)
        val completed = engine.completeCurrent(next, 3_000)

        assertEquals(RunStatus.COMPLETED, completed.status)
        assertFalse(completed.finishConfirmationRequested)
        assertEquals(3_000L, completed.endedAtEpochMillis)
        assertEquals(3_000L, engine.toCompletionEvent(completed).endedAtEpochMillis)
    }

    /** Use this function to verify confirmation flags and terminal event timestamps. */
    @Test
    fun terminalConfirmationProducesAccurateEvent() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)
        val finalTask = engine.selectTask(started, 1, 2_000)
        val requested = engine.completeCurrent(finalTask, 3_000)
        val completed = engine.confirmComplete(requested, 4_000)
        val event = engine.toCompletionEvent(completed)

        assertTrue(requested.finishConfirmationRequested)
        assertFalse(requested.abortConfirmationRequested)
        assertEquals(RunStatus.COMPLETED, event.status)
        assertEquals(1_000L, event.startedAtEpochMillis)
        assertEquals(4_000L, event.endedAtEpochMillis)
        assertEquals(4_000L, completed.endedAtEpochMillis)
    }

    /** Use this function to verify transition pause/resume behavior without counting time spent on other tasks. */
    @Test
    fun backPausesUnfinishedTaskAndRevisitingResumesRemainingTime() {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Test Routine",
            tasks = listOf(
                RoutineTask(RoutineTaskId("task1"), "Task 1", timerSeconds = 10),
                RoutineTask(RoutineTaskId("task2"), "Task 2", timerSeconds = 15),
            ),
        )

        // Start on task 0 at t = 1,000
        val started = engine.start(routine, RoutineRunId("run"), 1_000L)
        assertEquals(10_000L, engine.remainingMillis(started, 1_000L))

        // Navigate to task 1 at t = 3,000 (task 0 ran for 2s, 8s remaining)
        val onTask1 = engine.selectTask(started, 1, 3_000L)
        assertEquals(15_000L, engine.remainingMillis(onTask1, 3_000L))

        // Spend 4s on task 1 (from 3,000 to 7,000; remaining on task 1 is 11s)
        assertEquals(11_000L, engine.remainingMillis(onTask1, 7_000L))

        // Back to task 0 at t = 7,000. Task 1 is paused.
        val backToTask0 = engine.back(onTask1, 7_000L)
        assertEquals(0, backToTask0.currentTaskIndex)
        // Task 0 was paused at 3,000 with 2s elapsed. It resumes at 7,000 so remaining is still 8s.
        assertEquals(8_000L, engine.remainingMillis(backToTask0, 7_000L))

        // Stay on task 0 for 5s until t = 12,000. Remaining on task 0 becomes 3s.
        assertEquals(3_000L, engine.remainingMillis(backToTask0, 12_000L))

        // Return to task 1 at t = 12,000.
        val backToTask1 = engine.selectTask(backToTask0, 1, 12_000L)
        assertEquals(1, backToTask1.currentTaskIndex)
        // Task 1 had 4s elapsed before pause. Resuming at 12,000, remaining must still be 11s (15s - 4s).
        assertEquals(11_000L, engine.remainingMillis(backToTask1, 12_000L))

        // Complete task 1 at t = 15,000 (3s additional on task 1; total actual duration = 4s + 3s = 7s).
        val completedTask1 = engine.completeCurrent(backToTask1, 15_000L)
        val task1 = completedTask1.tasks[1]
        assertEquals(RunTaskStatus.COMPLETED, task1.status)
        assertEquals(7_000L, task1.actualDurationMillis)
        // Duration does not grow after completion
        assertEquals(8_000L, engine.remainingMillis(completedTask1.copy(currentTaskIndex = 1), 20_000L))
    }
}
