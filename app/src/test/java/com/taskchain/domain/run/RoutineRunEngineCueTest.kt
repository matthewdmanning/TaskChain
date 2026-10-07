package com.taskchain.domain.run

import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineCue
import com.taskchain.domain.model.RoutineCueId
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
import com.taskchain.domain.model.SoundToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards nested cue timing, manual advancement, and immutable run snapshots. */
class RoutineRunEngineCueTest {
    /** Use this function to build the ten-minute main task used by cue timing checks. */
    private fun routine(): RoutineTemplate = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Routine",
        steps = listOf(
            RoutineStep(
                id = RoutineStepId("main"),
                title = "Main",
                cues = listOf(
                    RoutineCue(RoutineCueId("cue-1"), "First", 180),
                    RoutineCue(RoutineCueId("cue-2"), "Second", 420),
                ),
            ),
            RoutineStep(RoutineStepId("next"), "Next"),
        ),
    )

    /** Use this function to verify manual cue advancement leaves the main task active and preserves its clock. */
    @Test
    fun advancingCueKeepsMainTaskActiveAndUsesMainElapsedTime() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val advanced = engine.completeCurrent(started, 2 * 60_000L)

        assertEquals(RunStatus.ACTIVE, advanced.status)
        assertEquals(0, advanced.currentStepIndex)
        assertEquals(0L, advanced.steps.first().startedAtEpochMillis)
        assertEquals(RoutineCueId("cue-2"), advanced.steps.first().activeCueId)
        assertEquals(2 * 60_000L, advanced.steps.first().cueAdvancements.single().elapsedMillis)
        assertEquals(2 * 60_000L, engine.elapsedMillis(advanced, 2 * 60_000L))
        assertEquals(420_000L, engine.cueRemainingMillis(advanced, 2 * 60_000L))
        assertFalse(engine.isBehindCueSchedule(advanced, 10 * 60_000L))
        assertTrue(engine.isBehindCueSchedule(advanced, 10 * 60_000L + 1L))
    }

    /** Use this function to verify cue and main clocks freeze through skip, navigation, and explicit resume. */
    @Test
    fun skippedCueTaskRestoresFrozenMainAndCueState() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val advanced = engine.completeCurrent(started, 2 * 60_000L)
        val paused = engine.pauseCurrent(advanced, 3 * 60_000L)
        val skipped = engine.skipCurrent(paused, 4 * 60_000L)
        val revisited = engine.back(skipped, 10 * 60_000L)
        val resumed = engine.resumeCurrent(revisited, 20 * 60_000L)

        assertEquals(RunStepStatus.SKIPPED, revisited.steps.first().status)
        assertEquals(RoutineCueId("cue-2"), revisited.steps.first().activeCueId)
        assertEquals(3 * 60_000L, revisited.steps.first().actualDurationMillis)
        assertEquals(RunStepStatus.PENDING, resumed.steps.first().status)
        assertEquals(RoutineCueId("cue-2"), resumed.steps.first().activeCueId)
        assertEquals(3 * 60_000L, engine.elapsedMillis(resumed, 20 * 60_000L))
        assertEquals(2 * 60_000L, engine.cueElapsedMillis(resumed, RoutineCueId("cue-1"), 20 * 60_000L))
        assertEquals(60_000L, engine.cueElapsedMillis(resumed, RoutineCueId("cue-2"), 20 * 60_000L))
    }

    /** Use this function to verify the last cue completes its main task and requests normal final confirmation. */
    @Test
    fun completingLastCueCompletesMainTask() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val first = engine.completeCurrent(started, 60_000L)
        val second = engine.completeCurrent(first, 120_000L)

        assertEquals(RunStepStatus.COMPLETED, second.steps.first().status)
        assertEquals(2, second.steps.first().cueAdvancements.size)
        assertEquals(RoutineCueId("cue-2"), second.steps.first().cueAdvancements.last().cueId)
        assertTrue(second.steps.first().activeCueId == null)
        assertEquals(1, second.currentStepIndex)
    }

    /** Use this function to verify a completed cue task still requests confirmation when an earlier task is unfinished. */
    @Test
    fun completingLastCueUsesMainFinalConfirmation() {
        val engine = RoutineRunEngine()
        val cueStep = routine().steps.first().copy(id = RoutineStepId("cue-main"))
        val started = engine.start(
            routine().copy(steps = listOf(RoutineStep(RoutineStepId("pending"), "Pending"), cueStep)),
            RoutineRunId("run"),
            0,
        )
        val onCueStep = engine.selectStep(started, 1, 1_000L)
        val first = engine.completeCurrent(onCueStep, 2_000L)
        val requested = engine.completeCurrent(first, 3_000L)

        assertEquals(RunStatus.ACTIVE, requested.status)
        assertTrue(requested.finishConfirmationRequested)
        assertEquals(listOf(0), engine.unfinishedStepIndexes(requested))
        assertEquals(RunStatus.COMPLETED, engine.confirmComplete(requested, 4_000L).status)
    }

    /** Use this function to verify Complete advances a skipped cue while preserving its frozen main state until Resume. */
    @Test
    fun completingSkippedCuePreservesFrozenStateUntilResume() {
        val engine = RoutineRunEngine()
        val threeCueStep = routine().steps.first().copy(
            cues = routine().steps.first().cues + RoutineCue(RoutineCueId("cue-3"), "Third", 60),
        )
        val started = engine.start(
            routine().copy(steps = listOf(threeCueStep, routine().steps[1])),
            RoutineRunId("run"),
            0,
        )
        val firstCue = engine.completeCurrent(started, 60_000L)
        assertEquals(0L, engine.cueElapsedMillis(firstCue, RoutineCueId("cue-3"), 60_000L))
        val skipped = engine.skipCurrent(firstCue, 120_000L)
        val revisited = engine.back(skipped, 180_000L)
        val secondCue = engine.completeCurrent(revisited, 180_000L)
        val resumed = engine.resumeCurrent(secondCue, 600_000L)

        assertEquals(RunStepStatus.SKIPPED, secondCue.steps.first().status)
        assertEquals(RoutineCueId("cue-3"), secondCue.steps.first().activeCueId)
        assertEquals(2, secondCue.steps.first().cueAdvancements.size)
        assertEquals(120_000L, secondCue.steps.first().actualDurationMillis)
        assertEquals(RunStepStatus.PENDING, resumed.steps.first().status)
        assertEquals(RoutineCueId("cue-3"), resumed.steps.first().activeCueId)
    }

    /** Use this function to verify cancelling final-cue confirmation restores the last cue for a later Complete. */
    @Test
    fun continuingFinalCueConfirmationRestoresActiveCueSnapshot() {
        val engine = RoutineRunEngine()
        val cueStep = routine().steps.first().copy(id = RoutineStepId("cue-main"))
        val started = engine.start(
            routine().copy(steps = listOf(RoutineStep(RoutineStepId("pending"), "Pending"), cueStep)),
            RoutineRunId("run"),
            0,
        )
        val onCueStep = engine.selectStep(started, 1, 1_000L)
        val first = engine.completeCurrent(onCueStep, 2_000L)
        val requested = engine.completeCurrent(first, 3_000L)
        val continued = engine.continueRun(requested, 10_000L)
        val completedCue = engine.completeCurrent(continued, 11_000L)

        assertEquals(RoutineCueId("cue-2"), continued.steps.last().activeCueId)
        assertEquals(1, continued.steps.last().cueAdvancements.size)
        assertEquals(2, completedCue.steps.last().cueAdvancements.size)
        assertTrue(completedCue.finishConfirmationRequested)
    }

    /** Use this function to verify cue feedback is emitted once without pretending a nonfinal cue completed the task. */
    @Test
    fun cueAdvancementFeedbackIsOnceOnlyAndFinalCueAlsoAdvances() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val advanced = engine.completeCurrent(started, 60_000L)
        val feedback = RunFeedbackPolicy.stateEntryEvents(started, advanced)

        assertEquals(listOf(RunFeedbackEvent(SoundToken.CueAdvanced, 0)), feedback)
        assertTrue(RunFeedbackPolicy.stateEntryEvents(advanced, advanced).isEmpty())

        val single = routine().copy(steps = listOf(routine().steps.first()))
        val singleStarted = engine.start(single, RoutineRunId("single"), 0)
        val singleAdvanced = engine.completeCurrent(singleStarted, 60_000L)
        val singleCompleted = engine.completeCurrent(singleAdvanced, 120_000L)

        assertEquals(
            listOf(
                RunFeedbackEvent(SoundToken.CueAdvanced, 0),
                RunFeedbackEvent(SoundToken.TaskCompleted, 0),
            ),
            RunFeedbackPolicy.stateEntryEvents(singleAdvanced, singleCompleted),
        )
    }

    /** Use this function to verify cue definitions are copied into a run instead of sharing mutable caller state. */
    @Test
    fun startDeepCopiesCueDefinitions() {
        val cues = mutableListOf(RoutineCue(RoutineCueId("cue"), "Original", 60))
        val source = routine().copy(steps = listOf(routine().steps.first().copy(cues = cues)))
        val run = RoutineRunEngine().start(source, RoutineRunId("run"), 0)

        cues[0] = cues[0].copy(title = "Edited")

        assertNotSame(source.steps.first().cues, run.steps.first().source.cues)
        assertEquals("Original", run.steps.first().source.cues.single().title)
    }
}
