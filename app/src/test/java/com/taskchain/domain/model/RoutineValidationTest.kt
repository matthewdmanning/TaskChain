package com.taskchain.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies shared routine validity, temporal checks, and immutable save normalization. */
class RoutineValidationTest {
    /** Use this function to verify requireRunnable and issue reporting reject the same malformed routine. */
    @Test
    fun issuesMatchRequireRunnableForInvalidRoutine() {
        val routine = routine(
            title = "",
            tasks = listOf(task(id = "", title = "", timerSeconds = 0)),
            deadlineEpochMillis = -1,
        )

        val issues = routine.validationIssues()

        assertTrue(issues.any { it.error == RoutineValidationError.ROUTINE_NAME_REQUIRED })
        assertTrue(issues.any { it.error == RoutineValidationError.TASK_NAME_REQUIRED && it.taskIndex == 0 })
        assertTrue(issues.any { it.error == RoutineValidationError.TASK_ID_INVALID && it.taskIndex == 0 })
        assertTrue(issues.any { it.error == RoutineValidationError.TASK_DURATION_INVALID && it.taskIndex == 0 })
        assertTrue(issues.any { it.error == RoutineValidationError.DEADLINE_INVALID })
        assertThrows(IllegalArgumentException::class.java) { routine.requireRunnable() }
    }

    /** Use this function to verify clock-aware reminder and one-time schedule checks leave deadlines unchanged. */
    @Test
    fun temporalValidationAddsOnlyFutureRequirements() {
        val now = 100L
        val reminder = routine(reminderAtEpochMillis = now)
        val once = routine(schedule = ScheduleRule(ScheduleFrequency.ONCE, 9, 0, oneTimeEpochMillis = now))
        val deadline = routine(deadlineEpochMillis = 0L)

        assertTrue(reminder.validationIssues().isEmpty())
        assertTrue(reminder.validationIssues(now).any { it.error == RoutineValidationError.REMINDER_MUST_BE_FUTURE })
        assertTrue(once.validationIssues(now).any { it.error == RoutineValidationError.SCHEDULE_DATE_MUST_BE_FUTURE })
        assertTrue(deadline.validationIssues(now).isEmpty())
    }

    /** Use this function to verify subtask errors are attributed to their main task and overflow is safe. */
    @Test
    fun subtaskIssuesIdentifyTaskAndOverflow() {
        val duplicate = RoutineSubtaskId("duplicate")
        val routine = routine(
            tasks = listOf(
                task("one", subtasks = listOf(
                    RoutineSubtask(duplicate, "", MAX_SAFE_DURATION_SECONDS),
                    RoutineSubtask(RoutineSubtaskId("two"), "Two", 1),
                )),
                task("two", subtasks = listOf(RoutineSubtask(duplicate, "Again", 1))),
            ),
        )

        val issues = routine.validationIssues()

        assertTrue(issues.any { it.error == RoutineValidationError.SUBTASK_TITLE_REQUIRED && it.taskIndex == 0 })
        assertTrue(issues.count { it.error == RoutineValidationError.SUBTASK_ID_DUPLICATED } == 2)
        assertTrue(issues.any { it.error == RoutineValidationError.SUBTASK_TOTAL_TOO_LONG && it.taskIndex == 0 })
        assertFalse(issues.any { it.error == RoutineValidationError.SUBTASK_TOTAL_TOO_LONG && it.taskIndex == 1 })
    }

    /** Use this function to verify normalization trims text, clears legacy fields, and preserves durable identity. */
    @Test
    fun normalizationPreservesIdentityAnchorAndFeedback() {
        val taskId = RoutineTaskId("task")
        val anchor = RoutineTaskId("anchor")
        val source = routine(
            title = "  Morning  ",
            description = "  Desc  ",
            tasks = listOf(
                task(
                    id = taskId.value,
                    title = "  Step  ",
                    stackingAnchorTaskId = anchor,
                    subtasks = listOf(RoutineSubtask(RoutineSubtaskId("sub"), "  Sub  ", 1)),
                    soundEnabled = false,
                    vibrateEnabled = false,
                    reminderAtEpochMillis = 200,
                ),
            ),
            reminderAtEpochMillis = 200,
        )

        val normalized = source.copy(reminderAtEpochMillis = null).normalizedForSave(100)

        assertNotSame(source, normalized)
        assertEquals(source.id, normalized.id)
        assertEquals(source.metadata, normalized.metadata)
        assertEquals("Morning", normalized.title)
        assertEquals("Desc", normalized.description)
        assertEquals(anchor, normalized.tasks.single().stackingAnchorTaskId)
        assertEquals("Step", normalized.tasks.single().title)
        assertEquals("Sub", normalized.tasks.single().subtasks.single().title)
        assertFalse(normalized.tasks.single().soundEnabled)
        assertFalse(normalized.tasks.single().vibrateEnabled)
        assertTrue(normalized.tasks.single().reminderAtEpochMillis == null)
    }

    /** Use this function to verify conflicting routine and legacy settings fail before normalization. */
    @Test
    fun normalizationRejectsConflictingLegacySettingsWithoutMutation() {
        val source = routine(
            schedule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0),
            tasks = listOf(task("task", reminderAtEpochMillis = 200)),
        )

        assertTrue(source.hasConflictingLegacyRoutineSettings())
        assertThrows(IllegalArgumentException::class.java) { source.normalizedForSave(100) }
        assertEquals(200L, source.tasks.single().reminderAtEpochMillis)
    }

    private fun routine(
        title: String = "Routine",
        description: String = "",
        tasks: List<RoutineTask> = listOf(task()),
        schedule: ScheduleRule? = null,
        deadlineEpochMillis: Long? = null,
        reminderAtEpochMillis: Long? = null,
    ): RoutineTemplate = RoutineTemplate(
        id = RoutineId("routine"),
        metadata = EntityMetadata(1, 2, revision = 3),
        title = title,
        description = description,
        tasks = tasks,
        schedule = schedule,
        deadlineEpochMillis = deadlineEpochMillis,
        reminderAtEpochMillis = reminderAtEpochMillis,
    )

    private fun task(
        id: String = "task",
        title: String = "Task",
        timerSeconds: Long? = null,
        subtasks: List<RoutineSubtask> = emptyList(),
        stackingAnchorTaskId: RoutineTaskId? = null,
        reminderAtEpochMillis: Long? = null,
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true,
    ): RoutineTask = RoutineTask(
        id = RoutineTaskId(id),
        title = title,
        timerSeconds = timerSeconds,
        subtasks = subtasks,
        stackingAnchorTaskId = stackingAnchorTaskId,
        reminderAtEpochMillis = reminderAtEpochMillis,
        soundEnabled = soundEnabled,
        vibrateEnabled = vibrateEnabled,
    )
}
