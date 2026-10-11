package com.taskchain.domain.model

/** A specific routine or task invariant that prevents a routine from being saved or run. */
enum class RoutineValidationError {
    INVALID_ROUTINE_ID,
    ROUTINE_NAME_REQUIRED,
    TASK_REQUIRED,
    TASK_NAME_REQUIRED,
    TASK_ID_INVALID,
    TASK_ID_DUPLICATED,
    TASK_DURATION_INVALID,
    SUBTASK_ID_INVALID,
    SUBTASK_ID_DUPLICATED,
    SUBTASK_TITLE_REQUIRED,
    SUBTASK_DURATION_INVALID,
    SUBTASK_TOTAL_TOO_LONG,
    DEADLINE_INVALID,
    REMINDER_INVALID,
    REMINDER_MUST_BE_FUTURE,
    SCHEDULE_REMINDER_EXCLUSIVE,
    SCHEDULE_TIME_INVALID,
    SCHEDULE_DAY_INVALID,
    SELECTED_DAY_REQUIRED,
    SCHEDULE_DATE_REQUIRED,
    SCHEDULE_DATE_INVALID,
    SCHEDULE_DATE_MUST_BE_FUTURE,
}

/** One routine validation error, optionally attributed to its owning main task index. */
data class RoutineValidationIssue(
    val error: RoutineValidationError,
    val taskIndex: Int? = null,
)

/** Largest safe duration in seconds before conversion to milliseconds can overflow. */
internal const val MAX_SAFE_DURATION_SECONDS: Long = Long.MAX_VALUE / 1_000L

/**
 * Lists all routine validation failures without throwing.
 * Use this function for field-aware validation before persistence; `nowEpochMillis` adds future
 * checks for direct reminders and one-time schedules when supplied. Dependencies are domain model values only.
 */
fun RoutineTemplate.validationIssues(nowEpochMillis: Long? = null): List<RoutineValidationIssue> {
    val issues = mutableListOf<RoutineValidationIssue>()
    fun add(error: RoutineValidationError, taskIndex: Int? = null) {
        issues += RoutineValidationIssue(error, taskIndex)
    }

    if (id.value.isBlank()) add(RoutineValidationError.INVALID_ROUTINE_ID)
    if (title.isBlank()) add(RoutineValidationError.ROUTINE_NAME_REQUIRED)
    if (tasks.isEmpty()) add(RoutineValidationError.TASK_REQUIRED)

    val duplicateTaskIds = tasks.groupingBy { it.id }.eachCount()
        .filterValues { it > 1 }
        .keys
    tasks.forEachIndexed { taskIndex, task ->
        if (task.title.isBlank()) add(RoutineValidationError.TASK_NAME_REQUIRED, taskIndex)
        if (task.id.value.isBlank()) add(RoutineValidationError.TASK_ID_INVALID, taskIndex)
        if (task.id in duplicateTaskIds && task.id.value.isNotBlank()) {
            add(RoutineValidationError.TASK_ID_DUPLICATED, taskIndex)
        }
        if (task.subtasks.isEmpty()) {
            if (task.timerSeconds != null && task.timerSeconds !in 1..MAX_SAFE_DURATION_SECONDS) {
                add(RoutineValidationError.TASK_DURATION_INVALID, taskIndex)
            }
        }
    }

    val duplicateSubtaskIds = tasks.flatMap { it.subtasks }
        .groupingBy { it.id }
        .eachCount()
        .filterValues { it > 1 }
        .keys
    tasks.forEachIndexed { taskIndex, task ->
        var totalDuration = 0L
        var totalTooLong = false
        task.subtasks.forEach { subtask ->
            if (subtask.id.value.isBlank()) add(RoutineValidationError.SUBTASK_ID_INVALID, taskIndex)
            if (subtask.id in duplicateSubtaskIds && subtask.id.value.isNotBlank()) {
                add(RoutineValidationError.SUBTASK_ID_DUPLICATED, taskIndex)
            }
            if (subtask.title.isBlank()) add(RoutineValidationError.SUBTASK_TITLE_REQUIRED, taskIndex)
            if (subtask.durationSeconds <= 0L || subtask.durationSeconds > MAX_SAFE_DURATION_SECONDS) {
                add(RoutineValidationError.SUBTASK_DURATION_INVALID, taskIndex)
            } else if (totalDuration > MAX_SAFE_DURATION_SECONDS - subtask.durationSeconds) {
                totalTooLong = true
            } else {
                totalDuration += subtask.durationSeconds
            }
        }
        if (totalTooLong) add(RoutineValidationError.SUBTASK_TOTAL_TOO_LONG, taskIndex)
    }

    if (deadlineEpochMillis != null && deadlineEpochMillis < 0L) add(RoutineValidationError.DEADLINE_INVALID)
    if (reminderAtEpochMillis != null) {
        if (reminderAtEpochMillis < 0L) add(RoutineValidationError.REMINDER_INVALID)
        else if (nowEpochMillis != null && reminderAtEpochMillis <= nowEpochMillis) {
            add(RoutineValidationError.REMINDER_MUST_BE_FUTURE)
        }
    }
    if (listOfNotNull(schedule, deadlineEpochMillis, reminderAtEpochMillis).size > 1) {
        add(RoutineValidationError.SCHEDULE_REMINDER_EXCLUSIVE)
    }
    if (remindEveryMinutes != null && (schedule == null || remindEveryMinutes <= 0)) {
        add(RoutineValidationError.SCHEDULE_REMINDER_EXCLUSIVE)
    }
    schedule?.let { rule ->
        if (rule.localHour !in 0..23 || rule.localMinute !in 0..59) {
            add(RoutineValidationError.SCHEDULE_TIME_INVALID)
        }
        if (rule.daysOfWeek.any { it !in 1..7 }) add(RoutineValidationError.SCHEDULE_DAY_INVALID)
        if (rule.frequency == ScheduleFrequency.SELECTED_DAYS && rule.daysOfWeek.isEmpty()) {
            add(RoutineValidationError.SELECTED_DAY_REQUIRED)
        }
        if (rule.frequency == ScheduleFrequency.ONCE) {
            when (val date = rule.oneTimeEpochMillis) {
                null -> add(RoutineValidationError.SCHEDULE_DATE_REQUIRED)
                else -> when {
                    date < 0L -> add(RoutineValidationError.SCHEDULE_DATE_INVALID)
                    nowEpochMillis != null && date <= nowEpochMillis -> {
                        add(RoutineValidationError.SCHEDULE_DATE_MUST_BE_FUTURE)
                    }
                }
            }
        }
    }
    return issues
}

