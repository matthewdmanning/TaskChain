package com.taskchain.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.example.cyberpunkandroid.config.CyberConfig
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.icons.CyberDialTicks
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.RoutineRun
import kotlinx.coroutines.delay

/** Adds a time-derived cyberpunkAndroid progress halo without duplicating the runner timer text. */
@Composable
internal fun RunnerFeatureOverlay(
    container: AppContainer,
    run: RoutineRun,
    modifier: Modifier = Modifier,
) {
    val current = run.steps.getOrNull(run.currentStepIndex) ?: return
    val timerSeconds = current.source.timerSeconds ?: return
    var nowEpochMillis by remember(run.id, run.currentStepIndex) { mutableLongStateOf(container.now()) }

    LaunchedEffect(run.id, run.currentStepIndex) {
        while (true) {
            nowEpochMillis = container.now()
            delay(CyberPrimitives.Durations.ms300.toLong())
        }
    }

    val remainingMillis = container.runEngine.remainingMillis(run, nowEpochMillis)
    val elapsedProgress = countdownProgress(timerSeconds, remainingMillis)
    val remainingProgress = (PROGRESS_END - elapsedProgress).coerceIn(PROGRESS_START, PROGRESS_END)
    val paintedProgress by animateFloatAsState(
        targetValue = remainingProgress,
        animationSpec = tween(
            durationMillis = CyberPrimitives.Durations.ms300,
            easing = CyberConfig.Easings.CyberEasing,
        ),
        label = "RunnerRemainingTimeHalo",
    )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val availableDiameter = minOf(maxWidth, maxHeight) - CyberPrimitives.Spacing.dp64
        val diameter = minOf(
            availableDiameter,
            dimensionResource(R.dimen.runner_progress_ring_max_diameter),
        ).coerceAtLeast(CyberPrimitives.IconSizes.dp64)

        Box(
            modifier = Modifier
                .size(diameter)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = paintedProgress,
                        range = PROGRESS_RANGE,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            CyberDialTicks(
                size = diameter,
                color = CyberTheme.colors.border,
                tickLength = CyberPrimitives.Spacing.dp8,
                majorTickLength = CyberPrimitives.Spacing.dp16,
                strokeWidth = CyberPrimitives.BorderWidths.dp1,
            )

            // #fallback: cyberpunkAndroid provides CyberDialTicks but no progress-aware mask;
            // Compose draw clipping is used only to reveal the library component by remaining-time fraction.
            CyberDialTicks(
                modifier = Modifier.drawWithContent {
                    val paintedArea = Path().apply {
                        moveTo(center.x, center.y)
                        arcTo(
                            rect = Rect(0f, 0f, size.width, size.height),
                            startAngleDegrees = TOP_START_ANGLE_DEGREES,
                            sweepAngleDegrees = FULL_CIRCLE_DEGREES * paintedProgress,
                            forceMoveTo = false,
                        )
                        close()
                    }
                    clipPath(paintedArea) { this@drawWithContent.drawContent() }
                },
                size = diameter,
                color = CyberTheme.colors.primary,
                tickLength = CyberPrimitives.Spacing.dp12,
                majorTickLength = CyberPrimitives.Spacing.dp24,
                strokeWidth = CyberPrimitives.BorderWidths.dp2,
            )
        }
    }
}

private const val PROGRESS_START = 0f
private const val PROGRESS_END = 1f
private val PROGRESS_RANGE = PROGRESS_START..PROGRESS_END
private const val TOP_START_ANGLE_DEGREES = -90f
private const val FULL_CIRCLE_DEGREES = 360f
