package com.taskchain.features.running

import com.taskchain.core.data.BuiltInRoutineLibrary
import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.core.platform.TaskFeedback
import com.taskchain.core.platform.TimerFeedback
import com.taskchain.features.running.domain.NudgeDecision
import com.taskchain.features.running.domain.RoutineRunEngine
import com.taskchain.features.running.domain.RunFeedbackPolicy
import com.taskchain.features.running.ui.RunningEntryViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

/*
 * Public entry of the Running feature. Root wiring imports only this package: the engine name, the nudge rules it
 * binds into neighbor contracts, the bounded runtime and its ViewModel factory. The routes stay in `ui`.
 */

/** Public name of the Running engine that root constructs once and binds into neighbor contracts. */
typealias RunEngine = RoutineRunEngine

/** Use this function to bind Running's nudge decision into a neighbor contract; [RunFeedbackPolicy] owns the rule. */
fun evaluateRunNudge(run: RoutineRun, nowEpochMillis: Long, intervalMillis: Long, emit: Boolean): NudgeDecision =
    RunFeedbackPolicy.evaluateNudge(run, nowEpochMillis, intervalMillis, emit)

/** Use this function to bind Running's next nudge prediction into a neighbor contract; null when none is due. */
fun nextRunNudgeAt(run: RoutineRun, intervalMillis: Long): Long? = RunFeedbackPolicy.nextNudgeAt(run, intervalMillis)

/** Bounded shared runtime used by the runner UI and the Running Android adapters. */
class RunningDependencies(
    /** Local routine definitions used when starting and completing runs. */
    val routines: RoutineRepository,
    /** Persisted active run storage. */
    val activeRun: RoutineRunRepository,
    /** Append-only completion history. */
    val completions: CompletionRepository,
    /** User-level runner preferences. */
    val preferences: UserPreferenceRepository,
    /** Built-in routine fallback used when a saved routine is unavailable. */
    val builtInLibrary: BuiltInRoutineLibrary,
    /** Domain engine for every run transition and time calculation. */
    val runEngine: RoutineRunEngine,
    /** Core reminder scheduling port used after run completion and start. */
    val reminders: ReminderScheduler,
    /** Shared lock for runner and background receiver transitions. */
    val runMutex: Mutex,
    /** Revision incremented after a background receiver saves a run. */
    val runRevision: MutableStateFlow<Long>,
    /** Shared visibility state selecting runner ticks or background alarms. */
    @field:Volatile var runnerVisible: Boolean,
    /** Shared timer-expiry output. */
    val timerFeedback: TimerFeedback,
    /** Shared semantic task feedback output. */
    val taskFeedback: TaskFeedback,
    /** Publishes the presented run through root-injected notification integration. */
    val syncRunNotification: (RoutineRun, Long) -> Unit,
    /** Hands the persisted run and visibility to root-injected background alerts. */
    val syncRunAlerts: (RoutineRun?, Long, Boolean) -> Unit,
    /** Whether the run has exhausted its notification reminder window. */
    val remindersExhausted: (RoutineRun, Long) -> Boolean,
    /** Local hour at which a TaskChain day begins (`home_day_start_hour`); decides a completion's day. */
    val dayStartHour: Int,
    /** Runner nudge interval in milliseconds. */
    val taskNudgeIntervalMillis: Long,
    /** Creates a fresh persisted run identity. */
    val newRunId: () -> RoutineRunId,
    /** Supplies the current wall-clock epoch milliseconds. */
    val now: () -> Long,
    /** Recovers a terminal run after history persistence failed. */
    val recoverTerminalRun: suspend () -> Unit,
) {
    /** Use this function as the root ViewModel factory for routine start and start-conflict state. */
    fun entryViewModel(): RunningEntryViewModel = RunningEntryViewModel(activeRun)

    /** Use this function to observe which routine's run must hold the Run screen, or null; root only navigates. */
    fun runnerScreenRoutineIds(): Flow<RoutineId?> = activeRun.observeActive()
        .map { run -> run?.takeIf(runEngine::requiresRunnerScreen)?.routineId }
        .distinctUntilChanged()
}
