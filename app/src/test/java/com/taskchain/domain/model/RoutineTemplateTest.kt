package com.taskchain.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards routine-level scheduling invariants and legacy task decoding. */
class RoutineTemplateTest {
    @Test
    fun rejectsConflictingRoutineSettingsAndInvalidRepeatRules() {
        val task = RoutineTask(RoutineTaskId("task"), "Prepare")
        val routine = RoutineTemplate(RoutineId("routine"), EntityMetadata(0, 0), "Morning", tasks = listOf(task))
        val daily = ScheduleRule(ScheduleFrequency.DAILY, 9, 0)

        assertThrows(IllegalArgumentException::class.java) {
            routine.copy(schedule = daily, deadlineEpochMillis = 10).requireRunnable()
        }
        assertThrows(IllegalArgumentException::class.java) {
            routine.copy(schedule = daily, reminderAtEpochMillis = 10).requireRunnable()
        }
        assertThrows(IllegalArgumentException::class.java) {
            routine.copy(deadlineEpochMillis = 10, reminderAtEpochMillis = 20).requireRunnable()
        }
        assertThrows(IllegalArgumentException::class.java) {
            routine.copy(remindEveryMinutes = 5).requireRunnable()
        }
        assertThrows(IllegalArgumentException::class.java) {
            routine.copy(schedule = daily, remindEveryMinutes = 0).requireRunnable()
        }
        assertThrows(IllegalArgumentException::class.java) {
            routine.copy(schedule = ScheduleRule(ScheduleFrequency.SELECTED_DAYS, 9, 0)).requireRunnable()
        }

        routine.copy(schedule = daily, remindEveryMinutes = 5).requireRunnable()
        routine.copy(reminderAtEpochMillis = 10).requireRunnable()
        routine.copy(deadlineEpochMillis = 10).requireRunnable()
    }

    @Test
    fun readsLegacyTaskSettingsForBuilderMigration() {
        val legacy = Json { ignoreUnknownKeys = true }.decodeFromString<RoutineTask>(
            """{"id":"legacy","title":"Drink water","stackingAnchorTaskId":"prior","deadlineEpochMillis":100,"reminderAtEpochMillis":200,"schedule":{"frequency":"DAILY","localHour":9,"localMinute":0,"daysOfWeek":[],"oneTimeEpochMillis":null},"remindEveryMinutes":5,"soundEnabled":false,"vibrateEnabled":false,"kind":"HABIT"}""",
        )

        assertEquals("Drink water", legacy.title)
        assertEquals(RoutineTaskId("prior"), legacy.stackingAnchorTaskId)
        assertEquals(100L, legacy.deadlineEpochMillis)
        assertEquals(200L, legacy.reminderAtEpochMillis)
        assertEquals(ScheduleRule(ScheduleFrequency.DAILY, 9, 0), legacy.schedule)
        assertEquals(5, legacy.remindEveryMinutes)
        assertTrue(!legacy.soundEnabled)
        assertTrue(!legacy.vibrateEnabled)
    }
}
