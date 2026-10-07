package com.taskchain.domain.model

import kotlinx.serialization.Serializable

/** Stable routine identity that remains valid across file moves and future storage adapters. */
@Serializable
@JvmInline
value class RoutineId(val value: String)

/** Stable identity for one step inside a routine snapshot. */
@Serializable
@JvmInline
value class RoutineStepId(val value: String)

/** Stable identity for one execution of a routine. */
@Serializable
@JvmInline
value class RoutineRunId(val value: String)

/** Stable identity for one ordered cue nested within a routine step. */
@Serializable
@JvmInline
value class RoutineCueId(val value: String)

/** A manually advanced cue that partitions one main routine step. */
@Serializable
data class RoutineCue(
    val id: RoutineCueId,
    val title: String,
    val durationSeconds: Long,
)

/** Records one manual cue advancement using cumulative active time on its main step. */
@Serializable
data class CueAdvancement(
    val cueId: RoutineCueId,
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
data class RoutineStep(
    val id: RoutineStepId,
    val title: String,
    val timerSeconds: Long? = null,
    val cues: List<RoutineCue> = emptyList(),
    val stackingAnchorStepId: RoutineStepId? = null,
    val deadlineEpochMillis: Long? = null,
    val reminderAtEpochMillis: Long? = null,
    val schedule: ScheduleRule? = null,
    val remindEveryMinutes: Int? = null,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
) {
    /** Total configured duration in seconds, using cue duration when cues are present. */
    val durationSeconds: Long?
        get() = if (cues.isEmpty()) timerSeconds else cues.fold(0L) { total, cue ->
            if (total > Long.MAX_VALUE - cue.durationSeconds) Long.MAX_VALUE
            else total + cue.durationSeconds
        }
}

/** A reusable ordered routine definition edited by the builder. */
@Serializable
data class RoutineTemplate(
    val id: RoutineId,
    val metadata: EntityMetadata,
    val title: String,
    val description: String = "",
    val steps: List<RoutineStep>,
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
        require(steps.isNotEmpty())
        require(steps.all { it.title.isNotBlank() })
        require(steps.all { it.id.value.isNotBlank() })
        require(steps.map { it.id }.distinct().size == steps.size)
        require(steps.flatMap { it.cues }.map { it.id }.distinct().size == steps.sumOf { it.cues.size })
        require(steps.all { step ->
            if (step.cues.isEmpty()) {
                step.timerSeconds == null || step.timerSeconds in 1..MAX_SAFE_DURATION_SECONDS
            } else {
                var total = 0L
                step.cues.isNotEmpty() && step.cues.all { cue ->
                    val valid = cue.id.value.isNotBlank() && cue.title.isNotBlank() &&
                        cue.durationSeconds > 0 && total <= MAX_SAFE_DURATION_SECONDS - cue.durationSeconds
                    if (valid) total += cue.durationSeconds
                    valid
                } && step.cues.map { it.id }.distinct().size == step.cues.size
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

/** Durable execution status for one step in a run snapshot. */
@Serializable
enum class RunStepStatus { PENDING, COMPLETED, SKIPPED }

/** Durable lifecycle status for a routine run. */
@Serializable
enum class RunStatus { ACTIVE, COMPLETED, ABORTED }

/** Per-run step state, including timing and feedback state needed after process death. */
@Serializable
data class RoutineRunStep(
    val source: RoutineStep,
    val status: RunStepStatus = RunStepStatus.PENDING,
    val startedAtEpochMillis: Long? = null,
    val finishedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    val actualDurationMillis: Long? = null,
    val timerFeedbackAtEpochMillis: Long? = null,
    val pausedAtEpochMillis: Long? = null,
    val taskNudgeCount: Long = 0,
    val activeCueId: RoutineCueId? = null,
    val cueAdvancements: List<CueAdvancement> = emptyList(),
)

/** Immutable snapshot of an active or completed routine execution. */
@Serializable
data class RoutineRun(
    val id: RoutineRunId,
    val routineId: RoutineId,
    val routineTitle: String,
    val steps: List<RoutineRunStep>,
    val currentStepIndex: Int,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val status: RunStatus = RunStatus.ACTIVE,
    val finishConfirmationRequested: Boolean = false,
    val abortConfirmationRequested: Boolean = false,
    val confirmationStartedAtEpochMillis: Long? = null,
    val stepBeforeFinishConfirmation: RoutineRunStep? = null,
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
    val steps: List<RoutineRunStep>,
)

/** User-level behavior and appearance preferences that may roam in a future adapter. */
@Serializable
data class UserPreferences(
    val selectedTheme: String = "system",
    val continueTimerPastZero: Boolean = true,
    val vibrationIntensity: Float = 1f,
    val screenTransitionsEnabled: Boolean = true,
    val bubbleOnMinimize: Boolean = false,
    val showCueTimeRemaining: Boolean = false,
) {
    init {
        require(vibrationIntensity in 0f..1f)
    }
}

private const val MAX_SAFE_DURATION_SECONDS: Long = Long.MAX_VALUE / 1_000L
