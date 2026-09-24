package com.taskchain.ui

import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies routine-level builder validation and migration without Android or persistence setup. */
class BuilderValidationTest {
    /** Use this function to create the smallest valid step for validation scenarios. */
    private fun step(title: String = "Task") = RoutineStep(RoutineStepId(title), title)

    @Test
    fun distinguishesMissingRoutineFieldsFromAnEmptyStepList() {
        assertEquals(
            setOf(BuilderValidationError.ROUTINE_NAME_REQUIRED, BuilderValidationError.STEP_REQUIRED),
            validateBuilderState(BuilderState(), 1_000L),
        )
        assertEquals(
            setOf(BuilderValidationError.ROUTINE_NAME_REQUIRED),
            validateBuilderState(BuilderState(steps = listOf(step())), 1_000L),
        )
    }

    @Test
    fun validatesRequiredScheduleAndReminderFields() {
        val now = 1_000L
        val selectedDays = BuilderState(
            title = "Routine",
            steps = listOf(step()),
            scheduleEnabled = true,
            scheduleFrequency = ScheduleFrequency.SELECTED_DAYS,
        )
        val onceMissing = BuilderState(
            title = "Routine",
            steps = listOf(step()),
            scheduleEnabled = true,
            scheduleFrequency = ScheduleFrequency.ONCE,
        )
        val oncePast = onceMissing.copy(scheduleOneTimeEpochMillis = now)
        val reminderPast = BuilderState(title = "Routine", steps = listOf(step()), reminderAtEpochMillis = now)

        assertTrue(BuilderValidationError.SELECTED_DAY_REQUIRED in validateBuilderState(selectedDays, now))
        assertTrue(BuilderValidationError.SCHEDULE_DATE_REQUIRED in validateBuilderState(onceMissing, now))
        assertTrue(BuilderValidationError.SCHEDULE_DATE_MUST_BE_FUTURE in validateBuilderState(oncePast, now))
        assertTrue(BuilderValidationError.REMINDER_MUST_BE_FUTURE in validateBuilderState(reminderPast, now))
    }

    @Test
    fun enforcesRoutineSettingExclusivityAndAcceptsFutureValues() {
        val now = 1_000L
        val base = BuilderState(title = "Routine", steps = listOf(step()))
        val daily = base.copy(scheduleEnabled = true)
        val scheduleAndDeadline = daily.copy(deadlineEpochMillis = now + 1)
        val scheduleAndReminder = daily.copy(reminderAtEpochMillis = now + 1)
        val deadlineAndReminder = base.copy(deadlineEpochMillis = now + 1, reminderAtEpochMillis = now + 2)

        assertTrue(BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE in validateBuilderState(scheduleAndDeadline, now))
        assertTrue(BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE in validateBuilderState(scheduleAndReminder, now))
        assertTrue(BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE in validateBuilderState(deadlineAndReminder, now))
        assertTrue(validateBuilderState(daily.copy(remindEveryMinutes = 5), now).isEmpty())
        assertTrue(validateBuilderState(base.copy(reminderAtEpochMillis = now + 1), now).isEmpty())
        assertTrue(validateBuilderState(base.copy(deadlineEpochMillis = now + 1), now).isEmpty())
    }

    @Test
    fun preservesInvalidStepInputWhileReportingIt() {
        val original = BuilderState(
            title = "Routine",
            steps = listOf(step()),
            editingStepIndex = 0,
            pendingStepTitle = "  ",
            pendingTimerSeconds = "0",
        )

        val errors = validateBuilderState(original, 1_000L)

        assertTrue(BuilderValidationError.STEP_NAME_REQUIRED in errors)
        assertTrue(BuilderValidationError.STEP_TIMER_MUST_BE_POSITIVE in errors)
        assertEquals("  ", original.pendingStepTitle)
        assertEquals("0", original.pendingTimerSeconds)
        assertEquals("Task", original.steps.single().title)
    }

    @Test
    fun migratesConsistentLegacyStepSettingsWithoutClearingTheirSource() {
        val schedule = ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 8, 30, daysOfWeek = setOf(2, 4))
        val legacyStep = step("Legacy").copy(schedule = schedule, remindEveryMinutes = 15)
        val routine = RoutineTemplate(RoutineId("routine"), EntityMetadata(0, 0), "Routine", steps = listOf(legacyStep))

