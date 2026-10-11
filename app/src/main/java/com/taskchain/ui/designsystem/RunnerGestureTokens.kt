package com.taskchain.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cyberpunkandroid.config.CyberPrimitives

/**
 * A size that follows the screen's smaller dimension and stays inside a usable range.
 *
 * @param fraction Share of the smaller screen dimension.
 * @param min Lower bound, so the size stays usable on small screens.
 * @param max Upper bound, so the size stays proportionate on tablets.
 */
class ScreenRelativeSize(private val fraction: Float, private val min: Dp, private val max: Dp) {
    fun resolve(smallerScreenDimension: Dp): Dp = (smallerScreenDimension * fraction).coerceIn(min, max)

    /** Use this in composition to resolve against the current window. */
    @Composable
    fun current(): Dp = LocalConfiguration.current.let { resolve(minOf(it.screenWidthDp, it.screenHeightDp).dp) }
}

/**
 * Semantic, screen-relative sizes for runner gestures. Bounds are multiples of one grid unit; the fractions give
 * the earlier fixed values (72, 192 and 40dp) at a 400dp-wide phone.
 */
object RunnerGestureTokens {
    private val unit: Dp = CyberPrimitives.Spacing.dp8

    /** Finger travel at which releasing a swipe commits its action. */
    val swipeCommitDistance = ScreenRelativeSize(fraction = 0.18f, min = unit * 7, max = unit * 12)

    /** Size of the large symbol shown while swiping. */
    val swipeCueIconSize = ScreenRelativeSize(fraction = 0.48f, min = unit * 18, max = unit * 32)

    /** Glow spread around the Pause or Play flash icon. */
    val toggleFlashGlowRadius = ScreenRelativeSize(fraction = 0.10f, min = unit * 3, max = unit * 7)
}
