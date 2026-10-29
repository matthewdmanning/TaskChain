package com.taskchain.core.data

import android.util.AtomicFile
import androidx.core.util.readText
import androidx.core.util.writeText
import com.taskchain.core.model.CompletionEvent
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineTemplate
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Use this function to identify a recoverable AtomicFile base or backup. Input: receiver base path; dependencies: [File.exists]. */
private fun File.hasRecoverableAtomicFile(): Boolean = exists() || File("$path.bak").exists()

/**
 * Use this function when malformed JSON must be preserved before a later write can replace it.
 * Input: receiver [File] is the AtomicFile base path; dependencies: [File.renameTo], [File.exists], and [UUID].
 */
internal fun File.quarantineCorrupt() {
    val parent = parentFile ?: throw IOException("Cannot quarantine storage file without a parent: $path")
    val target = File(parent, "$name.corrupt-${UUID.randomUUID()}")
    if (!renameTo(target) || exists() || !target.exists()) {
        throw IOException("Could not quarantine malformed storage file for recovery: $path")
    }
}

/** Use this function to decode a present file while quarantining only serialization failures. Inputs: receiver base path, `json`, and `deserializer`; dependencies: [AtomicFile], [Json.decodeStored], and [File.quarantineCorrupt]. */
private fun <T> File.readStoredOrNull(json: Json, deserializer: DeserializationStrategy<T>): T? {
    if (!hasRecoverableAtomicFile()) return null
    return try {
        json.decodeStored(AtomicFile(this).readText(), deserializer)
    } catch (error: SerializationException) {
        quarantineCorrupt()
        throw IOException("Malformed storage JSON was quarantined for recovery: $path", error)
    }
}

/** Use this function before mutation to reject newer schemas and quarantine malformed JSON safely. Inputs: receiver base path and `json`; dependencies: [AtomicFile], [Json.validateStored], and [File.quarantineCorrupt]. */
private fun File.validateBeforeMutation(json: Json) {
    if (!hasRecoverableAtomicFile()) return
    try {
        json.validateStored(AtomicFile(this).readText())
    } catch (error: SerializationException) {
        quarantineCorrupt()
        throw IOException("Malformed storage JSON was quarantined for recovery: $path", error)
    }
}

/** Use this function to map a directory entry to its AtomicFile base path. Input: receiver directory entry; dependency: [File]. */
private fun File.atomicBaseFile(): File = if (name.endsWith(".bak")) File(path.removeSuffix(".bak")) else this

/** File-backed routine storage using one atomic JSON file per routine. */
class FileRoutineRepository(
    private val directory: File,
    private val json: Json,
) : RoutineRepository {
    private val changes = MutableStateFlow(0L)

    /** Use this function when UI needs routines refreshed after every local write. */
    override fun observeAll(): Flow<List<RoutineTemplate>> = flow {
        emit(readAll())
        changes.collect { emit(readAll()) }
    }.flowOn(Dispatchers.IO)

    /** Use this function when opening one routine without loading every definition. */
    override suspend fun get(id: RoutineId): RoutineTemplate? = withContext(Dispatchers.IO) {
        StorageIo.mutex.withLock { read(fileFor(id)) }
    }

    /** Use this function when atomically creating or replacing a routine file. */
    override suspend fun save(routine: RoutineTemplate) = withContext(Dispatchers.IO) {
        StorageIo.mutex.withLock {
            routine.requireRunnable()
            directory.mkdirs()
            val file = fileFor(routine.id)
            file.validateBeforeMutation(json)
            AtomicFile(file).writeText(json.encodeStored(RoutineTemplate.serializer(), routine))
            changes.value += 1
        }
    }

    /** Use this function when deleting a routine through its repository boundary. */
    override suspend fun delete(id: RoutineId) = withContext(Dispatchers.IO) {
        StorageIo.mutex.withLock {
            val file = fileFor(id)
            file.validateBeforeMutation(json)
            AtomicFile(file).delete()
            changes.value += 1
        }
    }

    /** Use this function to load all non-deleted routine files for an observation refresh. */
    private suspend fun readAll(): List<RoutineTemplate> = StorageIo.mutex.withLock {
        if (!directory.exists()) return@withLock emptyList()
        val files = directory.listFiles { file ->
            file.name.endsWith(".$JSON_EXTENSION") || file.name.endsWith(".$JSON_EXTENSION.bak")
        } ?: throw IOException("Could not read routine storage directory: ${directory.path}")
        files
            .map(File::atomicBaseFile)
            .distinctBy(File::getPath)
            .mapNotNull(::read)
            .filter { it.metadata.deletedAtEpochMillis == null }
            .sortedBy { it.title.lowercase() }
    }

    /** Use this function when a missing or not-yet-created routine file is valid. */
    private fun read(file: File): RoutineTemplate? = file.readStoredOrNull(json, RoutineTemplate.serializer())

    /**
     * Use this function to map stable routine identity to its private JSON file.
     * [id] is checked because it becomes a file name; depends on [SAFE_ID] and [File].
     */
    private fun fileFor(id: RoutineId): File {
        require(SAFE_ID.matches(id.value)) { "Routine id is not usable as a file name: ${id.value}" }
        return File(directory, "${id.value}.$JSON_EXTENSION")
    }

    private companion object {
        const val JSON_EXTENSION = "json"
        val SAFE_ID = Regex("[A-Za-z0-9_-]+")
    }
}

