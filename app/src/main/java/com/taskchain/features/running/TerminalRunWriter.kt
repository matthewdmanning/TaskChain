package com.taskchain.features.running

import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.core.schedule.ReminderPlanner
import com.taskchain.features.running.domain.RoutineRunEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Persists a terminal run: it saves the terminal snapshot, appends its history event once, clears the active run,
 * and resumes a completed routine's next reminder. An Aborted run is only saved; it never enters history.
 * Storage failures propagate so the caller can offer a retry; a failed reminder reschedule never fails the write.
 * Constructor inputs: `activeRun`, `completions` — Core storage; `engine` — history event mapping;
 * `dayStartHour` — the day start (`home_day_start_hour`) that decides which day a completion belongs to;
 * `reminders` — reminder port, or null when only history recovery runs; `findRoutine` — the routine definition used
 * for the next reminder; `now` — wall-clock epoch milliseconds.
 */
class TerminalRunWriter(
    private val activeRun: RoutineRunRepository,
    private val completions: CompletionRepository,
    private val engine: RoutineRunEngine,
    private val dayStartHour: Int,
    private val reminders: ReminderScheduler? = null,
    private val findRoutine: suspend (RoutineId) -> RoutineTemplate? = { null },
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Use this function when a transition ends the run, and again when the user retries a failed write.
     * Inputs: `terminal` — a run that is no longer active.
     */
    suspend fun write(terminal: RoutineRun) {
        require(terminal.status != RunStatus.ACTIVE)
        activeRun.saveActive(terminal)
        if (terminal.status == RunStatus.ABORTED) return
        recordHistory(terminal)
        if (terminal.status == RunStatus.COMPLETED) rescheduleAfterCompletion(terminal)
    }

    /** Use this function before showing the app, after a crash may have interrupted history persistence. */
    suspend fun recoverPersisted() {
        val persisted = activeRun.observeActive().first() ?: return
        if (persisted.status != RunStatus.COMPLETED) return
        recordHistory(persisted)
    }

    /** Use this function to append the run's history event unless a retry already did, then clear the active run. */
    private suspend fun recordHistory(terminal: RoutineRun) {
        if (completions.observeAll().first().none { it.runId == terminal.id }) {
            completions.append(engine.toCompletionEvent(terminal))
        }
        activeRun.clearActive()
    }

    /** Use this function after a completed run to resume only the routine's next recurrence, best effort. */
    private suspend fun rescheduleAfterCompletion(terminal: RoutineRun) {
        val scheduler = reminders ?: return
        try {
            val routine = findRoutine(terminal.routineId)
            val plan = ReminderPlanner.afterCompletion(
                routine,
                terminal.routineId,
                terminal.endedAtEpochMillis ?: now(),
                dayStartHour,
            )
            plan.cancelRequestCodes.forEach(scheduler::cancel)
            plan.requests.forEach(scheduler::schedule)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // A reminder failure must not undo or block the completed run.
        }
    }
}
