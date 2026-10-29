package com.taskchain.features.execution

import com.taskchain.core.data.BuiltInRoutineLibrary
import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.core.platform.TaskFeedback
import com.taskchain.core.platform.TimerFeedback
import com.taskchain.features.execution.domain.RoutineRunEngine
import com.taskchain.features.execution.platform.RunAlerts
import com.taskchain.features.execution.platform.RunNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

/** Bounded shared runtime used by the runner UI and the execution Android adapters. */
class ExecutionDependencies(
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
    /** Ongoing run notification adapter. */
    val runNotification: RunNotification,
    /** Background run alert adapter. */
    val runAlerts: RunAlerts,
    /** Runner nudge interval in milliseconds. */
    val taskNudgeIntervalMillis: Long,
    /** Untimed-run reminder limit in milliseconds. */
    val untimedReminderLimitMillis: Long,
    /** Creates a fresh persisted run identity. */
    val newRunId: () -> RoutineRunId,
    /** Supplies the current wall-clock epoch milliseconds. */
    val now: () -> Long,
    /** Recovers a terminal run after history persistence failed. */
    val recoverTerminalRun: suspend () -> Unit,
)
