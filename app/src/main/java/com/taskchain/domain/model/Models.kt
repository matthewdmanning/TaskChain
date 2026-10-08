package com.taskchain.domain.model

import kotlinx.serialization.Serializable

/** Stable routine identity that remains valid across file moves and future storage adapters. */
@Serializable
@JvmInline
value class RoutineId(val value: String)

/** Stable identity for one task inside a routine snapshot. */
@Serializable
@JvmInline
value class RoutineTaskId(val value: String)

/** Stable identity for one execution of a routine. */
@Serializable
@JvmInline
value class RoutineRunId(val value: String)

/** Stable identity for one ordered subtask nested within a routine task. */
@Serializable
@JvmInline
value class RoutineSubtaskId(val value: String)

/** A manually advanced subtask that partitions one main routine task. */
@Serializable
data class RoutineSubtask(
    val id: RoutineSubtaskId,
    val title: String,
    val durationSeconds: Long,
)

/** Records one manual subtask advancement using cumulative active time on its main task. */
@Serializable
data class SubtaskAdvancement(
    val subtaskId: RoutineSubtaskId,
    val elapsedMillis: Long,
    val atEpochMillis: Long,
)

/** Metadata shared by persistent domain records without exposing file-storage details. */
@Serializable
data class EntityMetadata(
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val revision: Long = 1,
    val deletedAtEpochMillis: Long? = null,
)

/** Platform-neutral recurrence choices understood by the scheduling adapter. */
@Serializable
enum class ScheduleFrequency { ONCE, DAILY, WEEKDAYS, SELECTED_DAYS }

/** A local schedule definition that stays independent from Android alarms. */
@Serializable
data class ScheduleRule(
    val frequency: ScheduleFrequency,
    val localHour: Int,
    val localMinute: Int,
    val daysOfWeek: Set<Int> = emptySet(),
    val oneTimeEpochMillis: Long? = null,
)

/** A reusable task within a routine. */
@Serializable
data class RoutineTask(
    val id: RoutineTaskId,
    val title: String,
    val timerSeconds: Long? = null,
    val subtasks: List<RoutineSubtask> = emptyList(),
    val stackingAnchorTaskId: RoutineTaskId? = null,
    val deadlineEpochMillis: Long? = null,
    val reminderAtEpochMillis: Long? = null,
    val schedule: ScheduleRule? = null,
    val remindEveryMinutes: Int? = null,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
) {
    /** Total configured duration in seconds, using subtask duration when subtasks are present. */
    val durationSeconds: Long?
        get() = if (subtasks.isEmpty()) timerSeconds else subtasks.fold(0L) { total, subtask ->
            if (total > Long.MAX_VALUE - subtask.durationSeconds) Long.MAX_VALUE
            else total + subtask.durationSeconds
        }
}

