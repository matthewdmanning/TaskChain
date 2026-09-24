package com.taskchain.domain.run

import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
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

    /** Use this function to verify cancelling final Complete restores a timed step before the dialog pause. */
    @Test
    fun cancellingFinalCompleteRestoresTimedStepBeforeDialogPause() {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Morning",
            steps = listOf(RoutineStep(RoutineStepId("one"), "One", timerSeconds = 10)),
        )
        val started = engine.start(routine, RoutineRunId("run"), 1_000)

        val requested = engine.completeCurrent(started, 5_000)
        val continued = engine.continueRun(requested, 9_000)
        val step = continued.steps.single()

        assertEquals(RunStepStatus.PENDING, step.status)
        assertEquals(5_000L, step.startedAtEpochMillis)
        assertEquals(null, step.finishedAtEpochMillis)
        assertEquals(null, step.actualDurationMillis)
        assertEquals(6_000L, engine.remainingMillis(continued, 9_000))
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
