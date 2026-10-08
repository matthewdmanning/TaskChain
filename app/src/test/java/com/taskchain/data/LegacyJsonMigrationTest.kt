package com.taskchain.data

import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineSubtaskId
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.SoundToken
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Verifies that JSON saved before the task/subtask rename decodes with every renamed field intact. */
class LegacyJsonMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Use this function to decode legacy text the same way the file repositories do. */
    private fun <T> decodeLegacy(text: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): T =
        json.decodeFromJsonElement(deserializer, json.parseToJsonElement(text).withLegacyKeysRenamed())

    @Test
    fun legacyRoutineKeysDecodeAsTasksAndSubtasks() {
        val routine = decodeLegacy(
            """{"id":"r","metadata":{"createdAtEpochMillis":0,"updatedAtEpochMillis":0},"title":"Morning",
               "steps":[{"id":"a","title":"Main","stackingAnchorStepId":"b",
                         "cues":[{"id":"c","title":"Prepare","durationSeconds":180}]},
                        {"id":"b","title":"Other"}],
               "soundSettings":{"entries":{"CueAdvanced":{"enabled":false}}}}""",
            RoutineTemplate.serializer(),
        )

        assertEquals(listOf("a", "b"), routine.tasks.map { it.id.value })
        assertEquals(RoutineTaskId("b"), routine.tasks.first().stackingAnchorTaskId)
        assertEquals(RoutineSubtaskId("c"), routine.tasks.first().subtasks.single().id)
        assertFalse(routine.soundSettings[SoundToken.SubtaskAdvanced].enabled)
    }

    @Test
    fun legacyRunKeysDecodeWithIndexesAndAdvancements() {
        val legacyRunTask = """{"source":{"id":"a","title":"Main","cues":[{"id":"c","title":"Prepare","durationSeconds":60}]},
            "activeCueId":"c","cueAdvancements":[{"cueId":"c","elapsedMillis":5,"atEpochMillis":6}]}"""
        val run = decodeLegacy(
            """{"id":"run","routineId":"r","routineTitle":"Morning","steps":[$legacyRunTask],
               "currentStepIndex":0,"startedAtEpochMillis":1,"stepBeforeFinishConfirmation":$legacyRunTask}""",
            RoutineRun.serializer(),
        )

        val task = run.tasks.single()
        assertEquals(0, run.currentTaskIndex)
        assertEquals(RoutineSubtaskId("c"), task.activeSubtaskId)
        assertEquals(RoutineSubtaskId("c"), task.subtaskAdvancements.single().subtaskId)
        assertEquals(task, run.taskBeforeFinishConfirmation)
    }
}
