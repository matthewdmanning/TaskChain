package com.taskchain.ui

import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineSubtask
import com.taskchain.domain.model.RoutineSubtaskId
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies routine-level builder validation and migration without Android or persistence setup. */
class BuilderValidationTest {
    /** Use this function to create the smallest valid task for validation scenarios. */
    private fun task(title: String = "Task") = RoutineTask(RoutineTaskId(title), title)

    @Test
    fun distinguishesMissingRoutineFieldsFromAnEmptyTaskList() {
        assertEquals(
            setOf(BuilderValidationError.ROUTINE_NAME_REQUIRED, BuilderValidationError.TASK_REQUIRED),
            validateBuilderState(BuilderState(), 1_000L),
        )
        assertEquals(
            setOf(BuilderValidationError.ROUTINE_NAME_REQUIRED),
            validateBuilderState(BuilderState(tasks = listOf(task())), 1_000L),
        )
    }

    @Test
    fun validatesRequiredScheduleAndReminderFields() {
        val now = 1_000L
        val selectedDays = BuilderState(
            title = "Routine",
            tasks = listOf(task()),
            scheduleEnabled = true,
            scheduleFrequency = ScheduleFrequency.SELECTED_DAYS,
        )
        val onceMissing = BuilderState(
            title = "Routine",
            tasks = listOf(task()),
            scheduleEnabled = true,
            scheduleFrequency = ScheduleFrequency.ONCE,
        )
        val oncePast = onceMissing.copy(scheduleOneTimeEpochMillis = now)
        val reminderPast = BuilderState(title = "Routine", tasks = listOf(task()), reminderAtEpochMillis = now)

        assertTrue(BuilderValidationError.SELECTED_DAY_REQUIRED in validateBuilderState(selectedDays, now))
        assertTrue(BuilderValidationError.SCHEDULE_DATE_REQUIRED in validateBuilderState(onceMissing, now))
        assertTrue(BuilderValidationError.SCHEDULE_DATE_MUST_BE_FUTURE in validateBuilderState(oncePast, now))
        assertTrue(BuilderValidationError.REMINDER_MUST_BE_FUTURE in validateBuilderState(reminderPast, now))
    }

    @Test
    fun enforcesRoutineSettingExclusivityAndAcceptsFutureValues() {
        val now = 1_000L
        val base = BuilderState(title = "Routine", tasks = listOf(task()))
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
    fun preservesInvalidTaskInputWhileReportingIt() {
        val original = BuilderState(
            title = "Routine",
            tasks = listOf(task()),
            editingTaskIndex = 0,
            pendingTaskTitle = "  ",
            pendingTimerSeconds = "0",
        )

        val errors = validateBuilderState(original, 1_000L)

        assertTrue(BuilderValidationError.TASK_NAME_REQUIRED in errors)
        assertTrue(BuilderValidationError.TASK_TIMER_MUST_BE_POSITIVE in errors)
        assertEquals("  ", original.pendingTaskTitle)
        assertEquals("0", original.pendingTimerSeconds)
        assertEquals("Task", original.tasks.single().title)
    }

    @Test
    fun migratesConsistentLegacyTaskSettingsWithoutClearingTheirSource() {
        val schedule = ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 8, 30, daysOfWeek = setOf(2, 4))
        val legacyTask = task("Legacy").copy(schedule = schedule, remindEveryMinutes = 15)
        val routine = RoutineTemplate(RoutineId("routine"), EntityMetadata(0, 0), "Routine", tasks = listOf(legacyTask))

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
        assertEquals(schedule, draft.tasks.single().schedule)
        assertFalse(BuilderValidationError.LEGACY_SETTINGS_CONFLICT in draft.validationErrors)
    }