        val draft = routine.toBuilderState()

        assertTrue(draft.scheduleEnabled)
        assertEquals(schedule, ScheduleRule(
            draft.scheduleFrequency,
            draft.scheduleHour,
            draft.scheduleMinute,
            draft.scheduleDaysOfWeek,
            draft.scheduleOneTimeEpochMillis,
        ))
        assertEquals(15, draft.remindEveryMinutes)
        assertEquals(schedule, draft.steps.single().schedule)
        assertFalse(BuilderValidationError.LEGACY_SETTINGS_CONFLICT in draft.validationErrors)
    }

    @Test
    fun exposesConflictingLegacySettingsWithoutDiscardingThem() {
        val schedule = ScheduleRule(ScheduleFrequency.DAILY, 8, 30)
        val reminderAt = 2_000L
        val routine = RoutineTemplate(
            RoutineId("routine"), EntityMetadata(0, 0), "Routine",
            steps = listOf(step("Scheduled").copy(schedule = schedule), step("Reminded").copy(reminderAtEpochMillis = reminderAt)),
        )

        val draft = routine.toBuilderState()
        val errors = validateBuilderState(draft, 1_000L)

        assertTrue(BuilderValidationError.LEGACY_SETTINGS_CONFLICT in errors)
        assertTrue(BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE in errors)
        assertEquals(schedule, draft.steps[0].schedule)
        assertEquals(reminderAt, draft.steps[1].reminderAtEpochMillis)
        assertEquals(reminderAt, draft.reminderAtEpochMillis)
    }

    @Test
    fun preservesRecurringScheduleWhenConvertedToBuilderState() {
        val schedule = ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 14, 45, daysOfWeek = setOf(1, 3, 5))
        val routine = RoutineTemplate(
            RoutineId("routine"), EntityMetadata(0, 0), "Custom Routine",
            steps = listOf(step("Step 1")),
            schedule = schedule,
        )
        val draft = routine.toBuilderState()
        assertTrue(draft.scheduleEnabled)
        assertEquals(ScheduleFrequency.SELECTED_DAYS, draft.scheduleFrequency)
        assertEquals(setOf(1, 3, 5), draft.scheduleDaysOfWeek)
        assertEquals(14, draft.scheduleHour)
        assertEquals(45, draft.scheduleMinute)
        assertTrue(validateBuilderState(draft, 1_000L).isEmpty())
    }

    @Test
    fun formatsRoutineItemCountAndTotalTime() {
        val emptyRoutine = RoutineTemplate(RoutineId("empty"), EntityMetadata(0, 0), "Empty", steps = emptyList())
        assertEquals("0 Items", formatRoutineItemCount(emptyRoutine))
        assertEquals(null, formatRoutineTotalTime(emptyRoutine))

        val untimedRoutine = RoutineTemplate(
            RoutineId("untimed"),
            EntityMetadata(0, 0),
            "Untimed",
            steps = listOf(step("Step 1")),
        )
        assertEquals("1 Item", formatRoutineItemCount(untimedRoutine))
        assertEquals(null, formatRoutineTotalTime(untimedRoutine))

        val timedRoutine = RoutineTemplate(
            RoutineId("timed"),
            EntityMetadata(0, 0),
            "Timed",
            steps = listOf(
                step("Step 1").copy(timerSeconds = 120L),
                step("Step 2").copy(timerSeconds = 180L),
                step("Step 3"),
            ),
        )
        assertEquals("3 Items", formatRoutineItemCount(timedRoutine))
        assertEquals("Total Time: 5:00", formatRoutineTotalTime(timedRoutine))

        val longTimedRoutine = RoutineTemplate(
            RoutineId("long"),
            EntityMetadata(0, 0),
            "Long",
            steps = listOf(step("Step 1").copy(timerSeconds = 3665L)),
        )
        assertEquals("Total Time: 1:01:05", formatRoutineTotalTime(longTimedRoutine))
    }
}
