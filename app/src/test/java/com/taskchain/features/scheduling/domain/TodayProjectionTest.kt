package com.taskchain.features.scheduling.domain

import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.EntityMetadata
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineRunTask
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.model.ScheduleRule
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies Home categorizes each routine from its schedule and occurrence completion. */
class TodayProjectionTest {
    private val zone = TimeZone.getTimeZone("America/New_York")
    private val now = local("2026-01-05 09:15")

    /** Use this check to verify manual routines stay Manual even after completion. */
    @Test
    fun keepsManualRoutinesManual() {
        val routine = routine("manual")
        val history = listOf(completion(routine, now))

        assertEquals(listOf(routine), projectTodayRoutines(listOf(routine), null, history, now, 2, zone).manual)
    }

    /** Use this check to verify active and durable scheduled completions remain Completed until the next trigger. */
    @Test
    fun classifiesActiveAndDurableCompletionsAsCompleted() {
        val routine = routine("daily", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val beforeNextTrigger = local("2026-01-06 01:59")

        assertEquals(
            listOf(routine),
            projectTodayRoutines(listOf(routine), activeRun(routine, now), emptyList(), beforeNextTrigger, 2, zone).completed,
        )
        assertEquals(
            listOf(routine),
            projectTodayRoutines(listOf(routine), null, listOf(completion(routine, now)), beforeNextTrigger, 2, zone).completed,
        )
    }

    /** Use this check to verify a skip moves a card to Completed with a flag until the next day start. */
    @Test
    fun skippedRoutineStaysCompletedAndFlaggedUntilDayStart() {
        val routine = routine("daily", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val skipBeforeTrigger = skipEvent(routine, RoutineRunId("skip"), local("2026-01-05 07:00"))

        fun project(at: String, history: List<CompletionEvent>) =
            projectTodayRoutines(listOf(routine), null, history, local(at), 2, zone)

        val flagged = project("2026-01-05 12:00", listOf(skipBeforeTrigger))
        assertEquals(listOf(routine), flagged.completed)
        assertEquals(setOf(routine.id), flagged.skipped)
        assertEquals(listOf(routine), project("2026-01-06 01:59", listOf(skipBeforeTrigger)).completed)
        assertEquals(listOf(routine), project("2026-01-06 02:00", listOf(skipBeforeTrigger)).scheduled)
    }

    /** Use this check to verify a weekly routine returns at day start on the day it is next due, not the day after. */
    @Test
    fun weeklyRoutineReturnsAtDayStartOfItsNextOccurrence() {
        val monday = ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 9, 0, daysOfWeek = setOf(1))
        val routine = routine("weekly", monday)
        val history = listOf(completion(routine, local("2026-01-05 09:30")))

        fun project(at: String) = projectTodayRoutines(listOf(routine), null, history, local(at), 2, zone)

        assertEquals(listOf(routine), project("2026-01-12 01:59").completed)
        assertEquals(listOf(routine), project("2026-01-12 02:00").scheduled)
    }

    /** Use this check to verify undoing a skip returns the card to Scheduled without a flag. */
    @Test
    fun undoneSkipReturnsToScheduled() {
        val routine = routine("daily", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val history = listOf(
            skipEvent(routine, RoutineRunId("skip"), local("2026-01-05 07:00")),
            skipUndoneEvent(routine, RoutineRunId("undo"), local("2026-01-05 07:05")),
        )

        val projection = projectTodayRoutines(listOf(routine), null, history, local("2026-01-05 12:00"), 2, zone)
        assertEquals(listOf(routine), projection.scheduled)
        assertTrue(projection.skipped.isEmpty())
    }

    /** Use this check to keep a run pending until its final confirmation is accepted. */
    @Test
    fun doesNotCompleteAnActiveRunBeforeFinalConfirmation() {
        val routine = routine("daily", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val awaitingConfirmation = activeRun(routine, now).copy(
            status = RunStatus.ACTIVE,
            endedAtEpochMillis = null,
            finishConfirmationRequested = true,
        )

        assertEquals(
            listOf(routine),
            projectTodayRoutines(listOf(routine), awaitingConfirmation, emptyList(), now, 2, zone).scheduled,
        )
    }

    /** Use this check to verify completion returns to Scheduled at the day start in the supplied time zone. */
    @Test
    fun returnsToScheduledAtDayStartInSuppliedTimeZone() {
        val routine = routine("daily", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val history = listOf(completion(routine, local("2026-01-05 08:59")))
        val newYorkOneFiftyNine = local("2026-01-06 01:59")
        val utc = TimeZone.getTimeZone("UTC")

        assertEquals(listOf(routine), projectTodayRoutines(listOf(routine), null, history, newYorkOneFiftyNine, 2, zone).completed)
        assertEquals(listOf(routine), projectTodayRoutines(listOf(routine), null, history, newYorkOneFiftyNine, 2, utc).scheduled)
    }

    /** Use this check to verify Home follows the terminal run time when completion crosses a schedule trigger. */
    @Test
    fun keepsRoutineCompletedWhenRunFinishesAfterItsScheduleTrigger() {
        val routine = routine("daily", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val history = listOf(
            completion(routine, local("2026-01-05 08:59")).copy(
                endedAtEpochMillis = local("2026-01-05 09:01"),
            ),
        )

        assertEquals(
            listOf(routine),
            projectTodayRoutines(listOf(routine), null, history, local("2026-01-05 09:02"), 2, zone).completed,
        )
    }

    /** Use this check to verify every input appears in exactly one category and manual status wins. */
    @Test
    fun projectsEveryRoutineExactlyOnce() {
        val manual = routine("manual")
        val scheduled = routine("scheduled", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val completed = routine("completed", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val routines = listOf(manual, scheduled, completed)

        val projection = projectTodayRoutines(routines, null, listOf(completion(completed, now)), now, 2, zone)
        val categorized = projection.scheduled + projection.manual + projection.completed

        assertEquals(listOf(manual), projection.manual)
        assertEquals(listOf(scheduled), projection.scheduled)
        assertEquals(listOf(completed), projection.completed)
        assertEquals(routines.sortedBy { it.id.value }, categorized.sortedBy { it.id.value })
        assertEquals(routines.size, categorized.size)
    }

    /** Use this function to build a routine with its template-level schedule. */
    private fun routine(id: String, schedule: ScheduleRule? = null): RoutineTemplate = RoutineTemplate(
        id = RoutineId(id),
        metadata = EntityMetadata(0, 0),
        title = id,
        tasks = listOf(RoutineTask(RoutineTaskId("task-$id"), "Task")),
        schedule = schedule,
    )

    /** Use this function to represent durable completion history for a routine. */
    private fun completion(routine: RoutineTemplate, completedAt: Long): CompletionEvent {
        val task = routine.tasks.single()
        return CompletionEvent(
            runId = RoutineRunId("history-$completedAt"),
            routineId = routine.id,
            routineTitle = routine.title,
            startedAtEpochMillis = completedAt,
            endedAtEpochMillis = completedAt,
            status = RunStatus.COMPLETED,
            tasks = listOf(RoutineRunTask(task, RunTaskStatus.COMPLETED, completedAtEpochMillis = completedAt)),
        )
    }

    /** Use this function to represent an active run with a completed routine task. */
    private fun activeRun(routine: RoutineTemplate, completedAt: Long): RoutineRun {
        val task = routine.tasks.single()
        return RoutineRun(
            id = RoutineRunId("active"),
            routineId = routine.id,
            routineTitle = routine.title,
            tasks = listOf(RoutineRunTask(task, RunTaskStatus.COMPLETED, completedAtEpochMillis = completedAt)),
            currentTaskIndex = 0,
            startedAtEpochMillis = completedAt,
            endedAtEpochMillis = completedAt,
            status = RunStatus.COMPLETED,
        )
    }

    /** Use this function to parse a deterministic local wall-clock timestamp. */
    private fun local(value: String): Long = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = zone
    }.parse(value)!!.time
}
