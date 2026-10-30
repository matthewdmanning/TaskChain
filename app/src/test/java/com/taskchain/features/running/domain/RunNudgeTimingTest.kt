package com.taskchain.features.running.domain

import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Guards that the predicted next nudge matches the bucket rule the runner and background alarm acknowledge. */
class RunNudgeTimingTest {
    private val engine = RoutineRunEngine()
    private val interval = 60_000L

    /** Use this function to build a two-task untimed routine. */
    private fun routine() = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Routine",
        tasks = listOf(
            RoutineTask(RoutineTaskId("one"), "One"),
            RoutineTask(RoutineTaskId("two"), "Two"),
        ),
    )

    /** Use this function to assert a bucket fires exactly from the predicted time onward, sampled around it. */
    private fun assertPredictionMatchesBucket(run: RoutineRun) {
        val predicted = RunFeedbackPolicy.nextNudgeAt(run, interval)
        val samples = if (predicted == null) listOf(0L, 59_999L, 60_000L, 600_000L) else listOf(predicted - 1, predicted, predicted + 1)
        samples.forEach { now ->
            val fires = RunFeedbackPolicy.evaluateNudge(run, now, interval, emit = true).shouldFire
            assertEquals("now=$now predicted=$predicted", predicted != null && now >= predicted, fires)
        }
    }

    /** Use this function to verify the first and following buckets of a running task. */
    @Test
    fun predictsEachBucketOfRunningTask() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        assertEquals(60_000L, RunFeedbackPolicy.nextNudgeAt(started, interval))
        assertPredictionMatchesBucket(started)

        val acknowledged = RunFeedbackPolicy.evaluateNudge(started, 60_000L, interval, emit = true).run
        assertEquals(120_000L, RunFeedbackPolicy.nextNudgeAt(acknowledged, interval))
        assertPredictionMatchesBucket(acknowledged)
    }

    /** Use this function to verify paused time and a readiness-delayed start shift the prediction like active time. */
    @Test
    fun followsActiveTimeAfterResumeAndDelayedStart() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val resumed = engine.resumeCurrent(engine.pauseCurrent(started, 10_000L), 40_000L)
        assertEquals(90_000L, RunFeedbackPolicy.nextNudgeAt(resumed, interval))
        assertPredictionMatchesBucket(resumed)

        val delayed = engine.completeCurrentWithReadiness(started, 5_000L, RunReadiness.taskStartDelayMillis)
        assertEquals(5_000L + RunReadiness.taskStartDelayMillis + interval, RunFeedbackPolicy.nextNudgeAt(delayed, interval))
        assertPredictionMatchesBucket(delayed)
    }

    /** Use this function to verify no nudge is predicted while paused, confirming, or on a revisited skipped task. */
    @Test
    fun predictsNothingWhenNudgesAreSuppressed() {
        val started = engine.start(routine(), RoutineRunId("run"), 0)
        val paused = engine.pauseCurrent(started, 10_000L)
        val confirming = engine.back(started, 10_000L)
        val revisitedSkipped = engine.previousTask(engine.skipCurrent(started, 10_000L), 20_000L)

        listOf(paused, confirming, revisitedSkipped).forEach { run ->
            assertNull(RunFeedbackPolicy.nextNudgeAt(run, interval))
            assertPredictionMatchesBucket(run)
        }
    }
}
