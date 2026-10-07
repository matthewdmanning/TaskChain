package com.taskchain.domain.run

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.CueAdvancement
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunStep
import com.taskchain.domain.model.RoutineCueId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus

/** Owns all deterministic routine-run transitions so UI and persistence remain policy-free. */
class RoutineRunEngine {
    /** Use this function when starting a fresh run from a validated routine snapshot. */
    fun start(routine: RoutineTemplate, runId: RoutineRunId, nowEpochMillis: Long): RoutineRun {
        routine.requireRunnable()
        return RoutineRun(
            id = runId,
            routineId = routine.id,
            routineTitle = routine.title,
            steps = routine.steps.mapIndexed { index, step ->
                val snapshot = step.copy(cues = step.cues.map { it.copy() })
                RoutineRunStep(
                    source = snapshot,
                    startedAtEpochMillis = nowEpochMillis.takeIf { index == 0 },
                    activeCueId = snapshot.cues.firstOrNull()?.id,
                )
            },
            currentStepIndex = 0,
            startedAtEpochMillis = nowEpochMillis,
            routineSoundEnabled = routine.soundEnabled,
            soundSettings = routine.soundSettings,
            routineVibrateEnabled = routine.vibrateEnabled,
        )
    }

    /** Use this function when Complete is pressed for the currently displayed run step. */
    fun completeCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun =
        finishCurrent(run, nowEpochMillis, RunStepStatus.COMPLETED)

    /** Use this function when Skip is pressed for the currently displayed run step. */
    fun skipCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun =
        finishCurrent(run, nowEpochMillis, RunStepStatus.SKIPPED)

