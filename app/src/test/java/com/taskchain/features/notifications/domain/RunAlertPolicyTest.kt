package com.taskchain.features.notifications.domain

import com.taskchain.notificationRunRules
import com.taskchain.features.running.domain.RoutineRunEngine
import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards when background alerts are due and when periodic reminders stop, independent of Android. */
class RunAlertPolicyTest {
    private val engine = RoutineRunEngine()
    private val rules = notificationRunRules(engine)
    private val interval = 60_000L
    private val idleLimit = 15 * 60_000L

    /** Use this function to build a routine whose tasks have the given timers; null means no timer. */
    private fun routine(vararg timers: Long?) = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Morning",
        tasks = timers.mapIndexed { index, timer -> RoutineTask(RoutineTaskId("t$index"), "Task $index", timerSeconds = timer) },
    )

    /** Use this function to verify an untimed run stops reminding 15 minutes after the run starts. */
    @Test
    fun untimedRunStopsAfterIdleLimit() {
        val run = engine.start(routine(null, null), RoutineRunId("run"), 0)

        assertFalse(RunAlertPolicy.remindersExhausted(run, rules, idleLimit - 1, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(run, rules, idleLimit, idleLimit))
    }

    /** Use this function to verify pausing and navigating restart the idle limit. */
    @Test
    fun pauseAndNavigationRestartIdleLimit() {
        val started = engine.start(routine(null, null), RoutineRunId("run"), 0)
        val resumed = engine.resumeCurrent(engine.pauseCurrent(started, 600_000L), 660_000L)

        assertFalse(RunAlertPolicy.remindersExhausted(resumed, rules, 660_000L + idleLimit - 1, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(resumed, rules, 660_000L + idleLimit, idleLimit))

        val navigated = engine.skipCurrent(started, 840_000L)
        assertFalse(RunAlertPolicy.remindersExhausted(navigated, rules, 840_000L + idleLimit - 1, idleLimit))
    }

    /** Use this function to verify the limit is the configured sum, whatever the tasks' remaining or passed time. */
    @Test
    fun timedRunLimitIsConfiguredSumFromLatestInteraction() {
        val started = engine.start(routine(60L, 120L), RoutineRunId("run"), 0)

        assertFalse(RunAlertPolicy.remindersExhausted(started, rules, 179_999L, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(started, rules, 180_000L, idleLimit))

        // Completing the first task early does not shorten the limit; it restarts the same 180 s.
        val advanced = engine.completeCurrent(started, 10_000L)
        assertFalse(RunAlertPolicy.remindersExhausted(advanced, rules, 189_999L, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(advanced, rules, 190_000L, idleLimit))
    }

    /** Use this function to verify the next alarm is the earlier of timer expiry and the next reminder. */
    @Test
    fun nextAlertIsEarlierOfExpiryAndReminder() {
        val shortTimer = engine.start(routine(30L), RoutineRunId("run"), 0)
        val longTimer = engine.start(routine(120L), RoutineRunId("run"), 0)
        val untimed = engine.start(routine(null), RoutineRunId("run"), 0)

        assertEquals(30_000L, RunAlertPolicy.nextAlertAt(shortTimer, rules, 0, interval, idleLimit))
        assertEquals(60_000L, RunAlertPolicy.nextAlertAt(longTimer, rules, 0, interval, idleLimit))
        assertEquals(60_000L, RunAlertPolicy.nextAlertAt(untimed, rules, 0, interval, idleLimit))
    }

    /** Use this function to verify no alarm is wanted while paused or after reminders end. */
    @Test
    fun noAlertWhilePausedOrExhausted() {
        val started = engine.start(routine(null), RoutineRunId("run"), 0)

        assertNull(RunAlertPolicy.nextAlertAt(engine.pauseCurrent(started, 1_000L), rules, 2_000L, interval, idleLimit))
        assertNull(RunAlertPolicy.nextAlertAt(started, rules, idleLimit, interval, idleLimit))
    }

    /** Use this function to verify a due reminder is reported once. */
    @Test
    fun dueReminderIsReportedOnce() {
        val run = engine.start(routine(120L), RoutineRunId("run"), 0)

        val first = RunAlertPolicy.takeDue(run, rules, 60_000L, interval, idleLimit)
        assertTrue(first.nudge)
        assertFalse(first.timerExpired)

        assertFalse(RunAlertPolicy.takeDue(first.run, rules, 60_000L, interval, idleLimit).nudge)
    }

    /** Use this function to verify a timer expiry is reported once. */
    @Test
    fun timerExpiryIsReportedOnce() {
        val run = engine.start(routine(30L), RoutineRunId("run"), 0)

        val first = RunAlertPolicy.takeDue(run, rules, 30_000L, interval, idleLimit)
        assertTrue(first.timerExpired)

        assertFalse(RunAlertPolicy.takeDue(first.run, rules, 30_000L, interval, idleLimit).timerExpired)
    }

    /** Use this function to verify an exhausted run still acknowledges buckets but stays silent. */
    @Test
    fun exhaustedRunStaysSilent() {
        val run = engine.start(routine(null), RoutineRunId("run"), 0)

        val due = RunAlertPolicy.takeDue(run, rules, idleLimit + interval, interval, idleLimit)

        assertFalse(due.nudge)
        assertFalse(due.timerExpired)
    }
}
