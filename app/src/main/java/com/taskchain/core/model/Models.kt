package com.taskchain.core.model

import kotlinx.serialization.Serializable

/** Milliseconds in one second, shared by the run engine and its presentation. */
const val MILLIS_PER_SECOND = 1_000L

/** Stable routine identity that remains valid across file moves and future storage adapters. */
@Serializable
@JvmInline
value class RoutineId(val value: String)

/** Stable identity for one task inside a routine snapshot. */
@Serializable
@JvmInline
value class RoutineTaskId(val value: String)

/** Stable identity for one run of a routine. */
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
    /** Content URI of a device sound that replaces the routine's nudge and timer-expiry sounds for this task. */
    val soundUri: String? = null,
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
    fun requireRunnable(nowEpochMillis: Long? = null): RoutineTemplate = apply {
        require(validationIssues(nowEpochMillis).isEmpty())
    }
}

/** Durable run status for one task in a run snapshot. */
@Serializable
enum class RunTaskStatus { PENDING, COMPLETED, SKIPPED }

/** Durable lifecycle status for a routine run. */
@Serializable
enum class RunStatus { ACTIVE, COMPLETED, ABORTED, SKIPPED, SKIP_UNDONE }

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

/** Immutable snapshot of an active or completed routine run. */
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
    /** Time of the latest navigation, pause, resume, skip or complete; null until the first one. */
    val lastInteractionAtEpochMillis: Long? = null,
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
    /** Persisted UI component implementation, independent of light/dark appearance. */
    val selectedUiProvider: String = "cyberpunk",
    val continueTimerPastZero: Boolean = true,
    val vibrationIntensity: Float = 1f,
    val screenTransitionsEnabled: Boolean = true,
    val showSubtaskTimeRemaining: Boolean = false,
    /** Keeps the display awake on the Run screen while a run is active. */
    val keepScreenOnDuringRun: Boolean = true,
    /** Device sound the user picked per semantic token; a missing token keeps the built-in sound. */
    val defaultSoundUris: Map<SoundToken, String> = emptyMap(),
) {
    init {
        require(vibrationIntensity in 0f..1f)
    }
}
