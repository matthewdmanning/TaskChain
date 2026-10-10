package com.taskchain.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunkandroid.components.CyberProgress
import com.example.cyberpunkandroid.components.CyberProgressVariant
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunTaskStatus

/** Use this function to animate main-task progress and feedback from existing runner presentation and time.
 * Inputs: displayed run, sampled time, completion hold, foreground and animation gates.
 * Dependencies: main-task statuses/timestamps, CyberProgress, and existing completion presentation.
 */
@Composable
internal fun RunnerMainProgress(
    run: RoutineRun,
    nowEpochMillis: Long,
    holdingCompletion: Boolean,
    foreground: Boolean,
    animationsEnabled: Boolean,
) {
    val finished = run.tasks.count { it.status != RunTaskStatus.PENDING }
    val animate = foreground && animationsEnabled
    val progress by animateFloatAsState(
        finished.toFloat() / run.tasks.size.coerceAtLeast(1),
        animationSpec = if (animate) tween(500) else snap(), label = "MainTaskProgress",
    )
    val recent = run.tasks.filter { it.status != RunTaskStatus.PENDING }
        .maxByOrNull { it.finishedAtEpochMillis ?: Long.MIN_VALUE }
    val feedback = if (!animate) null else if (holdingCompletion) RunTaskStatus.COMPLETED else
        recent?.takeIf { task -> task.finishedAtEpochMillis?.let { nowEpochMillis - it in 0..500L } == true }?.status
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CyberProgress(
            progress = progress, variant = CyberProgressVariant.Segmented,
            segments = run.tasks.size.coerceAtLeast(1),
            customA11y = stringResource(R.string.runner_main_progress, finished, run.tasks.size),
        )
        AnimatedContent(
            targetState = feedback,
            transitionSpec = { fadeIn(if (animate) tween(150) else snap()) togetherWith
                fadeOut(if (animate) tween(150) else snap()) }, label = "MainTaskFeedback",
        ) { status ->
            if (status != null) {
                Text(stringResource(if (status == RunTaskStatus.COMPLETED) R.string.runner_task_complete_feedback
                    else R.string.runner_task_skipped_feedback),
                    color = if (status == RunTaskStatus.COMPLETED) CyberTheme.semantics.colors.success
                        else CyberTheme.semantics.colors.warning,
                    style = CyberTheme.typography.body)
            }
        }
    }
}
