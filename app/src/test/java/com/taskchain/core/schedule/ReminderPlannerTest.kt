package com.taskchain.core.schedule

import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.model.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Verifies reminder planning at save, restore, completion, and delivery boundaries. */
class ReminderPlannerTest {
    /** Use this function to verify save and restore map the same future routine reminder and feedback gates. */
    @Test
    fun saveAndRestoreShareRoutineMapping() {
        val scheduled = routine(schedule = ScheduleRule(ScheduleFrequency.ONCE, 9, 0, oneTimeEpochMillis = 10_000))
            .copy(remindEveryMinutes = 15, soundEnabled = false, vibrateEnabled = false)
        val direct = routine(reminderAtEpochMillis = 10_000)
        for (routine in listOf(scheduled, direct)) {
            val saved = ReminderPlanner.forSave(routine, routine, 1_000)
            assertEquals(ReminderPlanner.forRestore(routine, 1_000), saved)
            assertEquals(routine.toReminderRequest(10_000), saved.requests.single())
            assertEquals(listOf("step:task".hashCode()), saved.cancelRequestCodes)
        }
    }

    /** Use this function to verify invalid and expired schedules reject a save plan. */
    @Test
    fun saveRejectsInvalidOrExpiredSchedule() {
        val expired = routine(schedule = ScheduleRule(ScheduleFrequency.ONCE, 9, 0, oneTimeEpochMillis = 100))
        val invalid = routine(schedule = ScheduleRule(ScheduleFrequency.DAILY, 24, 0))

        assertIllegalArgument { ReminderPlanner.forSave(expired, null, 1_000) }
        assertIllegalArgument { ReminderPlanner.forSave(invalid, null, 1_000) }
    }

    /** Use this function to verify removed legacy task alarms are cancelled while a disabled routine alarm is cleared. */
    @Test
    fun saveCancelsRemovedTaskAndDisabledRoutine() {
        val previous = routine(tasks = listOf(task("removed")))
        val current = routine(tasks = listOf(task("kept")))

        val plan = ReminderPlanner.forSave(current, previous, 1_000)

        assertTrue(plan.cancelRequestCodes.contains("step:removed".hashCode()))
        assertTrue(plan.cancelRequestCodes.contains("routine".hashCode()))
        assertTrue(plan.requests.isEmpty())
    }

    /** Use this function to verify reboot restoration of routine settings suppresses every legacy task alarm. */
    @Test
    fun restoreRoutineSettingsSuppressesLegacyTasks() {
        val future = 10_000L
        val routine = routine(
            tasks = listOf(task("legacy", reminderAtEpochMillis = future)),
            schedule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0),
        )

        val plan = ReminderPlanner.forRestore(routine, 1_000)

