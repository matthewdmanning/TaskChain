package com.taskchain.data

import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineSubtaskId
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.SoundToken
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies that JSON saved before the task/subtask rename decodes with every renamed field intact. */
class LegacyJsonMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Use this function to decode legacy text the same way the file repositories do. */
    private fun <T> decodeLegacy(text: String, deserializer: kotlinx.serialization.DeserializationStrategy<T>): T =
        json.decodeStored(text, deserializer)

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

    /** Use this test when startup migration must be repeatable and retain current-key values. Inputs: none; dependencies: [migrateLegacyDataFiles] and [Json]. */
    @Test
    fun migrationIsRepeatableAndCurrentKeysWin() {
        val first = json.migrateStored("""{"steps":[{"id":"legacy"}],"tasks":[{"id":"current"}]}""")
        assertEquals(first, json.migrateStored(first))
        assertEquals("current", json.parseToJsonElement(first).jsonObject["data"]!!.jsonObject["tasks"]!!
            .jsonArray.single().jsonObject["id"]!!.toString().trim('"'))
        assertTrue(json.parseToJsonElement(first).jsonObject.containsKey("schemaVersion"))
    }

    /** Use this test when current-only fields must survive key canonicalization. Inputs: none; dependencies: [JsonElement.withLegacyKeysRenamed]. */
    @Test
    fun currentKeysRemainWhenNoLegacyKeysArePresent() {
        val result = json.parseToJsonElement("""{"tasks":[{"id":"current"}],"title":"Kept"}""")
            .withLegacyKeysRenamed().jsonObject

        assertEquals("Kept", result["title"]!!.toString().trim('"'))
        assertEquals("current", result["tasks"]!!.jsonArray.single().jsonObject["id"]!!.toString().trim('"'))
    }
}
