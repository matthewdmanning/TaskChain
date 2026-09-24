package com.taskchain.data

import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.run.RoutineRunEngine
import kotlinx.coroutines.flow.first

/** Use this function before showing the app after a crash interrupts terminal history persistence. */
suspend fun recoverTerminalSession(
    activeRun: RoutineRunRepository,
    completions: CompletionRepository,
    engine: RoutineRunEngine,
) {
    val persisted = activeRun.observeActive().first() ?: return
    if (persisted.status == RunStatus.ACTIVE) return
    completions.append(engine.toCompletionEvent(persisted))
    activeRun.clearActive()
}
