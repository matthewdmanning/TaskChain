package com.taskchain.reminder

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that repeated alerts stop only for the task's current scheduled cycle. */
class ReminderRepeatTest {
    /** Verifies callers cannot omit routine-owned reminder settings while constructing an adapter request. */
    @Test
    fun routineOwnsCompleteReminderRequestMapping() {
        val schedule = ScheduleRule(ScheduleFrequency.DAILY, 9, 0)
        val routine = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Morning",
            tasks = listOf(RoutineTask(RoutineTaskId("task"), "Work")),
            schedule = schedule,
            remindEveryMinutes = 15,
            soundEnabled = false,
            vibrateEnabled = true,
        )

        assertEquals(
            ReminderRequest(
                requestCode = "routine".hashCode(),
                routineId = "routine",
                title = "Morning",
                triggerAtEpochMillis = 1_000,
                schedule = schedule,
                remindEveryMinutes = 15,
                cycleStartEpochMillis = 1_000,
                soundEnabled = false,
                vibrateEnabled = true,
            ),
            routine.toReminderRequest(1_000),
        )
    }

    /** Use this function to verify active and historical completions stop repeats without hiding a new occurrence. */
    @Test
    fun completedTaskStopsOnlyItsCurrentCycle() {
        val task = RoutineRunTask(
            source = RoutineTask(RoutineTaskId("task"), "Work"),
            status = RunTaskStatus.COMPLETED,
            finishedAtEpochMillis = 20,
            completedAtEpochMillis = 30,
        )
        val active = RoutineRun(RoutineRunId("run"), RoutineId("routine"), "Morning", listOf(task), 0, 10)
        val event = CompletionEvent(active.id, active.routineId, active.routineTitle, 10, 40, RunStatus.COMPLETED, listOf(task))

        assertTrue(completedSinceCycle("routine", "task", 15, active, emptyList()))
        assertTrue(completedSinceCycle("routine", "task", 15, null, listOf(event)))
        assertTrue(completedSinceCycle("routine", "task", 25, active, listOf(event)))
        assertFalse(completedSinceCycle("routine", "task", 35, active, listOf(event)))
        assertFalse(completedSinceCycle("another", "task", 15, active, listOf(event)))
    }
}
