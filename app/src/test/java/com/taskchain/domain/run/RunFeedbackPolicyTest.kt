package com.taskchain.domain.run

import com.taskchain.domain.model.EntityMetadata
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises state-entry and persisted active-time feedback rules independently of Android adapters. */
class RunFeedbackPolicyTest {
    /** Use this function to build a valid run fixture with timed or untimed tasks. */
    private fun routine(timerSeconds: Long? = null): RoutineTemplate = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Routine",
        tasks = listOf(
            RoutineTask(RoutineTaskId("one"), "One", timerSeconds = timerSeconds),
            RoutineTask(RoutineTaskId("two"), "Two"),
        ),
    )

    /** Use this function to verify state-entry events cover start, pause, resume, and completion navigation. */
    @Test
    fun emitsOnlyStateEntryEvents() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        assertEquals(listOf(RunFeedbackEvent(SoundToken.TaskRunning, 0)), RunFeedbackPolicy.stateEntryEvents(null, started))

        val paused = engine.pauseCurrent(started, 10)
        assertTrue(RunFeedbackPolicy.stateEntryEvents(null, paused).isEmpty())
        assertTrue(RunFeedbackPolicy.stateEntryEvents(null, engine.back(started, 10)).isEmpty())
        assertEquals(listOf(RunFeedbackEvent(SoundToken.TaskPaused, 0)), RunFeedbackPolicy.stateEntryEvents(started, paused))
        val resumed = engine.resumeCurrent(paused, 20)
        assertEquals(listOf(RunFeedbackEvent(SoundToken.TaskRunning, 0)), RunFeedbackPolicy.stateEntryEvents(paused, resumed))

        val advanced = engine.completeCurrent(resumed, 30)
        assertEquals(
            listOf(
                RunFeedbackEvent(SoundToken.TaskCompleted, 0),
                RunFeedbackEvent(SoundToken.TaskRunning, 1),
            ),
            RunFeedbackPolicy.stateEntryEvents(resumed, advanced),
        )
        assertTrue(RunFeedbackPolicy.stateEntryEvents(advanced, advanced).isEmpty())
        val confirmingWhilePaused = engine.back(paused, 30)
        assertTrue(RunFeedbackPolicy.stateEntryEvents(confirmingWhilePaused, engine.continueRun(confirmingWhilePaused, 40)).isEmpty())
    }

    /** Use this function to verify exact-minute cadence, persisted idempotence, and leap-clock no-burst behavior. */
    @Test
    fun acknowledgesOneNudgeBucketPerTick() {
        val started = RoutineRunEngine().start(routine(), RoutineRunId("run"), 0)
        val beforeDue = RunFeedbackPolicy.evaluateNudge(started, 59_999, 60_000, emit = true)
        assertFalse(beforeDue.shouldFire)
        assertSame(started, beforeDue.run)

        val due = RunFeedbackPolicy.evaluateNudge(started, 60_000, 60_000, emit = true)
        assertTrue(due.shouldFire)
        assertEquals(1L, due.run.tasks.first().taskNudgeCount)
        val recreated = due.run.copy()
        val repeated = RunFeedbackPolicy.evaluateNudge(recreated, 60_000, 60_000, emit = true)
        assertFalse(repeated.shouldFire)
        assertSame(recreated, repeated.run)

        val leap = RunFeedbackPolicy.evaluateNudge(started, 5 * 60_000L, 60_000, emit = true)
        assertTrue(leap.shouldFire)
        assertEquals(5L, leap.run.tasks.first().taskNudgeCount)
        assertFalse(RunFeedbackPolicy.evaluateNudge(leap.run, 5 * 60_000L, 60_000, emit = true).shouldFire)
    }

    /** Use this function to verify paused, confirmation, background, untimed, and overtime cadence boundaries. */
    @Test
    fun suppressesInactiveNudgesAndSkipsBackgroundBuckets() {
        val engine = RoutineRunEngine()
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val paused = engine.pauseCurrent(started, 30_000)
        assertFalse(RunFeedbackPolicy.evaluateNudge(paused, 120_000, 60_000, emit = true).shouldFire)
        val pausedWithDueBuckets = engine.pauseCurrent(started, 120_000)
        assertEquals(2L, RunFeedbackPolicy.skipMissedNudges(pausedWithDueBuckets, 300_000, 60_000).tasks.first().taskNudgeCount)

        val abortConfirmation = engine.back(started, 30_000)
        assertFalse(RunFeedbackPolicy.evaluateNudge(abortConfirmation, 120_000, 60_000, emit = true).shouldFire)
        assertEquals(2L, RunFeedbackPolicy.skipMissedNudges(engine.back(started, 120_000), 300_000, 60_000).tasks.first().taskNudgeCount)

        val skippedMissed = RunFeedbackPolicy.skipMissedNudges(started, 180_000, 60_000)
        assertEquals(3L, skippedMissed.tasks.first().taskNudgeCount)
        assertFalse(RunFeedbackPolicy.evaluateNudge(skippedMissed, 180_000, 60_000, emit = true).shouldFire)
        val nextActiveBucket = RunFeedbackPolicy.evaluateNudge(skippedMissed, 240_000, 60_000, emit = true)
        assertTrue(nextActiveBucket.shouldFire)

        val timed = engine.start(routine(timerSeconds = 1), RoutineRunId("timed"), 0)
        assertTrue(RunFeedbackPolicy.evaluateNudge(timed, 60_000, 60_000, emit = true).shouldFire)
        assertEquals(RunStatus.ACTIVE, timed.status)
        assertEquals(RunTaskStatus.PENDING, timed.tasks.first().status)
    }
}
