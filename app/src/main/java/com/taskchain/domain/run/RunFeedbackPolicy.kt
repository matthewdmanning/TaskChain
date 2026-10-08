package com.taskchain.domain.run

import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import com.taskchain.domain.model.SoundToken

/** Represents one semantic feedback event and the run task that caused it. */
data class RunFeedbackEvent(
    val token: SoundToken,
    val taskIndex: Int,
)

/** Contains the persisted nudge bucket and whether the current tick should emit it. */
data class NudgeDecision(
    val run: RoutineRun,
    val shouldFire: Boolean,
)

/**
 * Owns run feedback state-entry detection and active-time nudge bucket policy.
 * Constructor inputs: none. Dependencies: persisted `RoutineRun` timestamps and statuses.
 */
object RunFeedbackPolicy {
    /**
     * Use this function after a run transition to identify state-entry feedback exactly once.
     * Inputs: `previous` — persisted run before the transition; `updated` — persisted run after the transition.
     * Dependencies: `RoutineRun` task statuses and pause/confirmation state.
     */
    fun stateEntryEvents(previous: RoutineRun?, updated: RoutineRun): List<RunFeedbackEvent> {
        if (updated.status != RunStatus.ACTIVE) {
            return previous?.let { subtaskAdvancedEvents(it, updated) + completedEvents(it, updated) } ?: emptyList()
        }
        if (previous == null) {
            val current = updated.tasks.getOrNull(updated.currentTaskIndex)
            return if (
                current?.status == RunTaskStatus.PENDING &&
                current.pausedAtEpochMillis == null &&
                !updated.finishConfirmationRequested &&
                !updated.abortConfirmationRequested
            ) {
                listOf(RunFeedbackEvent(SoundToken.TaskRunning, updated.currentTaskIndex))
            } else {
                emptyList()
            }
        }

        val events = (subtaskAdvancedEvents(previous, updated) + completedEvents(previous, updated)).toMutableList()
        val previousTask = previous.tasks.getOrNull(previous.currentTaskIndex)
        val updatedTask = updated.tasks.getOrNull(updated.currentTaskIndex)
        val enteredPendingTask = updatedTask?.status == RunTaskStatus.PENDING &&
            updatedTask.pausedAtEpochMillis == null &&
            !updated.finishConfirmationRequested && !updated.abortConfirmationRequested && (
            previous.currentTaskIndex != updated.currentTaskIndex ||
                previousTask?.status != RunTaskStatus.PENDING ||
                previousTask?.pausedAtEpochMillis != null
            )
        if (enteredPendingTask) {
            events += RunFeedbackEvent(SoundToken.TaskRunning, updated.currentTaskIndex)
        } else if (
            previous.currentTaskIndex == updated.currentTaskIndex &&
            previousTask?.pausedAtEpochMillis == null &&
            updatedTask?.pausedAtEpochMillis != null
        ) {
            events += RunFeedbackEvent(SoundToken.TaskPaused, updated.currentTaskIndex)
        }
        return events
    }

    /**
     * Use this function on the existing runner tick to acknowledge one active-time nudge bucket.
     * Inputs: `run` — persisted run snapshot; `nowEpochMillis` — wall-clock time; `intervalMillis` — nudge cadence;
     * `emit` — whether a newly due bucket should produce feedback or only be skipped on foreground resume.
     * Dependencies: current-task timestamps, pause state, confirmation state, and persisted nudge count.
     */
    fun evaluateNudge(
        run: RoutineRun,
        nowEpochMillis: Long,
        intervalMillis: Long,
        emit: Boolean,
    ): NudgeDecision {
        require(intervalMillis > 0)
        if (run.status != RunStatus.ACTIVE) {
            return NudgeDecision(run, shouldFire = false)
        }
        val index = run.currentTaskIndex
        val task = run.tasks.getOrNull(index) ?: return NudgeDecision(run, shouldFire = false)
        if (task.status != RunTaskStatus.PENDING) {
            return NudgeDecision(run, shouldFire = false)
        }
        if (emit && (run.finishConfirmationRequested || run.abortConfirmationRequested || task.pausedAtEpochMillis != null)) {
            return NudgeDecision(run, shouldFire = false)
        }
        val startedAt = task.startedAtEpochMillis ?: return NudgeDecision(run, shouldFire = false)
        val effectiveNow = task.pausedAtEpochMillis ?: run.confirmationStartedAtEpochMillis ?: nowEpochMillis
        val activeElapsedMillis = (effectiveNow - startedAt).coerceAtLeast(0L)
        val dueBucket = activeElapsedMillis / intervalMillis
        if (dueBucket <= task.taskNudgeCount) return NudgeDecision(run, shouldFire = false)
        val acknowledged = run.copy(
            tasks = run.tasks.toMutableList().also {
                it[index] = task.copy(taskNudgeCount = dueBucket)
            },
        )
        return NudgeDecision(acknowledged, shouldFire = emit)
    }

    /**
     * Use this function when entering the foreground to discard missed background buckets without catch-up sound.
     * Inputs: `run` — persisted run snapshot; `nowEpochMillis` — wall-clock time; `intervalMillis` — nudge cadence.
     * Dependencies: `evaluateNudge` and persisted task timestamps.
     */
    fun skipMissedNudges(run: RoutineRun, nowEpochMillis: Long, intervalMillis: Long): RoutineRun =
        evaluateNudge(run, nowEpochMillis, intervalMillis, emit = false).run

    /**
     * Use this function to report tasks that became completed during one transition.
     * Inputs: `previous` — run before the transition; `updated` — run after the transition.
     * Dependencies: `RoutineRunTask.status` and `SoundToken.TaskCompleted`.
     */
    private fun completedEvents(previous: RoutineRun, updated: RoutineRun): List<RunFeedbackEvent> =
        updated.tasks.mapIndexedNotNull { index, task ->
            val prior = previous.tasks.getOrNull(index)
            if (prior?.status != RunTaskStatus.COMPLETED && task.status == RunTaskStatus.COMPLETED) {
                RunFeedbackEvent(SoundToken.TaskCompleted, index)
            } else {
                null
            }
        }

    /**
     * Use this function after a run transition to detect newly persisted manual subtask advancements.
     * Inputs: `previous` — run before the transition; `updated` — run after the transition.
     * Dependencies: `RoutineRunTask.subtaskAdvancements` and `SoundToken.SubtaskAdvanced`.
     */
    private fun subtaskAdvancedEvents(previous: RoutineRun, updated: RoutineRun): List<RunFeedbackEvent> =
        updated.tasks.mapIndexedNotNull { index, task ->
            val priorCount = previous.tasks.getOrNull(index)?.subtaskAdvancements?.size ?: 0
            if (task.subtaskAdvancements.size > priorCount) {
                RunFeedbackEvent(SoundToken.SubtaskAdvanced, index)
            } else {
                null
            }
        }
}
