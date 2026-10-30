package com.taskchain.features.authoring.ui

import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineSubtask
import com.taskchain.core.model.RoutineSubtaskId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.model.ScheduleRule
import com.taskchain.core.model.normalizedForSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that the builder's draft-to-template mapping, Core-backed validation, and save normalization
 * produce the same results as the builder's former inline rules.
 */
class BuilderTemplateMappingTest {
    private val now = 1_000L

    /** Use this function to create the smallest valid task for mapping scenarios. */
    private fun task(title: String = "Task") = RoutineTask(RoutineTaskId(title), title)

    /** Use this function to build the persisted routine the way `RoutineBuilderViewModel.save` does. */
    private fun BuilderState.savedTemplate(metadata: EntityMetadata = EntityMetadata(now, now)): RoutineTemplate =
        toTemplate(RoutineId("routine"), metadata).normalizedForSave(now)

    @Test
    fun toTemplateBuildsTheRoutineScheduleOnlyWhenEnabled() {
        val draft = BuilderState(
            title = "Routine",
            tasks = listOf(task()),
            scheduleFrequency = ScheduleFrequency.SELECTED_DAYS,
            scheduleHour = 7,
            scheduleMinute = 15,
            scheduleDaysOfWeek = setOf(2, 6),
        )

        assertNull(draft.toTemplate().schedule)
        assertEquals(
            ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 7, 15, setOf(2, 6), null),
            draft.copy(scheduleEnabled = true).toTemplate().schedule,
        )
    }

    @Test
    fun toTemplateKeepsRawTitlesAndLegacyTaskSettingsForValidation() {
        val legacy = task(" Legacy ").copy(reminderAtEpochMillis = 5_000L)
        val draft = BuilderState(title = " Routine ", description = " About ", tasks = listOf(legacy))

        val template = draft.toTemplate()

        assertEquals(" Routine ", template.title)
        assertEquals(" About ", template.description)
        assertEquals(listOf(legacy), template.tasks)
    }

    @Test
    fun saveTrimsTitlesClearsLegacyTaskSettingsAndKeepsRoutineSettings() {
        val schedule = ScheduleRule(ScheduleFrequency.DAILY, 8, 0)
        val legacyTask = task("Legacy").copy(
            title = "  Legacy  ",
            schedule = schedule,
            remindEveryMinutes = 10,
            subtasks = listOf(RoutineSubtask(RoutineSubtaskId("s"), "  Step  ", 30)),
        )
        val draft = BuilderState(
            title = "  Morning  ",
            description = "  Wake up  ",
            tasks = listOf(legacyTask),
            remindEveryMinutes = 10,
            soundEnabled = false,
            vibrateEnabled = false,
            scheduleEnabled = true,
            scheduleHour = 8,
            scheduleMinute = 0,
        )
        val metadata = EntityMetadata(1L, now, revision = 4)
        assertTrue(validateBuilderState(draft, now).isEmpty())

        val saved = draft.savedTemplate(metadata)

        assertEquals(legacyFormerSave(draft, metadata), saved)
        assertEquals("Morning", saved.title)
        assertEquals("Wake up", saved.description)
        assertEquals("Legacy", saved.tasks.single().title)
        assertEquals("Step", saved.tasks.single().subtasks.single().title)
        assertNull(saved.tasks.single().schedule)
        assertNull(saved.tasks.single().remindEveryMinutes)
        assertEquals(schedule, saved.schedule)
        assertEquals(10, saved.remindEveryMinutes)
        assertEquals(metadata, saved.metadata)
        assertEquals(false, saved.soundEnabled)
        assertEquals(false, saved.vibrateEnabled)
    }

    @Test
    fun saveKeepsFutureReminderAndDeadline() {
        val reminder = BuilderState(title = "Routine", tasks = listOf(task()), reminderAtEpochMillis = now + 1)
        val deadline = BuilderState(title = "Routine", tasks = listOf(task()), deadlineEpochMillis = now + 1)

        assertEquals(now + 1, reminder.savedTemplate().reminderAtEpochMillis)
        assertEquals(legacyFormerSave(reminder, EntityMetadata(now, now)), reminder.savedTemplate())
        assertEquals(now + 1, deadline.savedTemplate().deadlineEpochMillis)
        assertEquals(legacyFormerSave(deadline, EntityMetadata(now, now)), deadline.savedTemplate())
    }

    @Test
    fun reminderAtOrBeforeNowIsShownAsMustBeFuture() {
        val base = BuilderState(title = "Routine", tasks = listOf(task()))

        assertEquals(
            setOf(BuilderValidationError.REMINDER_MUST_BE_FUTURE),
            validateBuilderState(base.copy(reminderAtEpochMillis = now), now),
        )
        assertEquals(
            setOf(BuilderValidationError.REMINDER_MUST_BE_FUTURE),
            validateBuilderState(base.copy(reminderAtEpochMillis = -1L), now),
        )
        assertTrue(validateBuilderState(base.copy(reminderAtEpochMillis = now + 1), now).isEmpty())
    }

    @Test
    fun negativeOneTimeDateIsShownAsMustBeFuture() {
        val once = BuilderState(
            title = "Routine",
            tasks = listOf(task()),
            scheduleEnabled = true,
            scheduleFrequency = ScheduleFrequency.ONCE,
            scheduleOneTimeEpochMillis = -1L,
        )

        assertEquals(setOf(BuilderValidationError.SCHEDULE_DATE_MUST_BE_FUTURE), validateBuilderState(once, now))
    }

    @Test
    fun legacyConflictIsShownAndBlocksNormalization() {
        val draft = BuilderState(
            title = "Routine",
            tasks = listOf(task("A").copy(deadlineEpochMillis = 5_000L), task("B").copy(deadlineEpochMillis = 6_000L)),
            deadlineEpochMillis = 5_000L,
        )

        assertEquals(setOf(BuilderValidationError.LEGACY_SETTINGS_CONFLICT), validateBuilderState(draft, now))
        assertThrows(IllegalArgumentException::class.java) { draft.savedTemplate() }
    }

    @Test
    fun loadedConflictIsFlaggedWhileConsistentLegacyIsNot() {
        val consistent = RoutineTemplate(
            RoutineId("r"), EntityMetadata(0, 0), "Routine",
            tasks = listOf(task("A").copy(reminderAtEpochMillis = 5_000L), task("B").copy(reminderAtEpochMillis = 5_000L)),
        )
        val conflicting = consistent.copy(
            tasks = listOf(task("A").copy(reminderAtEpochMillis = 5_000L), task("B").copy(reminderAtEpochMillis = 7_000L)),
        )

        assertTrue(consistent.toBuilderState().validationErrors.isEmpty())
        assertEquals(setOf(BuilderValidationError.LEGACY_SETTINGS_CONFLICT), conflicting.toBuilderState().validationErrors)
    }

    @Test
    fun coreRulesTheBuilderDoesNotShowStillFailTheSave() {
        val base = BuilderState(title = "Routine", tasks = listOf(task()))
        val negativeDeadline = base.copy(deadlineEpochMillis = -1L)
        val invalidHour = base.copy(scheduleEnabled = true, scheduleHour = 24)
        val invalidTimer = base.copy(tasks = listOf(task().copy(timerSeconds = 0L)))

        listOf(negativeDeadline, invalidHour, invalidTimer).forEach { draft ->
            assertTrue(validateBuilderState(draft, now).isEmpty())
            assertThrows(IllegalArgumentException::class.java) { draft.savedTemplate() }
        }
    }

    @Test
    fun whitespaceOnlyNamesAreRequiredAndNotTrimmedIntoValidity() {
        val draft = BuilderState(title = "   ", tasks = listOf(task("   ")))

        assertEquals(
            setOf(BuilderValidationError.ROUTINE_NAME_REQUIRED, BuilderValidationError.TASK_NAME_REQUIRED),
            validateBuilderState(draft, now),
        )
    }

    /** Compares Core-backed validation with the former inline builder rules over schedule, reminder and legacy inputs. */
    @Test
    fun validationMatchesFormerBuilderRulesAcrossRoutineSettings() {
        val legacyTasks = listOf(
            listOf(task()),
            listOf(task().copy(schedule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0))),
            listOf(task().copy(reminderAtEpochMillis = now + 5)),
            listOf(task().copy(stackingAnchorTaskId = RoutineTaskId("anchor"), remindEveryMinutes = 5)),
        )
        val dateChoices = listOf(null, -1L, now, now + 1)
        val instantChoices = listOf(null, -1L, now, now + 5)
        var checked = 0
        for (tasks in legacyTasks) for (enabled in listOf(false, true)) for (frequency in ScheduleFrequency.entries)
            for (days in listOf(emptySet<Int>(), setOf(3))) for (date in dateChoices) for (reminder in instantChoices)
                for (deadline in listOf(null, now + 5)) for (every in listOf(null, 0, 5)) {
                    val draft = BuilderState(
                        title = "Routine",
                        tasks = tasks,
                        scheduleEnabled = enabled,
                        scheduleFrequency = frequency,
                        scheduleDaysOfWeek = days,
                        scheduleOneTimeEpochMillis = date,
                        reminderAtEpochMillis = reminder,
                        deadlineEpochMillis = deadline,
                        remindEveryMinutes = every,
                    )
                    assertEquals(draft.toString(), legacyFormerValidation(draft, now), validateBuilderState(draft, now))
                    checked++
                }
        assertTrue(checked > 0)
    }

    /** Former `save()` template construction, kept as an oracle for the `normalizedForSave` path. */
    private fun legacyFormerSave(draft: BuilderState, metadata: EntityMetadata): RoutineTemplate = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = metadata,
        title = draft.title.trim(),
        description = draft.description.trim(),
        tasks = draft.tasks.map { task ->
            task.copy(
                title = task.title.trim(),
                subtasks = task.subtasks.map { subtask -> subtask.copy(title = subtask.title.trim()) },
                deadlineEpochMillis = null,
                reminderAtEpochMillis = null,
                schedule = null,
                remindEveryMinutes = null,
            )
        },
        schedule = if (draft.scheduleEnabled) ScheduleRule(
            draft.scheduleFrequency,
            draft.scheduleHour,
            draft.scheduleMinute,
            draft.scheduleDaysOfWeek,
            draft.scheduleOneTimeEpochMillis,
        ) else null,
        deadlineEpochMillis = draft.deadlineEpochMillis,
        reminderAtEpochMillis = draft.reminderAtEpochMillis,
        remindEveryMinutes = draft.remindEveryMinutes,
        soundEnabled = draft.soundEnabled,
        vibrateEnabled = draft.vibrateEnabled,
        soundSettings = draft.soundSettings,
    ).requireRunnable()

    /** Former inline `validateBuilderState` rules, kept as an oracle for the Core-backed mapping. */
    private fun legacyFormerValidation(state: BuilderState, nowEpochMillis: Long): Set<BuilderValidationError> {
        val errors = mutableSetOf<BuilderValidationError>()
        if (state.title.isBlank()) errors += BuilderValidationError.ROUTINE_NAME_REQUIRED
        if (state.tasks.isEmpty()) errors += BuilderValidationError.TASK_REQUIRED
        state.editingTaskIndex?.takeIf { it in state.tasks.indices }?.let {
            if (state.pendingTaskTitle.isBlank()) errors += BuilderValidationError.TASK_NAME_REQUIRED
        }
        state.tasks.forEach { task ->
            if (task.title.isBlank()) errors += BuilderValidationError.TASK_NAME_REQUIRED
        }
        val schedule = if (state.scheduleEnabled) ScheduleRule(
            state.scheduleFrequency, state.scheduleHour, state.scheduleMinute,
            state.scheduleDaysOfWeek, state.scheduleOneTimeEpochMillis,
        ) else null
        schedule?.let {
            if (it.frequency == ScheduleFrequency.SELECTED_DAYS && it.daysOfWeek.isEmpty()) {
                errors += BuilderValidationError.SELECTED_DAY_REQUIRED
            }
            if (it.frequency == ScheduleFrequency.ONCE) {
                when (val date = it.oneTimeEpochMillis) {
                    null -> errors += BuilderValidationError.SCHEDULE_DATE_REQUIRED
                    else -> if (date <= nowEpochMillis) errors += BuilderValidationError.SCHEDULE_DATE_MUST_BE_FUTURE
                }
            }
        }
        if (state.reminderAtEpochMillis?.let { it <= nowEpochMillis } == true) {
            errors += BuilderValidationError.REMINDER_MUST_BE_FUTURE
        }
        val every = state.remindEveryMinutes
        if ((schedule != null && (state.deadlineEpochMillis != null || state.reminderAtEpochMillis != null)) ||
            (state.deadlineEpochMillis != null && state.reminderAtEpochMillis != null) ||
            (every != null && (schedule == null || every <= 0))
        ) {
            errors += BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE
        }
        if (legacyFormerConflict(state, schedule)) errors += BuilderValidationError.LEGACY_SETTINGS_CONFLICT
        state.tasks.forEach { errors += state.subtaskErrorsFor(it) }
        return errors
    }

    /** Former builder copy of the legacy-conflict rule. */
    private fun legacyFormerConflict(state: BuilderState, routineSchedule: ScheduleRule?): Boolean {
        if (state.tasks.none {
                it.stackingAnchorTaskId != null || it.deadlineEpochMillis != null || it.reminderAtEpochMillis != null ||
                    it.schedule != null || it.remindEveryMinutes != null
            }
        ) return false
        val schedules = listOfNotNull(routineSchedule) + state.tasks.mapNotNull { it.schedule }
        val deadlines = listOfNotNull(state.deadlineEpochMillis) + state.tasks.mapNotNull { it.deadlineEpochMillis }
        val reminders = listOfNotNull(state.reminderAtEpochMillis) + state.tasks.mapNotNull { it.reminderAtEpochMillis }
        val repeats = listOfNotNull(state.remindEveryMinutes) + state.tasks.mapNotNull { it.remindEveryMinutes }
        val choiceCount = listOf(schedules, deadlines, reminders).count { it.isNotEmpty() }
        return schedules.distinct().size > 1 || deadlines.distinct().size > 1 ||
            reminders.distinct().size > 1 || repeats.distinct().size > 1 || choiceCount > 1 ||
            (repeats.isNotEmpty() && schedules.isEmpty())
    }
}
