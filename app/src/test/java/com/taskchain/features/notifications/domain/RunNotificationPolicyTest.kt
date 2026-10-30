package com.taskchain.features.notifications.domain

import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineRunTask
import com.taskchain.core.model.RoutineSubtask
import com.taskchain.core.model.RoutineSubtaskId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.features.notifications.NotificationRunRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the content and actions of the ongoing run notification against a fake Running contract.
 * The same scenarios against the real engine binding live in the root `NotificationIntegrationTest`.
 */
class RunNotificationPolicyTest {
    private val main = RoutineTask(
        id = RoutineTaskId("main"),
        title = "Main",
        subtasks = listOf(
            RoutineSubtask(RoutineSubtaskId("s1"), "First", 180),
            RoutineSubtask(RoutineSubtaskId("s2"), "Second", 420),
        ),
    )
    private val middle = RoutineTask(RoutineTaskId("middle"), "Middle", timerSeconds = 300)
    private val last = RoutineTask(RoutineTaskId("last"), "Last")

    /** Fake timing: 60 s elapsed, 120 s left in the subtask and 500 s left in the main task. */
    private val rules = fakeRunRules(
        remainingMillis = { _, _ -> 500_000L },
        subtaskRemainingMillis = { _, _ -> 120_000L },
        elapsedMillis = { _, _ -> 60_000L },
    )

    /** Use this function to hand-build the routine's run on [index], with the first subtask active on the main task. */
    private fun runAt(index: Int, activeSubtask: String? = "s1", startedAt: Long? = 100_000L): RoutineRun = fakeRun(
        tasks = listOf(
            RoutineRunTask(source = main, startedAtEpochMillis = 0, activeSubtaskId = activeSubtask?.let(::RoutineSubtaskId)),
            RoutineRunTask(source = middle, startedAtEpochMillis = startedAt.takeIf { index == 1 }),
            RoutineRunTask(source = last, startedAtEpochMillis = startedAt.takeIf { index == 2 }),
        ),
        currentTaskIndex = index,
    )

    /** Use this function to replace the current task of [run]. */
    private fun RoutineRun.withCurrent(transform: (RoutineRunTask) -> RoutineRunTask): RoutineRun =
        copy(tasks = tasks.mapIndexed { index, task -> if (index == currentTaskIndex) transform(task) else task })

    /** Use this function to read content that the tests expect to exist. */
    private fun content(run: RoutineRun, rules: NotificationRunRules = this.rules) =
        requireNotNull(RunNotificationPolicy.content(run, rules, 60_000L))

    /** Use this function to verify the active subtask name and the subtask allowance replace the main task's. */
    @Test
    fun subtaskNameAndTimeReplaceTaskNameAndTime() {
        val content = content(runAt(0))

        assertEquals("First", content.title)
        assertEquals("Main", content.taskTitle)
        assertEquals("Morning", content.routineTitle)
        assertEquals(RunNotificationTarget(RoutineRunId("run"), 0, RoutineSubtaskId("s1")), content.target)
        // Start 0 + elapsed 60 s + subtask remaining 120 s.
        assertEquals(RunNotificationClock.CountDown(180_000L), content.clock)
    }

    /** Use this function to verify a task without an active subtask shows its own name and remaining time. */
    @Test
    fun taskWithoutSubtasksUsesTaskNameAndTimer() {
        val content = content(runAt(1))

        assertEquals("Middle", content.title)
        assertNull(content.taskTitle)
        assertEquals(RunNotificationTarget(RoutineRunId("run"), 1, null), content.target)
        assertEquals(RunNotificationClock.CountDown(100_000L + 60_000L + 500_000L), content.clock)
    }

    /** Use this function to verify an untimed task counts up from its start. */
    @Test
    fun untimedTaskCountsUp() {
        val untimed = fakeRunRules()

        assertEquals(RunNotificationClock.CountUp(100_000L), content(runAt(2), untimed).clock)
    }

    /** Use this function to verify a paused task shows the frozen remaining time. */
    @Test
    fun pausedTaskShowsFrozenRemainingTime() {
        val paused = runAt(1).withCurrent { it.copy(pausedAtEpochMillis = 70_000L) }

        assertEquals(RunNotificationClock.Paused(500_000L), content(paused).clock)
    }

