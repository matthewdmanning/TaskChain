package com.taskchain.domain.run

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.SubtaskAdvancement
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunTask
import com.taskchain.domain.model.RoutineSubtaskId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus

/** Owns all deterministic routine-run transitions so UI and persistence remain policy-free. */
class RoutineRunEngine {
    /** Use this function when starting a fresh run from a validated routine snapshot. */
    fun start(routine: RoutineTemplate, runId: RoutineRunId, nowEpochMillis: Long): RoutineRun {
        routine.requireRunnable()
        return RoutineRun(
            id = runId,
            routineId = routine.id,
            routineTitle = routine.title,
            tasks = routine.tasks.mapIndexed { index, task ->
                val snapshot = task.copy(subtasks = task.subtasks.map { it.copy() })
                RoutineRunTask(
                    source = snapshot,
                    startedAtEpochMillis = nowEpochMillis.takeIf { index == 0 },
                    activeSubtaskId = snapshot.subtasks.firstOrNull()?.id,
                )
            },
            currentTaskIndex = 0,
            startedAtEpochMillis = nowEpochMillis,
            routineSoundEnabled = routine.soundEnabled,
            soundSettings = routine.soundSettings,
            routineVibrateEnabled = routine.vibrateEnabled,
        )
    }

    /** Use this function when Complete is pressed for the currently displayed run task. */
    fun completeCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun =
        finishCurrent(run, nowEpochMillis, RunTaskStatus.COMPLETED)

    /** Use this function when Skip is pressed for the currently displayed run task. */
    fun skipCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun =
        finishCurrent(run, nowEpochMillis, RunTaskStatus.SKIPPED)

