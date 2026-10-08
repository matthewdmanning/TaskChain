package com.taskchain.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.taskchain.R
import kotlin.math.absoluteValue

/**
 * Adds runner-wide horizontal swipe and long-press actions while leaving the existing CyberButton controls
 * available for discoverability and accessibility.
 *
 * @param paused Whether the active task timer is currently paused.
 * @param onAdvance Invoked after a committed right swipe.
 * @param onSkip Invoked after a committed left swipe.
 * @param onPause Invoked after a long press while running.
 * @param onResume Invoked after a long press while paused.
 */
@Composable
internal fun Modifier.runnerGestureTracking(
    paused: Boolean,
    onAdvance: () -> Unit,
    onSkip: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val swipeThresholdPx = with(density) { CyberPrimitives.IconSizes.dp48.toPx() }
    val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis
    val completeLabel = stringResource(R.string.punch_gesture_advance)
    val skipLabel = stringResource(R.string.runner_gesture_skip)
    val pauseResumeLabel = stringResource(
        if (paused) R.string.runner_gesture_resume else R.string.runner_gesture_pause,
    )
    val currentAdvance by rememberUpdatedState(onAdvance)
    val currentSkip by rememberUpdatedState(onSkip)
    val currentPause by rememberUpdatedState(onPause)
    val currentResume by rememberUpdatedState(onResume)
    val currentPaused by rememberUpdatedState(paused)

    // #fallback: cyberpunkAndroid intentionally supplies visual primitives rather than a gesture recognizer.
    // Compose observes before children, then consumes only a committed gesture so taps and scrolling remain native.
    return pointerInput(swipeThresholdPx, longPressTimeoutMillis, viewConfiguration.touchSlop) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val pointerId = down.id
            var released = false
            var cancelled = false
            var recognized = false
            var longPressEligible = true
            var horizontalDistance = 0f
            var verticalDistance = 0f

            withTimeoutOrNull(longPressTimeoutMillis) {
                while (!released && !cancelled && !recognized) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId }
                    if (change == null || change.changedToUp()) {
                        released = true
                        continue
                    }
                    horizontalDistance = change.position.x - down.position.x
                    verticalDistance = change.position.y - down.position.y
                    if (
                        horizontalDistance.absoluteValue >= swipeThresholdPx &&
                        horizontalDistance.absoluteValue > verticalDistance.absoluteValue
                    ) {
                        recognized = true
                        change.consume()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (horizontalDistance > 0f) currentAdvance() else currentSkip()
                    } else if (
                        verticalDistance.absoluteValue >= viewConfiguration.touchSlop &&
                        verticalDistance.absoluteValue >= horizontalDistance.absoluteValue
                    ) {
                        cancelled = true
                    } else if (
                        horizontalDistance.absoluteValue >= viewConfiguration.touchSlop
                    ) {
                        longPressEligible = false
                    }
                }
            }

            if (!released && !cancelled && !recognized && longPressEligible) {
                recognized = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (currentPaused) currentResume() else currentPause()
            }

            while (!cancelled && !released) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null) {
                    released = true
                } else {
                    if (!recognized) {
                        horizontalDistance = change.position.x - down.position.x
                        verticalDistance = change.position.y - down.position.y
                        if (horizontalDistance.absoluteValue >= swipeThresholdPx &&
                            horizontalDistance.absoluteValue > verticalDistance.absoluteValue) {
                            recognized = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (horizontalDistance > 0f) currentAdvance() else currentSkip()
                        } else if (verticalDistance.absoluteValue >= viewConfiguration.touchSlop &&
                            verticalDistance.absoluteValue >= horizontalDistance.absoluteValue) cancelled = true
                    }
                    if (recognized) change.consume()
                    if (change.changedToUpIgnoreConsumed()) released = true
                }
            }
        }
    }.semantics {
        customActions = listOf(
            CustomAccessibilityAction(completeLabel) {
                currentAdvance()
                true
            },
            CustomAccessibilityAction(skipLabel) {
                currentSkip()
                true
            },
            CustomAccessibilityAction(pauseResumeLabel) {
                if (paused) currentResume() else currentPause()
                true
            },
        )
    }
}