    /** Use this function to verify no clock shows in confirmation, on a finished task, or before the task starts. */
    @Test
    fun hiddenClockCases() {
        assertEquals(RunNotificationClock.Hidden, content(runAt(1).copy(abortConfirmationRequested = true)).clock)
        assertEquals(RunNotificationClock.Hidden, content(runAt(1).withCurrent { it.copy(status = RunTaskStatus.COMPLETED) }).clock)
        assertEquals(RunNotificationClock.Hidden, content(runAt(1, startedAt = null)).clock)
    }

    /** Use this function to verify a finished run has no notification. */
    @Test
    fun nonActiveRunHasNoContent() {
        assertNull(RunNotificationPolicy.content(runAt(0).copy(status = RunStatus.ABORTED), rules, 0L))
        assertNull(RunNotificationPolicy.content(runAt(0).copy(status = RunStatus.COMPLETED), rules, 0L))
    }

    /** Use this function to verify non-final tasks keep both buttons in the notification. */
    @Test
    fun nonFinalTaskActionsStayInNotification() {
        listOf(runAt(0), runAt(1)).forEach { run ->
            assertFalse(content(run).skipNeedsApp)
            assertFalse(content(run).completeNeedsApp)
        }
    }

    /** Use this function to verify the last task opens the app, except Complete on a subtask that is not the last one. */
    @Test
    fun lastTaskActionsNeedTheApp() {
        val content = content(runAt(2))
        assertTrue(content.skipNeedsApp)
        assertTrue(content.completeNeedsApp)

        val lastWithSubtasks = runAt(2).withCurrent { it.copy(source = main, activeSubtaskId = RoutineSubtaskId("s1")) }
        assertTrue(content(lastWithSubtasks).skipNeedsApp)
        assertFalse(content(lastWithSubtasks).completeNeedsApp)
        val finalSubtask = lastWithSubtasks.withCurrent { it.copy(activeSubtaskId = RoutineSubtaskId("s2")) }
        assertTrue(content(finalSubtask).completeNeedsApp)
    }

    /** Use this function to verify an open confirmation dialog sends both buttons to the app. */
    @Test
    fun confirmationNeedsTheApp() {
        val content = content(runAt(0).copy(finishConfirmationRequested = true))

        assertEquals(RunNotificationClock.Hidden, content.clock)
        assertTrue(content.skipNeedsApp)
        assertTrue(content.completeNeedsApp)
    }

    /** Use this function to verify Skip and Complete delegate to the injected transitions and return their result. */
    @Test
    fun actionsApplyInjectedTransitions() {
        val skipped = runAt(1)
        val completed = runAt(2)
        val rules = fakeRunRules(skipCurrent = { _, _ -> skipped }, completeCurrent = { _, _ -> completed })
        val run = runAt(0)
        val target = content(run, rules).target

        assertSame(skipped, RunNotificationPolicy.applyAction(run, rules, RunNotificationAction.SKIP, target, 60_000L))
        assertSame(completed, RunNotificationPolicy.applyAction(run, rules, RunNotificationAction.COMPLETE, target, 60_000L))
    }

    /** Use this function to verify a stale target, or an action that needs the app, applies no transition. */
    @Test
    fun staleOrAppOnlyActionIsIgnored() {
        val rules = fakeRunRules(
            skipCurrent = { _, _ -> error("Skip must not apply") },
            completeCurrent = { _, _ -> error("Complete must not apply") },
        )
        val run = runAt(0)
        val target = content(run, rules).target

        listOf(
            target.copy(runId = RoutineRunId("other")),
            target.copy(taskIndex = 1),
            target.copy(subtaskId = RoutineSubtaskId("s2")),
        ).forEach { stale ->
            assertNull(RunNotificationPolicy.applyAction(run, rules, RunNotificationAction.COMPLETE, stale, 60_000L))
            assertNull(RunNotificationPolicy.applyAction(run, rules, RunNotificationAction.SKIP, stale, 60_000L))
        }
        val atLast = runAt(2)
        val lastTarget = content(atLast, rules).target
        assertNull(RunNotificationPolicy.applyAction(atLast, rules, RunNotificationAction.COMPLETE, lastTarget, 60_000L))
        assertNull(RunNotificationPolicy.applyAction(atLast, rules, RunNotificationAction.SKIP, lastTarget, 60_000L))
    }
}
