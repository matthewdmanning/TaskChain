package com.taskchain.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Task-entry animation values for the runner's title-to-countdown handoff.
 * Completion-effect and readiness-countdown timing feeds persisted run timing, so Running's domain owns it.
 */
object RunnerMotion {
    val titleStartSize = 40.sp
    val titleEndSize = 28.sp
    /** Two 32dp spacing increments above the original 280dp dial cap. */
    val dialDiameter = 344.dp
    val titleStartOffset = 0.dp
    val titleEndOffset = (-8).dp
    const val durationMillis = 650
    val easing = FastOutSlowInEasing
}
