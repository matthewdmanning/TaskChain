package com.taskchain.features.notifications.domain

import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RunStatus
import com.taskchain.features.notifications.NotificationNudge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards when background alerts are due and when periodic reminders stop, against a fake Running contract.
 * The same assertions against the real engine binding live in the root `NotificationIntegrationTest`.
 */
class RunAlertPolicyTest {
    private val interval = 60_000L
    private val idleLimit = 15 * 60_000L

    /** Use this function to verify a run without planned time stops reminding the idle limit after it starts. */
    @Test
    fun untimedRunStopsAfterIdleLimit() {
        val rules = fakeRunRules(plannedRunMillis = { 0L })

        assertFalse(RunAlertPolicy.remindersExhausted(fakeRun(), rules, idleLimit - 1, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(fakeRun(), rules, idleLimit, idleLimit))
    }

    /** Use this function to verify the limit counts from the latest persisted interaction instead of the start. */
    @Test
    fun limitCountsFromLatestInteraction() {
        val interacted = fakeRun().copy(lastInteractionAtEpochMillis = 660_000L)

        assertFalse(RunAlertPolicy.remindersExhausted(interacted, fakeRunRules(), 660_000L + idleLimit - 1, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(interacted, fakeRunRules(), 660_000L + idleLimit, idleLimit))
    }

    /** Use this function to verify a positive planned run time replaces the idle limit. */
    @Test
    fun plannedRunTimeIsTheLimit() {
        val rules = fakeRunRules(plannedRunMillis = { 180_000L })

        assertFalse(RunAlertPolicy.remindersExhausted(fakeRun(), rules, 179_999L, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(fakeRun(), rules, 180_000L, idleLimit))
        val interacted = fakeRun().copy(lastInteractionAtEpochMillis = 10_000L)
        assertFalse(RunAlertPolicy.remindersExhausted(interacted, rules, 189_999L, idleLimit))
        assertTrue(RunAlertPolicy.remindersExhausted(interacted, rules, 190_000L, idleLimit))
    }

    /** Use this function to verify the next alarm is the earlier of timer expiry and the predicted nudge. */
    @Test
    fun nextAlertIsEarlierOfExpiryAndReminder() {
        fun rules(remaining: Long?) = fakeRunRules(remainingMillis = { _, _ -> remaining }, nextNudgeAt = { _, _ -> 60_000L })

        assertEquals(30_000L, RunAlertPolicy.nextAlertAt(fakeRun(), rules(30_000L), 0, interval, idleLimit))
        assertEquals(60_000L, RunAlertPolicy.nextAlertAt(fakeRun(), rules(120_000L), 0, interval, idleLimit))
        assertEquals(60_000L, RunAlertPolicy.nextAlertAt(fakeRun(), rules(null), 0, interval, idleLimit))
    }

    /** Use this function to verify an acknowledged timer expiry no longer schedules an alarm. */
    @Test
    fun acknowledgedTimerExpiryIsNotRescheduled() {
        val run = fakeRun().let { it.copy(tasks = listOf(it.tasks.single().copy(timerFeedbackAtEpochMillis = 30_000L))) }
        val rules = fakeRunRules(remainingMillis = { _, _ -> 0L })

        assertNull(RunAlertPolicy.nextAlertAt(run, rules, 40_000L, interval, idleLimit))
    }

    /** Use this function to verify no alarm is wanted while paused, confirming, not active, or after reminders end. */
    @Test
    fun noAlertWhilePausedConfirmingInactiveOrExhausted() {
        val rules = fakeRunRules(nextNudgeAt = { _, _ -> 60_000L })
        val paused = fakeRun().let { it.copy(tasks = listOf(it.tasks.single().copy(pausedAtEpochMillis = 1_000L))) }

        assertNull(RunAlertPolicy.nextAlertAt(paused, rules, 2_000L, interval, idleLimit))
        assertNull(RunAlertPolicy.nextAlertAt(fakeRun().copy(finishConfirmationRequested = true), rules, 0, interval, idleLimit))
        assertNull(RunAlertPolicy.nextAlertAt(fakeRun().copy(abortConfirmationRequested = true), rules, 0, interval, idleLimit))
        assertNull(RunAlertPolicy.nextAlertAt(fakeRun().copy(status = RunStatus.COMPLETED), rules, 0, interval, idleLimit))
        assertNull(RunAlertPolicy.nextAlertAt(fakeRun(), rules, idleLimit, interval, idleLimit))
    }

    /** Use this function to verify a due nudge is emitted only while reminders last, and its acknowledgement is kept. */
    @Test
    fun dueNudgeFollowsInjectedDecision() {
        val acknowledged = fakeRun().copy(lastInteractionAtEpochMillis = 1L)
        val emits = mutableListOf<Boolean>()
        val rules = fakeRunRules(evaluateNudge = { _, _, _, emit ->
            emits += emit
            NotificationNudge(acknowledged, shouldFire = emit)
        })

        val due = RunAlertPolicy.takeDue(fakeRun(), rules, 60_000L, interval, idleLimit)
        assertTrue(due.nudge)
        assertFalse(due.timerExpired)
        assertSame(acknowledged, due.run)

        // An exhausted run still acknowledges the bucket but stays silent.
        val silent = RunAlertPolicy.takeDue(fakeRun(), rules, idleLimit + interval, interval, idleLimit)
        assertFalse(silent.nudge)
        assertSame(acknowledged, silent.run)
        assertEquals(listOf(true, false), emits)
    }

    /** Use this function to verify a due timer expiry is reported and acknowledged through the injected rules. */
    @Test
    fun timerExpiryIsAcknowledged() {
        val acknowledged = fakeRun().copy(lastInteractionAtEpochMillis = 2L)
        val rules = fakeRunRules(
            needsTimerFeedback = { run, _ -> run != acknowledged },
            acknowledgeTimerFeedback = { _, _ -> acknowledged },
        )

        val first = RunAlertPolicy.takeDue(fakeRun(), rules, 30_000L, interval, idleLimit)
        assertTrue(first.timerExpired)
        assertSame(acknowledged, first.run)
        assertFalse(RunAlertPolicy.takeDue(first.run, rules, 30_000L, interval, idleLimit).timerExpired)
    }

    /** Use this function to verify nothing is due, and the run is untouched, while paused or confirming. */
    @Test
    fun nothingDueWhilePausedOrConfirming() {
        val rules = fakeRunRules(
            needsTimerFeedback = { _, _ -> true },
            evaluateNudge = { _, _, _, _ -> error("Not evaluated while paused or confirming") },
        )
        val paused = fakeRun().let { it.copy(tasks = listOf(it.tasks.single().copy(pausedAtEpochMillis = 1_000L))) }
        val confirming: RoutineRun = fakeRun().copy(finishConfirmationRequested = true)

        listOf(paused, confirming).forEach { run ->
            val due = RunAlertPolicy.takeDue(run, rules, 60_000L, interval, idleLimit)
            assertFalse(due.nudge)
            assertFalse(due.timerExpired)
            assertSame(run, due.run)
        }
    }
}
