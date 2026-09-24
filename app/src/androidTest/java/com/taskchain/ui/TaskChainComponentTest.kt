package com.taskchain.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.taskchain.ui.designsystem.TaskChainTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the accessible semantics and state transitions of reusable Compose controls. */
@RunWith(AndroidJUnit4::class)
class TaskChainComponentTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Use this test when confirming that a labeled switch exposes one merged, stateful target. */
    @Test
    fun labeledSwitchRow_mergesLabelAndTogglesExactlyOncePerClick() {
        var checked by mutableStateOf(false)
        composeRule.setContent {
            TaskChainTheme("system") {
                LabeledSwitchRow("Sound", checked) { checked = it }
            }
        }

        val switchMatcher = isToggleable() and androidx.compose.ui.test.hasText("Sound")
        composeRule.onAllNodes(switchMatcher).assertCountEquals(1)
        val switch = composeRule.onNode(switchMatcher)
        switch.assertIsOff()
        switch.assertTextContains("Sound")
        switch.performClick()
        switch.assertIsOn()
        switch.performClick()
        switch.assertIsOff()
    }

    /** Use this test when confirming that every weekday chip exposes and updates selected semantics. */
    @Test
    fun weekdaySelector_eachDayCanBeSelected() {
        var selectedDays by mutableStateOf(emptySet<Int>())
        composeRule.setContent {
            TaskChainTheme("system") {
                WeekdaySelector(selectedDays) { day ->
                    selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                }
            }
        }

        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            val chip = composeRule.onNodeWithText(day)
            chip.assertIsNotSelected()
            chip.performClick()
            chip.assertIsSelected()
        }
    }

    /** Use this test when confirming that an empty Today list exposes its create-routine call to the user. */
    @Test
    fun routineList_emptyStateShowsCreateAction() {
        var createCount = 0
        composeRule.setContent {
            TaskChainTheme("system") {
                RoutineList(
                    routines = emptyList(),
                    padding = PaddingValues(),
                    listState = rememberLazyListState(),
                    emptyText = "Nothing scheduled",
                    onCreate = { createCount += 1 },
                    onStart = {},
                )
            }
        }

        composeRule.onNodeWithText("Nothing scheduled").assertExists()
        composeRule.onNodeWithText("New routine").assertExists().performClick()
        check(createCount == 1) { "Expected New routine to invoke onCreate exactly once" }
    }
}