    /** Use this function to pause the current pending step without losing elapsed time. */
    fun pauseCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(!run.finishConfirmationRequested && !run.abortConfirmationRequested)
        val index = run.currentStepIndex
        require(index in run.steps.indices)
        val current = run.steps[index]
        if (current.status != RunStepStatus.PENDING || current.pausedAtEpochMillis != null) return run
        return run.copy(steps = run.steps.replaceAt(index, current.copy(pausedAtEpochMillis = nowEpochMillis)))
    }

    /** Use this function to resume a paused pending step or explicitly reopen the skipped current step. */
    fun resumeCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(!run.finishConfirmationRequested && !run.abortConfirmationRequested)
        val index = run.currentStepIndex
        require(index in run.steps.indices)
        val current = run.steps[index]
        if (current.status == RunStepStatus.SKIPPED) {
            val elapsed = current.actualDurationMillis ?: 0L
            return run.copy(
                steps = run.steps.replaceAt(
                    index,
                    current.copy(
                        status = RunStepStatus.PENDING,
                        startedAtEpochMillis = nowEpochMillis - elapsed,
                        finishedAtEpochMillis = null,
                        completedAtEpochMillis = null,
                        actualDurationMillis = null,
                        pausedAtEpochMillis = null,
                    ),
                ),
            )
        }
        val pausedAt = current.pausedAtEpochMillis ?: return run
        if (current.status != RunStepStatus.PENDING) return run
        val pausedDuration = (nowEpochMillis - pausedAt).coerceAtLeast(0)
        val startedAt = current.startedAtEpochMillis?.plus(pausedDuration) ?: nowEpochMillis
        return run.copy(
            steps = run.steps.replaceAt(
                index,
                current.copy(startedAtEpochMillis = startedAt, pausedAtEpochMillis = null),
            ),
        )
    }

    fun selectStep(run: RoutineRun, index: Int, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(index in run.steps.indices)
        val currentIndex = run.currentStepIndex
        var steps = run.steps

        if (currentIndex in steps.indices && currentIndex != index) {
            val departing = steps[currentIndex]
            if (departing.status == RunStepStatus.PENDING && departing.startedAtEpochMillis != null && departing.pausedAtEpochMillis == null) {
                steps = steps.replaceAt(currentIndex, departing.copy(pausedAtEpochMillis = nowEpochMillis))
            }
        }

        val arriving = steps[index]
        if (arriving.status == RunStepStatus.PENDING) {
            val updatedArriving = if (arriving.startedAtEpochMillis == null) {
                arriving.copy(startedAtEpochMillis = nowEpochMillis, pausedAtEpochMillis = null)
            } else if (arriving.pausedAtEpochMillis != null) {
                val pauseDuration = (nowEpochMillis - arriving.pausedAtEpochMillis).coerceAtLeast(0)
                arriving.copy(
                    startedAtEpochMillis = (arriving.startedAtEpochMillis) + pauseDuration,
                    pausedAtEpochMillis = null,
                )
            } else {
                arriving
            }
            steps = steps.replaceAt(index, updatedArriving)
        }

        return run.copy(
            steps = steps,
            currentStepIndex = index,
            finishConfirmationRequested = false,
            abortConfirmationRequested = false,
            confirmationStartedAtEpochMillis = null,
            stepBeforeFinishConfirmation = null,
        )
    }

    /**
     * Use this function when a right swipe should move forward to the next already finished task.
     * Inputs: `run` — the active routine run; `nowEpochMillis` — the navigation timestamp.
     * Dependencies: `selectStep` and `RunStepStatus`.
     */
    fun advanceToNextFinishedStep(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentStepIndex in run.steps.indices)
        val nextIndex = (run.currentStepIndex + 1 until run.steps.size)
            .firstOrNull { run.steps[it].status != RunStepStatus.PENDING }
            ?: return run
        return selectStep(run, nextIndex, nowEpochMillis)
    }

    /**
     * Use this function when a newly entered pending task must wait for the ready transition before timing.
     * Inputs: `run` — the active run after completion advanced to its next task; `nowEpochMillis` — the completion time;
     * `delayMillis` — the total presentation delay before the timer starts.
     * Dependencies: `RoutineRun`, `RunStatus`, and `RunStepStatus`.
     */
    fun prepareNextStep(run: RoutineRun, nowEpochMillis: Long, delayMillis: Long): RoutineRun {
        require(delayMillis >= 0L)
        if (run.status != RunStatus.ACTIVE || run.currentStepIndex !in run.steps.indices) return run
        val index = run.currentStepIndex
        val current = run.steps[index]
        if (current.status != RunStepStatus.PENDING || current.startedAtEpochMillis != nowEpochMillis) return run
        return run.copy(
            steps = run.steps.replaceAt(
                index,
                current.copy(startedAtEpochMillis = nowEpochMillis + delayMillis),
            ),
        )
    }

    /** Use this function when Back is pressed so the first step can request abort confirmation. */
    fun back(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentStepIndex in run.steps.indices)
        return if (run.currentStepIndex == 0) {
            run.copy(
                finishConfirmationRequested = false,
                abortConfirmationRequested = true,
                confirmationStartedAtEpochMillis = nowEpochMillis,
                stepBeforeFinishConfirmation = null,
            )
        } else {
            selectStep(run, run.currentStepIndex - 1, nowEpochMillis)
        }
    }

    /** Use this function when a user cancels either run-ending confirmation dialog. */
    fun continueRun(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        val index = run.currentStepIndex
        require(index in run.steps.indices)
        val prior = run.stepBeforeFinishConfirmation ?: run.steps[index]
        val pauseDuration = run.confirmationStartedAtEpochMillis
            ?.let { (nowEpochMillis - it).coerceAtLeast(0) }
            ?: 0
        val resumed = if (prior.status == RunStepStatus.PENDING && prior.startedAtEpochMillis != null &&
            prior.pausedAtEpochMillis == null
        ) {
            prior.copy(startedAtEpochMillis = prior.startedAtEpochMillis + pauseDuration)
        } else {
            prior
        }
        return run.copy(
            steps = run.steps.replaceAt(index, resumed),
            finishConfirmationRequested = false,
            abortConfirmationRequested = false,
            confirmationStartedAtEpochMillis = null,
            stepBeforeFinishConfirmation = null,
        )
    }

    /** Use this function after the finish dialog is confirmed, even if skipped tasks remain. */
    fun confirmComplete(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.finishConfirmationRequested)
        return run.copy(
            status = RunStatus.COMPLETED,
            endedAtEpochMillis = nowEpochMillis,
            finishConfirmationRequested = false,
            abortConfirmationRequested = false,
            confirmationStartedAtEpochMillis = null,
            stepBeforeFinishConfirmation = null,
        )
    }

    /** Use this function after the abort dialog is confirmed. */
    fun abort(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.abortConfirmationRequested)
        return run.copy(
            status = RunStatus.ABORTED,
            endedAtEpochMillis = nowEpochMillis,
            finishConfirmationRequested = false,
            abortConfirmationRequested = false,
            confirmationStartedAtEpochMillis = null,
            stepBeforeFinishConfirmation = null,
        )
    }

    /** Use this function to list tasks that must be surfaced by the finish dialog. */
    fun unfinishedStepIndexes(run: RoutineRun): List<Int> = run.steps.mapIndexedNotNull { index, step ->
        index.takeIf { step.status != RunStepStatus.COMPLETED }
    }

    /** Use this function to derive countdown or overtime from persisted timestamps. */
    fun remainingMillis(run: RoutineRun, nowEpochMillis: Long): Long? {
        require(run.currentStepIndex in run.steps.indices)
        val step = run.steps[run.currentStepIndex]
        val durationMillis = step.source.durationSeconds?.let(::durationMillis) ?: return null
        if (step.status != RunStepStatus.PENDING && step.actualDurationMillis == null) return null
        return durationMillis - elapsedMillis(run, nowEpochMillis)
    }

    /**
     * Use this function to derive one main step's active time from its persisted timestamps.
     * Inputs: `run` — the persisted routine run; `nowEpochMillis` — the wall-clock sample.
     * Dependencies: `RoutineRunStep` status, start, pause, confirmation, and actual-duration fields.
     */
    fun elapsedMillis(run: RoutineRun, nowEpochMillis: Long): Long {
        require(run.currentStepIndex in run.steps.indices)
        val step = run.steps[run.currentStepIndex]
        return elapsedMillis(step, run.confirmationStartedAtEpochMillis, nowEpochMillis)
    }

    /**
     * Use this function to read active time for one cue while preserving the main step clock.
     * Inputs: `run` — the persisted routine run; `cueId` — the cue to inspect; `nowEpochMillis` — the wall-clock sample.
     * Dependencies: `elapsedMillis`, the cue definition order, and persisted cue advancement markers.
     */
    fun cueElapsedMillis(run: RoutineRun, cueId: RoutineCueId, nowEpochMillis: Long): Long {
        require(run.currentStepIndex in run.steps.indices)
        val step = run.steps[run.currentStepIndex]
        val cueIndex = step.source.cues.indexOfFirst { it.id == cueId }
        require(cueIndex >= 0)
        val marker = step.cueAdvancements.firstOrNull { it.cueId == cueId }
        val activeCueIndex = step.source.cues.indexOfFirst { it.id == step.activeCueId }
        if (marker == null && cueIndex != activeCueIndex) return 0L
        val previousElapsed = step.source.cues.getOrNull(cueIndex - 1)?.id
            ?.let { previousId -> step.cueAdvancements.firstOrNull { it.cueId == previousId }?.elapsedMillis }
            ?: 0L
        val endElapsed = marker?.elapsedMillis ?: elapsedMillis(run, nowEpochMillis)
        return (endElapsed - previousElapsed).coerceAtLeast(0L)
    }

    /**
     * Use this function to derive the active cue's allowance minus its persisted active time.
     * Inputs: `run` — the persisted routine run; `nowEpochMillis` — the wall-clock sample.
     * Dependencies: `cueElapsedMillis` and the current step's active cue definition.
     */
    fun cueRemainingMillis(run: RoutineRun, nowEpochMillis: Long): Long? {
        require(run.currentStepIndex in run.steps.indices)
        val step = run.steps[run.currentStepIndex]
        val activeCue = step.source.cues.firstOrNull { it.id == step.activeCueId } ?: return null
        return durationMillis(activeCue.durationSeconds) - cueElapsedMillis(run, activeCue.id, nowEpochMillis)
    }

    /**
     * Use this function to determine whether the main step is past the active cue's planned schedule.
     * Inputs: `run` — the persisted routine run; `nowEpochMillis` — the wall-clock sample.
     * Dependencies: `elapsedMillis`, active cue order, and cue durations.
     */
    fun isBehindCueSchedule(run: RoutineRun, nowEpochMillis: Long): Boolean {
        require(run.currentStepIndex in run.steps.indices)
        val step = run.steps[run.currentStepIndex]
        val cueIndex = step.source.cues.indexOfFirst { it.id == step.activeCueId }
        if (cueIndex < 0) return false
        var plannedEndSeconds = 0L
        step.source.cues.take(cueIndex + 1).forEach { cue -> plannedEndSeconds += cue.durationSeconds }
        return elapsedMillis(run, nowEpochMillis) > durationMillis(plannedEndSeconds)
    }

    /**
     * Use this function when UI needs the active cue's ordered position for a run step.
     * Inputs: `step` — the persisted run step containing its cue snapshot and active identity.
     * Dependencies: `RoutineRunStep.source.cues` and `RoutineRunStep.activeCueId`.
     */
    fun activeCueIndex(step: RoutineRunStep): Int =
        step.source.cues.indexOfFirst { it.id == step.activeCueId }

    /** Use this function before firing audio and haptics so zero feedback occurs only once. */
    fun needsTimerFeedback(run: RoutineRun, nowEpochMillis: Long): Boolean {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentStepIndex in run.steps.indices)
        val step = run.steps[run.currentStepIndex]
        return step.status == RunStepStatus.PENDING &&
            step.source.durationSeconds != null &&
            step.timerFeedbackAtEpochMillis == null &&
            remainingMillis(run, nowEpochMillis)?.let { it <= 0 } == true
    }

    /** Use this function before dispatching timer feedback so the persisted acknowledgement prevents duplicate delivery. */
    fun acknowledgeTimerFeedback(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentStepIndex in run.steps.indices)
        val index = run.currentStepIndex
        val step = run.steps[index]
        if (step.timerFeedbackAtEpochMillis != null) return run
        return run.copy(steps = run.steps.replaceAt(index, step.copy(timerFeedbackAtEpochMillis = nowEpochMillis)))
    }

    /** Use this function when persisting a terminal run to append-only progress history. */
    fun toCompletionEvent(run: RoutineRun): CompletionEvent {
        require(run.status != RunStatus.ACTIVE)
        return CompletionEvent(
            runId = run.id,
            routineId = run.routineId,
            routineTitle = run.routineTitle,
            startedAtEpochMillis = run.startedAtEpochMillis,
            endedAtEpochMillis = requireNotNull(run.endedAtEpochMillis),
            status = run.status,
            steps = run.steps,
        )
    }

    /** Use this function to apply Complete or Skip and advance without duplicating transition rules. */
    private fun finishCurrent(run: RoutineRun, nowEpochMillis: Long, status: RunStepStatus): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        val index = run.currentStepIndex
        require(index in run.steps.indices)
        val current = run.steps[index]
        if (status == RunStepStatus.COMPLETED && current.status != RunStepStatus.COMPLETED && current.source.cues.isNotEmpty()) {
            val activeIndex = activeCueIndex(current).takeIf { it >= 0 }
                ?: current.source.cues.indexOfFirst { cue -> current.cueAdvancements.none { it.cueId == cue.id } }
            require(activeIndex in current.source.cues.indices)
            val activeCue = current.source.cues[activeIndex]
            val advanced = current.copy(
                activeCueId = current.source.cues.getOrNull(activeIndex + 1)?.id,
                cueAdvancements = current.cueAdvancements + CueAdvancement(
                    cueId = activeCue.id,
                    elapsedMillis = elapsedMillis(run, nowEpochMillis),
                    atEpochMillis = nowEpochMillis,
                ),
            )
            val withAdvancement = run.copy(steps = run.steps.replaceAt(index, advanced))
            if (activeIndex < current.source.cues.lastIndex) return withAdvancement
            return finishMainStep(withAdvancement, nowEpochMillis, status, confirmationStep = current)
        }
        return finishMainStep(run, nowEpochMillis, status)
    }

    /**
     * Use this function to apply one main-step terminal transition after cue handling.
     * Inputs: `run` — the run with any cue marker already recorded; `nowEpochMillis` — transition time;
     * `status` — the requested main-step terminal status; `confirmationStep` — the pre-transition snapshot to restore
     * if final completion opens confirmation.
     * Dependencies: `unfinishedStepIndexes`, `selectStep`, and persisted main-step timestamps.
     */
    private fun finishMainStep(
        run: RoutineRun,
        nowEpochMillis: Long,
        status: RunStepStatus,
        confirmationStep: RoutineRunStep? = null,
    ): RoutineRun {
        val index = run.currentStepIndex
        val current = run.steps[index]
        val nextStatus = if (current.status == RunStepStatus.COMPLETED && status == RunStepStatus.SKIPPED) {
            RunStepStatus.COMPLETED
        } else {
            status
        }
        val effectiveEnd = current.pausedAtEpochMillis ?: nowEpochMillis
        val elapsed = current.startedAtEpochMillis?.let { (effectiveEnd - it).coerceAtLeast(0) }
        val finished = current.copy(
            status = nextStatus,
            finishedAtEpochMillis = current.finishedAtEpochMillis ?: nowEpochMillis,
            completedAtEpochMillis = if (nextStatus == RunStepStatus.COMPLETED && current.status != RunStepStatus.COMPLETED)
                nowEpochMillis else current.completedAtEpochMillis,
            actualDurationMillis = current.actualDurationMillis ?: elapsed,
            pausedAtEpochMillis = null,
        )
        val updated = run.copy(steps = run.steps.replaceAt(index, finished))
        if (index == run.steps.lastIndex) {
            if (unfinishedStepIndexes(updated).isEmpty()) {
                return updated.copy(
                    status = RunStatus.COMPLETED,
                    endedAtEpochMillis = nowEpochMillis,
                    finishConfirmationRequested = false,
                    abortConfirmationRequested = false,
                    confirmationStartedAtEpochMillis = null,
                    stepBeforeFinishConfirmation = null,
                )
            }
            return updated.copy(
                finishConfirmationRequested = true,
                abortConfirmationRequested = false,
                confirmationStartedAtEpochMillis = nowEpochMillis,
                stepBeforeFinishConfirmation = confirmationStep ?: current,
            )
        }
        return selectStep(updated, index + 1, nowEpochMillis)
    }

    /**
     * Use this function to convert a validated positive duration into milliseconds.
     * Inputs: `seconds` — a duration bounded by `RoutineTemplate.requireRunnable`.
     * Dependencies: the engine's millisecond-per-second constant.
     */
    private fun durationMillis(seconds: Long): Long = seconds * MILLIS_PER_SECOND

    /**
     * Use this function to derive one step's active time from persisted lifecycle fields.
     * Inputs: `step` — the run step; `confirmationStartedAtEpochMillis` — an optional confirmation freeze;
     * `nowEpochMillis` — the wall-clock sample.
     * Dependencies: step status, start, pause, and actual-duration fields.
     */
    private fun elapsedMillis(
        step: RoutineRunStep,
        confirmationStartedAtEpochMillis: Long?,
        nowEpochMillis: Long,
    ): Long {
        if (step.status != RunStepStatus.PENDING) return step.actualDurationMillis ?: 0L
        val startedAt = step.startedAtEpochMillis ?: return 0L
        val effectiveNow = step.pausedAtEpochMillis ?: confirmationStartedAtEpochMillis ?: nowEpochMillis
        return (effectiveNow - startedAt).coerceAtLeast(0L)
    }

    /** Use this function to immutably replace one run step without a mutable collection. */
    private fun List<RoutineRunStep>.replaceAt(index: Int, value: RoutineRunStep): List<RoutineRunStep> =
        toMutableList().also { it[index] = value }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