/**
 * Clears only legacy task-owned routine settings.
 * Use this function while moving scheduling ownership to the routine; the input task's identity,
 * subtasks, stacking anchor, timer, and feedback gates remain unchanged. Dependencies are [RoutineTask.copy].
 */
fun RoutineTask.withoutLegacyRoutineSettings(): RoutineTask = copy(
    deadlineEpochMillis = null,
    reminderAtEpochMillis = null,
    schedule = null,
    remindEveryMinutes = null,
)

/**
 * Detects conflicting routine and legacy task settings using the builder's existing policy.
 * Use it before normalization so conflicting source data is rejected before any legacy fields are cleared;
 * inputs are routine and task settings, with no Android or UI dependencies.
 */
fun RoutineTemplate.hasConflictingLegacyRoutineSettings(): Boolean {
    if (tasks.none {
        it.stackingAnchorTaskId != null || it.deadlineEpochMillis != null || it.reminderAtEpochMillis != null ||
            it.schedule != null || it.remindEveryMinutes != null
        }
    ) return false
    val schedules = listOfNotNull(schedule) + tasks.mapNotNull { it.schedule }
    val deadlines = listOfNotNull(deadlineEpochMillis) + tasks.mapNotNull { it.deadlineEpochMillis }
    val reminders = listOfNotNull(reminderAtEpochMillis) + tasks.mapNotNull { it.reminderAtEpochMillis }
    val repeats = listOfNotNull(remindEveryMinutes) + tasks.mapNotNull { it.remindEveryMinutes }
    val choiceCount = listOf(schedules, deadlines, reminders).count { it.isNotEmpty() }
    return schedules.distinct().size > 1 || deadlines.distinct().size > 1 ||
        reminders.distinct().size > 1 || repeats.distinct().size > 1 || choiceCount > 1 ||
        (repeats.isNotEmpty() && schedules.isEmpty())
}

/**
 * Produces the trimmed, routine-owned form used for persistence.
 * Use it after raw builder input is assembled; conflicting legacy settings fail before clearing,
 * then the normalized copy is validated with the optional clock and all source identities/metadata remain.
 */
fun RoutineTemplate.normalizedForSave(nowEpochMillis: Long): RoutineTemplate {
    require(!hasConflictingLegacyRoutineSettings())
    val normalized = copy(
        title = title.trim(),
        description = description.trim(),
        tasks = tasks.map { task ->
            task.withoutLegacyRoutineSettings().copy(
                title = task.title.trim(),
                subtasks = task.subtasks.map { subtask -> subtask.copy(title = subtask.title.trim()) },
            )
        },
    )
    return normalized.requireRunnable(nowEpochMillis)
}
