package com.taskchain.data

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.run.RoutineRunEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Checks recovery ordering at the active-run and append-only history boundary. */
class RunRecoveryTest {
    /** Use this function to verify that a terminal session is appended before its active file is cleared. */
    @Test
    fun recoversTerminalRunBeforeClearingIt() = runBlocking {
        val engine = RoutineRunEngine()
        val routine = RoutineTemplate(
            RoutineId("routine"), EntityMetadata(0, 0), "Morning",
            steps = listOf(RoutineStep(RoutineStepId("step"), "Work")),
        )
        val requested = engine.completeCurrent(engine.start(routine, RoutineRunId("run"), 1_000), 2_000)
        val terminal = engine.confirmComplete(requested, 3_000)
        val active = object : RoutineRunRepository {
            var current: RoutineRun? = terminal
            /** Use this function when the recovery check reads the fake active session. */
            override fun observeActive(): Flow<RoutineRun?> = flowOf(current)
            /** Use this function if the recovery check needs to replace its fake active session. */
            override suspend fun saveActive(run: RoutineRun) { current = run }
            /** Use this function after the fake history has accepted the terminal event. */
            override suspend fun clearActive() { current = null }
        }
        val history = object : CompletionRepository {
            val events = mutableListOf<CompletionEvent>()
            /** Use this function when a test consumer observes fake completion history. */
            override fun observeAll(): Flow<List<CompletionEvent>> = flowOf(events.toList())
            /** Use this function to assert recovery appends before clearing the active session. */
            override suspend fun append(event: CompletionEvent) {
                assertEquals(terminal, active.current)
                events += event
            }
        }

        recoverTerminalSession(active, history, engine)

        assertEquals(1, history.events.size)
        assertNull(active.current)
    }
}
