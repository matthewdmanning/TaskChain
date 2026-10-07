package com.taskchain.ui

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
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
 * @param paused Whether the active step timer is currently paused.
 * @param onComplete Invoked after a committed right swipe.
 * @param onSkip Invoked after a committed left swipe.
 * @param onPause Invoked after a long press while running.
 * @param onResume Invoked after a long press while paused.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun Modifier.runnerGestureTracking(
    paused: Boolean,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
): Modifier {
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { CyberPrimitives.IconSizes.dp48.toPx() }
    val completeLabel = stringResource(R.string.runner_gesture_complete)
    val skipLabel = stringResource(R.string.runner_gesture_skip)
    val pauseResumeLabel = stringResource(
        if (paused) R.string.runner_gesture_resume else R.string.runner_gesture_pause,
    )
    val currentComplete by rememberUpdatedState(onComplete)
    val currentSkip by rememberUpdatedState(onSkip)
    val currentPause by rememberUpdatedState(onPause)
    val currentResume by rememberUpdatedState(onResume)

    DisposableEffect(view, paused, swipeThresholdPx) {
        var consumeUntilRelease = false
        var downX = 0f
        var downY = 0f
        var trackingTouch = false

        /** Cancels a Compose press after this detector has claimed the gesture. */
        fun cancelUnderlyingPress(source: MotionEvent) {
            val cancelEvent = MotionEvent.obtain(source).apply { setAction(MotionEvent.ACTION_CANCEL) }
            view.onTouchEvent(cancelEvent)
            cancelEvent.recycle()
        }

        // #fallback: cyberpunkAndroid intentionally supplies visual primitives rather than a gesture recognizer.
        // Android's standard touch and GestureDetector APIs are used so normal taps still reach CyberButton.
        val detector = GestureDetector(
            view.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(event: MotionEvent): Boolean = true

                override fun onLongPress(event: MotionEvent) {
                    consumeUntilRelease = true
                    cancelUnderlyingPress(event)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (paused) currentResume() else currentPause()
                }
            },
        )

        view.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    trackingTouch = true
                }
                MotionEvent.ACTION_UP -> {
                    if (!consumeUntilRelease && trackingTouch) {
                        val horizontalDistance = event.x - downX
                        val verticalDistance = event.y - downY
                        if (
                            horizontalDistance.absoluteValue >= swipeThresholdPx &&
                            horizontalDistance.absoluteValue > verticalDistance.absoluteValue
                        ) {
                            consumeUntilRelease = true
                            cancelUnderlyingPress(event)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (horizontalDistance > 0f) currentComplete() else currentSkip()
                        }
                    }
                    trackingTouch = false
                }
                MotionEvent.ACTION_CANCEL -> trackingTouch = false
            }

            val consume = consumeUntilRelease
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                consumeUntilRelease = false
            }
            consume
        }

        onDispose { view.setOnTouchListener(null) }
    }

    return this.semantics {
        customActions = listOf(
            CustomAccessibilityAction(completeLabel) {
                currentComplete()
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