    @Test
    fun exposesConflictingLegacySettingsWithoutDiscardingThem() {
        val schedule = ScheduleRule(ScheduleFrequency.DAILY, 8, 30)
        val reminderAt = 2_000L
        val routine = RoutineTemplate(
            RoutineId("routine"), EntityMetadata(0, 0), "Routine",
            tasks = listOf(task("Scheduled").copy(schedule = schedule), task("Reminded").copy(reminderAtEpochMillis = reminderAt)),
        )

        val draft = routine.toBuilderState()
        val errors = validateBuilderState(draft, 1_000L)

        assertTrue(BuilderValidationError.LEGACY_SETTINGS_CONFLICT in errors)
        assertTrue(BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE in errors)
        assertEquals(schedule, draft.tasks[0].schedule)
        assertEquals(reminderAt, draft.tasks[1].reminderAtEpochMillis)
        assertEquals(reminderAt, draft.reminderAtEpochMillis)
    }

    @Test
    fun preservesRecurringScheduleWhenConvertedToBuilderState() {
        val schedule = ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 14, 45, daysOfWeek = setOf(1, 3, 5))
        val routine = RoutineTemplate(
            RoutineId("routine"), EntityMetadata(0, 0), "Custom Routine",
            tasks = listOf(task("Task 1")),
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
        val emptyRoutine = RoutineTemplate(RoutineId("empty"), EntityMetadata(0, 0), "Empty", tasks = emptyList())
        assertEquals("0 Items", formatRoutineItemCount(emptyRoutine))
        assertEquals(null, formatRoutineTotalTime(emptyRoutine))

        val untimedRoutine = RoutineTemplate(
            RoutineId("untimed"),
            EntityMetadata(0, 0),
            "Untimed",
            tasks = listOf(task("Task 1")),
        )
        assertEquals("1 Item", formatRoutineItemCount(untimedRoutine))
        assertEquals(null, formatRoutineTotalTime(untimedRoutine))

        val timedRoutine = RoutineTemplate(
            RoutineId("timed"),
            EntityMetadata(0, 0),
            "Timed",
            tasks = listOf(
                task("Task 1").copy(timerSeconds = 120L),
                task("Task 2").copy(timerSeconds = 180L),
                task("Task 3"),
            ),
        )
        assertEquals("3 Items", formatRoutineItemCount(timedRoutine))
        assertEquals("Total Time: 5:00", formatRoutineTotalTime(timedRoutine))

        val longTimedRoutine = RoutineTemplate(
            RoutineId("long"),
            EntityMetadata(0, 0),
            "Long",
            tasks = listOf(task("Task 1").copy(timerSeconds = 3665L)),
        )
        assertEquals("Total Time: 1:01:05", formatRoutineTotalTime(longTimedRoutine))
    }

    /** Use this function to verify invalid raw subtask durations and duplicate subtask identities block a draft save. */
    @Test
    fun rejectsRawSubtaskDurationAndDuplicateSubtaskIds() {
        val subtaskId = RoutineSubtaskId("subtask")
        val first = task("First").copy(subtasks = listOf(RoutineSubtask(subtaskId, "Prepare", 60)))
        val second = task("Second").copy(subtasks = listOf(RoutineSubtask(subtaskId, "Repeat", 60)))
        val state = BuilderState(
            title = "Routine",
            tasks = listOf(first, second),
            pendingSubtaskDurations = mapOf("First:subtask" to "not-a-number"),
        )

        val errors = validateBuilderState(state, 1_000L)
        assertTrue(BuilderValidationError.SUBTASK_DURATION_INVALID in errors)
        assertTrue(BuilderValidationError.SUBTASK_ID_DUPLICATED in errors)
        assertEquals(setOf(subtaskId), state.invalidSubtaskDurationIds)
    }

    /** Use this function to verify a blank subtask title reports only the title error, and only on its own task. */
    @Test
    fun blankSubtaskTitleErrorBelongsOnlyToItsTaskAndClearsWhenTitled() {
        val blank = task("Blank").copy(subtasks = listOf(RoutineSubtask(RoutineSubtaskId("new"), "", 60)))
        val valid = task("Valid").copy(subtasks = listOf(RoutineSubtask(RoutineSubtaskId("ok"), "Prepare", 60)))
        val state = BuilderState(title = "Routine", tasks = listOf(blank, valid))

        assertEquals(setOf(BuilderValidationError.SUBTASK_TITLE_REQUIRED), validateBuilderState(state, 1_000L))
        assertEquals(setOf(BuilderValidationError.SUBTASK_TITLE_REQUIRED), state.subtaskErrorsFor(blank))
        assertTrue(state.subtaskErrorsFor(valid).isEmpty())

        val titled = blank.copy(subtasks = listOf(blank.subtasks.single().copy(title = "Stretch")))
        assertTrue(validateBuilderState(state.copy(tasks = listOf(titled, valid)), 1_000L).isEmpty())
    }

