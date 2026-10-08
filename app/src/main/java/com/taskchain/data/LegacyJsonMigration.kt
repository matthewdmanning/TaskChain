package com.taskchain.data

import android.util.AtomicFile
import androidx.core.util.readText
import androidx.core.util.writeText
import java.io.File
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Stored JSON keys renamed when "step" became "task" and "cue" became "subtask". */
private val LEGACY_KEY_RENAMES = mapOf(
    "steps" to "tasks",
    "cues" to "subtasks",
    "stackingAnchorStepId" to "stackingAnchorTaskId",
    "activeCueId" to "activeSubtaskId",
    "cueAdvancements" to "subtaskAdvancements",
    "cueId" to "subtaskId",
    "currentStepIndex" to "currentTaskIndex",
    "stepBeforeFinishConfirmation" to "taskBeforeFinishConfirmation",
    "CueAdvanced" to "SubtaskAdvanced",
)

/** Use this function before decoding stored JSON so files written before the rename still load. */
fun JsonElement.withLegacyKeysRenamed(): JsonElement = when (this) {
    is JsonObject -> JsonObject(
        entries.associate { (key, value) -> (LEGACY_KEY_RENAMES[key] ?: key) to value.withLegacyKeysRenamed() },
    )
    is JsonArray -> JsonArray(map { it.withLegacyKeysRenamed() })
    else -> this
}

/**
 * Use this function once at startup to rewrite legacy JSON files under [directory] with current keys.
 * Unreadable files are left in place for the repositories to quarantine.
 */
fun migrateLegacyDataFiles(directory: File, json: Json) {
    directory.walkTopDown()
        .filter { it.isFile && it.extension == "json" }
        .forEach { file ->
            try {
                val atomicFile = AtomicFile(file)
                val stored = json.parseToJsonElement(atomicFile.readText())
                val migrated = stored.withLegacyKeysRenamed()
                if (migrated != stored) atomicFile.writeText(json.encodeToString(JsonElement.serializer(), migrated))
            } catch (error: SerializationException) {
                Unit
            } catch (error: IOException) {
                Unit
            }
        }
}
