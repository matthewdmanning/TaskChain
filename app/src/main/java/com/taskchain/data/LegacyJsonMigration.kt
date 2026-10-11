package com.taskchain.data

import android.util.AtomicFile
import androidx.core.util.readText
import androidx.core.util.writeText
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
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

/** Use this function before decoding stored JSON so files written before the task rename still load. */
fun JsonElement.withLegacyKeysRenamed(): JsonElement = when (this) {
    is JsonObject -> {
        val sourceEntries = entries
        val renamed = linkedMapOf<String, JsonElement>().apply {
            sourceEntries.filter { (key, _) -> key !in LEGACY_KEY_RENAMES }
                .forEach { (key, value) -> put(key, value.withLegacyKeysRenamed()) }
            sourceEntries.filter { (key, _) -> key in LEGACY_KEY_RENAMES }
                .forEach { (key, value) -> putIfAbsent(LEGACY_KEY_RENAMES.getValue(key), value.withLegacyKeysRenamed()) }
        }
        JsonObject(renamed)
    }
    is JsonArray -> JsonArray(map { it.withLegacyKeysRenamed() })
    else -> this
}

/**
 * Use this function once at startup to rewrite legacy JSON files under [directory] with current keys.
 * [json] parses and writes records; [directory] is the app's durable data root.
 * Depends on [StorageIo], [AtomicFile], and [File.quarantineCorrupt].
 */
fun migrateLegacyDataFiles(directory: File, json: Json) {
    runBlocking {
        StorageIo.mutex.withLock {
            directory.walkTopDown()
                .onFail { _, error -> throw error }
                .filter { it.isFile && (it.extension == "json" || it.name.endsWith(".json.bak")) }
                .map { if (it.name.endsWith(".bak")) File(it.path.removeSuffix(".bak")) else it }
                .distinctBy(File::getPath)
                .forEach { file ->
                    try {
                        val atomicFile = AtomicFile(file)
                        val original = atomicFile.readText()
                        val migrated = json.migrateStored(original)
                        if (migrated != original) atomicFile.writeText(migrated)
                    } catch (error: SerializationException) {
                        file.quarantineCorrupt()
                        throw IOException("Malformed storage JSON was quarantined for recovery: ${file.path}", error)
                    }
                }
        }
    }
}
