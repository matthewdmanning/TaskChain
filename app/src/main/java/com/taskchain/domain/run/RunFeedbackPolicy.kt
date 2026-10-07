package com.taskchain.domain.run

import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
import com.taskchain.domain.model.SoundToken

/** Represents one semantic feedback event and the run step that caused it. */
data class RunFeedbackEvent(
    val token: SoundToken,
    val stepIndex: Int,
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
     * Dependencies: `RoutineRun` step statuses and pause/confirmation state.
     */
    fun stateEntryEvents(previous: RoutineRun?, updated: RoutineRun): List<RunFeedbackEvent> {
        if (updated.status != RunStatus.ACTIVE) {
            return previous?.let { cueAdvancedEvents(it, updated) + completedEvents(it, updated) } ?: emptyList()
        }
        if (previous == null) {
            val current = updated.steps.getOrNull(updated.currentStepIndex)
            return if (
                current?.status == RunStepStatus.PENDING &&
                current.pausedAtEpochMillis == null &&
                !updated.finishConfirmationRequested &&
                !updated.abortConfirmationRequested
            ) {
                listOf(RunFeedbackEvent(SoundToken.TaskRunning, updated.currentStepIndex))
            } else {
                emptyList()
            }
        }

        val events = (cueAdvancedEvents(previous, updated) + completedEvents(previous, updated)).toMutableList()
        val previousStep = previous.steps.getOrNull(previous.currentStepIndex)
        val updatedStep = updated.steps.getOrNull(updated.currentStepIndex)
        val enteredPendingStep = updatedStep?.status == RunStepStatus.PENDING &&
            updatedStep.pausedAtEpochMillis == null &&
            !updated.finishConfirmationRequested && !updated.abortConfirmationRequested && (
            previous.currentStepIndex != updated.currentStepIndex ||
                previousStep?.status != RunStepStatus.PENDING ||
                previousStep?.pausedAtEpochMillis != null
            )
        if (enteredPendingStep) {
            events += RunFeedbackEvent(SoundToken.TaskRunning, updated.currentStepIndex)
        } else if (
            previous.currentStepIndex == updated.currentStepIndex &&
            previousStep?.pausedAtEpochMillis == null &&
            updatedStep?.pausedAtEpochMillis != null
        ) {
            events += RunFeedbackEvent(SoundToken.TaskPaused, updated.currentStepIndex)
        }
        return events
    }

    /**
     * Use this function on the existing runner tick to acknowledge one active-time nudge bucket.
     * Inputs: `run` — persisted run snapshot; `nowEpochMillis` — wall-clock time; `intervalMillis` — nudge cadence;
     * `emit` — whether a newly due bucket should produce feedback or only be skipped on foreground resume.
     * Dependencies: current-step timestamps, pause state, confirmation state, and persisted nudge count.
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
        val index = run.currentStepIndex
        val step = run.steps.getOrNull(index) ?: return NudgeDecision(run, shouldFire = false)
        if (step.status != RunStepStatus.PENDING) {
            return NudgeDecision(run, shouldFire = false)
        }
        if (emit && (run.finishConfirmationRequested || run.abortConfirmationRequested || step.pausedAtEpochMillis != null)) {
            return NudgeDecision(run, shouldFire = false)
        }
        val startedAt = step.startedAtEpochMillis ?: return NudgeDecision(run, shouldFire = false)
        val effectiveNow = step.pausedAtEpochMillis ?: run.confirmationStartedAtEpochMillis ?: nowEpochMillis
        val activeElapsedMillis = (effectiveNow - startedAt).coerceAtLeast(0L)
        val dueBucket = activeElapsedMillis / intervalMillis
        if (dueBucket <= step.taskNudgeCount) return NudgeDecision(run, shouldFire = false)
        val acknowledged = run.copy(
            steps = run.steps.toMutableList().also {
                it[index] = step.copy(taskNudgeCount = dueBucket)
            },
        )
        return NudgeDecision(acknowledged, shouldFire = emit)
    }

    /**
     * Use this function when entering the foreground to discard missed background buckets without catch-up sound.
     * Inputs: `run` — persisted run snapshot; `nowEpochMillis` — wall-clock time; `intervalMillis` — nudge cadence.
     * Dependencies: `evaluateNudge` and persisted step timestamps.
     */
    fun skipMissedNudges(run: RoutineRun, nowEpochMillis: Long, intervalMillis: Long): RoutineRun =
        evaluateNudge(run, nowEpochMillis, intervalMillis, emit = false).run

    /**
     * Use this function to report steps that became completed during one transition.
     * Inputs: `previous` — run before the transition; `updated` — run after the transition.
     * Dependencies: `RoutineRunStep.status` and `SoundToken.TaskCompleted`.
     */
    private fun completedEvents(previous: RoutineRun, updated: RoutineRun): List<RunFeedbackEvent> =
        updated.steps.mapIndexedNotNull { index, step ->
            val prior = previous.steps.getOrNull(index)
            if (prior?.status != RunStepStatus.COMPLETED && step.status == RunStepStatus.COMPLETED) {
                RunFeedbackEvent(SoundToken.TaskCompleted, index)
            } else {
                null
            }
        }

    /**
     * Use this function after a run transition to detect newly persisted manual cue advancements.
     * Inputs: `previous` — run before the transition; `updated` — run after the transition.
     * Dependencies: `RoutineRunStep.cueAdvancements` and `SoundToken.CueAdvanced`.
     */
    private fun cueAdvancedEvents(previous: RoutineRun, updated: RoutineRun): List<RunFeedbackEvent> =
        updated.steps.mapIndexedNotNull { index, step ->
            val priorCount = previous.steps.getOrNull(index)?.cueAdvancements?.size ?: 0
            if (step.cueAdvancements.size > priorCount) {
                RunFeedbackEvent(SoundToken.CueAdvanced, index)
            } else {
                null
            }
        }
}
