package com.taskchain.ui.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Task-entry values for the runner's title-to-countdown handoff. */
object RunnerMotion {
    val titleStartSize = 40.sp
    val titleEndSize = 28.sp
    /** Two 32dp spacing increments above the original 280dp dial cap. */
    val dialDiameter = 344.dp
    val titleStartOffset = 0.dp
    val titleEndOffset = (-8).dp
    const val durationMillis = 650
    const val completionDurationMillis = 350
    const val taskReadyGetReadyDurationMillis = 2_000L
    const val taskReadyPhaseDurationMillis = 1_000L
    const val taskReadyTransitionDurationMillis = 5_000L
    val easing = FastOutSlowInEasing
}