/** File-backed active-run storage with atomic replacement after every transition. */
class FileRoutineRunRepository(
    private val file: File,
    private val json: Json,
) : RoutineRunRepository {
    private val changes = MutableStateFlow(0L)

    /** Use this function when runner state must be reconstructed from disk. */
    override fun observeActive(): Flow<RoutineRun?> = flow {
        emit(StorageIo.mutex.withLock { read() })
        changes.collect { emit(StorageIo.mutex.withLock { read() }) }
    }.flowOn(Dispatchers.IO)

    /** Use this function to atomically persist the authoritative run state. */
    override suspend fun saveActive(run: RoutineRun) = withContext(Dispatchers.IO) {
        StorageIo.mutex.withLock {
            file.parentFile?.mkdirs()
            file.validateBeforeMutation(json)
            AtomicFile(file).writeText(json.encodeStored(RoutineRun.serializer(), run))
            changes.value += 1
        }
    }

    /** Use this function only after history safely contains the terminal run. */
    override suspend fun clearActive() = withContext(Dispatchers.IO) {
        StorageIo.mutex.withLock {
            file.validateBeforeMutation(json)
            AtomicFile(file).delete()
            changes.value += 1
        }
    }

    /** Use this function when an absent active-session file means there is no run. */
    private fun read(): RoutineRun? = file.readStoredOrNull(json, RoutineRun.serializer())
}

/** File-backed append-only completion history used by progress projections. */
class FileCompletionRepository(
    private val file: File,
    private val json: Json,
) : CompletionRepository {
    private val changes = MutableStateFlow(0L)
    private val serializer = ListSerializer(CompletionEvent.serializer())

    /** Use this function when progress screens need refreshed event history. */
    override fun observeAll(): Flow<List<CompletionEvent>> = flow {
        emit(StorageIo.mutex.withLock { read().orEmpty() })
        changes.collect { emit(StorageIo.mutex.withLock { read().orEmpty() }) }
    }.flowOn(Dispatchers.IO)

    /** Use this function to durably append one terminal run event. */
    override suspend fun append(event: CompletionEvent) = withContext(Dispatchers.IO) {
        StorageIo.mutex.withLock {
            val events = read().orEmpty()
            if (events.none { it.runId == event.runId }) {
                file.parentFile?.mkdirs()
                file.validateBeforeMutation(json)
                AtomicFile(file).writeText(json.encodeStored(serializer, events + event))
                changes.value += 1
            }
        }
    }

    /** Use this function when an absent history file represents empty history. */
    private fun read(): List<CompletionEvent>? = file.readStoredOrNull(json, serializer)
}
