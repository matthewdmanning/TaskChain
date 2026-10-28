package com.taskchain.ui.runner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.AppContainer
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import com.taskchain.domain.model.SoundToken
import com.taskchain.domain.run.RunFeedbackEvent
import com.taskchain.domain.run.RunFeedbackPolicy
import com.taskchain.domain.run.RunNotificationPolicy
import com.taskchain.domain.schedule.ReminderPlanner
import com.taskchain.ui.designsystem.RunnerMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/** Runner UI state with persisted run state plus current wall-clock display time. */
data class RunnerState(
    val run: RoutineRun? = null,
    val nowEpochMillis: Long = 0,
    val finished: Boolean = false,
    val historySaveFailed: Boolean = false,
)

/** Coordinates runner UI and delegates every transition to the RoutineRunEngine. */
class RoutineRunnerViewModel(
    private val container: AppContainer,
    private val routineId: RoutineId,
) : ViewModel() {
    val state = MutableStateFlow(RunnerState(nowEpochMillis = container.now()))
    private val runMutex = container.runMutex
    private var seenRunRevision = container.runRevision.value
    private var foreground = false
    private var foregroundSynchronized = false
    private var initialRunningFeedbackPending = false
    private var transitionsEnabled = true
    private var feedbackIntensity = 1f

    init {
        viewModelScope.launch {
            container.preferences.observe().collect { feedbackIntensity = it.vibrationIntensity }
        }
        viewModelScope.launch {
            runMutex.withLock {
                val active = container.activeRun.observeActive().first()
                val run = active?.takeIf { it.status == RunStatus.ACTIVE && it.routineId == routineId } ?: startRun()
                initialRunningFeedbackPending = active?.let { it.status != RunStatus.ACTIVE || it.routineId != routineId } ?: true
                container.activeRun.saveActive(run)
                state.value = state.value.copy(run = run)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                tick()
            }
        }
        viewModelScope.launch {
            // Equal content means the step, its pause state, and its clock anchors are unchanged, so nothing is reposted.
            state.filter { it.run != null }
                .map { RunNotificationPolicy.content(it.run!!, container.runEngine, it.nowEpochMillis) }
                .distinctUntilChanged()
                .collect { container.runNotification.show(it) }
        }
    }

    /**
     * Use this function once per readiness countdown number (3, 2, 1) before a task starts.
     * It gives one short pulse only while the screen is foreground, the routine and upcoming task allow vibration,
     * and the user's haptic intensity is above zero.
     */
    fun countdownPulse() {
        val run = state.value.run ?: return
        val task = run.tasks.getOrNull(run.currentTaskIndex) ?: return
        if (!foreground) return
        runCatching {
            container.taskFeedback.pulse(
                vibrateEnabled = run.routineVibrateEnabled && task.source.vibrateEnabled,
                intensity = feedbackIntensity,
            )
        }
    }

    /** Use this function when the runner route enters or leaves the foreground lifecycle state. */
    fun setForeground(active: Boolean) {
        if (foreground == active) return
        foreground = active
        foregroundSynchronized = false
    }

    /** Use this function when the route combines the user transition setting with the system animator gate. */
    fun setTransitionsEnabled(enabled: Boolean) {
        transitionsEnabled = enabled
    }

    /** Use this function when Complete is pressed for the displayed task. */
    fun complete() = transition { run, now ->
        val completed = container.runEngine.completeCurrent(run, now)
        val completedMain = completed.tasks.getOrNull(run.currentTaskIndex)?.status == RunTaskStatus.COMPLETED &&
            run.tasks.getOrNull(run.currentTaskIndex)?.status != RunTaskStatus.COMPLETED
        if (transitionsEnabled && foreground && completedMain && completed.currentTaskIndex != run.currentTaskIndex) {
            container.runEngine.prepareNextTask(
                completed,
                now,
                RunnerMotion.completionDurationMillis.toLong() + RunnerMotion.taskReadyTransitionDurationMillis,
            )
        } else {
            completed
        }
    }

    /** Use this function when Skip is pressed for the displayed task. */
    fun skip() = transition { run, now -> container.runEngine.skipCurrent(run, now) }

    /** Use this function when Back is pressed inside the runner. */
    fun back() = transition { run, now -> container.runEngine.back(run, now) }

    /** Use this function for a right swipe to return to the previous task; no-op on the first task. */
    fun goToPreviousTask() = transition { run, now -> container.runEngine.previousTask(run, now) }

    /** Use this function when Pause is pressed for the displayed task. */
    fun pause() = transition { run, now -> container.runEngine.pauseCurrent(run, now) }

    /** Use this function when Resume is pressed for the displayed task. */
    fun resume() = transition { run, now -> container.runEngine.resumeCurrent(run, now) }

    /** Use this function when a confirmation dialog should close without ending the run. */
    fun continueRun() = transition { run, now -> container.runEngine.continueRun(run, now) }

    /** Use this function when the user selects an unfinished task from the finish dialog. */
    fun selectTask(index: Int) = transition { run, now -> container.runEngine.selectTask(run, index, now) }

    /** Use this function when the user confirms a completed run. */
    fun confirmComplete() = finish { run, now -> container.runEngine.confirmComplete(run, now) }

    /** Use this function when the user confirms an aborted run. */
    fun abort() = finish { run, now -> container.runEngine.abort(run, now) }

    /** Use this function if a terminal run remains on disk after history persistence fails. */
    fun retryHistorySave() {
        viewModelScope.launch {
            runMutex.withLock {
                val terminal = state.value.run ?: return@withLock
                if (terminal.status == RunStatus.ACTIVE) return@withLock
                try {
                    container.activeRun.saveActive(terminal)
                    container.recoverTerminalRun()
                    if (terminal.status == RunStatus.COMPLETED) {
                        try {
                            rescheduleRoutineReminderAfterCompletion(
                                terminal.routineId,
                                terminal.endedAtEpochMillis ?: container.now(),
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                        }
                    }
                    state.value = RunnerState(nowEpochMillis = container.now(), run = terminal, finished = true)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    state.value = state.value.copy(historySaveFailed = true)
                }
            }
        }
    }

    /** Use this function on the runner clock cadence to update display and fire zero feedback once. */
    private suspend fun tick() = runMutex.withLock {
        val now = container.now()
        adoptExternalRunChange()
        state.value = state.value.copy(nowEpochMillis = now)
        var run = state.value.run ?: return@withLock
        if (foreground && !foregroundSynchronized) {
            val synchronized = RunFeedbackPolicy.skipMissedNudges(
                run,
                now,
                container.taskNudgeIntervalMillis,
            )
            if (synchronized != run) {
                try {
                    container.activeRun.saveActive(synchronized)
                } catch (_: Exception) {
                    return@withLock
                }
                state.value = state.value.copy(run = synchronized)
                run = synchronized
            }
            foregroundSynchronized = true
            if (initialRunningFeedbackPending) {
                RunFeedbackPolicy.stateEntryEvents(null, run).forEach { fireFeedback(run, it) }
                initialRunningFeedbackPending = false
            }
        }
        if (!foreground || run.status != RunStatus.ACTIVE || run.finishConfirmationRequested || run.abortConfirmationRequested) {
            return@withLock
        }
        val current = run.tasks.getOrNull(run.currentTaskIndex) ?: return@withLock
        if (current.status != RunTaskStatus.PENDING || current.pausedAtEpochMillis != null) return@withLock

        val nudge = RunFeedbackPolicy.evaluateNudge(
            run,
            now,
            container.taskNudgeIntervalMillis,
            // Reminders stop once active time reaches the sum of all task times; buckets are still acknowledged.
            emit = !container.runEngine.isPastPlannedRunTime(run, now),
        )
        if (nudge.run != run) {
            try {
                container.activeRun.saveActive(nudge.run)
            } catch (_: Exception) {
                return@withLock
            }
            state.value = state.value.copy(run = nudge.run)
            run = nudge.run
            if (nudge.shouldFire) {
                fireFeedback(run, RunFeedbackEvent(SoundToken.TaskNudge, run.currentTaskIndex))
            }
        }
        if (container.runEngine.needsTimerFeedback(run, now)) {
            val task = run.tasks[run.currentTaskIndex].source
            val acknowledged = container.runEngine.acknowledgeTimerFeedback(run, now)
            try {
                container.activeRun.saveActive(acknowledged)
            } catch (_: Exception) {
                return@withLock
            }
            state.value = state.value.copy(run = acknowledged)
            if (!foreground) return@withLock
            container.timerFeedback.fire(
                soundEnabled = run.routineSoundEnabled && task.soundEnabled,
                vibrateEnabled = run.routineVibrateEnabled && task.vibrateEnabled,
                soundSettings = run.soundSettings,
                intensity = feedbackIntensity,
            )
        }
    }

    /**
     * Use this function inside the run lock, before reading the run, to adopt a change a notification action saved.
     * A change to a different run is ignored.
     */
    private suspend fun adoptExternalRunChange() {
        val revision = container.runRevision.value
        if (revision == seenRunRevision) return
        seenRunRevision = revision
        val persisted = container.activeRun.observeActive().first() ?: return
        if (persisted.id == state.value.run?.id) state.value = state.value.copy(run = persisted)
    }

    /** Use this function to persist one ordinary active-run transition. */
    private fun transition(change: (RoutineRun, Long) -> RoutineRun) = withCurrentRun { run ->
        val updated = change(run, container.now())
        if (updated.status != RunStatus.ACTIVE) {
            if (persistTerminalRun(updated) && foreground) fireStateFeedback(run, updated)
        } else {
            container.activeRun.saveActive(updated)
            state.value = state.value.copy(run = updated)
            if (updated != run) initialRunningFeedbackPending = false
            if (foreground) fireStateFeedback(run, updated)
        }
    }

    /** Use this function to record a terminal run before clearing active-session storage. */
    private fun finish(change: (RoutineRun, Long) -> RoutineRun) = withCurrentRun { run ->
        persistTerminalRun(change(run, container.now()))
    }

    /**
     * Use this function to run `action` under the run lock only if the run still matches the snapshot the user
     * acted on. A stale action, for example a second tap after the task changed, does nothing.
     */
    private fun withCurrentRun(action: suspend (RoutineRun) -> Unit) {
        val expected = state.value.run ?: return
        viewModelScope.launch {
            runMutex.withLock {
                adoptExternalRunChange()
                val run = state.value.run ?: return@withLock
                if (run.status != RunStatus.ACTIVE || run.currentTaskIndex != expected.currentTaskIndex ||
                    run.tasks.getOrNull(run.currentTaskIndex)?.activeSubtaskId !=
                    expected.tasks.getOrNull(expected.currentTaskIndex)?.activeSubtaskId ||
                    run.finishConfirmationRequested != expected.finishConfirmationRequested ||
                    run.abortConfirmationRequested != expected.abortConfirmationRequested
                ) return@withLock
                action(run)
            }
        }
    }

    /** Use this function to persist a terminal run once before history clear and completion navigation. */
    private suspend fun persistTerminalRun(terminal: RoutineRun): Boolean {
        try {
            container.activeRun.saveActive(terminal)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            state.value = state.value.copy(run = terminal, historySaveFailed = true)
            return false
        }
        state.value = state.value.copy(run = terminal, historySaveFailed = false)
        if (terminal.status == RunStatus.ABORTED) {
            state.value = RunnerState(nowEpochMillis = container.now(), run = terminal, finished = true)
            return true
        }
        try {
            container.completions.append(container.runEngine.toCompletionEvent(terminal))
            container.activeRun.clearActive()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            state.value = state.value.copy(historySaveFailed = true)
            return false
        }
        if (terminal.status == RunStatus.COMPLETED) {
            try {
                rescheduleRoutineReminderAfterCompletion(
                    terminal.routineId,
                    terminal.endedAtEpochMillis ?: container.now(),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
        state.value = RunnerState(nowEpochMillis = container.now(), run = terminal, finished = true)
        return true
    }

    /** Use this function to emit state-entry events after their run transition is durably saved. */
    private fun fireStateFeedback(previous: RoutineRun, updated: RoutineRun) {
        RunFeedbackPolicy.stateEntryEvents(previous, updated).forEach { event -> fireFeedback(updated, event) }
    }

    /** Use this function to pass one semantic event through independent sound and haptic gates. */
    private fun fireFeedback(run: RoutineRun, event: RunFeedbackEvent) {
        if (!foreground) return
        val task = run.tasks.getOrNull(event.taskIndex) ?: return
        val soundEnabled = run.routineSoundEnabled && task.source.soundEnabled && run.soundSettings[event.token].enabled
        val vibrateEnabled = event.token == SoundToken.TaskNudge &&
            run.routineVibrateEnabled && task.source.vibrateEnabled
        runCatching {
            container.taskFeedback.fire(
                token = event.token,
                soundEnabled = soundEnabled,
                vibrateEnabled = vibrateEnabled,
                soundSettings = run.soundSettings,
                intensity = feedbackIntensity,
            )
        }
    }

    /** Use this function after a completed run to resume only the routine's next recurrence. */
    private suspend fun rescheduleRoutineReminderAfterCompletion(routineId: RoutineId, completedAtEpochMillis: Long) {
        val routine = container.routines.get(routineId)
            ?: container.builtInLibrary.load(container.now()).firstOrNull { it.id == routineId }
        val reminderPlan = ReminderPlanner.afterCompletion(routine, routineId, completedAtEpochMillis)
        reminderPlan.cancelRequestCodes.forEach(container.reminders::cancel)
        reminderPlan.requests.forEach(container.reminders::schedule)
    }

    /** Use this function when no compatible active run exists for the requested routine. */
    // Run replacement and resume behavior is a pending decision; see "Open decision" in CONTEXT.md and docs/adr/0001-one-run-file-per-routine.md.
    private suspend fun startRun(): RoutineRun {
        val routine = container.routines.get(routineId)
            ?: container.builtInLibrary.load(container.now()).first { it.id == routineId }
        // Starting the routine answers its reminder, so the delivered reminder notification leaves the shade.
        container.reminders.clearDelivered(ReminderPlanner.routineRequestCode(routineId))
        return container.runEngine.start(routine, container.newRunId(), container.now())
    }

    private companion object {
        const val TICK_MILLIS = 250L
    }
}
