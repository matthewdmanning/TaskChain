package com.taskchain

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the published navigation and builder flows through the real MainActivity composition. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    /** Use this function to verify visible builder handles reorder collapsed and expanded tasks through real pointer input. */
    @Test
    fun builder_dragHandlesReorderCollapsedAndExpandedTasks() {
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.onAllNodesWithText("Edit").onFirst().performClick()
        activityRule.onNodeWithText("Drink water").performScrollTo()
        activityRule.onAllNodesWithContentDescription("Reorder task", useUnmergedTree = true).assertCountEquals(2)
        activityRule.onAllNodesWithContentDescription("Reorder task", useUnmergedTree = true).onFirst().performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, 100f), delayMillis = 100)
            up()
        }
        activityRule.waitForIdle()
        check(activityRule.onNodeWithText("Review today").fetchSemanticsNode().boundsInRoot.top <
            activityRule.onNodeWithText("Drink water").fetchSemanticsNode().boundsInRoot.top) {
            "Dragging the collapsed first task down must swap the task order"
        }
        activityRule.onAllNodesWithContentDescription("CyberAccordionChevron", useUnmergedTree = true).onFirst().performClick()
        activityRule.onAllNodesWithContentDescription("Reorder task", useUnmergedTree = true).assertCountEquals(2)
        activityRule.onAllNodesWithContentDescription("Reorder task", useUnmergedTree = true).onFirst().performTouchInput {
            down(center)
            advanceEventTime(600)
            moveBy(Offset(0f, 100f), delayMillis = 100)
            up()
        }
        activityRule.waitForIdle()
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
        activityRule.onNodeWithText("Scheduled").assertExists()
        activityRule.onNodeWithText("Manual").assertExists()
        activityRule.onNodeWithText("Completed").assertExists()
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.onNodeWithText("New routine").performClick()

        activityRule.onNodeWithText("Set deadline").assertExists()
        activityRule.onNodeWithText("Set reminder").assertExists()
        activityRule.onAllNodesWithText("Schedule").assertCountEquals(2)
        activityRule.onNodeWithText("Tasks").assertExists()
        val title = "Builder flow ${System.currentTimeMillis()}"
        activityRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true).onFirst().performTextInput(title)
        activityRule.onNodeWithText("Save").performClick()
        activityRule.onAllNodesWithText("Fix the highlighted fields.").assertCountEquals(0)
        activityRule.onNodeWithContentDescription("Add task").performClick()
        activityRule.onNodeWithText("Duration: 0 m 00 s").assertExists()
        activityRule.onAllNodesWithText("Update task").assertCountEquals(0)

        activityRule.onNodeWithText("Save").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText(title).assertExists()
        activityRule.onAllNodesWithText("Routines").onFirst().assertExists()
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

        activityRule.onAllNodesWithText("Cancel").onFirst().performClick()
        activityRule.onNodeWithText("Discard changes?").assertExists()
        activityRule.onNodeWithText("Keep editing").performClick()
        activityRule.onNodeWithText(title).assertExists()

        activityRule.activity.onBackPressedDispatcher.onBackPressed()
        activityRule.waitForIdle()
        activityRule.onNodeWithText("Discard changes?").assertExists()
        activityRule.onNodeWithText("Keep editing").performClick()
        activityRule.onNodeWithText(title).assertExists()

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
        activityRule.onAllNodesWithText("Cancel").onFirst().performClick()

        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Morning reset").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Start").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Task 1 of 2").fetchSemanticsNodes().isNotEmpty()
        }
        activityRule.onNodeWithText("Task 1 of 2").assertExists()

        activityRule.onNodeWithText("Back").performClick()
        activityRule.onNodeWithText("Abort this run?").assertExists()
        activityRule.onNodeWithText("Abort run").performClick()
        activityRule.waitUntil(10_000) {
            activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
