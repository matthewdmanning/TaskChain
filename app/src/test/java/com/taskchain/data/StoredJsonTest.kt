package com.taskchain.data

import com.taskchain.domain.model.RoutineTask
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies versioned envelopes, legacy payloads, and unsupported-file preservation. Constructor inputs: none; dependencies: [Json] and the storage helpers. */
class StoredJsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Use this test when a legacy raw object or array must remain decodable. Inputs: none; dependencies: [Json.decodeStored]. */
    @Test
    fun rawLegacyObjectAndArrayDecode() {
        val objectValue = json.decodeStored("""{"title":"Task"}""", JsonObject.serializer())
        val arrayValue = json.decodeStored("""[{"title":"Task"}]""", JsonArray.serializer())

        assertEquals("Task", objectValue["title"]?.jsonPrimitive?.content)
        assertEquals(1, arrayValue.size)
    }

    /** Use this test when a current record must round-trip through the minimal envelope. Inputs: none; dependencies: [Json.decodeStored] and [Json.encodeStored]. */
    @Test
    fun envelopeRoundTripsAndOptionalDefaultsDecode() {
        val task = json.decodeStored("""{"id":"task","title":"Task"}""", RoutineTask.serializer())
        val encoded = json.encodeStored(RoutineTask.serializer(), task)
        val envelope = json.parseToJsonElement(encoded).jsonObject

        assertEquals(CURRENT_SCHEMA_VERSION, envelope["schemaVersion"]?.jsonPrimitive?.int)
        assertEquals("task", json.decodeStored(encoded, RoutineTask.serializer()).id.value)
        assertEquals(null, task.timerSeconds)
        assertTrue(task.subtasks.isEmpty())
    }

    /** Use this function to reject a newer schema before producing replacement JSON. Inputs: none; dependencies: Json.migrateStored and UnsupportedSchemaVersionException. */
    @Test
    fun newerSchemaIsRejectedWithoutChangingBytes() {
        val bytes = """{"schemaVersion":99,"data":{"future":true}}"""
        try {
            json.migrateStored(bytes)
            throw AssertionError("Expected unsupported schema")
        } catch (error: UnsupportedSchemaVersionException) {
            assertEquals(99, error.version)
        }

    }
}
