package com.taskchain.domain.run

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunStep
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
                RoutineRunStep(
                    source = step,
                    startedAtEpochMillis = nowEpochMillis.takeIf { index == 0 },
                )
            },
            currentStepIndex = 0,
            startedAtEpochMillis = nowEpochMillis,
        )
    }

    /** Use this function when Complete is pressed for the currently displayed run step. */
    fun completeCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun =
        finishCurrent(run, nowEpochMillis, RunStepStatus.COMPLETED)

    /** Use this function when Skip is pressed for the currently displayed run step. */
    fun skipCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun =
        finishCurrent(run, nowEpochMillis, RunStepStatus.SKIPPED)

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
        val resumed = if (prior.status == RunStepStatus.PENDING && prior.startedAtEpochMillis != null) {
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
        val seconds = step.source.timerSeconds ?: return null
        val startedAt = step.startedAtEpochMillis ?: return seconds * MILLIS_PER_SECOND
        if (step.status != RunStepStatus.PENDING) return step.actualDurationMillis?.let { seconds * MILLIS_PER_SECOND - it }
        val effectiveNow = run.confirmationStartedAtEpochMillis
            ?: step.pausedAtEpochMillis
            ?: nowEpochMillis
        return seconds * MILLIS_PER_SECOND - (effectiveNow - startedAt)
    }

    /** Use this function before firing audio and haptics so zero feedback occurs only once. */
    fun needsTimerFeedback(run: RoutineRun, nowEpochMillis: Long): Boolean {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentStepIndex in run.steps.indices)
        val step = run.steps[run.currentStepIndex]
        return step.status == RunStepStatus.PENDING &&
            step.source.timerSeconds != null &&
            step.timerFeedbackAtEpochMillis == null &&
            remainingMillis(run, nowEpochMillis)?.let { it <= 0 } == true
    }

    /** Use this function immediately after platform timer feedback succeeds. */
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
            return updated.copy(
                finishConfirmationRequested = true,
                abortConfirmationRequested = false,
                confirmationStartedAtEpochMillis = nowEpochMillis,
                stepBeforeFinishConfirmation = current,
            )
        }
        return selectStep(updated, index + 1, nowEpochMillis)
    }

    /** Use this function to immutably replace one run step without a mutable collection. */
    private fun List<RoutineRunStep>.replaceAt(index: Int, value: RoutineRunStep): List<RoutineRunStep> =
        toMutableList().also { it[index] = value }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
