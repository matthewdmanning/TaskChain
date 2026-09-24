package com.taskchain.reminder

import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineRunId
import com.taskchain.domain.model.RoutineRunStep
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
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
            steps = listOf(RoutineStep(RoutineStepId("step"), "Work")),
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
        val step = RoutineRunStep(
            source = RoutineStep(RoutineStepId("step"), "Work"),
            status = RunStepStatus.COMPLETED,
            finishedAtEpochMillis = 20,
            completedAtEpochMillis = 30,
        )
        val active = RoutineRun(RoutineRunId("run"), RoutineId("routine"), "Morning", listOf(step), 0, 10)
        val event = CompletionEvent(active.id, active.routineId, active.routineTitle, 10, 40, RunStatus.COMPLETED, listOf(step))

        assertTrue(completedSinceCycle("routine", "step", 15, active, emptyList()))
        assertTrue(completedSinceCycle("routine", "step", 15, null, listOf(event)))
        assertTrue(completedSinceCycle("routine", "step", 25, active, listOf(event)))
        assertFalse(completedSinceCycle("routine", "step", 35, active, listOf(event)))
        assertFalse(completedSinceCycle("another", "step", 15, active, listOf(event)))
    }
}
