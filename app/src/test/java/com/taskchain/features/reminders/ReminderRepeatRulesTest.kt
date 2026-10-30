package com.taskchain.features.reminders

import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineRunTask
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.model.ScheduleRule
import com.taskchain.core.schedule.NextTriggerCalculator
import com.taskchain.core.schedule.ReminderRequest
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that repeated reminders rearm inside their window and stop only for the current scheduled cycle. */
class ReminderRepeatRulesTest {
    /** Verifies repeats stop after the repeat window and resume at the next regular occurrence. */
    @Test
    fun repeatsStopAfterWindowAndResumeNextDay() {
        val zone = java.util.TimeZone.getDefault()
        val rule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0)
        val cycleStart = NextTriggerCalculator.nextTriggerEpochMillis(rule, 0, zone)!!
        val request = ReminderRequest(
            requestCode = 1, routineId = "r", title = "t", triggerAtEpochMillis = cycleStart,
            schedule = rule, remindEveryMinutes = 30, cycleStartEpochMillis = cycleStart,
        )
        val inWindow = cycleStart + 60 * 60_000L
        val pastWindow = cycleStart + ReminderDelivery.REPEAT_WINDOW_MILLIS
        assertEquals(inWindow + 30 * 60_000L, ReminderDelivery.afterDelivery(request, inWindow)!!.triggerAtEpochMillis)
        assertEquals(
            NextTriggerCalculator.nextTriggerEpochMillis(rule, pastWindow, zone),
            ReminderDelivery.afterDelivery(request, pastWindow)!!.triggerAtEpochMillis,
        )
    }

    /** Use this function to verify repeated delivery retains a valid cycle start and resets on regular recurrence. */
    @Test
    fun deliveryRetainsRepeatedCycleAndResetsAtRegularTrigger() {
        val deliveredAt = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val request = ReminderRequest(
            requestCode = 7,
            routineId = "routine",
            title = "Morning",
            triggerAtEpochMillis = deliveredAt,
            schedule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0),
            remindEveryMinutes = 15,
            cycleStartEpochMillis = deliveredAt - 60_000L,
            soundEnabled = false,
            vibrateEnabled = true,
        )

        val repeated = ReminderDelivery.afterDelivery(request, deliveredAt)
        val regular = ReminderDelivery.afterDelivery(request, deliveredAt, repeatNow = false)

        assertNotNull(repeated)
        assertEquals(deliveredAt + 15 * 60_000L, repeated!!.triggerAtEpochMillis)
        assertEquals(deliveredAt - 60_000L, repeated.cycleStartEpochMillis)
        assertEquals(false, repeated.soundEnabled)
        assertEquals(regular!!.triggerAtEpochMillis, regular.cycleStartEpochMillis)
        assertTrue(regular.triggerAtEpochMillis > repeated.triggerAtEpochMillis)
        val nearNextOccurrence = regular.triggerAtEpochMillis - 5 * 60_000L
        val capped = ReminderDelivery.afterDelivery(request, nearNextOccurrence)!!
        assertEquals(regular.triggerAtEpochMillis, capped.triggerAtEpochMillis)
        assertEquals(capped.triggerAtEpochMillis, capped.cycleStartEpochMillis)
    }

    /** Use this function to verify one-time reminders repeat only when explicitly configured. */
    @Test
    fun deliveryOnceRepeatsOnlyWithPositiveInterval() {
        val deliveredAt = 1_000L
        val once = ScheduleRule(ScheduleFrequency.ONCE, 9, 0, oneTimeEpochMillis = deliveredAt)
        val request = ReminderRequest(
            requestCode = 7,
            routineId = "routine",
            title = "Once",
            triggerAtEpochMillis = deliveredAt,
            schedule = once,
            cycleStartEpochMillis = 0,
        )

        assertNull(ReminderDelivery.afterDelivery(request, deliveredAt))
        val repeated = ReminderDelivery.afterDelivery(request.copy(remindEveryMinutes = 15), deliveredAt)
        assertNotNull(repeated)
        assertEquals(deliveredAt + 15 * 60_000L, repeated!!.triggerAtEpochMillis)
        assertEquals(deliveredAt, repeated.cycleStartEpochMillis)
    }

    /** Use this function to verify active and historical completions stop repeats without hiding a new occurrence. */
    @Test
    fun completedTaskStopsOnlyItsCurrentCycle() {
        val task = RoutineRunTask(
            source = RoutineTask(RoutineTaskId("task"), "Work"),
            status = RunTaskStatus.COMPLETED,
            finishedAtEpochMillis = 20,
            completedAtEpochMillis = 30,
        )
        val active = RoutineRun(RoutineRunId("run"), RoutineId("routine"), "Morning", listOf(task), 0, 10)
        val event = CompletionEvent(active.id, active.routineId, active.routineTitle, 10, 40, RunStatus.COMPLETED, listOf(task))

        assertTrue(completedSinceCycle("routine", "task", 15, active, emptyList()))
        assertTrue(completedSinceCycle("routine", "task", 15, null, listOf(event)))
        assertTrue(completedSinceCycle("routine", "task", 25, active, listOf(event)))
        assertFalse(completedSinceCycle("routine", "task", 35, active, listOf(event)))
        assertFalse(completedSinceCycle("another", "task", 15, active, listOf(event)))
    }

    /** Use this function to verify a skipped or pending task never counts as completed for the cycle. */
    @Test
    fun unfinishedTaskDoesNotStopRepeats() {
        val skipped = RoutineRunTask(
            source = RoutineTask(RoutineTaskId("task"), "Work"),
            status = RunTaskStatus.SKIPPED,
            finishedAtEpochMillis = 30,
        )
        val active = RoutineRun(RoutineRunId("run"), RoutineId("routine"), "Morning", listOf(skipped), 0, 10)

        assertFalse(completedSinceCycle("routine", "task", 15, active, emptyList()))
        assertFalse(completedSinceCycle("routine", "task", 0, active, emptyList()))
    }

    /** Use this function to verify completed routine cycles ignore stale, unrelated, and aborted runs. */
    @Test
    fun completedRoutineSinceCycleMatchesOnlyCurrentCompletedRoutine() {
        val completed = RoutineRun(
            id = RoutineRunId("run"),
            routineId = RoutineId("routine"),
            routineTitle = "Morning",
            tasks = emptyList(),
            currentTaskIndex = 0,
            startedAtEpochMillis = 10,
            endedAtEpochMillis = 30,
            status = RunStatus.COMPLETED,
        )
        val history = CompletionEvent(
            runId = completed.id,
            routineId = completed.routineId,
            routineTitle = completed.routineTitle,
            startedAtEpochMillis = 10,
            endedAtEpochMillis = 30,
            status = RunStatus.COMPLETED,
            tasks = emptyList(),
        )
        val aborted = history.copy(status = RunStatus.ABORTED, endedAtEpochMillis = 100)

        assertTrue(completedRoutineSinceCycle("routine", 20, completed, emptyList()))
        assertTrue(completedRoutineSinceCycle("routine", 20, null, listOf(history)))
        assertFalse(completedRoutineSinceCycle("routine", 35, completed, listOf(history)))
        assertFalse(completedRoutineSinceCycle("other", 20, completed, listOf(history)))
        assertFalse(completedRoutineSinceCycle("routine", 20, null, listOf(aborted)))
        assertFalse(completedRoutineSinceCycle("routine", 20, completed.copy(status = RunStatus.ACTIVE), emptyList()))
    }

    /** Use this function to verify a completed run without an end time uses its latest completed task. */
    @Test
    fun completedRoutineWithoutEndTimeUsesLatestCompletedTask() {
        val task = RoutineRunTask(
            source = RoutineTask(RoutineTaskId("task"), "Work"),
            status = RunTaskStatus.COMPLETED,
            completedAtEpochMillis = 30,
        )
        val completed = RoutineRun(
            RoutineRunId("run"), RoutineId("routine"), "Morning", listOf(task), 0, 10,
            endedAtEpochMillis = null, status = RunStatus.COMPLETED,
        )

        assertTrue(completedRoutineSinceCycle("routine", 20, completed, emptyList()))
        assertFalse(completedRoutineSinceCycle("routine", 35, completed, emptyList()))
    }
}
