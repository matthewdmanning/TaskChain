package com.taskchain.features.running

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
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.model.ScheduleRule
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.core.schedule.ReminderPlanner
import com.taskchain.core.schedule.ReminderRequest
import com.taskchain.features.running.domain.RoutineRunEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Guards terminal-run persistence shared by the runner, its retry, and startup recovery. */
class TerminalRunWriterTest {
    private val engine = RoutineRunEngine()

    /** Use this function to build a one-task routine, optionally scheduled daily. */
    private fun routine(scheduled: Boolean = false) = RoutineTemplate(
        RoutineId("routine"), EntityMetadata(0, 0), "Morning",
        tasks = listOf(RoutineTask(RoutineTaskId("task"), "Work")),
        schedule = if (scheduled) ScheduleRule(ScheduleFrequency.DAILY, localHour = 8, localMinute = 0) else null,
    )

    private fun completedRun(): RoutineRun =
        engine.completeCurrent(engine.start(routine(), RoutineRunId("run"), 1_000), 2_000)

    private fun abortedRun(): RoutineRun {
        val started = engine.start(routine(), RoutineRunId("run"), 1_000)
        return engine.abort(engine.back(started, 1_500), 2_000)
    }

    /** Use this function to build a writer over the given fakes. */
    private fun writer(
        active: FakeActiveRunRepository,
        history: FakeCompletionRepository,
        reminders: FakeReminderScheduler = FakeReminderScheduler(),
        definition: RoutineTemplate? = routine(),
    ) = TerminalRunWriter(active, history, engine, reminders, findRoutine = { definition }, now = { 9_000L })

    /** Use this function to verify a completed run is saved, appended once, cleared, and its reminder rearmed. */
    @Test
    fun completedRunEntersHistoryAndClears() = runBlocking {
        val terminal = completedRun()
        val active = FakeActiveRunRepository(null)
        val history = FakeCompletionRepository(active)
        val reminders = FakeReminderScheduler()

        writer(active, history, reminders).write(terminal)

        assertEquals(listOf(terminal), active.saved)
        assertEquals(listOf(engine.toCompletionEvent(terminal)), history.events)
        assertNull(active.current)
        // An unscheduled routine cancels its routine alarm after completion.
        assertEquals(listOf(ReminderPlanner.routineRequestCode(terminal.routineId)), reminders.cancelled)
    }

    /** Use this function to verify a scheduled routine schedules its next recurrence after completion. */
    @Test
    fun completedScheduledRoutineSchedulesNextReminder() = runBlocking {
        val active = FakeActiveRunRepository(null)
        val reminders = FakeReminderScheduler()

        writer(active, FakeCompletionRepository(active), reminders, definition = routine(scheduled = true))
            .write(completedRun())

        assertEquals(1, reminders.scheduled.size)
        assertTrue(reminders.cancelled.isEmpty())
    }

    /** Use this function to verify an Aborted run is kept on disk and never enters history or touches reminders. */
    @Test
    fun abortedRunIsOnlySaved() = runBlocking {
        val terminal = abortedRun()
        val active = FakeActiveRunRepository(null)
        val history = FakeCompletionRepository(active)
        val reminders = FakeReminderScheduler()

        writer(active, history, reminders).write(terminal)

        assertEquals(terminal, active.current)
        assertEquals(RunStatus.ABORTED, active.current?.status)
        assertTrue(history.events.isEmpty())
        assertEquals(0, active.clearCount)
        assertTrue(reminders.cancelled.isEmpty() && reminders.scheduled.isEmpty())
    }

    /** Use this function to verify a retry after a failed clear does not append the same history event twice. */
    @Test
    fun retryAfterClearFailureDoesNotDuplicateHistory() = runBlocking {
        val terminal = completedRun()
        val active = FakeActiveRunRepository(null, failFirstClear = true)
        val history = FakeCompletionRepository(active)
        val target = writer(active, history)

        try {
            target.write(terminal)
            fail("clear should fail")
        } catch (_: IllegalStateException) {
        }
        assertEquals(terminal, active.current)

        target.write(terminal)

        assertEquals(1, history.appendCount)
        assertNull(active.current)
        assertEquals(2, active.clearCount)
    }

    /** Use this function to verify a reminder failure never fails the completed write. */
    @Test
    fun reminderFailureDoesNotFailWrite() = runBlocking {
        val active = FakeActiveRunRepository(null)
        val history = FakeCompletionRepository(active)

        writer(active, history, FakeReminderScheduler(fail = true)).write(completedRun())

        assertEquals(1, history.events.size)
        assertNull(active.current)
    }

    /** Use this function to verify a failed save propagates before history is touched. */
    @Test
    fun failedSavePropagatesBeforeHistory() = runBlocking {
        val active = FakeActiveRunRepository(null, failSave = true)
        val history = FakeCompletionRepository(active)

        try {
            writer(active, history).write(completedRun())
            fail("save should fail")
        } catch (_: IllegalStateException) {
        }

        assertEquals(0, history.appendCount)
        assertEquals(0, active.clearCount)
    }

    /** Fake active-run storage with controllable save and clear failures. */
    private class FakeActiveRunRepository(
        initial: RoutineRun?,
        private var failFirstClear: Boolean = false,
        private val failSave: Boolean = false,
    ) : RoutineRunRepository {
        var current: RoutineRun? = initial
        val saved = mutableListOf<RoutineRun>()
        var clearCount = 0

        override fun observeActive(): Flow<RoutineRun?> = flowOf(current)

        override suspend fun saveActive(run: RoutineRun) {
            if (failSave) throw IllegalStateException("save failed")
            saved += run
            current = run
        }

        override suspend fun clearActive() {
            clearCount++
            if (failFirstClear) {
                failFirstClear = false
                throw IllegalStateException("clear failed")
            }
            current = null
        }
    }

    /** Fake append-only history that checks the terminal run is saved before it is appended. */
    private class FakeCompletionRepository(private val active: FakeActiveRunRepository) : CompletionRepository {
        val events = mutableListOf<CompletionEvent>()
        var appendCount = 0

        override fun observeAll(): Flow<List<CompletionEvent>> = flowOf(events.toList())

        override suspend fun append(event: CompletionEvent) {
            assertEquals(event.runId, active.current?.id)
            appendCount++
            events += event
        }
    }

    /** Fake reminder port that records calls, or fails every call. */
    private class FakeReminderScheduler(private val fail: Boolean = false) : ReminderScheduler {
        val scheduled = mutableListOf<ReminderRequest>()
        val cancelled = mutableListOf<Int>()

        override fun schedule(request: ReminderRequest) {
            if (fail) throw IllegalStateException("schedule failed")
            scheduled += request
        }

        override fun cancel(requestCode: Int) {
            if (fail) throw IllegalStateException("cancel failed")
            cancelled += requestCode
        }

        override fun clearDelivered(requestCode: Int) = Unit
    }
}
