package com.taskchain.features.running.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.example.cyberpunkandroid.components.CyberDialTicks
import com.example.cyberpunkandroid.components.CyberSectorRim
import com.example.cyberpunkandroid.components.GlowingText
import com.example.cyberpunkandroid.effects.cyberRadialPulse
import com.example.cyberpunkandroid.effects.rememberCyberRadialPulse
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.icons.SemanticIcons
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.core.model.MILLIS_PER_SECOND
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.features.running.domain.RunReadiness

/** Paint elapsed countdown progress clockwise over the library's dial ticks with time-responsive effects and styles. */
@Composable
internal fun RunCountdownDial(
    timer: String,
    progress: Float,
    remainingMillis: Long?,
    diameter: Dp,
    behindSubtaskSchedule: Boolean,
    status: RunTaskStatus,
    paused: Boolean,
    completedPulse: Boolean,
    foreground: Boolean,
    animationsEnabled: Boolean,
) {
    val painted by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        animationSpec = if (animationsEnabled) tween(800) else snap(),
        label = "CountdownTicks",
    )

    val isOvertime = remainingMillis != null && remainingMillis < 0L
    val remainingRatio = (1f - progress).coerceIn(0f, 1f)
    val isUrgent = isOvertime || remainingRatio <= 0.15f
    val isWarning = !isUrgent && remainingRatio <= 0.35f

    val targetAccentColor = when {
        status == RunTaskStatus.COMPLETED -> TaskChainDesignSystem.Colors.success
        status == RunTaskStatus.SKIPPED -> TaskChainDesignSystem.Colors.warning
        behindSubtaskSchedule -> TaskChainDesignSystem.Colors.warning
        isOvertime -> TaskChainDesignSystem.Colors.error
        isUrgent || isWarning -> TaskChainDesignSystem.Colors.warning
        else -> TaskChainDesignSystem.Colors.info
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = if (animationsEnabled) tween(600) else snap(),
        label = "DialColorAnimation",
    )

    val pulseAlpha = if (foreground && animationsEnabled && isUrgent && status == RunTaskStatus.PENDING && !paused) {
        val infiniteTransition = rememberInfiniteTransition(label = "DialPulseTransition")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (isOvertime) 300 else 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "DialPulseAlpha",
        )
        alpha
    } else 1f

    val currentGlowColor = if (isUrgent) animatedAccentColor.copy(alpha = pulseAlpha) else animatedAccentColor
    val glowRadius = if (isUrgent) TaskChainDesignSystem.Dial.urgentTimerGlowRadius else TaskChainDesignSystem.Dial.timerGlowRadius
    val radialModifier = when {
        !foreground || !animationsEnabled -> Modifier
        completedPulse -> Modifier.cyberRadialPulse(
            rememberCyberRadialPulse(animationSpec = tween(RunReadiness.completionDurationMillis.toInt())),
            color = TaskChainDesignSystem.Colors.success,
        )
        status == RunTaskStatus.PENDING && paused -> Modifier.cyberRadialPulse(
            rememberCyberRadialPulse(animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing))),
            color = TaskChainDesignSystem.Colors.warning,
        )
        // The sweep head follows the same elapsed fraction as the ticks rather than an independent loop.
        status == RunTaskStatus.PENDING -> Modifier.drawWithContent {
            drawArc(animatedAccentColor.copy(alpha = 0.15f), -90f, 360f * painted, useCenter = true)
            drawLine(animatedAccentColor, center, center + androidx.compose.ui.geometry.Offset(
                kotlin.math.cos((painted * 360f - 90f) * kotlin.math.PI / 180).toFloat() * size.width / 2,
                kotlin.math.sin((painted * 360f - 90f) * kotlin.math.PI / 180).toFloat() * size.height / 2,
            ), strokeWidth = TaskChainDesignSystem.Dial.sweepStroke.toPx())
            drawContent()
        }
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(if (behindSubtaskSchedule) TaskChainDesignSystem.Colors.warning.copy(alpha = BEHIND_SCHEDULE_FILL_ALPHA)
                else androidx.compose.ui.graphics.Color.Transparent)
            .then(radialModifier)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        CyberSectorRim(
            size = diameter,
            color = TaskChainDesignSystem.Colors.border,
            thickness = TaskChainDesignSystem.Dial.rimThickness,
            sectorAngles = listOf(80f, 80f, 80f, 80f),
            gapAngle = 10f,
        )

        CyberSectorRim(
            modifier = Modifier.drawWithContent {
                val elapsedArea = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(Rect(0f, 0f, size.width, size.height), -90f, 360f * painted, false)
                    close()
                }
                clipPath(elapsedArea) { this@drawWithContent.drawContent() }
            },
            size = diameter,
            color = animatedAccentColor.copy(alpha = pulseAlpha),
            thickness = TaskChainDesignSystem.Dial.rimThickness,
            sectorAngles = listOf(80f, 80f, 80f, 80f),
            gapAngle = 10f,
        )

        CyberDialTicks(
            size = diameter * 0.92f,
            color = TaskChainDesignSystem.Colors.border.copy(alpha = 0.35f),
            tickLength = TaskChainDesignSystem.Dial.tickLength,
            majorTickLength = TaskChainDesignSystem.Dial.majorTickLength,
            strokeWidth = TaskChainDesignSystem.Dial.rimThickness,
        )

        CyberDialTicks(
            modifier = Modifier.drawWithContent {
                val paintedArea = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(Rect(0f, 0f, size.width, size.height), -90f, 360f * painted, false)
                    close()
                }
                clipPath(paintedArea) { this@drawWithContent.drawContent() }
            },
            size = diameter * 0.92f,
            color = animatedAccentColor.copy(alpha = if (isUrgent) pulseAlpha else 1.0f),
            tickLength = TaskChainDesignSystem.Dial.tickLength,
            majorTickLength = TaskChainDesignSystem.Dial.majorTickLength,
            strokeWidth = TaskChainDesignSystem.Dial.rimThickness,
        )

        if (timer.isNotEmpty()) {
            GlowingText(
                text = timer,
                glowRadius = glowRadius,
                glowColor = currentGlowColor,
                textColor = TaskChainDesignSystem.Colors.textPrimary,
                fontSize = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (diameter.toPx() * 0.68f / (timer.length.coerceAtLeast(1) * 0.65f)).toSp().value.coerceAtMost(64f)
                }.let { androidx.compose.ui.unit.TextUnit(it, androidx.compose.ui.unit.TextUnitType.Sp) },
            )
        } else {
            CyberIcon(
                iconRes = when {
                    status == RunTaskStatus.COMPLETED -> SemanticIcons.Success
                    status == RunTaskStatus.SKIPPED -> SemanticIcons.Caution
                    paused -> CyberIcons.Pause
                    else -> CyberIcons.Play
                },
                contentDescription = null,
                size = TaskChainDesignSystem.Sizes.iconLarge,
                tint = animatedAccentColor,
            )
        }
    }
}

internal fun countdownProgress(timerSeconds: Long?, remainingMillis: Long?): Float {
    if (timerSeconds == null || timerSeconds <= 0 || remainingMillis == null) return 0f
    return (1.0 - remainingMillis.toDouble() / (timerSeconds.toDouble() * MILLIS_PER_SECOND))
        .coerceIn(0.0, 1.0).toFloat()
}

/** Warning tint behind the dial when the subtask schedule runs late. */
private const val BEHIND_SCHEDULE_FILL_ALPHA = 0.3f
