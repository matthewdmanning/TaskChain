package com.taskchain.core.schedule

import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.model.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Verifies reminder planning at save, restore, and completion boundaries. */
class ReminderPlannerTest {
    /** Verifies callers cannot omit routine-owned reminder settings while constructing an adapter request. */
    @Test
    fun routineOwnsCompleteReminderRequestMapping() {
        val schedule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0)
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Morning",
            tasks = listOf(RoutineTask(RoutineTaskId("task"), "Work")),
            schedule = schedule,
            remindEveryMinutes = 15,
            soundEnabled = false,
            vibrateEnabled = true,
        )

        assertEquals(
            ReminderRequest(
                requestCode = "routine".hashCode(),
                routineId = "routine",
                title = "Morning",
                triggerAtEpochMillis = 1_000,
                schedule = schedule,
                remindEveryMinutes = 15,
                cycleStartEpochMillis = 1_000,
                soundEnabled = false,
                vibrateEnabled = true,
            ),
            routine.toReminderRequest(1_000),
        )
    }

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

        val plan = ReminderPlanner.afterCompletion(routine, RoutineId("routine"), completedAt, DAY_START_HOUR)

        assertEquals(listOf("routine".hashCode()), plan.cancelRequestCodes)
        assertTrue(plan.requests.isEmpty())
        assertEquals(plan, ReminderPlanner.afterCompletion(null, RoutineId("routine"), completedAt, DAY_START_HOUR))
        assertEquals(plan, ReminderPlanner.afterCompletion(routine.copy(schedule = null), RoutineId("routine"), completedAt, DAY_START_HOUR))
    }

    /** Use this function to verify recurring completion after the day start schedules the next day. */
    @Test
    fun completionOfDailyRoutineSchedulesNextDay() {
        val completedAt = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val routine = routine(schedule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0))

        val request = ReminderPlanner.afterCompletion(routine, RoutineId("routine"), completedAt, DAY_START_HOUR).requests.single()
        val trigger = Calendar.getInstance().apply { timeInMillis = request.triggerAtEpochMillis }

        assertEquals(Calendar.FEBRUARY, trigger.get(Calendar.MONTH))
        assertEquals(1, trigger.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, trigger.get(Calendar.HOUR_OF_DAY))
    }

    /** Use this function to verify a completion before the 2 AM day start keeps that same night's occurrence. */
    @Test
    fun completionBeforeDayStartSchedulesSameNight() {
        val completedAt = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 1, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val routine = routine(schedule = ScheduleRule(ScheduleFrequency.DAILY, 23, 0))

        val request = ReminderPlanner.afterCompletion(routine, RoutineId("routine"), completedAt, DAY_START_HOUR).requests.single()
        val trigger = Calendar.getInstance().apply { timeInMillis = request.triggerAtEpochMillis }

        assertEquals(Calendar.JANUARY, trigger.get(Calendar.MONTH))
        assertEquals(31, trigger.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, trigger.get(Calendar.HOUR_OF_DAY))
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

    private companion object {
        /** The default `home_day_start_hour`. */
        const val DAY_START_HOUR = 2
    }
}
