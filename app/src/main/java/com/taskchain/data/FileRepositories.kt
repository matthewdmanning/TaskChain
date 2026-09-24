package com.taskchain.data

import android.util.AtomicFile
import androidx.core.util.readText
import androidx.core.util.writeText
import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineTemplate
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Use this function when a damaged file must not crash startup or silently read as empty.
 * The unreadable file is renamed aside so a later write cannot overwrite recoverable data.
 */
private fun <T> File.decodeOrQuarantine(decode: (String) -> T): T? = try {
    decode(AtomicFile(this).readText())
} catch (error: SerializationException) {
    quarantine()
    null
} catch (error: IOException) {
    quarantine()
    null
}

/** Use this function to move an unreadable file out of the path its repository reads. */
private fun File.quarantine() {
    renameTo(File(parentFile, "$name.corrupt-${System.currentTimeMillis()}"))
}

/** File-backed routine storage using one atomic JSON file per routine. */
class FileRoutineRepository(
    private val directory: File,
    private val json: Json,
) : RoutineRepository {
    private val changes = MutableStateFlow(0L)
    private val mutex = Mutex()

    /** Use this function when UI needs routines refreshed after every local write. */
    override fun observeAll(): Flow<List<RoutineTemplate>> = changes
        .onStart { emit(changes.value) }
        .map { readAll() }
        .flowOn(Dispatchers.IO)

    /** Use this function when opening one routine without loading every definition. */
    override suspend fun get(id: RoutineId): RoutineTemplate? = withContext(Dispatchers.IO) {
        mutex.withLock { read(fileFor(id)) }
    }

    /** Use this function when atomically creating or replacing a routine file. */
    override suspend fun save(routine: RoutineTemplate) = withContext(Dispatchers.IO) {
        mutex.withLock {
            routine.requireRunnable()
            directory.mkdirs()
            AtomicFile(fileFor(routine.id)).writeText(json.encodeToString(RoutineTemplate.serializer(), routine))
            changes.value += 1
        }
    }

    /** Use this function when deleting a routine through its repository boundary. */
    override suspend fun delete(id: RoutineId) = withContext(Dispatchers.IO) {
        mutex.withLock {
            AtomicFile(fileFor(id)).delete()
            changes.value += 1
        }
    }

    /** Use this function to load all non-deleted routine files for an observation refresh. */
    private fun readAll(): List<RoutineTemplate> = directory
        .listFiles { file -> file.extension == JSON_EXTENSION }
        .orEmpty()
        .mapNotNull(::read)
        .filter { it.metadata.deletedAtEpochMillis == null }
        .sortedBy { it.title.lowercase() }

    /** Use this function when a missing or not-yet-created routine file is valid. */
    private fun read(file: File): RoutineTemplate? = file.takeIf(File::exists)
        ?.decodeOrQuarantine { json.decodeFromString(RoutineTemplate.serializer(), it) }

    /**
     * Use this function to map stable routine identity to its private JSON file.
     * The identity is checked because it becomes a file name, and a separator or a
     * parent reference in it would escape the repository directory.
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
    private val mutex = Mutex()

    /** Use this function when runner state must be reconstructed from disk. */
    override fun observeActive(): Flow<RoutineRun?> = changes
        .onStart { emit(changes.value) }
        .map { read() }
        .flowOn(Dispatchers.IO)

    /** Use this function to atomically persist the authoritative run state. */
    override suspend fun saveActive(run: RoutineRun) = withContext(Dispatchers.IO) {
        mutex.withLock {
            file.parentFile?.mkdirs()
            AtomicFile(file).writeText(json.encodeToString(RoutineRun.serializer(), run))
            changes.value += 1
        }
    }

    /** Use this function only after history safely contains the terminal run. */
    override suspend fun clearActive() = withContext(Dispatchers.IO) {
        mutex.withLock {
            AtomicFile(file).delete()
            changes.value += 1
        }
    }

    /** Use this function when an absent active-session file means there is no run. */
    private fun read(): RoutineRun? = file.takeIf(File::exists)
        ?.decodeOrQuarantine { json.decodeFromString(RoutineRun.serializer(), it) }
}

/** File-backed append-only completion history used by progress projections. */
class FileCompletionRepository(
    private val file: File,
    private val json: Json,
) : CompletionRepository {
    private val changes = MutableStateFlow(0L)
    private val mutex = Mutex()
    private val serializer = ListSerializer(CompletionEvent.serializer())

    /** Use this function when progress screens need refreshed event history. */
    override fun observeAll(): Flow<List<CompletionEvent>> = flow {
        emit(read())
        changes.collect { emit(read()) }
    }.flowOn(Dispatchers.IO)

    /** Use this function to durably append one terminal run event. */
    override suspend fun append(event: CompletionEvent) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val events = read()
            if (events.none { it.runId == event.runId }) {
                file.parentFile?.mkdirs()
                AtomicFile(file).writeText(json.encodeToString(serializer, events + event))
                changes.value += 1
            }
        }
    }

    /** Use this function when an absent history file represents empty history. */
    private fun read(): List<CompletionEvent> = file.takeIf(File::exists)
        ?.decodeOrQuarantine { json.decodeFromString(serializer, it) }
        .orEmpty()
}
