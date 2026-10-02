package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.example.cyberpunkandroid.components.CyberAccordion
import com.example.cyberpunkandroid.components.CyberBadge
import com.example.cyberpunkandroid.components.CyberBadgeVariant
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStepStatus

/** Shows only sub-steps anchored to the active task, collapsed until the user requests detail. */
@Composable
internal fun RunnerFeatureOverlay(
    container: AppContainer,
    run: RoutineRun,
    modifier: Modifier = Modifier,
) {
    val current = run.steps.getOrNull(run.currentStepIndex) ?: return
    val subSteps = run.steps.filter { step ->
        step.source.stackingAnchorStepId == current.source.id
    }
    if (subSteps.isEmpty()) return

    var expanded by remember(current.source.id) { mutableStateOf(false) }

    Box(
        modifier = modifier.padding(WindowInsets.safeDrawing.asPaddingValues()),
    ) {
        CyberAccordion(
            title = stringResource(R.string.runner_substeps_label, subSteps.size),
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = dimensionResource(R.dimen.content_max_width))
                .fillMaxWidth()
                .padding(
                    start = CyberPrimitives.Spacing.dp24,
                    end = CyberPrimitives.Spacing.dp24,
                    bottom = CyberPrimitives.IconSizes.dp48 + CyberPrimitives.Spacing.dp32,
                ),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp12)) {
                subSteps.forEach { step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp12),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = step.source.title,
                            modifier = Modifier.weight(1f),
                            style = CyberTheme.typography.body,
                            color = CyberTheme.colors.textPrimary,
                        )
                        CyberBadge(
                            text = stringResource(step.status.labelResource()),
                            variant = step.status.badgeVariant(),
                        )
                    }
                }
            }
        }
    }
}

/** Maps durable run state to local status copy for the nested list. */
private fun RunStepStatus.labelResource(): Int = when (this) {
    RunStepStatus.PENDING -> R.string.runner_substep_pending
    RunStepStatus.COMPLETED -> R.string.runner_substep_completed
    RunStepStatus.SKIPPED -> R.string.runner_substep_skipped
}

/** Maps durable run state to cyberpunkAndroid semantic badge variants. */
private fun RunStepStatus.badgeVariant(): CyberBadgeVariant = when (this) {
    RunStepStatus.PENDING -> CyberBadgeVariant.Outline
    RunStepStatus.COMPLETED -> CyberBadgeVariant.Success
    RunStepStatus.SKIPPED -> CyberBadgeVariant.Caution
}
