package com.taskchain.core.ui

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests predictive progress, cancellation, and completion through ComponentActivity's dispatcher. */
@RunWith(AndroidJUnit4::class)
class NavigationBackHandlerTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Use this function to verify gesture previews reset without committing the Back action; depends on compose's dispatcher and NavigationBackHandler. */
    @Test
    fun cancelledGestureResetsPreviewAndCompletedBackCommitsOnce() {
        val completed = mutableIntStateOf(0)
        compose.setContent {
            val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
            NavigationBackHandler(state = state, onBackCompleted = { completed.intValue++ })
            val progress = (state.transitionState as? NavigationEventTransitionState.InProgress)
                ?.latestEvent?.progress ?: 0f
            Text("$progress:${completed.intValue}", modifier = androidx.compose.ui.Modifier.navigationBackPreview(state.transitionState))
        }
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT),
            )
            compose.activity.onBackPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(100f, 0f, 0.5f, BackEventCompat.EDGE_LEFT),
            )
        }
        compose.onNodeWithText("0.5:0").assertExists()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        compose.onNodeWithText("0.0:0").assertExists()
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(100f, 0f, 0f, BackEventCompat.EDGE_RIGHT),
            )
            compose.activity.onBackPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0f, 0f, 0.75f, BackEventCompat.EDGE_RIGHT),
            )
        }
        compose.onNodeWithText("0.75:0").assertExists()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("0.0:1").assertExists()
    }
}
