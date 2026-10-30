package com.taskchain.features.notifications.domain

import com.taskchain.notificationRunRules
import com.taskchain.features.execution.domain.RoutineRunEngine
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the content and actions of the ongoing run notification, independent of Android. */
class RunNotificationPolicyTest {
    private val engine = RoutineRunEngine()
    private val rules = notificationRunRules(engine)

    /** Use this function to build a routine with a subtask task, a timed task, and a final untimed task. */
    private fun routine(): RoutineTemplate = RoutineTemplate(
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

    /** Use this function to start the routine at time zero. */
    private fun started() = engine.start(routine(), RoutineRunId("run"), 0)

    /** Use this function to read content that the tests expect to exist. */
    private fun content(run: RoutineRun, now: Long): RunNotificationContent =
        requireNotNull(RunNotificationPolicy.content(run, rules, now))

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
