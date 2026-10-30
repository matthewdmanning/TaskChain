package com.taskchain.features.running

import com.taskchain.core.data.BuiltInRoutineLibrary
import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.core.platform.TaskFeedback
import com.taskchain.core.platform.TimerFeedback
import com.taskchain.features.running.domain.RoutineRunEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

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
    /** Runner nudge interval in milliseconds. */
    val taskNudgeIntervalMillis: Long,
    /** Creates a fresh persisted run identity. */
    val newRunId: () -> RoutineRunId,
    /** Supplies the current wall-clock epoch milliseconds. */
    val now: () -> Long,
    /** Recovers a terminal run after history persistence failed. */
    val recoverTerminalRun: suspend () -> Unit,
)
