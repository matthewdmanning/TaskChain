package com.taskchain.data

import android.content.Context
import com.taskchain.domain.model.EntityMetadata
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineTask
import com.taskchain.domain.model.RoutineTaskId
import com.taskchain.domain.model.RoutineTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Reads predefined routines from assets without coupling the domain to asset JSON. */
class BuiltInRoutineLibrary(
    private val context: Context,
    private val json: Json,
) {
    /** Use this function to seed or display the bundled routine library. */
    suspend fun load(nowEpochMillis: Long): List<RoutineTemplate> = withContext(Dispatchers.IO) {
        context.assets.open(ASSET_PATH).bufferedReader().use { reader ->
            json.decodeFromString<LibraryDto>(reader.readText()).routines.map { it.toDomain(nowEpochMillis) }
        }
    }

    /** Asset-only schema kept independent from persistent and domain representations. */
    @Serializable
    private data class LibraryDto(val formatVersion: Int, val routines: List<RoutineDto>)

    /** Asset-only representation of one predefined routine. */
    @Serializable
    private data class RoutineDto(
        val id: String,
        val title: String,
        val description: String,
        val tasks: List<TaskDto>,
    ) {
        /** Use this function when exposing a predefined routine to domain consumers. */
        fun toDomain(nowEpochMillis: Long): RoutineTemplate = RoutineTemplate(
            id = RoutineId(id),
            metadata = EntityMetadata(nowEpochMillis, nowEpochMillis),
            title = title,
            description = description,
            tasks = tasks.map(TaskDto::toDomain),
        )
    }

    /** Asset-only representation of one predefined task. */
    @Serializable
    private data class TaskDto(
        val id: String,
        val title: String,
        val timerSeconds: Long? = null,
    ) {
        /** Use this function when copying a predefined task into a domain routine. */
        fun toDomain(): RoutineTask = RoutineTask(RoutineTaskId(id), title, timerSeconds)
    }

    private companion object {
        const val ASSET_PATH = "config/routine_library.json"
    }
}
