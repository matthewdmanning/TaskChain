package com.taskchain.domain.home

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunTask
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
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

        assertEquals(listOf(routine), projectTodayRoutines(listOf(routine), null, history, now, zone).manual)
    }

    /** Use this check to verify active and durable scheduled completions remain Completed until the next trigger. */
    @Test
    fun classifiesActiveAndDurableCompletionsAsCompleted() {
        val routine = routine("daily", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val beforeNextTrigger = local("2026-01-06 08:59")

        assertEquals(
            listOf(routine),
            projectTodayRoutines(listOf(routine), activeRun(routine, now), emptyList(), beforeNextTrigger, zone).completed,
        )
        assertEquals(
            listOf(routine),
            projectTodayRoutines(listOf(routine), null, listOf(completion(routine, now)), beforeNextTrigger, zone).completed,
        )
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
            projectTodayRoutines(listOf(routine), awaitingConfirmation, emptyList(), now, zone).scheduled,
        )
    }

    /** Use this check to verify the next local schedule trigger moves completion back to Scheduled. */
    @Test
    fun returnsToScheduledAtNextTriggerInSuppliedTimeZone() {
        val routine = routine("daily", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val history = listOf(completion(routine, local("2026-01-05 08:59")))
        val atNewYorkTrigger = local("2026-01-05 09:00")

        assertEquals(listOf(routine), projectTodayRoutines(listOf(routine), null, history, atNewYorkTrigger, zone).scheduled)
        assertEquals(listOf(routine), projectTodayRoutines(listOf(routine), null, history, atNewYorkTrigger, TimeZone.getTimeZone("UTC")).completed)
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
            projectTodayRoutines(listOf(routine), null, history, local("2026-01-05 09:02"), zone).completed,
        )
    }

    /** Use this check to verify every input appears in exactly one category and manual status wins. */
    @Test
    fun projectsEveryRoutineExactlyOnce() {
        val manual = routine("manual")
        val scheduled = routine("scheduled", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val completed = routine("completed", ScheduleRule(ScheduleFrequency.DAILY, 9, 0))
        val routines = listOf(manual, scheduled, completed)

        val projection = projectTodayRoutines(routines, null, listOf(completion(completed, now)), now, zone)
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
