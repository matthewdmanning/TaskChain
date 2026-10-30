package com.taskchain.features.running

import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.features.running.domain.RoutineRunEngine

/** Use this function before showing the app after a crash interrupts terminal history persistence. */
suspend fun recoverTerminalSession(
    activeRun: RoutineRunRepository,
    completions: CompletionRepository,
    engine: RoutineRunEngine,
) {
    // Recovery only records history and never plans a reminder, so no day start applies.
    TerminalRunWriter(activeRun, completions, engine, dayStartHour = 0).recoverPersisted()
}
