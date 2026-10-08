package com.taskchain.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies on Android that the startup migration rewrites legacy JSON files with the current keys. */
@RunWith(AndroidJUnit4::class)
class LegacyDataMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun startupMigrationRewritesLegacyFilesPermanentlyAndSkipsUnreadableOnes() {
        val dataDirectory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "legacy-migration")
        dataDirectory.deleteRecursively()
        val routineFile = File(dataDirectory, "routines/r.json").apply {
            parentFile!!.mkdirs()
            writeText("""{"id":"r","steps":[{"id":"a","cues":[{"cueId":"c"}]}],"currentStepIndex":0}""")
        }
        val damagedFile = File(dataDirectory, "history/events.json").apply {
            parentFile!!.mkdirs()
            writeText("{not json")
        }

        migrateLegacyDataFiles(dataDirectory, json)

        assertEquals(
            json.parseToJsonElement("""{"id":"r","tasks":[{"id":"a","subtasks":[{"subtaskId":"c"}]}],"currentTaskIndex":0}"""),
            json.parseToJsonElement(routineFile.readText()),
        )
        assertEquals("{not json", damagedFile.readText())
        dataDirectory.deleteRecursively()
    }
}
