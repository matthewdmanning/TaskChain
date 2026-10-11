package com.taskchain.domain.model

import com.taskchain.domain.run.RoutineRunEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies subtask persistence, legacy defaults, and derived duration behavior. */
class SubtaskSerializationTest {
    /** Use this function to verify template, active-run, and history JSON retain subtask state. */
    @Test
    fun subtaskStateRoundTripsAcrossTemplateRunAndHistory() {
        val json = Json { }
        val template = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Routine",
            tasks = listOf(
                RoutineTask(
                    RoutineTaskId("task"),
                    "Task",
                    subtasks = listOf(
                        RoutineSubtask(RoutineSubtaskId("one"), "One", 60),
                        RoutineSubtask(RoutineSubtaskId("two"), "Two", 120),
                    ),
                ),
            ),
        )
        val run = RoutineRunEngine().completeCurrent(
            RoutineRunEngine().start(template, RoutineRunId("run"), 0),
            30_000L,
        )
        val event = RoutineRunEngine().toCompletionEvent(
            run.copy(
                tasks = run.tasks.map {
                    it.copy(status = RunTaskStatus.COMPLETED, actualDurationMillis = 30_000L)
                },
                status = RunStatus.COMPLETED,
                endedAtEpochMillis = 30_000L,
            ),
        )

        assertEquals(template, json.decodeFromString<RoutineTemplate>(json.encodeToString(template)))
        assertEquals(run, json.decodeFromString<RoutineRun>(json.encodeToString(run)))
        assertEquals(event, json.decodeFromString<CompletionEvent>(json.encodeToString(event)))
        assertEquals(180L, template.tasks.single().durationSeconds)
        assertTrue(defaultSoundSettings()[SoundToken.SubtaskAdvanced].enabled)
    }

    /** Use this function to verify old no-subtask records decode with empty subtask state and current preferences. */
    @Test
    fun legacyRecordsUseNoSubtaskDefaults() {
        val json = Json { ignoreUnknownKeys = true }
        val run = json.decodeFromString<RoutineRun>(
            """{"id":"run","routineId":"routine","routineTitle":"Legacy","tasks":[{"source":{"id":"task","title":"Task"},"startedAtEpochMillis":1000}],"currentTaskIndex":0,"startedAtEpochMillis":1000}""",
        )
        val preferences = json.decodeFromString<UserPreferences>("""{"selectedTheme":"system"}""")

        assertTrue(run.tasks.single().source.subtasks.isEmpty())
        assertEquals(null, run.tasks.single().activeSubtaskId)
        assertTrue(run.tasks.single().subtaskAdvancements.isEmpty())
        assertFalse(preferences.showSubtaskTimeRemaining)
    }

    /** Use this function to verify subtask validation rejects malformed identities, titles, durations, and totals. */
    @Test
    fun subtaskValidationRequiresPositiveUniqueSafeDefinitions() {
        val base = RoutineTemplate(
            id = RoutineId("routine"),
            metadata = EntityMetadata(0, 0),
            title = "Routine",
            tasks = listOf(RoutineTask(RoutineTaskId("task"), "Task")),
        )

        listOf(
            listOf(RoutineSubtask(RoutineSubtaskId(""), "Subtask", 1)),
            listOf(RoutineSubtask(RoutineSubtaskId("subtask"), "", 1)),
            listOf(RoutineSubtask(RoutineSubtaskId("subtask"), "Subtask", 0)),
            listOf(
                RoutineSubtask(RoutineSubtaskId("subtask"), "Subtask", 1),
                RoutineSubtask(RoutineSubtaskId("subtask"), "Again", 1),
            ),
            listOf(
                RoutineSubtask(RoutineSubtaskId("subtask"), "Subtask", Long.MAX_VALUE / 1_000L),
                RoutineSubtask(RoutineSubtaskId("other"), "Other", 1),
            ),
        ).forEach { subtasks ->
            val invalid = base.copy(tasks = listOf(base.tasks.single().copy(subtasks = subtasks)))
            runCatching { invalid.requireRunnable() }.onSuccess { error("Expected invalid subtask definition") }
        }
    }
}
