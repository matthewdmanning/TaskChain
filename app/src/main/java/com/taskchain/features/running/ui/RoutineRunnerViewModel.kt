package com.taskchain.features.running.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.core.model.FeedbackGates
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.core.model.SoundToken
import com.taskchain.core.model.withTaskSound
import com.taskchain.core.model.withUserDefaults
import com.taskchain.features.running.domain.RunFeedbackEvent
import com.taskchain.features.running.domain.RunFeedbackPolicy
import com.taskchain.features.running.domain.RunReadiness
import com.taskchain.features.running.domain.RunTiming
import com.taskchain.features.running.RunningDependencies
import com.taskchain.features.running.TerminalRunWriter
import com.taskchain.core.schedule.ReminderPlanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    private val dependencies: RunningDependencies,
    private val routineId: RoutineId,
) : ViewModel() {
    val state = MutableStateFlow(RunnerState(nowEpochMillis = dependencies.now()))
    private val runMutex = dependencies.runMutex
    private var seenRunRevision = dependencies.runRevision.value
    private var foreground = false
    private var foregroundSynchronized = false
    private var initialRunningFeedbackPending = false
    private var transitionsEnabled = true
    private var feedbackIntensity = 1f
    private val terminalWriter = TerminalRunWriter(
        activeRun = dependencies.activeRun,
        completions = dependencies.completions,
        engine = dependencies.runEngine,
        reminders = dependencies.reminders,
        findRoutine = { id ->
            dependencies.routines.get(id)
                ?: dependencies.builtInLibrary.load(dependencies.now()).firstOrNull { it.id == id }
        },
        now = dependencies.now,
    )

    init {
        viewModelScope.launch {
            dependencies.preferences.observe().collect { feedbackIntensity = it.vibrationIntensity }
        }
        viewModelScope.launch {
            runMutex.withLock {
                val active = dependencies.activeRun.observeActive().first()
                val run = active?.takeIf { it.status == RunStatus.ACTIVE && it.routineId == routineId } ?: startRun()
                initialRunningFeedbackPending = active?.let { it.status != RunStatus.ACTIVE || it.routineId != routineId } ?: true
                dependencies.activeRun.saveActive(run)
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
            state.filter { it.run != null }.collect {
                dependencies.syncRunNotification(it.run!!, it.nowEpochMillis)
            }
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
            dependencies.taskFeedback.pulse(
                vibrateEnabled = FeedbackGates.vibrateEnabled(run, task.source),
                intensity = feedbackIntensity,
            )
        }
    }

    /** Use this function when the runner route enters or leaves the foreground lifecycle state. */
    fun setForeground(active: Boolean) {
        if (foreground == active) return
        foreground = active
        foregroundSynchronized = false
        dependencies.runnerVisible = active
        // Hiding the screen hands reminders and timer expiry to an alarm; showing it hands them back.
        viewModelScope.launch {
            runMutex.withLock {
                dependencies.syncRunAlerts(dependencies.activeRun.observeActive().first(), dependencies.now(), active)
            }
        }
    }

    /** Use this function when the route combines the user transition setting with the system animator gate. */
    fun setTransitionsEnabled(enabled: Boolean) {
        transitionsEnabled = enabled
    }

    /**
     * Use this function to read the timer values for the run the route presents, which can be a held completion
     * snapshot rather than [RunnerState.run]. Inputs: `run` — the presented run; `nowEpochMillis` — the sampled time.
     */
    fun timingFor(run: RoutineRun?, nowEpochMillis: Long): RunTiming =
        run?.let { dependencies.runEngine.timing(it, nowEpochMillis) } ?: RunTiming.None

    /** Use this function when Complete is pressed for the displayed task. */
    fun complete() = transition { run, now ->
        dependencies.runEngine.completeCurrentWithReadiness(
            run,
            now,
            RunReadiness.startDelayMillis(presented = transitionsEnabled && foreground),
        )
    }

    /** Use this function when Skip is pressed for the displayed task. */
    fun skip() = transition { run, now -> dependencies.runEngine.skipCurrent(run, now) }

    /** Use this function when Back is pressed inside the runner. */
    fun back() = transition { run, now -> dependencies.runEngine.back(run, now) }

    /** Use this function for system Back: it closes an open confirmation, otherwise it applies [back]. */
    fun systemBack() = transition { run, now -> dependencies.runEngine.systemBack(run, now) }

    /** Use this function for a right swipe to return to the previous task; no-op on the first task. */
    fun goToPreviousTask() = transition { run, now -> dependencies.runEngine.previousTask(run, now) }

    /** Use this function when Pause is pressed for the displayed task. */
    fun pause() = transition { run, now -> dependencies.runEngine.pauseCurrent(run, now) }

    /** Use this function when Resume is pressed for the displayed task. */
    fun resume() = transition { run, now -> dependencies.runEngine.resumeCurrent(run, now) }

    /** Use this function when a confirmation dialog should close without ending the run. */
    fun continueRun() = transition { run, now -> dependencies.runEngine.continueRun(run, now) }

    /** Use this function when the user selects an unfinished task from the finish dialog. */
    fun selectTask(index: Int) = transition { run, now -> dependencies.runEngine.selectTask(run, index, now) }

    /** Use this function when the user confirms a completed run. */
    fun confirmComplete() = finish { run, now -> dependencies.runEngine.confirmComplete(run, now) }

    /** Use this function when the user confirms an aborted run. */
    fun abort() = finish { run, now -> dependencies.runEngine.abort(run, now) }

    /** Use this function if a terminal run remains on disk after history persistence fails. */
    fun retryHistorySave() {
        viewModelScope.launch {
            runMutex.withLock {
                val terminal = state.value.run ?: return@withLock
                if (terminal.status == RunStatus.ACTIVE) return@withLock
                persistTerminalRun(terminal)
            }
        }
    }

    /** Use this function on the runner clock cadence to update display and fire zero feedback once. */
    private suspend fun tick() = runMutex.withLock {
        val now = dependencies.now()
        adoptExternalRunChange()
        state.value = state.value.copy(nowEpochMillis = now)
        var run = state.value.run ?: return@withLock
        if (foreground && !foregroundSynchronized) {
            val synchronized = RunFeedbackPolicy.skipMissedNudges(
                run,
                now,
                dependencies.taskNudgeIntervalMillis,
            )
            if (synchronized != run) {
                try {
                    dependencies.activeRun.saveActive(synchronized)
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
            dependencies.taskNudgeIntervalMillis,
            // Reminders stop at the run's planned time, or after the idle limit for runs without timers.
            emit = !dependencies.remindersExhausted(run, now),
        )
        if (nudge.run != run) {
            try {
                dependencies.activeRun.saveActive(nudge.run)
            } catch (_: Exception) {
                return@withLock
            }
            state.value = state.value.copy(run = nudge.run)
            run = nudge.run
            if (nudge.shouldFire) {
                fireFeedback(run, RunFeedbackEvent(SoundToken.TaskNudge, run.currentTaskIndex))
            }
        }
        if (dependencies.runEngine.needsTimerFeedback(run, now)) {
            val task = run.tasks[run.currentTaskIndex].source
            val acknowledged = dependencies.runEngine.acknowledgeTimerFeedback(run, now)
            try {
                dependencies.activeRun.saveActive(acknowledged)
            } catch (_: Exception) {
                return@withLock
            }
            state.value = state.value.copy(run = acknowledged)
            if (!foreground) return@withLock
            dependencies.timerFeedback.fire(
                soundEnabled = FeedbackGates.soundEnabled(run, task, SoundToken.TimerExpired),
                vibrateEnabled = FeedbackGates.vibrateEnabled(run, task),
                soundSettings = run.soundSettings.withTaskSound(task.soundUri),
                intensity = feedbackIntensity,
            )
        }
    }

    /**
     * Use this function inside the run lock, before reading the run, to adopt a change a notification action saved.
     * A change to a different run is ignored.
     */
    private suspend fun adoptExternalRunChange() {
        val revision = dependencies.runRevision.value
        if (revision == seenRunRevision) return
        seenRunRevision = revision
        val persisted = dependencies.activeRun.observeActive().first() ?: return
        if (persisted.id == state.value.run?.id) state.value = state.value.copy(run = persisted)
    }

    /** Use this function to persist one ordinary active-run transition. */
    private fun transition(change: (RoutineRun, Long) -> RoutineRun) = withCurrentRun { run ->
        val updated = change(run, dependencies.now())
        if (updated.status != RunStatus.ACTIVE) {
            if (persistTerminalRun(updated) && foreground) fireStateFeedback(run, updated)
        } else {
            dependencies.activeRun.saveActive(updated)
            state.value = state.value.copy(run = updated)
            if (updated != run) initialRunningFeedbackPending = false
            if (foreground) fireStateFeedback(run, updated)
        }
    }

    /** Use this function to record a terminal run before clearing active-session storage. */
    private fun finish(change: (RoutineRun, Long) -> RoutineRun) = withCurrentRun { run ->
        persistTerminalRun(change(run, dependencies.now()))
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

    /**
     * Use this function to persist a terminal run, then show completion; on a storage failure the terminal run stays
     * presented with a retry. Also used by [retryHistorySave].
     */
    private suspend fun persistTerminalRun(terminal: RoutineRun): Boolean {
        try {
            terminalWriter.write(terminal)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            state.value = state.value.copy(run = terminal, historySaveFailed = true)
            return false
        }
        state.value = RunnerState(nowEpochMillis = dependencies.now(), run = terminal, finished = true)
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
        val soundEnabled = FeedbackGates.soundEnabled(run, task.source, event.token)
        val vibrateEnabled = event.token == SoundToken.TaskNudge && FeedbackGates.vibrateEnabled(run, task.source)
        runCatching {
            dependencies.taskFeedback.fire(
                token = event.token,
                soundEnabled = soundEnabled,
                vibrateEnabled = vibrateEnabled,
                soundSettings = run.soundSettings.withTaskSound(task.source.soundUri),
                intensity = feedbackIntensity,
            )
        }
    }

    /** Use this function when no compatible active run exists for the requested routine. */
    // Run replacement and resume behavior is a pending decision; see "Open decision" in CONTEXT.md and docs/adr/0001-one-run-file-per-routine.md.
    private suspend fun startRun(): RoutineRun {
        val routine = dependencies.routines.get(routineId)
            ?: dependencies.builtInLibrary.load(dependencies.now()).first { it.id == routineId }
        // Starting the routine answers its reminder, so the delivered reminder notification leaves the shade.
        dependencies.reminders.clearDelivered(ReminderPlanner.routineRequestCode(routineId))
        val defaults = dependencies.preferences.observe().first().defaultSoundUris
        return dependencies.runEngine.start(
            routine.copy(soundSettings = routine.soundSettings.withUserDefaults(defaults)),
            dependencies.newRunId(),
            dependencies.now(),
        )
    }

    private companion object {
        const val TICK_MILLIS = 250L
    }
}
