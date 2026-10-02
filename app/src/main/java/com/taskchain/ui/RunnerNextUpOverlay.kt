package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.example.cyberpunkandroid.components.CyberCard
import com.example.cyberpunkandroid.components.CyberProgress
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStepStatus

/** Shows the next pending steps without changing the authoritative runner or its transitions. */
@Composable
internal fun RunnerFeatureOverlay(
    container: AppContainer,
    run: RoutineRun,
    modifier: Modifier = Modifier,
) {
    val upcoming = run.steps
        .withIndex()
        .drop(run.currentStepIndex + 1)
        .filter { it.value.status == RunStepStatus.PENDING }
        .take(MAX_NEXT_UP_ITEMS)
    if (upcoming.isEmpty()) return

    val completedCount = run.steps.count { it.status != RunStepStatus.PENDING }
    val progress = completedCount.toFloat() / run.steps.size.coerceAtLeast(1)

    Box(
        modifier = modifier.padding(WindowInsets.safeDrawing.asPaddingValues()),
    ) {
        CyberCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = dimensionResource(R.dimen.content_max_width))
                .fillMaxWidth()
                .padding(
                    start = CyberPrimitives.Spacing.dp24,
                    end = CyberPrimitives.Spacing.dp24,
                    bottom = CyberPrimitives.IconSizes.dp48 + CyberPrimitives.Spacing.dp32,
                ),
            header = {
                Text(
                    text = stringResource(R.string.runner_next_up_label).uppercase(),
                    style = CyberTheme.typography.terminal,
                    color = CyberTheme.colors.textSecondary,
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8)) {
                upcoming.forEachIndexed { previewIndex, indexedStep ->
                    Text(
                        text = stringResource(
                            R.string.runner_next_up_step,
                            indexedStep.index + 1,
                            indexedStep.value.source.title,
                        ),
                        style = CyberTheme.typography.body,
                        color = if (previewIndex == 0) {
                            CyberTheme.colors.textPrimary
                        } else {
                            CyberTheme.colors.textSecondary
                        },
                    )
                }
                CyberProgress(
                    progress = progress,
                    customA11y = stringResource(R.string.runner_next_up_progress),
                )
            }
        }
    }
}

private const val MAX_NEXT_UP_ITEMS = 2
