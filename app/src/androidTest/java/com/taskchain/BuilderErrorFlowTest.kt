package com.taskchain

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies through the real builder that save errors are visible, announced, and cleared without saving data. */
@RunWith(AndroidJUnit4::class)
class BuilderErrorFlowTest {
    @get:Rule
    val activityRule = createAndroidComposeRule<MainActivity>()

    private val subtaskTitleError = "Enter a subtask title."

    /** Use this function to match the subtask title field while it carries its error semantics. */
    private fun subtaskTitleFieldWithError() =
        hasSetTextAction() and SemanticsMatcher.expectValue(SemanticsProperties.Error, subtaskTitleError)

    /** Use this function to reach a builder item, because the lazy list composes only the items that are on screen. */
    private fun scrollBuilderToAndClick(text: String) {
        activityRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        activityRule.onNodeWithText(text).performClick()
    }

    @Test
    fun saveErrorsOpenTheTaskAreAnnouncedAndClearWhenTheUserTypes() {
        activityRule.waitUntil(10_000) { activityRule.onAllNodesWithText("Routines").fetchSemanticsNodes().isNotEmpty() }
        activityRule.onNodeWithText("Routines").performClick()
        activityRule.onNodeWithText("New routine").performClick()
        activityRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true).onFirst()
            .performTextInput("Error flow ${System.currentTimeMillis()}")
        scrollBuilderToAndClick("Add task")
        scrollBuilderToAndClick("Add subtask")

        // No error is shown before the first save attempt.
        activityRule.onAllNodesWithText(subtaskTitleError).assertCountEquals(0)

        // Collapse the task, so the error is hidden before Save.
        // cyberpunkAndroid v1.0.8 labels the chevron "CyberTextGlow", so match it as the toggle in the accordion header.
        activityRule.onNode(isToggleable() and hasParent(hasContentDescription("CyberAccordion")), useUnmergedTree = true)
            .performClick()
        activityRule.waitForIdle()
        scrollBuilderToAndClick("Save")
        activityRule.waitForIdle()

        // Save opens the task with the error and shows the message next to the field, with error semantics.
        activityRule.waitUntil(5_000) { activityRule.onAllNodesWithText(subtaskTitleError).fetchSemanticsNodes().isNotEmpty() }
        activityRule.onAllNodes(subtaskTitleFieldWithError()).assertCountEquals(1)
        activityRule.onAllNodesWithText("Enter a task name.").fetchSemanticsNodes().isNotEmpty().let { check(it) {
            "A blank task name must also show its error after Save"
        } }

        // Typing a title removes the message and the error semantics immediately.
        activityRule.onNode(subtaskTitleFieldWithError()).performTextInput("Stretch")
        activityRule.waitForIdle()
        activityRule.onAllNodesWithText(subtaskTitleError).assertCountEquals(0)
        activityRule.onAllNodes(subtaskTitleFieldWithError()).assertCountEquals(0)

        // Leave without saving, so the device data does not change.
        scrollBuilderToAndClick("Discard")
        activityRule.onNodeWithText("Discard changes").performClick()
    }
}