    /** Use this function to pause the current pending task without losing elapsed time. */
    fun pauseCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(!run.finishConfirmationRequested && !run.abortConfirmationRequested)
        val index = run.currentTaskIndex
        require(index in run.tasks.indices)
        val current = run.tasks[index]
        if (current.status != RunTaskStatus.PENDING || current.pausedAtEpochMillis != null) return run
        return run.copy(tasks = run.tasks.replaceAt(index, current.copy(pausedAtEpochMillis = nowEpochMillis)))
    }

    /** Use this function to resume a paused pending task or explicitly reopen the skipped current task. */
    fun resumeCurrent(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(!run.finishConfirmationRequested && !run.abortConfirmationRequested)
        val index = run.currentTaskIndex
        require(index in run.tasks.indices)
        val current = run.tasks[index]
        if (current.status == RunTaskStatus.SKIPPED) {
            val elapsed = current.actualDurationMillis ?: 0L
            return run.copy(
                tasks = run.tasks.replaceAt(
                    index,
                    current.copy(
                        status = RunTaskStatus.PENDING,
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
        if (current.status != RunTaskStatus.PENDING) return run
        val pausedDuration = (nowEpochMillis - pausedAt).coerceAtLeast(0)
        val startedAt = current.startedAtEpochMillis?.plus(pausedDuration) ?: nowEpochMillis
        return run.copy(
            tasks = run.tasks.replaceAt(
                index,
                current.copy(startedAtEpochMillis = startedAt, pausedAtEpochMillis = null),
            ),
        )
    }

    fun selectTask(run: RoutineRun, index: Int, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(index in run.tasks.indices)
        val currentIndex = run.currentTaskIndex
        var tasks = run.tasks

        if (currentIndex in tasks.indices && currentIndex != index) {
            val departing = tasks[currentIndex]
            if (departing.status == RunTaskStatus.PENDING && departing.startedAtEpochMillis != null && departing.pausedAtEpochMillis == null) {
                tasks = tasks.replaceAt(currentIndex, departing.copy(pausedAtEpochMillis = nowEpochMillis))
            }
        }

        val arriving = tasks[index]
        if (arriving.status == RunTaskStatus.PENDING) {
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
            tasks = tasks.replaceAt(index, updatedArriving)
        }

        return run.copy(
            tasks = tasks,
            currentTaskIndex = index,
            finishConfirmationRequested = false,
            abortConfirmationRequested = false,
            confirmationStartedAtEpochMillis = null,
            taskBeforeFinishConfirmation = null,
        )
    }

    /**
     * Use this function when a right swipe should move forward to the next already finished task.
     * Inputs: `run` — the active routine run; `nowEpochMillis` — the navigation timestamp.
     * Dependencies: `selectTask` and `RunTaskStatus`.
     */
    fun advanceToNextFinishedTask(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentTaskIndex in run.tasks.indices)
        val nextIndex = (run.currentTaskIndex + 1 until run.tasks.size)
            .firstOrNull { run.tasks[it].status != RunTaskStatus.PENDING }
            ?: return run
        return selectTask(run, nextIndex, nowEpochMillis)
    }

    /**
     * Use this function when a newly entered pending task must wait for the ready transition before timing.
     * Inputs: `run` — the active run after completion advanced to its next task; `nowEpochMillis` — the completion time;
     * `delayMillis` — the total presentation delay before the timer starts.
     * Dependencies: `RoutineRun`, `RunStatus`, and `RunTaskStatus`.
     */
    fun prepareNextTask(run: RoutineRun, nowEpochMillis: Long, delayMillis: Long): RoutineRun {
        require(delayMillis >= 0L)
        if (run.status != RunStatus.ACTIVE || run.currentTaskIndex !in run.tasks.indices) return run
        val index = run.currentTaskIndex
        val current = run.tasks[index]
        if (current.status != RunTaskStatus.PENDING || current.startedAtEpochMillis != nowEpochMillis) return run
        return run.copy(
            tasks = run.tasks.replaceAt(
                index,
                current.copy(startedAtEpochMillis = nowEpochMillis + delayMillis),
            ),
        )
    }

    /** Use this function for a swipe to the previous task; the first task stays put and never requests abort. */
    fun previousTask(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentTaskIndex in run.tasks.indices)
        return if (run.currentTaskIndex == 0) run else selectTask(run, run.currentTaskIndex - 1, nowEpochMillis)
    }

    /** Use this function when Back is pressed so the first task can request abort confirmation. */
    fun back(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentTaskIndex in run.tasks.indices)
        return if (run.currentTaskIndex == 0) {
            run.copy(
                finishConfirmationRequested = false,
                abortConfirmationRequested = true,
                confirmationStartedAtEpochMillis = nowEpochMillis,
                taskBeforeFinishConfirmation = null,
            )
        } else {
            selectTask(run, run.currentTaskIndex - 1, nowEpochMillis)
        }
    }

    /** Use this function when a user cancels either run-ending confirmation dialog. */
    fun continueRun(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        val index = run.currentTaskIndex
        require(index in run.tasks.indices)
        val prior = run.taskBeforeFinishConfirmation ?: run.tasks[index]
        val pauseDuration = run.confirmationStartedAtEpochMillis
            ?.let { (nowEpochMillis - it).coerceAtLeast(0) }
            ?: 0
        val resumed = if (prior.status == RunTaskStatus.PENDING && prior.startedAtEpochMillis != null &&
            prior.pausedAtEpochMillis == null
        ) {
            prior.copy(startedAtEpochMillis = prior.startedAtEpochMillis + pauseDuration)
        } else {
            prior
        }
        return run.copy(
            tasks = run.tasks.replaceAt(index, resumed),
            finishConfirmationRequested = false,
            abortConfirmationRequested = false,
            confirmationStartedAtEpochMillis = null,
            taskBeforeFinishConfirmation = null,
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
            taskBeforeFinishConfirmation = null,
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
            taskBeforeFinishConfirmation = null,
        )
    }

    /** Use this function to list tasks that must be surfaced by the finish dialog. */
    fun unfinishedTaskIndexes(run: RoutineRun): List<Int> = run.tasks.mapIndexedNotNull { index, task ->
        index.takeIf { task.status != RunTaskStatus.COMPLETED }
    }

    /** Use this function to derive countdown or overtime from persisted timestamps. */
    fun remainingMillis(run: RoutineRun, nowEpochMillis: Long): Long? {
        require(run.currentTaskIndex in run.tasks.indices)
        val task = run.tasks[run.currentTaskIndex]
        val durationMillis = task.source.durationSeconds?.let(::durationMillis) ?: return null
        if (task.status != RunTaskStatus.PENDING && task.actualDurationMillis == null) return null
        return durationMillis - elapsedMillis(run, nowEpochMillis)
    }

    /**
     * Use this function to derive one main task's active time from its persisted timestamps.
     * Inputs: `run` — the persisted routine run; `nowEpochMillis` — the wall-clock sample.
     * Dependencies: `RoutineRunTask` status, start, pause, confirmation, and actual-duration fields.
     */
    fun elapsedMillis(run: RoutineRun, nowEpochMillis: Long): Long {
        require(run.currentTaskIndex in run.tasks.indices)
        val task = run.tasks[run.currentTaskIndex]
        return elapsedMillis(task, run.confirmationStartedAtEpochMillis, nowEpochMillis)
    }

    /**
     * Use this function to read active time for one subtask while preserving the main task clock.
     * Inputs: `run` — the persisted routine run; `subtaskId` — the subtask to inspect; `nowEpochMillis` — the wall-clock sample.
     * Dependencies: `elapsedMillis`, the subtask definition order, and persisted subtask advancement markers.
     */
    fun subtaskElapsedMillis(run: RoutineRun, subtaskId: RoutineSubtaskId, nowEpochMillis: Long): Long {
        require(run.currentTaskIndex in run.tasks.indices)
        val task = run.tasks[run.currentTaskIndex]
        val subtaskIndex = task.source.subtasks.indexOfFirst { it.id == subtaskId }
        require(subtaskIndex >= 0)
        val marker = task.subtaskAdvancements.firstOrNull { it.subtaskId == subtaskId }
        val activeSubtaskIndex = task.source.subtasks.indexOfFirst { it.id == task.activeSubtaskId }
        if (marker == null && subtaskIndex != activeSubtaskIndex) return 0L
        val previousElapsed = task.source.subtasks.getOrNull(subtaskIndex - 1)?.id
            ?.let { previousId -> task.subtaskAdvancements.firstOrNull { it.subtaskId == previousId }?.elapsedMillis }
            ?: 0L
        val endElapsed = marker?.elapsedMillis ?: elapsedMillis(run, nowEpochMillis)
        return (endElapsed - previousElapsed).coerceAtLeast(0L)
    }

    /**
     * Use this function to derive the active subtask's allowance minus its persisted active time.
     * Inputs: `run` — the persisted routine run; `nowEpochMillis` — the wall-clock sample.
     * Dependencies: `subtaskElapsedMillis` and the current task's active subtask definition.
     */
    fun subtaskRemainingMillis(run: RoutineRun, nowEpochMillis: Long): Long? {
        require(run.currentTaskIndex in run.tasks.indices)
        val task = run.tasks[run.currentTaskIndex]
        val activeSubtask = task.source.subtasks.firstOrNull { it.id == task.activeSubtaskId } ?: return null
        return durationMillis(activeSubtask.durationSeconds) - subtaskElapsedMillis(run, activeSubtask.id, nowEpochMillis)
    }

    /**
     * Use this function to determine whether the main task is past the active subtask's planned schedule.
     * Inputs: `run` — the persisted routine run; `nowEpochMillis` — the wall-clock sample.
     * Dependencies: `elapsedMillis`, active subtask order, and subtask durations.
     */
    fun isBehindSubtaskSchedule(run: RoutineRun, nowEpochMillis: Long): Boolean {
        require(run.currentTaskIndex in run.tasks.indices)
        val task = run.tasks[run.currentTaskIndex]
        val subtaskIndex = task.source.subtasks.indexOfFirst { it.id == task.activeSubtaskId }
        if (subtaskIndex < 0) return false
        var plannedEndSeconds = 0L
        task.source.subtasks.take(subtaskIndex + 1).forEach { subtask -> plannedEndSeconds += subtask.durationSeconds }
        return elapsedMillis(run, nowEpochMillis) > durationMillis(plannedEndSeconds)
    }

    /**
     * Use this function when UI needs the active subtask's ordered position for a run task.
     * Inputs: `task` — the persisted run task containing its subtask snapshot and active identity.
     * Dependencies: `RoutineRunTask.source.subtasks` and `RoutineRunTask.activeSubtaskId`.
     */
    fun activeSubtaskIndex(task: RoutineRunTask): Int =
        task.source.subtasks.indexOfFirst { it.id == task.activeSubtaskId }

    /** Use this function before firing audio and haptics so zero feedback occurs only once. */
    fun needsTimerFeedback(run: RoutineRun, nowEpochMillis: Long): Boolean {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentTaskIndex in run.tasks.indices)
        val task = run.tasks[run.currentTaskIndex]
        return task.status == RunTaskStatus.PENDING &&
            task.source.durationSeconds != null &&
            task.timerFeedbackAtEpochMillis == null &&
            remainingMillis(run, nowEpochMillis)?.let { it <= 0 } == true
    }

    /** Use this function before dispatching timer feedback so the persisted acknowledgement prevents duplicate delivery. */
    fun acknowledgeTimerFeedback(run: RoutineRun, nowEpochMillis: Long): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        require(run.currentTaskIndex in run.tasks.indices)
        val index = run.currentTaskIndex
        val task = run.tasks[index]
        if (task.timerFeedbackAtEpochMillis != null) return run
        return run.copy(tasks = run.tasks.replaceAt(index, task.copy(timerFeedbackAtEpochMillis = nowEpochMillis)))
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
            tasks = run.tasks,
        )
    }

    /** Use this function to apply Complete or Skip and advance without duplicating transition rules. */
    private fun finishCurrent(run: RoutineRun, nowEpochMillis: Long, status: RunTaskStatus): RoutineRun {
        require(run.status == RunStatus.ACTIVE)
        val index = run.currentTaskIndex
        require(index in run.tasks.indices)
        val current = run.tasks[index]
        if (status == RunTaskStatus.COMPLETED && current.status != RunTaskStatus.COMPLETED && current.source.subtasks.isNotEmpty()) {
            val activeIndex = activeSubtaskIndex(current).takeIf { it >= 0 }
                ?: current.source.subtasks.indexOfFirst { subtask -> current.subtaskAdvancements.none { it.subtaskId == subtask.id } }
            require(activeIndex in current.source.subtasks.indices)
            val activeSubtask = current.source.subtasks[activeIndex]
            val advanced = current.copy(
                activeSubtaskId = current.source.subtasks.getOrNull(activeIndex + 1)?.id,
                subtaskAdvancements = current.subtaskAdvancements + SubtaskAdvancement(
                    subtaskId = activeSubtask.id,
                    elapsedMillis = elapsedMillis(run, nowEpochMillis),
                    atEpochMillis = nowEpochMillis,
                ),
            )
            val withAdvancement = run.copy(tasks = run.tasks.replaceAt(index, advanced))
            if (activeIndex < current.source.subtasks.lastIndex) return withAdvancement
            return finishMainTask(withAdvancement, nowEpochMillis, status, confirmationTask = current)
        }
        return finishMainTask(run, nowEpochMillis, status)
    }

    /**
     * Use this function to apply one main-task terminal transition after subtask handling.
     * Inputs: `run` — the run with any subtask marker already recorded; `nowEpochMillis` — transition time;
     * `status` — the requested main-task terminal status; `confirmationTask` — the pre-transition snapshot to restore
     * if final completion opens confirmation.
     * Dependencies: `unfinishedTaskIndexes`, `selectTask`, and persisted main-task timestamps.
     */
    private fun finishMainTask(
        run: RoutineRun,
        nowEpochMillis: Long,
        status: RunTaskStatus,
        confirmationTask: RoutineRunTask? = null,
    ): RoutineRun {
        val index = run.currentTaskIndex
        val current = run.tasks[index]
        val nextStatus = if (current.status == RunTaskStatus.COMPLETED && status == RunTaskStatus.SKIPPED) {
            RunTaskStatus.COMPLETED
        } else {
            status
        }
        val effectiveEnd = current.pausedAtEpochMillis ?: nowEpochMillis
        val elapsed = current.startedAtEpochMillis?.let { (effectiveEnd - it).coerceAtLeast(0) }
        val finished = current.copy(
            status = nextStatus,
            finishedAtEpochMillis = current.finishedAtEpochMillis ?: nowEpochMillis,
            completedAtEpochMillis = if (nextStatus == RunTaskStatus.COMPLETED && current.status != RunTaskStatus.COMPLETED)
                nowEpochMillis else current.completedAtEpochMillis,
            actualDurationMillis = current.actualDurationMillis ?: elapsed,
            pausedAtEpochMillis = null,
        )
        val updated = run.copy(tasks = run.tasks.replaceAt(index, finished))
        if (index == run.tasks.lastIndex) {
            if (unfinishedTaskIndexes(updated).isEmpty()) {
                return updated.copy(
                    status = RunStatus.COMPLETED,
                    endedAtEpochMillis = nowEpochMillis,
                    finishConfirmationRequested = false,
                    abortConfirmationRequested = false,
                    confirmationStartedAtEpochMillis = null,
                    taskBeforeFinishConfirmation = null,
                )
            }
            return updated.copy(
                finishConfirmationRequested = true,
                abortConfirmationRequested = false,
                confirmationStartedAtEpochMillis = nowEpochMillis,
                taskBeforeFinishConfirmation = confirmationTask ?: current,
            )
        }
        return selectTask(updated, index + 1, nowEpochMillis)
    }

    /**
     * Use this function to convert a validated positive duration into milliseconds.
     * Inputs: `seconds` — a duration bounded by `RoutineTemplate.requireRunnable`.
     * Dependencies: the engine's millisecond-per-second constant.
     */
    private fun durationMillis(seconds: Long): Long = seconds * MILLIS_PER_SECOND

    /**
     * Use this function to derive one task's active time from persisted lifecycle fields.
     * Inputs: `task` — the run task; `confirmationStartedAtEpochMillis` — an optional confirmation freeze;
     * `nowEpochMillis` — the wall-clock sample.
     * Dependencies: task status, start, pause, and actual-duration fields.
     */
    private fun elapsedMillis(
        task: RoutineRunTask,
        confirmationStartedAtEpochMillis: Long?,
        nowEpochMillis: Long,
    ): Long {
        if (task.status != RunTaskStatus.PENDING) return task.actualDurationMillis ?: 0L
        val startedAt = task.startedAtEpochMillis ?: return 0L
        val effectiveNow = task.pausedAtEpochMillis ?: confirmationStartedAtEpochMillis ?: nowEpochMillis
        return (effectiveNow - startedAt).coerceAtLeast(0L)
    }

    /** Use this function to immutably replace one run task without a mutable collection. */
    private fun List<RoutineRunTask>.replaceAt(index: Int, value: RoutineRunTask): List<RoutineRunTask> =
        toMutableList().also { it[index] = value }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
