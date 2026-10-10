package com.taskchain.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import com.example.cyberpunkandroid.components.CyberGlowIcon
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.icons.SemanticIcons
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.ui.designsystem.RunnerGestureTokens

/** Fraction of full opacity the swipe symbol reaches at the commit distance. */
private const val SWIPE_CUE_MAX_ALPHA = 0.5f

/** Scale pop once the swipe is armed, so crossing the commit distance is visible without text. */
private const val SWIPE_CUE_ARMED_SCALE = 1.15f

/** Scale of the swipe symbol before any drag; it grows to full size at the commit distance. */
private const val SWIPE_CUE_START_SCALE = 0.8f

/**
 * Large centered symbol shown while swiping: the same caution symbol as a skipped task for a left swipe, a back
 * arrow for a right swipe. It fades in toward 50% opacity and pops once releasing would commit.
 */
@Composable
internal fun BoxScope.RunnerSwipeCue(state: RunnerGestureState) {
    val progress = state.swipeProgress
    if (progress <= 0f) return
    val previous = state.swipeDirection > 0f
    val scale = if (state.swipeArmed) SWIPE_CUE_ARMED_SCALE else SWIPE_CUE_START_SCALE + (1f - SWIPE_CUE_START_SCALE) * progress
    CyberIcon(
        iconRes = if (previous) CyberIcons.ArrowLeft else SemanticIcons.Caution,
        contentDescription = null,
        tint = if (previous) CyberTheme.colors.secondary else CyberTheme.semantics.colors.warning,
        size = RunnerGestureTokens.swipeCueIconSize.current(),
        modifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer {
                alpha = SWIPE_CUE_MAX_ALPHA * progress
                scaleX = scale
                scaleY = scale
            },
    )
}

/** Peak opacity of the pause or play flash. */
private const val TOGGLE_FLASH_PEAK_ALPHA = 0.875f

/** Share of the icon color kept for the icon body; the glow keeps the full color so the body is softer in dark mode. */
private const val TOGGLE_FLASH_BODY_BRIGHTNESS = 0.6f

/** Half-screen Pause or Play icon that fades in and out after a double tap. */
@Composable
internal fun BoxScope.RunnerToggleFlash(state: RunnerGestureState) {
    val alpha = state.flashAlpha
    if (alpha <= 0f) return
    val showPause = state.flashShowsPause
    BoxWithConstraints(modifier = Modifier.align(Alignment.Center).fillMaxSize()) {
        val color = if (showPause) CyberTheme.semantics.colors.warning else CyberTheme.semantics.colors.success
        CyberGlowIcon(
            iconRes = if (showPause) CyberIcons.Pause else CyberIcons.Play,
            contentDescription = null,
            color = lerp(Color.Black, color, TOGGLE_FLASH_BODY_BRIGHTNESS),
            glowColor = color,
            radius = RunnerGestureTokens.toggleFlashGlowRadius.current(),
            modifier = Modifier
                .align(Alignment.Center)
                .size(minOf(maxWidth, maxHeight / 2))
                .graphicsLayer { this.alpha = alpha * TOGGLE_FLASH_PEAK_ALPHA },
        )
    }
}
