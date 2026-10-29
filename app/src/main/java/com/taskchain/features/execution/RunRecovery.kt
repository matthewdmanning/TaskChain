package com.taskchain.features.execution

import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRunRepository

import com.taskchain.core.model.RunStatus
import com.taskchain.features.execution.domain.RoutineRunEngine
import kotlinx.coroutines.flow.first

/** Use this function before showing the app after a crash interrupts terminal history persistence. */
suspend fun recoverTerminalSession(
    activeRun: RoutineRunRepository,
    completions: CompletionRepository,
    engine: RoutineRunEngine,
) {
    val persisted = activeRun.observeActive().first() ?: return
    if (persisted.status != RunStatus.COMPLETED) return
    if (completions.observeAll().first().none { it.runId == persisted.id }) {
        completions.append(engine.toCompletionEvent(persisted))
    }
    activeRun.clearActive()
}
