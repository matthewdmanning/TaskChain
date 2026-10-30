package com.taskchain.core.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineRunTask
import com.taskchain.core.model.RoutineTask
import java.io.File
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies Android AtomicFile recovery, quarantine, migration, and serialized concurrent writes. Constructor inputs: none; dependencies: Android instrumentation and storage repositories. */
@RunWith(AndroidJUnit4::class)
class LegacyDataMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    /** Use this test when startup migration rewrites legacy files and quarantines malformed bytes. Inputs: none; dependencies: [migrateLegacyDataFiles] and [Json]. */
    @Test
    fun startupMigrationRewritesLegacyFilesAndQuarantinesMalformedOnes() {
        val dataDirectory = disposableDirectory("legacy-migration")
        val routineFile = File(dataDirectory, "routines/r.json").apply {
            parentFile!!.mkdirs()
            writeText("""{"id":"r","steps":[{"id":"a","cues":[{"cueId":"c"}]}],"currentStepIndex":0}""")
        }

        try {
            migrateLegacyDataFiles(dataDirectory, json)

            val migrated = json.parseToJsonElement(routineFile.readText()).jsonObject
            assertEquals(1, migrated["schemaVersion"]!!.jsonPrimitive.int)
            assertTrue(migrated["data"]!!.jsonObject.containsKey("tasks"))
            val firstMigration = routineFile.readText()
            migrateLegacyDataFiles(dataDirectory, json)
            assertEquals(firstMigration, routineFile.readText())

            val damagedFile = File(dataDirectory, "history/events.json").apply {
                parentFile!!.mkdirs()
                writeText("{not json")
            }
            try {
                migrateLegacyDataFiles(dataDirectory, json)
                throw AssertionError("Expected recovery IOException")
            } catch (error: IOException) {
                assertTrue(error.message!!.contains("quarantined for recovery"))
            }
            assertFalse(damagedFile.exists())
            assertEquals("{not json", dataDirectory.walkTopDown().first { it.name.startsWith("events.json.corrupt-") }.readText())
        } finally {
            dataDirectory.deleteRecursively()
        }
    }

    /** Use this test when AtomicFile must recover a backup even though its base path is absent. Inputs: none; dependencies: [FileRoutineRunRepository] and [AtomicFile]. */
    @Test
    fun activeRunReadsRecoverableBackup() = runBlocking {
        val directory = disposableDirectory("atomic-backup")
        val file = File(directory, "active.json")
        val run = sampleRun(1)
        try {
            File(file.path + ".bak").writeText(json.encodeStored(RoutineRun.serializer(), run))

            assertEquals(run, FileRoutineRunRepository(file, json).observeActive().first())
            assertTrue(file.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    /** Use this test when a newer schema must block both overwrite and delete. Inputs: none; dependencies: [FileRoutineRunRepository] and [UnsupportedSchemaVersionException]. */
    @Test
    fun newerSchemaBlocksActiveRunMutationAndPreservesBytes() = runBlocking {
        val directory = disposableDirectory("future-schema")
        val file = File(directory, "active.json")
        val bytes = """{"schemaVersion":99,"data":{"future":true}}"""
        file.writeText(bytes)
        try {
            val repository = FileRoutineRunRepository(file, json)
            try {
                repository.saveActive(sampleRun(2))
                throw AssertionError("Expected save rejection")
            } catch (_: UnsupportedSchemaVersionException) {
            }
            try {
                repository.clearActive()
                throw AssertionError("Expected delete rejection")
            } catch (_: UnsupportedSchemaVersionException) {
            }
            assertEquals(bytes, file.readText())
        } finally {
            directory.deleteRecursively()
        }
    }

    /** Use this test when malformed repository bytes must move aside without being lost. Inputs: none; dependencies: [FileRoutineRunRepository] and [File.quarantineCorrupt]. */
    @Test
    fun malformedActiveRunIsQuarantinedWithOriginalBytes() = runBlocking {
        val directory = disposableDirectory("atomic-quarantine")
        val file = File(directory, "active.json").apply { writeText("{not json") }
        try {
            try {
                FileRoutineRunRepository(file, json).observeActive().first()
                throw AssertionError("Expected recovery IOException")
            } catch (error: IOException) {
                assertTrue(error.message!!.contains("quarantined for recovery"))
            }
            assertFalse(file.exists())
            assertEquals("{not json", directory.listFiles()!!.single { it.name.startsWith("active.json.corrupt-") }.readText())
        } finally {
            directory.deleteRecursively()
        }
    }

    /** Use this test when quarantine cannot move a path and must report recovery failure. Inputs: none; dependencies: [File.quarantineCorrupt]. */
    @Test
    fun failedQuarantineReportsRecoveryError() {
        try {
            File("storage-quarantine-failure.json").quarantineCorrupt()
            throw AssertionError("Expected quarantine failure")
        } catch (error: IOException) {
            assertTrue(error.message!!.contains("without a parent"))
        }
    }

    /** Use this test when an I/O read failure must propagate without quarantine. Inputs: none; dependencies: [FileRoutineRunRepository] and [AtomicFile]. */
    @Test
    fun readIoFailureIsNotQuarantined() = runBlocking {
        val directory = disposableDirectory("io-failure")
        val file = File(directory, "active.json").apply { mkdirs() }
        try {
            try {
                FileRoutineRunRepository(file, json).observeActive().first()
                throw AssertionError("Expected I/O failure")
            } catch (_: IOException) {
            }
            assertTrue(file.isDirectory)
            assertTrue(directory.listFiles()!!.none { it.name.contains(".corrupt-") })
        } finally {
            directory.deleteRecursively()
        }
    }

    /** Use this test when concurrent transitions must leave one complete, decodable AtomicFile record. Inputs: none; dependencies: [FileRoutineRunRepository], coroutines, and [AtomicFile]. */
    @Test
    fun concurrentActiveWritesRemainDecodable() = runBlocking {
        val directory = disposableDirectory("atomic-concurrent")
        val file = File(directory, "active.json")
        try {
            val repository = FileRoutineRunRepository(file, json)
            val reader = FileRoutineRunRepository(file, json)
            coroutineScope {
                val reads = async {
                    repeat(32) {
                        reader.observeActive().first()?.let { run ->
                            assertTrue(run.startedAtEpochMillis in 0L..15L)
                        }
                    }
                }
                (0 until 16).map { value -> async { repository.saveActive(sampleRun(value.toLong())) } }.awaitAll()
                reads.await()
            }

            val stored = json.parseToJsonElement(file.readText()).jsonObject
            assertEquals(1, stored["schemaVersion"]!!.jsonPrimitive.int)
            assertTrue(stored["data"]!!.jsonObject.containsKey("startedAtEpochMillis"))
            assertTrue(repository.observeActive().first()!!.startedAtEpochMillis in 0L..15L)
        } finally {
            directory.deleteRecursively()
        }
    }

    /** Use this function to create an isolated Android cache directory for one storage test. Input: `name` is the test directory; dependencies: instrumentation cache and [File.deleteRecursively]. */
    private fun disposableDirectory(name: String): File = File(
        InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        name,
    ).apply {
        deleteRecursively()
        mkdirs()
    }

    /** Use this function to create a minimal valid run payload for AtomicFile tests. Input: `startedAt` is the persisted start time; dependencies: domain model constructors. */
    private fun sampleRun(startedAt: Long): RoutineRun = RoutineRun(
        id = RoutineRunId("run"),
        routineId = RoutineId("routine"),
        routineTitle = "Routine",
        tasks = listOf(
            RoutineRunTask(source = RoutineTask(id = com.taskchain.core.model.RoutineTaskId("task"), title = "Task")),
        ),
        currentTaskIndex = 0,
        startedAtEpochMillis = startedAt,
    )
}