        assertTrue(plan.cancelRequestCodes.contains("step:legacy".hashCode()))
        assertEquals(1, plan.requests.size)
        assertNull(plan.requests.single().taskId)
        assertEquals("routine", plan.requests.single().routineId)
    }

    /** Use this function to verify reboot restoration retains legacy task settings for manual routines. */
    @Test
    fun restoreWithoutRoutineSettingsRestoresLegacyTask() {
        val future = 10_000L
        val routine = routine(
            tasks = listOf(
                task("legacy", reminderAtEpochMillis = future, soundEnabled = false, vibrateEnabled = true),
                task("expired", reminderAtEpochMillis = 500),
            ),
        )

        val plan = ReminderPlanner.forRestore(routine, 1_000)

        assertTrue(plan.cancelRequestCodes.contains("routine".hashCode()))
        assertTrue(plan.cancelRequestCodes.contains("step:expired".hashCode()))
        assertEquals(
            ReminderRequest(
                requestCode = "step:legacy".hashCode(),
                routineId = "routine",
                title = "legacy",
                triggerAtEpochMillis = future,
                taskId = "legacy",
                soundEnabled = false,
                vibrateEnabled = true,
            ),
            plan.requests.single(),
        )
    }

    /** Use this function to verify a completed one-time routine cancels its nonrecurring alarm. */
    @Test
    fun completionOfOnceCancelsRoutineAlarm() {
        val completedAt = 1_000L
        val routine = routine(schedule = ScheduleRule(ScheduleFrequency.ONCE, 9, 0, oneTimeEpochMillis = 2_000))

        val plan = ReminderPlanner.afterCompletion(routine, RoutineId("routine"), completedAt)

        assertEquals(listOf("routine".hashCode()), plan.cancelRequestCodes)
        assertTrue(plan.requests.isEmpty())
        assertEquals(plan, ReminderPlanner.afterCompletion(null, RoutineId("routine"), completedAt))
        assertEquals(plan, ReminderPlanner.afterCompletion(routine.copy(schedule = null), RoutineId("routine"), completedAt))
    }

    /** Use this function to verify recurring completion schedules the next local calendar day. */
    @Test
    fun completionOfDailyRoutineSchedulesNextDay() {
        val completedAt = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val routine = routine(schedule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0))

        val request = ReminderPlanner.afterCompletion(routine, RoutineId("routine"), completedAt).requests.single()
        val trigger = Calendar.getInstance().apply { timeInMillis = request.triggerAtEpochMillis }

        assertEquals(Calendar.FEBRUARY, trigger.get(Calendar.MONTH))
        assertEquals(1, trigger.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, trigger.get(Calendar.HOUR_OF_DAY))
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

        val repeated = ReminderPlanner.afterDelivery(request, deliveredAt)
        val regular = ReminderPlanner.afterDelivery(request, deliveredAt, repeatNow = false)

        assertNotNull(repeated)
        assertEquals(deliveredAt + 15 * 60_000L, repeated!!.triggerAtEpochMillis)
        assertEquals(deliveredAt - 60_000L, repeated.cycleStartEpochMillis)
        assertEquals(false, repeated.soundEnabled)
        assertEquals(regular!!.triggerAtEpochMillis, regular.cycleStartEpochMillis)
        assertTrue(regular.triggerAtEpochMillis > repeated.triggerAtEpochMillis)
        val nearNextOccurrence = regular.triggerAtEpochMillis - 5 * 60_000L
        val capped = ReminderPlanner.afterDelivery(request, nearNextOccurrence)!!
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

        assertNull(ReminderPlanner.afterDelivery(request, deliveredAt))
        val repeated = ReminderPlanner.afterDelivery(request.copy(remindEveryMinutes = 15), deliveredAt)
        assertNotNull(repeated)
        assertEquals(deliveredAt + 15 * 60_000L, repeated!!.triggerAtEpochMillis)
        assertEquals(deliveredAt, repeated.cycleStartEpochMillis)
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
    }

    /** Use this function to build a routine snapshot from task, schedule, and direct-reminder inputs for planner tests. */
    private fun routine(
        tasks: List<RoutineTask> = listOf(task("task")),
        schedule: ScheduleRule? = null,
        reminderAtEpochMillis: Long? = null,
    ): RoutineTemplate = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(0, 0),
        title = "Morning",
        tasks = tasks,
        schedule = schedule,
        reminderAtEpochMillis = reminderAtEpochMillis,
    )

    /** Use this function to build a legacy task with a stable id, reminder timestamp, and feedback gates for restoration tests. */
    private fun task(
        id: String,
        reminderAtEpochMillis: Long? = null,
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true,
    ): RoutineTask = RoutineTask(
        id = RoutineTaskId(id),
        title = id,
        reminderAtEpochMillis = reminderAtEpochMillis,
        soundEnabled = soundEnabled,
        vibrateEnabled = vibrateEnabled,
    )

    /** Use this function to verify the supplied planner operation rejects a schedule before persistence. */
    private fun assertIllegalArgument(block: () -> Unit) {
        var thrown = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
