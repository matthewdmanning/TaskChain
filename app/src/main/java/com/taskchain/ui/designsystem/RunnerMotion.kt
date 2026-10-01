package com.taskchain.ui.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Step-entry values for the runner's title-to-countdown handoff. */
object RunnerMotion {
    val titleStartSize = 40.sp
    val titleEndSize = 28.sp
    val titleStartOffset = 0.dp
    val titleEndOffset = (-8).dp
    const val durationMillis = 650
    val easing = FastOutSlowInEasing
}
