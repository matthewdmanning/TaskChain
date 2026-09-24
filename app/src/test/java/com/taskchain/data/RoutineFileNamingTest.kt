package com.taskchain.data

import com.taskchain.domain.model.RoutineId
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Test

/** Checks that a routine identity cannot escape the directory its repository owns. */
class RoutineFileNamingTest {
    private val repository = FileRoutineRepository(File("build/tmp/routine-naming-test"), Json)

    /** Use this function to verify that a parent reference in an id is refused before any file access. */
    @Test
    fun rejectsIdThatWouldEscapeTheRepositoryDirectory() {
        listOf("../evil", "nested/child", "..", """back\slash""").forEach { unsafe ->
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.get(RoutineId(unsafe)) }
            }
        }
    }

    /** Use this function to verify that the ids the app actually generates stay accepted. */
    @Test
    fun acceptsGeneratedAndBundledIds() = runBlocking {
        listOf("library-morning-reset", "0f8fad5b-d9cb-469f-a165-70867728950e", "water").forEach { safe ->
            repository.get(RoutineId(safe))
        }
    }
}