    /** Use this function to verify a failed save targets the first task with a shown error, and that errors clear when fixed. */
    @Test
    fun failedSaveTargetsFirstTaskWithShownErrors() {
        val valid = task("Valid")
        val blank = task("Blank").copy(subtasks = listOf(RoutineSubtask(RoutineSubtaskId("new"), "", 60)))
        val draft = BuilderState(title = "Routine", tasks = listOf(valid, blank))

        assertEquals(null, draft.firstTaskIndexWithErrors())
        val failed = draft.copy(validationErrors = validateBuilderState(draft, 1_000L))
        assertEquals(1, failed.firstTaskIndexWithErrors())
        assertTrue(failed.taskErrorsFor(0).isEmpty())
        assertEquals(setOf(BuilderValidationError.SUBTASK_TITLE_REQUIRED), failed.taskErrorsFor(1))

        val invalidTimer = failed.copy(editingTaskIndex = 0, pendingTimerSeconds = "-5")
            .let { it.copy(validationErrors = validateBuilderState(it, 1_000L)) }
        assertEquals(0, invalidTimer.firstTaskIndexWithErrors())
    }

    /** Use this function to verify a failed save expands the failing task, opens its name field, and sets the scroll target. */
    @Test
    fun failedSaveRevealExpandsFirstTaskWithErrors() {
        val valid = task("Valid")
        val unnamed = RoutineTask(RoutineTaskId("unnamed"), "")
        val draft = BuilderState(title = "Routine", tasks = listOf(valid, unnamed))
        val revealed = draft.copy(validationErrors = validateBuilderState(draft, 1_000L)).revealingFirstError()

        assertEquals(unnamed.id, revealed.expandedTaskId)
        assertEquals(unnamed.id, revealed.editingNameTaskId)
        assertEquals(1, revealed.editingTaskIndex)
        assertEquals(BuilderRevealTarget.Task(1), revealed.revealTarget)

        val noName = BuilderState(tasks = listOf(valid))
        assertEquals(
            BuilderRevealTarget.RoutineName,
            noName.copy(validationErrors = validateBuilderState(noName, 1_000L)).revealingFirstError().revealTarget,
        )
    }

    /** Use this function to verify another task cannot expand while the edited task has invalid timer text. */
    @Test
    fun invalidPendingTimerBlocksExpandingAnotherTask() {
        val first = task("First")
        val second = task("Second")
        val draft = BuilderState(tasks = listOf(first, second), editingTaskIndex = 0, pendingTimerSeconds = "-5")

        assertEquals(null, draft.expandingTask(second.id))
        assertEquals(first.id, draft.expandingTask(first.id)?.expandedTaskId)
        assertEquals(second.id, draft.copy(pendingTimerSeconds = "30").expandingTask(second.id)?.expandedTaskId)
    }

    /** Use this function to verify nested subtasks contribute duration while counting only their main task.
     * Inputs: none. Dependencies: routine summary formatting and the subtask model.
     */
    @Test
    fun subtaskRoutineSummaryCountsMainTasksAndDerivedDuration() {
        val routine = RoutineTemplate(
            RoutineId("subtasks"), EntityMetadata(0, 0), "Subtasks",
            tasks = listOf(task("Main").copy(subtasks = listOf(
                RoutineSubtask(RoutineSubtaskId("one"), "One", 180),
                RoutineSubtask(RoutineSubtaskId("two"), "Two", 420),
            ))),
        )
        assertEquals("1 Item", formatRoutineItemCount(routine))
        assertEquals("Total Time: 10:00", formatRoutineTotalTime(routine))
    }
}
