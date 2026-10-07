package com.taskchain.domain.run

import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
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
        steps = listOf(
            RoutineStep(RoutineStepId("one"), "One", timerSeconds = timerSeconds),
            RoutineStep(RoutineStepId("two"), "Two"),
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

        assertEquals(41_000L, resumed.steps.first().startedAtEpochMillis)
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
            steps = listOf(
                RoutineStep(RoutineStepId("one"), "One", timerSeconds = 10),
                RoutineStep(RoutineStepId("two"), "Two"),
            ),
        )

        val started = engine.start(routine, RoutineRunId("run"), nowEpochMillis = 1_000)
        val skipped = engine.skipCurrent(started, nowEpochMillis = 4_000)
        val back = engine.back(skipped, nowEpochMillis = 5_000)
        val revisited = engine.selectStep(back, index = 1, nowEpochMillis = 6_000)
        val requested = engine.completeCurrent(revisited, nowEpochMillis = 7_000)

        assertEquals(RunStepStatus.SKIPPED, back.steps.first().status)
        assertEquals(3_000L, back.steps.first().actualDurationMillis)
        assertFalse(back.steps.first().startedAtEpochMillis == 5_000L)
        assertTrue(requested.finishConfirmationRequested)
        assertEquals(listOf(0), engine.unfinishedStepIndexes(requested))
        assertEquals(RunStatus.COMPLETED, engine.confirmComplete(requested, 8_000).status)
    }

    /** Use this function to verify that terminal runs reject further transitions. */
    @Test(expected = IllegalArgumentException::class)
    fun transitionsRequireAnActiveRun() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)
        val requested = engine.completeCurrent(engine.selectStep(started, 1, 2_000), 2_000)
        val completed = engine.confirmComplete(requested, 3_000)

        engine.selectStep(completed, 0, 4_000)
    }

    /** Use this function to verify that selecting an out-of-range step fails before mutation. */
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidStepIndex() {
        RoutineRunEngine().selectStep(
            RoutineRunEngine().start(routine(), RoutineRunId("run"), 1_000),
            2,
            2_000,
        )
    }

    /** Use this function to verify that completing a skipped step preserves its recorded timing. */
    @Test
    fun completingSkippedStepChangesOnlyItsStatus() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 10), RoutineRunId("run"), 1_000)
        val skipped = engine.skipCurrent(started, 4_000)
        val revisited = engine.selectStep(skipped, 0, 9_000)
        val completed = engine.completeCurrent(revisited, 10_000)
        val step = completed.steps.first()

        assertEquals(RunStepStatus.COMPLETED, step.status)
        assertEquals(1_000L, step.startedAtEpochMillis)
        assertEquals(4_000L, step.finishedAtEpochMillis)
        assertEquals(10_000L, step.completedAtEpochMillis)
        assertEquals(3_000L, step.actualDurationMillis)
    }

    /** Use this function to verify explicit pause and resume preserve elapsed timer time. */
    @Test
    fun pauseAndResumeExcludePausedTime() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 10), RoutineRunId("run"), 1_000)

        val paused = engine.pauseCurrent(started, 4_000)
        assertEquals(4_000L, paused.steps.first().pausedAtEpochMillis)
        assertEquals(7_000L, engine.remainingMillis(paused, 9_000))

        val resumed = engine.resumeCurrent(paused, 9_000)
        assertEquals(null, resumed.steps.first().pausedAtEpochMillis)
        assertEquals(6_000L, resumed.steps.first().startedAtEpochMillis)
        assertEquals(6_000L, engine.remainingMillis(resumed, 10_000))
    }

    /**
     * Use this function to verify that explicitly resuming a skipped paused task preserves elapsed time.
     * Inputs: none; the test builds an active run with a paused then skipped current step.
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

        assertEquals(RunStepStatus.SKIPPED, revisited.steps.first().status)
        assertEquals(1_000L, revisited.steps.first().startedAtEpochMillis)
        assertEquals(RunStepStatus.PENDING, resumed.steps.first().status)
        assertEquals(6_000L, resumed.steps.first().startedAtEpochMillis)
        assertEquals(null, resumed.steps.first().pausedAtEpochMillis)
        assertEquals(7_000L, engine.remainingMillis(resumed, 9_000))
    }

    /**
     * Use this function to verify right-swipe navigation selects only the next finished task.
     * Inputs: none; the test builds active runs with completed and skipped candidates after the current step.
     * Dependencies: `RoutineRunEngine`, `routine`, and JUnit assertions.
     */
    @Test
    fun advancesToNextCompletedOrSkippedTaskOnly() {
        val engine = RoutineRunEngine()
        listOf(RunStepStatus.COMPLETED, RunStepStatus.SKIPPED).forEach { finishedStatus ->
            val startedRun = engine.start(routine(), RoutineRunId("run-$finishedStatus"), 1_000)
            val started = startedRun.copy(
                steps = startedRun.steps.mapIndexed { index, step ->
                    if (index == 1) step.copy(status = finishedStatus) else step
                },
            )
            val advanced = engine.advanceToNextFinishedStep(started, 2_000)

            assertEquals(1, advanced.currentStepIndex)
            assertEquals(finishedStatus, advanced.steps[1].status)
            assertEquals(advanced, engine.advanceToNextFinishedStep(advanced, 3_000))
        }
    }

    /**
     * Use this function to verify that a newly entered timer waits for the presentation delay without changing persisted completion time.
     * Inputs: none; the test builds a two-step timed run and completes the first step.
     * Dependencies: `RoutineRunEngine`, `RoutineTemplate`, and JUnit assertions.
     */
    @Test
    fun preparedNextStepStartsAfterReadyTransition() {
        val engine = RoutineRunEngine()
        val baseRoutine = routine(timerSeconds = 10)
        val timedRoutine = baseRoutine.copy(steps = baseRoutine.steps.map { it.copy(timerSeconds = 10) })
        val started = engine.start(timedRoutine, RoutineRunId("run"), 1_000)
        val completed = engine.completeCurrent(started, 2_000)
        val prepared = engine.prepareNextStep(completed, 2_000, 5_350)

        assertEquals(2_000L, prepared.steps.first().finishedAtEpochMillis)
        assertEquals(7_350L, prepared.steps.last().startedAtEpochMillis)
        assertEquals(10_000L, engine.remainingMillis(prepared, 2_000))
        assertEquals(10_000L, engine.remainingMillis(prepared, 7_350))
        assertEquals(prepared, engine.prepareNextStep(prepared, 3_000, 5_350))
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
        assertEquals(2_000L, repeated.steps.first().timerFeedbackAtEpochMillis)
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
        val finalStep = engine.selectStep(started, 1, 2_000)
        val firstRequest = engine.completeCurrent(finalStep, 3_000)
        val continued = engine.continueRun(firstRequest, 3_500)
        val secondRequest = engine.skipCurrent(continued, 4_000)

        assertEquals(RunStepStatus.SKIPPED, secondRequest.steps.last().status)
        assertTrue(secondRequest.finishConfirmationRequested)
        assertEquals(4_000L, secondRequest.steps.last().finishedAtEpochMillis)
    }

    /** Use this function to verify cancelling final Skip restores an untimed pending step without timing it. */
    @Test
    fun cancellingFinalUntimedSkipRestoresPendingWithoutStartingTiming() {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Morning",
            steps = listOf(RoutineStep(RoutineStepId("one"), "One")),
        )
        val started = engine.start(routine, RoutineRunId("run"), 1_000)

        val requested = engine.skipCurrent(started, 2_000)
        val continued = engine.continueRun(requested, 9_000)
        val step = continued.steps.single()

        assertEquals(RunStepStatus.PENDING, step.status)
        assertEquals(8_000L, step.startedAtEpochMillis)
        assertEquals(null, step.finishedAtEpochMillis)
        assertEquals(null, step.actualDurationMillis)
        assertEquals(null, engine.remainingMillis(continued, 9_000))
    }

    /** Use this function to verify completing a timed final step ends the run without confirmation. */
    @Test
    fun completingFinalTimedTaskEndsImmediately() {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Morning",
            steps = listOf(RoutineStep(RoutineStepId("one"), "One", timerSeconds = 10)),
        )
        val started = engine.start(routine, RoutineRunId("run"), 1_000)

        val completed = engine.completeCurrent(started, 5_000)
        val step = completed.steps.single()

        assertEquals(RunStatus.COMPLETED, completed.status)
        assertFalse(completed.finishConfirmationRequested)
        assertEquals(RunStepStatus.COMPLETED, step.status)
        assertEquals(5_000L, step.finishedAtEpochMillis)
        assertEquals(4_000L, step.actualDurationMillis)
    }

    /** Use this function to verify a pending timer freezes while an abort dialog is open. */
    @Test
    fun abortConfirmationPausesPendingTimer() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(timerSeconds = 10), RoutineRunId("run"), 1_000)

        val requested = engine.back(started, 5_000)

        assertEquals(6_000L, engine.remainingMillis(requested, 9_000))
        val continued = engine.continueRun(requested, 9_000)
        assertEquals(5_000L, continued.steps.first().startedAtEpochMillis)
        assertEquals(6_000L, engine.remainingMillis(continued, 9_000))
    }

    /** Use this function to verify final Skip confirms and exposes pending and skipped tasks. */
    @Test
    fun skippingPendingFinalStepRequestsConfirmationWithAllUnfinishedTasks() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)
        val finalStep = engine.selectStep(started, 1, 2_000)
        val requested = engine.skipCurrent(finalStep, 3_000)

        assertTrue(requested.finishConfirmationRequested)
        assertEquals(listOf(0, 1), engine.unfinishedStepIndexes(requested))
        assertEquals(RunStepStatus.PENDING, requested.steps.first().status)
        assertEquals(RunStepStatus.SKIPPED, requested.steps.last().status)
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
        val finalStep = engine.selectStep(started, 1, 2_000)
        val requested = engine.completeCurrent(finalStep, 3_000)
        val completed = engine.confirmComplete(requested, 4_000)
        val event = engine.toCompletionEvent(completed)

        assertTrue(requested.finishConfirmationRequested)
        assertFalse(requested.abortConfirmationRequested)
        assertEquals(RunStatus.COMPLETED, event.status)
        assertEquals(1_000L, event.startedAtEpochMillis)
        assertEquals(4_000L, event.endedAtEpochMillis)
        assertEquals(4_000L, completed.endedAtEpochMillis)
    }

    /** Use this function to verify transition pause/resume behavior without counting time spent on other steps. */
    @Test
    fun backPausesUnfinishedStepAndRevisitingResumesRemainingTime() {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Test Routine",
            steps = listOf(
                RoutineStep(RoutineStepId("step1"), "Step 1", timerSeconds = 10),
                RoutineStep(RoutineStepId("step2"), "Step 2", timerSeconds = 15),
            ),
        )

        // Start on step 0 at t = 1,000
        val started = engine.start(routine, RoutineRunId("run"), 1_000L)
        assertEquals(10_000L, engine.remainingMillis(started, 1_000L))

        // Navigate to step 1 at t = 3,000 (step 0 ran for 2s, 8s remaining)
        val onStep1 = engine.selectStep(started, 1, 3_000L)
        assertEquals(15_000L, engine.remainingMillis(onStep1, 3_000L))

        // Spend 4s on step 1 (from 3,000 to 7,000; remaining on step 1 is 11s)
        assertEquals(11_000L, engine.remainingMillis(onStep1, 7_000L))

        // Back to step 0 at t = 7,000. Step 1 is paused.
        val backToStep0 = engine.back(onStep1, 7_000L)
        assertEquals(0, backToStep0.currentStepIndex)
        // Step 0 was paused at 3,000 with 2s elapsed. It resumes at 7,000 so remaining is still 8s.
        assertEquals(8_000L, engine.remainingMillis(backToStep0, 7_000L))

        // Stay on step 0 for 5s until t = 12,000. Remaining on step 0 becomes 3s.
        assertEquals(3_000L, engine.remainingMillis(backToStep0, 12_000L))

        // Return to step 1 at t = 12,000.
        val backToStep1 = engine.selectStep(backToStep0, 1, 12_000L)
        assertEquals(1, backToStep1.currentStepIndex)
        // Step 1 had 4s elapsed before pause. Resuming at 12,000, remaining must still be 11s (15s - 4s).
        assertEquals(11_000L, engine.remainingMillis(backToStep1, 12_000L))

        // Complete step 1 at t = 15,000 (3s additional on step 1; total actual duration = 4s + 3s = 7s).
        val completedStep1 = engine.completeCurrent(backToStep1, 15_000L)
        val step1 = completedStep1.steps[1]
        assertEquals(RunStepStatus.COMPLETED, step1.status)
        assertEquals(7_000L, step1.actualDurationMillis)
        // Duration does not grow after completion
        assertEquals(8_000L, engine.remainingMillis(completedStep1.copy(currentStepIndex = 1), 20_000L))
    }
}