/** A reusable ordered routine definition edited by the builder. */
@Serializable
data class RoutineTemplate(
    val id: RoutineId,
    val metadata: EntityMetadata,
    val title: String,
    val description: String = "",
    val tasks: List<RoutineTask>,
    val schedule: ScheduleRule? = null,
    val deadlineEpochMillis: Long? = null,
    val reminderAtEpochMillis: Long? = null,
    val remindEveryMinutes: Int? = null,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val soundSettings: SoundSettings = defaultSoundSettings(),
) {
    /** Use this function before starting or saving a routine that must be executable. */
    fun requireRunnable(): RoutineTemplate = apply {
        require(id.value.isNotBlank())
        require(title.isNotBlank())
        require(tasks.isNotEmpty())
        require(tasks.all { it.title.isNotBlank() })
        require(tasks.all { it.id.value.isNotBlank() })
        require(tasks.map { it.id }.distinct().size == tasks.size)
        require(tasks.flatMap { it.subtasks }.map { it.id }.distinct().size == tasks.sumOf { it.subtasks.size })
        require(tasks.all { task ->
            if (task.subtasks.isEmpty()) {
                task.timerSeconds == null || task.timerSeconds in 1..MAX_SAFE_DURATION_SECONDS
            } else {
                var total = 0L
                task.subtasks.isNotEmpty() && task.subtasks.all { subtask ->
                    val valid = subtask.id.value.isNotBlank() && subtask.title.isNotBlank() &&
                        subtask.durationSeconds > 0 && total <= MAX_SAFE_DURATION_SECONDS - subtask.durationSeconds
                    if (valid) total += subtask.durationSeconds
                    valid
                } && task.subtasks.map { it.id }.distinct().size == task.subtasks.size
            }
        })
        require(deadlineEpochMillis == null || deadlineEpochMillis >= 0)
        require(reminderAtEpochMillis == null || reminderAtEpochMillis >= 0)
        require(listOfNotNull(schedule, deadlineEpochMillis, reminderAtEpochMillis).size <= 1)
        require(remindEveryMinutes == null || (schedule != null && remindEveryMinutes > 0))
        listOfNotNull(schedule).forEach { rule ->
            require(rule.localHour in 0..23 && rule.localMinute in 0..59)
            require(rule.daysOfWeek.all { it in 1..7 })
            when (rule.frequency) {
                ScheduleFrequency.ONCE -> require(rule.oneTimeEpochMillis != null && rule.oneTimeEpochMillis >= 0)
                ScheduleFrequency.SELECTED_DAYS -> require(rule.daysOfWeek.isNotEmpty())
                else -> Unit
            }
        }
    }
}

/** Durable execution status for one task in a run snapshot. */
@Serializable
enum class RunTaskStatus { PENDING, COMPLETED, SKIPPED }

/** Durable lifecycle status for a routine run. */
@Serializable
enum class RunStatus { ACTIVE, COMPLETED, ABORTED }

/** Per-run task state, including timing and feedback state needed after process death. */
@Serializable
data class RoutineRunTask(
    val source: RoutineTask,
    val status: RunTaskStatus = RunTaskStatus.PENDING,
    val startedAtEpochMillis: Long? = null,
    val finishedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    val actualDurationMillis: Long? = null,
    val timerFeedbackAtEpochMillis: Long? = null,
    val pausedAtEpochMillis: Long? = null,
    val taskNudgeCount: Long = 0,
    val activeSubtaskId: RoutineSubtaskId? = null,
    val subtaskAdvancements: List<SubtaskAdvancement> = emptyList(),
)

/** Immutable snapshot of an active or completed routine execution. */
@Serializable
data class RoutineRun(
    val id: RoutineRunId,
    val routineId: RoutineId,
    val routineTitle: String,
    val tasks: List<RoutineRunTask>,
    val currentTaskIndex: Int,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val status: RunStatus = RunStatus.ACTIVE,
    val finishConfirmationRequested: Boolean = false,
    val abortConfirmationRequested: Boolean = false,
    val confirmationStartedAtEpochMillis: Long? = null,
    val taskBeforeFinishConfirmation: RoutineRunTask? = null,
    val routineSoundEnabled: Boolean = true,
    val soundSettings: SoundSettings = defaultSoundSettings(),
    val routineVibrateEnabled: Boolean = true,
)

/** Append-only history record used to derive progress without mutable counters. */
@Serializable
data class CompletionEvent(
    val runId: RoutineRunId,
    val routineId: RoutineId,
    val routineTitle: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val status: RunStatus,
    val tasks: List<RoutineRunTask>,
)

/** User-level behavior and appearance preferences that may roam in a future adapter. */
@Serializable
data class UserPreferences(
    val selectedTheme: String = "system",
    val continueTimerPastZero: Boolean = true,
    val vibrationIntensity: Float = 1f,
    val screenTransitionsEnabled: Boolean = true,
    val bubbleOnMinimize: Boolean = false,
    val showSubtaskTimeRemaining: Boolean = false,
) {
    init {
        require(vibrationIntensity in 0f..1f)
    }
}

private const val MAX_SAFE_DURATION_SECONDS: Long = Long.MAX_VALUE / 1_000L
