package com.taskchain.features.notifications.domain

import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineRunTask
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Guards that the background alarm uses the injected Running nudge prediction instead of its own bucket math. */
class RunAlertNudgeTimingTest {
    private val interval = 60_000L
    private val idleLimit = 15 * 60_000L

    /** Use this function to build a running one-task run started at zero. */
    private fun runningRun() = RoutineRun(
        id = RoutineRunId("run"),
        routineId = RoutineId("routine"),
        routineTitle = "Routine",
        tasks = listOf(RoutineRunTask(source = RoutineTask(RoutineTaskId("task"), "Task"), startedAtEpochMillis = 0)),
        currentTaskIndex = 0,
        startedAtEpochMillis = 0,
    )

    /** Use this function to build fake rules with the given nudge prediction and timer remaining. */
    private fun rules(
        nextNudgeAt: (RoutineRun, Long) -> Long?,
        remainingMillis: Long? = null,
        seenIntervals: MutableList<Long> = mutableListOf(),
    ) = NotificationRunRules(
        remainingMillis = { _, _ -> remainingMillis },
        subtaskRemainingMillis = { _, _ -> null },
        elapsedMillis = { _, _ -> 0L },
        plannedRunMillis = { 0L },
        skipCurrent = { run, _ -> run },
        completeCurrent = { run, _ -> run },
        needsTimerFeedback = { _, _ -> false },
        acknowledgeTimerFeedback = { run, _ -> run },
        evaluateNudge = { run, _, _, _ -> NotificationNudge(run, shouldFire = false) },
        nextNudgeAt = { run, intervalMillis ->
            seenIntervals += intervalMillis
            nextNudgeAt(run, intervalMillis)
        },
    )

    /** Use this function to verify the alarm time is the injected prediction, called with the alert cadence. */
    @Test
    fun alarmUsesInjectedPrediction() {
        val intervals = mutableListOf<Long>()
        val rules = rules(nextNudgeAt = { _, _ -> 42_000L }, seenIntervals = intervals)

        assertEquals(42_000L, RunAlertPolicy.nextAlertAt(runningRun(), rules, 1_000L, interval, idleLimit))
        assertEquals(listOf(interval), intervals)
    }

    /** Use this function to verify an earlier timer expiry still wins, and no prediction and no timer means no alarm. */
    @Test
    fun timerExpiryAndMissingPrediction() {
        val withTimer = rules(nextNudgeAt = { _, _ -> 42_000L }, remainingMillis = 9_000L)
        assertEquals(10_000L, RunAlertPolicy.nextAlertAt(runningRun(), withTimer, 1_000L, interval, idleLimit))

        val none = rules(nextNudgeAt = { _, _ -> null })
        assertNull(RunAlertPolicy.nextAlertAt(runningRun(), none, 1_000L, interval, idleLimit))
    }

    /** Use this function to verify exhausted reminders skip the prediction entirely. */
    @Test
    fun exhaustedRemindersIgnorePrediction() {
        val intervals = mutableListOf<Long>()
        val rules = rules(nextNudgeAt = { _, _ -> idleLimit + 1 }, seenIntervals = intervals)

        assertNull(RunAlertPolicy.nextAlertAt(runningRun(), rules, idleLimit, interval, idleLimit))
        assertEquals(emptyList<Long>(), intervals)
    }
}
