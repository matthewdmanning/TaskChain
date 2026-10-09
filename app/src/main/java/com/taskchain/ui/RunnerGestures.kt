package com.taskchain.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.taskchain.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign

/** Share of finger travel shown on screen; below 1 gives the content a resistive feel. */
private const val SWIPE_DRAG_RESISTANCE = 0.5f

/** Fade in plus fade out time of the pause or play flash, per phase (2.5 s in total). */
private const val TOGGLE_FLASH_PHASE_MILLIS = 1250

/** Symmetric ease-in-out (sine): the fade eases in and out with the same gentle shape, no fast-out start. */
private val TOGGLE_FLASH_EASING = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/**
 * Observable runner gesture progress shared by the gesture modifiers and their visual cues.
 *
 * Swipe progress runs 0..1 toward the commit distance and `swipeArmed` means releasing now commits.
 * The toggle flash is a short fade of a Pause or Play icon confirming a double tap.
 */
@Stable
internal class RunnerGestureState(
    private val scope: CoroutineScope,
    val swipeCommitPx: Float,
) {
    private var dragPx by mutableFloatStateOf(0f)
    private var settleJob: Job? = null
    private var flashJob: Job? = null

    var flashAlpha by mutableFloatStateOf(0f)
        private set
    var flashShowsPause by mutableStateOf(true)
        private set

    /** Signed finger travel in px; positive is a right swipe. */
    val swipeDirection: Float get() = dragPx.sign
    val swipeProgress: Float get() = (dragPx.absoluteValue / swipeCommitPx).coerceIn(0f, 1f)
    val swipeArmed: Boolean get() = dragPx.absoluteValue >= swipeCommitPx

    /** Translation to apply to the content that follows the finger. */
    val contentShiftPx: Float get() = dragPx * SWIPE_DRAG_RESISTANCE

    internal fun dragTo(travelPx: Float) {
        settleJob?.cancel()
        dragPx = travelPx
    }

    internal fun settleSwipe() {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(dragPx, 0f, animationSpec = spring(stiffness = Spring.StiffnessMedium)) { value, _ -> dragPx = value }
        }
    }

    /** Use this function to flash the icon of the state the timer just entered: Pause when paused, else Play. */
    internal fun flashToggle(showPause: Boolean) {
        flashJob?.cancel()
        flashShowsPause = showPause
        flashJob = scope.launch {
            animate(0f, 1f, animationSpec = tween(TOGGLE_FLASH_PHASE_MILLIS, easing = TOGGLE_FLASH_EASING)) { v, _ -> flashAlpha = v }
            animate(1f, 0f, animationSpec = tween(TOGGLE_FLASH_PHASE_MILLIS, easing = TOGGLE_FLASH_EASING)) { v, _ -> flashAlpha = v }
        }
    }
}

@Composable
internal fun rememberRunnerGestureState(): RunnerGestureState {
    val scope = rememberCoroutineScope()
    val commitPx = with(LocalDensity.current) { dimensionResource(R.dimen.runner_swipe_commit_distance).toPx() }
    return remember(scope, commitPx) { RunnerGestureState(scope, commitPx) }
}

/** Use this modifier on the content that should follow the finger during a swipe. */
internal fun Modifier.followRunnerSwipe(state: RunnerGestureState): Modifier =
    graphicsLayer { translationX = state.contentShiftPx }

/**
 * Horizontal swipe with the standard swipe-to-act contract: content follows the finger, a haptic tick marks the
 * commit threshold, the action runs only on release past it, and releasing earlier or dragging back cancels.
 *
 * @param onPrevious Invoked when a right swipe is released past the commit distance.
 * @param onSkip Invoked when a left swipe is released past the commit distance.
 */
@Composable
internal fun Modifier.runnerSwipeGesture(
    state: RunnerGestureState,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val currentPrevious by rememberUpdatedState(onPrevious)
    val currentSkip by rememberUpdatedState(onSkip)

    // #fallback: cyberpunkAndroid supplies visual primitives, not a gesture recognizer. Observing in the Initial
    // pass lets this run before children while taps and vertical scrolling stay native until a swipe is claimed.
    return pointerInput(state, touchSlop) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var claimed = false
            var wasArmed = false
            var released = false
            while (!released) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                if (change == null) break
                released = change.changedToUpIgnoreConsumed()
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (!claimed) {
                    if (dy.absoluteValue >= touchSlop && dy.absoluteValue >= dx.absoluteValue) break
                    claimed = dx.absoluteValue >= touchSlop && dx.absoluteValue > dy.absoluteValue
                }
                if (claimed && !released) {
                    change.consume()
                    state.dragTo(dx)
                    if (state.swipeArmed != wasArmed) {
                        wasArmed = state.swipeArmed
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
            if (claimed && released && state.swipeArmed) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                if (state.swipeDirection > 0f) currentPrevious() else currentSkip()
            }
            state.settleSwipe()
        }
    }
}

/** Double tap toggles pause and resume. Single taps are not consumed, so controls underneath keep working. */
@Composable
internal fun Modifier.runnerDoubleTapGesture(onToggle: () -> Unit): Modifier {
    val currentToggle by rememberUpdatedState(onToggle)
    return pointerInput(Unit) { detectTapGestures(onDoubleTap = { currentToggle() }) }
}

/**
 * Keeps the gestures reachable without touch gestures: previous, skip and pause or resume are also exposed as
 * accessibility custom actions on the runner surface.
 */
@Composable
internal fun Modifier.runnerGestureActions(
    paused: Boolean,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
): Modifier {
    val previousLabel = stringResource(R.string.runner_gesture_previous)
    val skipLabel = stringResource(R.string.runner_gesture_skip)
    val pauseResumeLabel = stringResource(if (paused) R.string.runner_gesture_resume else R.string.runner_gesture_pause)
    return semantics {
        customActions = listOf(
            CustomAccessibilityAction(previousLabel) { onPrevious(); true },
            CustomAccessibilityAction(skipLabel) { onSkip(); true },
            CustomAccessibilityAction(pauseResumeLabel) { if (paused) onResume() else onPause(); true },
        )
    }
}
