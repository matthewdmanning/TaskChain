package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.example.cyberpunkandroid.components.CyberCard
import com.example.cyberpunkandroid.components.CyberPixelTransition
import com.example.cyberpunkandroid.components.CyberProgress
import com.example.cyberpunkandroid.components.CyberProgressVariant
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.icons.SemanticIcons
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStepStatus
import kotlinx.coroutines.delay

/** Adds completion celebrations and a smooth cyberpunkAndroid segmented progress tracker. */
@Composable
internal fun RunnerFeatureOverlay(
    container: AppContainer,
    run: RoutineRun,
    modifier: Modifier = Modifier,
) {
    var previousStatuses by remember(run.id) { mutableStateOf(run.steps.map { it.status }) }
    var feedback by remember(run.id) { mutableStateOf<StepFeedback?>(null) }
    val currentStatuses = run.steps.map { it.status }

    LaunchedEffect(currentStatuses) {
        val changedIndex = currentStatuses.indices.firstOrNull { index ->
            previousStatuses.getOrNull(index) != currentStatuses[index] &&
                currentStatuses[index] != RunStepStatus.PENDING
        }
        previousStatuses = currentStatuses
        if (changedIndex != null) {
            val event = StepFeedback(changedIndex, currentStatuses[changedIndex])
            feedback = event
            delay(CyberPrimitives.Durations.ms500.toLong())
            if (feedback == event) feedback = null
        }
    }

    val finishedCount = run.steps.count { it.status != RunStepStatus.PENDING }
    val progress = finishedCount.toFloat() / run.steps.size.coerceAtLeast(1)

    Box(
        modifier = modifier.padding(WindowInsets.safeDrawing.asPaddingValues()),
    ) {
        CyberProgress(
            progress = progress,
            variant = CyberProgressVariant.Segmented,
            segments = run.steps.size.coerceAtLeast(1),
            customA11y = stringResource(R.string.runner_progress_feedback),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = dimensionResource(R.dimen.content_max_width))
                .fillMaxWidth()
                .padding(
                    start = CyberPrimitives.Spacing.dp24,
                    end = CyberPrimitives.Spacing.dp24,
                    bottom = CyberPrimitives.IconSizes.dp48 + CyberPrimitives.Spacing.dp24,
                ),
        )

        feedback?.let { event ->
            CyberPixelTransition(
                targetKey = event,
                color = CyberTheme.colors.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CyberCard(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .padding(horizontal = CyberPrimitives.Spacing.dp32),
                    holo = true,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CyberIcon(
                            iconRes = if (event.status == RunStepStatus.COMPLETED) {
                                SemanticIcons.Success
                            } else {
                                CyberIcons.SkipForward
                            },
                            contentDescription = null,
                            size = CyberPrimitives.IconSizes.dp48,
                            tint = CyberTheme.colors.primary,
                        )
                        Text(
                            text = stringResource(
                                if (event.status == RunStepStatus.COMPLETED) {
                                    R.string.runner_step_complete_feedback
                                } else {
                                    R.string.runner_step_skipped_feedback
                                },
                            ),
                            style = CyberTheme.typography.display,
                            color = CyberTheme.colors.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

private data class StepFeedback(
    val stepIndex: Int,
    val status: RunStepStatus,
)
