package com.taskchain

import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineSubtask
import com.taskchain.core.model.RoutineSubtaskId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.features.notifications.domain.RunAlertPolicy
import com.taskchain.features.notifications.domain.RunNotificationAction
import com.taskchain.features.notifications.domain.RunNotificationClock
import com.taskchain.features.notifications.domain.RunNotificationContent
import com.taskchain.features.notifications.domain.RunNotificationPolicy
import com.taskchain.features.running.RunEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * System test of the root binding [notificationRunRules]: Notifications background alerts and the ongoing
 * run notification driven by the real Running engine and nudge rules, independent of Android.
 */
class NotificationIntegrationTest {
    private val engine = RunEngine()
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

    /** Use this function to build a routine with a subtask task, a timed task, and a final untimed task. */
    private fun subtaskRoutine(): RoutineTemplate = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Morning",
        tasks = listOf(
            RoutineTask(
                id = RoutineTaskId("main"),
                title = "Main",
                subtasks = listOf(
                    RoutineSubtask(RoutineSubtaskId("s1"), "First", 180),
                    RoutineSubtask(RoutineSubtaskId("s2"), "Second", 420),
                ),
            ),
            RoutineTask(RoutineTaskId("middle"), "Middle", timerSeconds = 300),
            RoutineTask(RoutineTaskId("last"), "Last"),
        ),
    )

    /** Use this function to start the subtask routine at time zero. */
    private fun started() = engine.start(subtaskRoutine(), RoutineRunId("run"), 0)

    /** Use this function to read notification content that the tests expect to exist. */
    private fun content(run: RoutineRun, now: Long): RunNotificationContent =
        requireNotNull(RunNotificationPolicy.content(run, rules, now))

    // Background alerts: reminder limit, alarm time and acknowledgement through the bound engine.

    /** Use this function to verify an untimed run stops reminding 15 minutes after the run starts. */
    @Test
    fun untimedRunStopsAfterIdleLimit() {
        val run = engine.start(routine(null, null), RoutineRunId("run"), 0)

        assertFalse(RunAlertPolicy.remindersExhausted(run, rules, idleLimit - 1, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(run, rules, idleLimit, idleLimit))
    }

    /** Use this function to verify engine pause, resume and navigation restart the idle limit. */
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

    // Ongoing run notification: content and actions through the bound engine.

    /** Use this function to verify the active subtask name and its remaining time replace the main task's. */
    @Test
    fun subtaskNameAndTimeReplaceTaskNameAndTime() {
        val content = content(started(), 60_000L)

        assertEquals("First", content.title)
        assertEquals("Main", content.taskTitle)
        assertEquals("Morning", content.routineTitle)
        assertEquals(RunNotificationClock.CountDown(180_000L), content.clock)
    }

    /** Use this function to verify advancing a subtask moves the countdown to the next subtask's allowance. */
    @Test
    fun advancedSubtaskCountsDownFromItsOwnAllowance() {
        val advanced = engine.completeCurrent(started(), 120_000L)

        assertEquals(RunNotificationClock.CountDown(540_000L), content(advanced, 120_000L).clock)
        assertEquals("Second", content(advanced, 200_000L).title)
        // The anchor stays equal while the step runs, so the adapter does not repost every tick.
        assertEquals(content(advanced, 120_000L), content(advanced, 200_000L))
    }

    /** Use this function to verify a task without subtasks shows its own name and timer. */
    @Test
    fun timedTaskWithoutSubtasksUsesTaskNameAndTimer() {
        val atMiddle = engine.completeCurrent(engine.completeCurrent(started(), 60_000L), 120_000L)
        val content = content(atMiddle, 150_000L)

        assertEquals("Middle", content.title)
        assertNull(content.taskTitle)
        assertEquals(RunNotificationClock.CountDown(420_000L), content.clock)
    }

    /** Use this function to verify an untimed task counts up from its start. */
    @Test
    fun untimedTaskCountsUp() {
        val atLast = engine.skipCurrent(engine.skipCurrent(started(), 60_000L), 90_000L)
        val content = content(atLast, 100_000L)

        assertEquals("Last", content.title)
        assertEquals(RunNotificationClock.CountUp(90_000L), content.clock)
    }

    /** Use this function to verify a paused task freezes its remaining time. */
    @Test
    fun pausedTaskShowsFrozenRemainingTime() {
        val paused = engine.pauseCurrent(started(), 60_000L)

        assertEquals(RunNotificationClock.Paused(120_000L), content(paused, 500_000L).clock)
    }

    /** Use this function to verify a task whose start is delayed keeps one stable end time during the delay. */
    @Test
    fun delayedStartKeepsStableEndTime() {
        val atMiddle = engine.completeCurrent(engine.completeCurrent(started(), 60_000L), 120_000L)
        val prepared = engine.prepareNextTask(atMiddle, 120_000L, delayMillis = 5_000L)

        assertEquals(RunNotificationClock.CountDown(425_000L), content(prepared, 121_000L).clock)
        assertEquals(content(prepared, 121_000L).clock, content(prepared, 124_000L).clock)
    }

    /** Use this function to verify a finished run has no notification. */
    @Test
    fun nonActiveRunHasNoContent() {
        assertNull(RunNotificationPolicy.content(started().copy(status = RunStatus.ABORTED), rules, 0L))
    }

    /** Use this function to verify Complete advances a subtask without opening the app. */
    @Test
    fun completeAdvancesSubtaskWithoutApp() {
        val run = started()
        val target = content(run, 60_000L).target
        val updated = RunNotificationPolicy.applyAction(run, rules, RunNotificationAction.COMPLETE, target, 60_000L)

        assertNotNull(updated)
        assertEquals(RoutineSubtaskId("s2"), updated!!.tasks.first().activeSubtaskId)
        assertEquals(0, updated.currentTaskIndex)
    }

    /** Use this function to verify Skip moves to the next main task and keeps the run active. */
    @Test
    fun skipMovesToNextTask() {
        val run = started()
        val target = content(run, 60_000L).target
        val updated = RunNotificationPolicy.applyAction(run, rules, RunNotificationAction.SKIP, target, 60_000L)

        assertEquals(1, updated!!.currentTaskIndex)
        assertEquals(RunTaskStatus.SKIPPED, updated.tasks.first().status)
    }

    /** Use this function to verify an action built for an earlier step is ignored. */
    @Test
    fun staleActionIsIgnored() {
        val run = started()
        val staleTarget = content(run, 0L).target
        val advanced = engine.completeCurrent(run, 60_000L)

        assertNull(RunNotificationPolicy.applyAction(advanced, rules, RunNotificationAction.COMPLETE, staleTarget, 61_000L))
        assertNull(
            RunNotificationPolicy.applyAction(
                advanced,
                rules,
                RunNotificationAction.SKIP,
                staleTarget.copy(runId = RoutineRunId("other")),
                61_000L,
            ),
        )
    }

    /** Use this function to verify the last task's buttons open the app because ending it needs the finish dialog. */
    @Test
    fun lastTaskActionsNeedTheApp() {
        val atLast = engine.skipCurrent(engine.skipCurrent(started(), 60_000L), 90_000L)
        val content = content(atLast, 100_000L)

        assertTrue(content.skipNeedsApp)
        assertTrue(content.completeNeedsApp)
        assertNull(RunNotificationPolicy.applyAction(atLast, rules, RunNotificationAction.COMPLETE, content.target, 100_000L))
        assertNull(RunNotificationPolicy.applyAction(atLast, rules, RunNotificationAction.SKIP, content.target, 100_000L))
    }

    /** Use this function to verify an open confirmation dialog sends both buttons to the app. */
    @Test
    fun confirmationNeedsTheApp() {
        val confirming = started().copy(finishConfirmationRequested = true, confirmationStartedAtEpochMillis = 10_000L)
        val content = content(confirming, 20_000L)

        assertEquals(RunNotificationClock.Hidden, content.clock)
        assertTrue(content.skipNeedsApp)
        assertTrue(content.completeNeedsApp)
    }

    /** Use this function to verify a non-final task does not send its buttons to the app. */
    @Test
    fun firstTaskActionsStayInNotification() {
        val content = content(started(), 0L)

        assertFalse(content.skipNeedsApp)
        assertFalse(content.completeNeedsApp)
    }
}
