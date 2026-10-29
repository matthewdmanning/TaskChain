package com.taskchain.features.execution

import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRunRepository

import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import com.taskchain.features.execution.domain.RoutineRunEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/** Checks recovery ordering at the active-run and append-only history boundary. */
class RunRecoveryTest {
    /** Use this function to verify that a terminal session is appended before its active file is cleared. */
    @Test
    fun recoversTerminalRunBeforeClearingIt() = runBlocking {
        val engine = RoutineRunEngine()
        val terminal = completedRun(engine)
        val active = FakeActiveRunRepository(terminal)
        val history = FakeCompletionRepository(active, terminal)

        recoverTerminalSession(active, history, engine)

        assertEquals(1, history.events.size)
        assertNull(active.current)
        assertEquals(1, active.clearCount)
    }

    /** Use this function to verify an active persisted run remains available for the runner. */
    @Test
    fun ignoresActiveRun() = runBlocking {
        val engine = RoutineRunEngine()
        val active = FakeActiveRunRepository(startedRun(engine))
        val history = FakeCompletionRepository(active, active.current!!)

        recoverTerminalSession(active, history, engine)

        assertEquals(RunStatus.ACTIVE, active.current?.status)
        assertEquals(0, history.events.size)
        assertEquals(0, active.clearCount)
    }

    /** Use this function to verify aborting never creates history or clears the persisted run. */
    @Test
    fun ignoresAbortedRun() = runBlocking {
        val engine = RoutineRunEngine()
        val active = FakeActiveRunRepository(abortedRun(engine))
        val history = FakeCompletionRepository(active, active.current!!)

        recoverTerminalSession(active, history, engine)

        assertEquals(RunStatus.ABORTED, active.current?.status)
        assertEquals(0, history.events.size)
        assertEquals(0, active.clearCount)
    }

    /** Use this function to verify a failed history append leaves the completed run available for retry. */
    @Test
    fun failedAppendKeepsCompletedRun() = runBlocking {
        val engine = RoutineRunEngine()
        val terminal = completedRun(engine)
        val active = FakeActiveRunRepository(terminal)
        val history = FakeCompletionRepository(active, terminal, failAppend = true)

        try {
            recoverTerminalSession(active, history, engine)
            fail("history append should fail")
        } catch (_: IllegalStateException) {
        }

        assertEquals(terminal, active.current)
        assertEquals(0, active.clearCount)
    }

    /** Use this function to verify retry after clear failure does not append the same completed event twice. */
    @Test
    fun retryAfterClearFailureDoesNotDuplicateHistory() = runBlocking {
        val engine = RoutineRunEngine()
        val terminal = completedRun(engine)
        val active = FakeActiveRunRepository(terminal, failFirstClear = true)
        val history = FakeCompletionRepository(active, terminal)

        runCatching { recoverTerminalSession(active, history, engine) }
        recoverTerminalSession(active, history, engine)

        assertEquals(1, history.events.size)
        assertEquals(1, history.appendCount)
        assertNull(active.current)
        assertEquals(2, active.clearCount)
    }

    /** Use this function to create the smallest active run used by recovery tests. Inputs: `engine`; dependencies: run engine and routine snapshot. */
    private fun startedRun(engine: RoutineRunEngine): RoutineRun {
        return engine.start(routine(), RoutineRunId("run"), 1_000)
    }

    /** Use this function to create the completed run used by recovery tests. Inputs: `engine`; dependencies: run engine and routine snapshot. */
    private fun completedRun(engine: RoutineRunEngine): RoutineRun {
        return engine.completeCurrent(startedRun(engine), 2_000)
    }

    /** Use this function to create the aborted run used by recovery tests. Inputs: `engine`; dependencies: run engine and routine snapshot. */
    private fun abortedRun(engine: RoutineRunEngine): RoutineRun {
        return engine.abort(engine.back(startedRun(engine), 1_500), 2_000)
    }

    /** Use this function to provide one valid routine snapshot. Inputs: none; dependencies: domain model identifiers. */
    private fun routine() = RoutineTemplate(
        RoutineId("routine"), EntityMetadata(0, 0), "Morning",
        tasks = listOf(RoutineTask(RoutineTaskId("task"), "Work")),
    )

    /** Fake active-run storage with controllable clear failure. Inputs: initial run and clear behavior; dependencies: repository contract. */
    private class FakeActiveRunRepository(
        initial: RoutineRun?,
        private var failFirstClear: Boolean = false,
    ) : RoutineRunRepository {
        var current: RoutineRun? = initial
        var clearCount = 0
        /** Use this function to expose the fake saved run; inputs none, dependency current. */
        override fun observeActive(): Flow<RoutineRun?> = flowOf(current)
        /** Use this function to replace the fake saved run; input run is its snapshot, dependency current. */
        override suspend fun saveActive(run: RoutineRun) { current = run }
        /** Use this function to simulate clearing after persistence; inputs none, dependencies current and failure controls. */
        override suspend fun clearActive() {
            clearCount++
            if (failFirstClear) {
                failFirstClear = false
                throw IllegalStateException("clear failed")
            }
            current = null
        }
    }

    /** Fake append-only history with ordering and failure controls. Inputs: active repository and run; dependencies: repository contract. */
    private class FakeCompletionRepository(
        private val active: FakeActiveRunRepository,
        private val expected: RoutineRun,
        private val failAppend: Boolean = false,
    ) : CompletionRepository {
        val events = mutableListOf<CompletionEvent>()
        var appendCount = 0
        /** Use this function to expose fake history; inputs none, dependency events. */
        override fun observeAll(): Flow<List<CompletionEvent>> = flowOf(events.toList())
        /** Use this function to check append-before-clear ordering; input event is completed history, dependencies active and failure controls. */
        override suspend fun append(event: CompletionEvent) {
            assertEquals(expected, active.current)
            appendCount++
            if (failAppend) throw IllegalStateException("append failed")
            events += event
        }
    }
}
