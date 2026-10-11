package com.taskchain

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Exercises saved-data startup failure and Retry on the isolated testing application.
 * Constructor inputs: none. Dependencies: MainActivity, its startup screen, versioned routine files.
 */
@RunWith(AndroidJUnit4::class)
class StartupRecoveryTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var futureFile: File
    private val futureBytes = """{"schemaVersion":999,"data":{}}""".toByteArray()
    private val fixture = object : ExternalResource() {
        /** Use this function before Activity launch to seed an unsupported record; inputs none, dependencies target context and futureFile. */
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(context.packageName == "com.taskchain.testapp") { "Storage tests require the isolated testing app" }
            futureFile = File(context.filesDir, "data/routines/startup-future-test.json")
            check(!futureFile.exists()) { "Refusing to overwrite an existing fixture" }
            check(futureFile.parentFile!!.isDirectory || futureFile.parentFile!!.mkdirs())
            futureFile.writeBytes(futureBytes)
        }

        /** Use this function after Activity disposal to remove only this fixture; inputs none, dependency futureFile. */
        override fun after() {
            if (::futureFile.isInitialized && futureFile.exists()) check(futureFile.delete())
        }
    }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(fixture).around(compose)

    /**
     * Use this function to verify unsupported data is preserved and Retry can reopen a repaired store.
     * Inputs: none. Dependencies: compose rule, futureFile, MainActivity startup flow.
     */
    @Test
    fun futureSchemaIsPreservedAndRetryOpensRepairedStorage() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Couldn’t open saved data").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        assertArrayEquals(futureBytes, futureFile.readBytes())
        assertTrue(futureFile.delete())
        compose.onNodeWithText("Retry").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Routines").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }
}
