package com.taskchain

import androidx.activity.BackEventCompat
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the published navigation and builder flows through the real MainActivity composition. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    /** Use this function before each activity test to remove prior isolated test routines and runs. Inputs: none. Dependencies: instrumentation target context and resetTestAppState. */
    @Before
    fun clearIsolatedTestState() {
        resetTestAppState(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Use this function after each activity test to remove only state created in the isolated test package. Inputs: none. Dependencies: instrumentation target context and resetTestAppState. */
    @After
    fun clearIsolatedTestStateAfterTest() {
        resetTestAppState(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Use this function to verify visible builder handles reorder collapsed and expanded tasks through real pointer input. */
    @Test
    fun builder_dragHandlesReorderCollapsedAndExpandedTasks() {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.onAllNodesWithText("Edit").onFirst().performClick()
        activityRule.onNodeWithText("Drink water").performScrollTo()
        activityRule.onAllNodesWithContentDescription("Collapse task", useUnmergedTree = true).assertCountEquals(0)
        activityRule.onNode(
            hasContentDescription("Reorder task") and hasParent(hasAnyDescendant(hasText("Drink water"))),
            useUnmergedTree = true,
        ).performScrollTo()
        // The first task follows the routine fields, settings, and Tasks heading.
        // Put its handle near the top so the downward drag stays inside the screen.
        activityRule.onNode(hasScrollToNodeAction()).performScrollToIndex(3)
        activityRule.onNode(
            hasContentDescription("Reorder task") and hasParent(hasAnyDescendant(hasText("Drink water"))),
            useUnmergedTree = true,
        ).performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, center.y * 4f), delayMillis = 100)
            up()
        }
        activityRule.waitForIdle()
        activityRule.onNodeWithText("Review today").performScrollTo()
        check(activityRule.onNodeWithText("Review today").fetchSemanticsNode().boundsInRoot.top <
            activityRule.onNodeWithText("Drink water").fetchSemanticsNode().boundsInRoot.top) {
            "Dragging the collapsed first task down must swap the task order"
        }
        activityRule.onNode(
            isToggleable() and hasAnyDescendant(hasText("Review today")),
            useUnmergedTree = true,
        ).performClick()
        activityRule.onNodeWithContentDescription("Collapse task", useUnmergedTree = true).performScrollTo()
        activityRule.onNodeWithContentDescription("Collapse task", useUnmergedTree = true).assertExists().performClick()
        activityRule.onAllNodesWithContentDescription("Collapse task", useUnmergedTree = true).assertCountEquals(0)
        activityRule.onNode(
            isToggleable() and hasAnyDescendant(hasText("Review today")),
            useUnmergedTree = true,
        ).performClick()
        activityRule.onNode(
            hasContentDescription("Reorder task") and hasParent(hasAnyDescendant(hasText("Review today"))),
            useUnmergedTree = true,
        ).performScrollTo()
        activityRule.onNode(hasScrollToNodeAction()).performScrollToIndex(3)
        activityRule.onNode(
            hasContentDescription("Reorder task") and hasParent(hasAnyDescendant(hasText("Review today"))),
            useUnmergedTree = true,
        ).performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, center.y * 4f), delayMillis = 100)
            up()
        }
        activityRule.waitForIdle()
        activityRule.onNodeWithContentDescription("Collapse task", useUnmergedTree = true).performScrollTo()
        activityRule.onNodeWithContentDescription("Collapse task", useUnmergedTree = true).performClick()
        activityRule.onAllNodesWithContentDescription("Collapse task", useUnmergedTree = true).assertCountEquals(0)
        activityRule.onNodeWithText("Drink water").performScrollTo()
        check(activityRule.onNodeWithText("Drink water").fetchSemanticsNode().boundsInRoot.top <
            activityRule.onNodeWithText("Review today").fetchSemanticsNode().boundsInRoot.top) {
            "Dragging the expanded first task down must swap the task order"
        }
    }

    /** Verifies routine-owned controls, terminal task creation, autosaved editing, and Save navigation. */
    @Test
    fun builder_savesRoutineOwnedSettingsAndReturnsToRoutines() {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
        listOf("Scheduled", "Manual", "Completed").forEach { section ->
            activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(section, ignoreCase = true))
            activityRule.onNode(hasText(section, ignoreCase = true)).assertExists()
        }
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.onNodeWithText("New routine").performClick()

        val title = "Builder flow ${System.currentTimeMillis()}"
        activityRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true).onFirst().performTextInput(title)
        Espresso.closeSoftKeyboard()
        activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(isToggleable() and hasText("Sound"))
        activityRule.onNode(isToggleable() and hasText("Sound")).performScrollTo().performClick()
        activityRule.onNode(isToggleable() and hasText("Vibrate")).performScrollTo().performClick()
        activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Add task"))
        activityRule.onNodeWithText("Add task").performClick()
        val taskTitle = "Builder task"
        activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Task name"))
        activityRule.onNodeWithText("Task name").performClick()
        activityRule.onNode(hasSetTextAction() and hasAnyAncestor(isToggleable()), useUnmergedTree = true)
            .performTextInput(taskTitle)
        Espresso.closeSoftKeyboard()

        activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Save"))
        activityRule.onNodeWithText("Save").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText(title).assertExists()
        activityRule.onAllNodesWithText("Routines").onFirst().assertExists()
        val savedRoutine = runBlocking {
            testAppContainer(InstrumentationRegistry.getInstrumentation().targetContext)
                .routines.observeAll().first().single { it.title == title }
        }
        check(!savedRoutine.soundEnabled && !savedRoutine.vibrateEnabled) {
            "Saved routine must retain the Sound and Vibrate settings"
        }
        check(savedRoutine.tasks.single().title == taskTitle) {
            "Saved routine must retain the task title"
        }
    }

    /** Use this test when verifying that Cancel and Android Back both offer discard choices without losing a draft. */
    @Test
    fun builderCancelAndBack_offerDiscardChoicesAndKeepDraftInput() {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("New routine").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("New routine").performClick()

        val title = "Instrumentation ${System.currentTimeMillis()}"
        activityRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true).onFirst().performTextInput(title)
        activityRule.onNodeWithText(title).assertExists()

        activityRule.runOnUiThread {
            val dispatcher = activityRule.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 0f, 0.5f, BackEventCompat.EDGE_LEFT))
        }
        activityRule.onAllNodesWithText("Discard changes?").assertCountEquals(0)
        activityRule.runOnUiThread {
            activityRule.activity.onBackPressedDispatcher.dispatchOnBackCancelled()
        }
        activityRule.onNodeWithText(title).assertExists()
        activityRule.onAllNodesWithText("Discard changes?").assertCountEquals(0)

        activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Discard"))
        activityRule.onNodeWithText("Discard").performClick()
        activityRule.onNodeWithText("Discard changes?").assertExists()
        activityRule.onNodeWithText("Keep editing").performClick()
        activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(title))
        activityRule.onNodeWithText(title).assertExists()

        activityRule.runOnUiThread { activityRule.activity.onBackPressedDispatcher.onBackPressed() }
        activityRule.waitForIdle()
        activityRule.onNodeWithText("Discard changes?").assertExists()
        activityRule.onNodeWithText("Keep editing").performClick()
        activityRule.onNodeWithText(title).assertExists()

        activityRule.runOnUiThread { activityRule.activity.onBackPressedDispatcher.onBackPressed() }
        activityRule.onNodeWithText("Discard changes").performClick()
        activityRule.onNodeWithText("New routine").assertExists()
    }

    /** Use this test when confirming that routine-card text is inert and Edit/Start remain separate actions. */
    @Test
    fun routines_keepCardTextInertAndExposeSeparateEditAndStartActions() {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Morning reset").fetchSemanticsNodes().isNotEmpty()
        }

        val cardText = activityRule.onNodeWithText("Morning reset")
        cardText.assertHasNoClickAction()
        cardText.performTouchInput { click() }
        activityRule.onAllNodesWithText("Routine builder").assertCountEquals(0)
        activityRule.onNodeWithText("Edit").assertExists()
        activityRule.onNodeWithText("Start").assertExists()

        activityRule.onNodeWithText("Edit").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routine builder").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Morning reset").assertExists()
        activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Discard"))
        activityRule.onNodeWithText("Discard").performClick()

        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Morning reset").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Start").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithContentDescription("Task 1: Drink water, unfinished").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithContentDescription("Task 1: Drink water, unfinished").assertExists()
        activityRule.waitForIdle()

        activityRule.runOnUiThread { activityRule.activity.onBackPressedDispatcher.onBackPressed() }
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Abort this run?", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Abort this run?", useUnmergedTree = true).assertExists()
        activityRule.runOnUiThread { activityRule.activity.onBackPressedDispatcher.onBackPressed() }
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Abort this run?", useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
        activityRule.onAllNodesWithText("Abort this run?", useUnmergedTree = true).assertCountEquals(0)
        activityRule.onNodeWithText("Back").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Abort this run?", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Abort this run?", useUnmergedTree = true).assertExists()
        activityRule.onNode(hasText("Abort run") and hasClickAction()).performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
