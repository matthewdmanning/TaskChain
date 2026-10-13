package com.taskchain

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dropbox.dropshots.Dropshots
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compares stable screens against reference screenshots recorded on the reference device.
 * Constructor inputs: none.
 * Dependencies: MainActivity, Dropshots and resetTestAppState. See docs/testing/device-testing.md.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val dropshots = Dropshots()

    /** Use this function before each test to start from the seeded routines. Inputs: none. Dependencies: instrumentation target context and resetTestAppState. */
    @Before
    fun clearIsolatedTestState() {
        resetTestAppState(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Use this function after each test to remove only state created in the isolated test package. Inputs: none. Dependencies: instrumentation target context and resetTestAppState. */
    @After
    fun clearIsolatedTestStateAfterTest() {
        resetTestAppState(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Use this test to catch visual regressions on the Routines tab with the seeded routines. */
    @Test
    fun routinesTab_matchesReference() {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Morning reset").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.waitForIdle()
        dropshots.assertSnapshot(activityRule.activity, "routines_tab")
    }
}
