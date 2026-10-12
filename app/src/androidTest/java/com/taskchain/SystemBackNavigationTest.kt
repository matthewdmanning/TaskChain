package com.taskchain

import android.view.KeyEvent
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies real system Back key events reach the runner and Home destinations.
 * Constructor inputs: none.
 * Dependencies: MainActivity, Espresso key injection and resetTestAppState.
 */
@RunWith(AndroidJUnit4::class)
class SystemBackNavigationTest {
    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    /** Use this function before each test to remove prior isolated test routines and runs. Inputs: none. Dependencies: instrumentation target context and resetTestAppState. */
    @Before
    fun clearIsolatedTestState() {
        resetTestAppState(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Use this function after each test to remove only state created in the isolated test package. Inputs: none. Dependencies: instrumentation target context and resetTestAppState. */
    @After
    fun clearIsolatedTestStateAfterTest() {
        resetTestAppState(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Use this test to confirm a system Back on the first runner task asks to abort, a second Back dismisses it, and the run stays open. */
    @Test
    fun runnerSystemBack_asksToAbortThenDismissesAndKeepsRun() {
        startMorningResetRun()

        sendSystemBack()
        waitForAbortPrompt(visible = true)

        sendSystemBack()
        waitForAbortPrompt(visible = false)
        activityRule.onNodeWithContentDescription("Task 1: Drink water, unfinished").assertExists()

        sendSystemBack()
        waitForAbortPrompt(visible = true)
        activityRule.onNode(hasText("Abort run") and hasClickAction()).performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Use this test to confirm a system Back on Home leaves the foreground instead of being consumed by the app. */
    @Test
    fun homeSystemBack_leavesTheAppForeground() {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }

        Espresso.pressBackUnconditionally()

        activityRule.waitUntil(10_000) {
            !activityRule.activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
    }

    /** Use this function to open the built-in Morning reset routine in the runner. Inputs: none. Dependencies: activityRule and the seeded Morning reset routine. */
    private fun startMorningResetRun() {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Morning reset").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Start").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithContentDescription("Task 1: Drink water, unfinished").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Use this function to send a Back key to the focused window, including the abort dialog. Inputs: none. Dependencies: Instrumentation.sendKeyDownUpSync, which must run off the main thread. */
    private fun sendSystemBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
    }

    /** Use this function to wait until the abort prompt is shown or gone. Inputs: visible. Dependencies: activityRule. */
    private fun waitForAbortPrompt(visible: Boolean) {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Abort this run?", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty() == visible
        }
    }
}
