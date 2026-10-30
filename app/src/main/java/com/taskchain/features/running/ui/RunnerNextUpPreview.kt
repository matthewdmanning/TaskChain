package com.taskchain.features.running.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import com.example.cyberpunkandroid.components.CyberCard
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.R
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RunTaskStatus

/** Use this function to preview pending main tasks from the same snapshot displayed by the runner.
 * Inputs: run snapshot. Dependencies: RoutineRun and the configured preview-count resource.
 */
@Composable
internal fun RunnerNextUpPreview(run: RoutineRun) {
    val count = integerResource(R.integer.runner_next_up_preview_count).coerceAtLeast(0)
    val upcoming = run.tasks.withIndex().drop(run.currentTaskIndex + 1)
        .filter { it.value.status == RunTaskStatus.PENDING }.take(count)
    if (upcoming.isEmpty()) return
    CyberCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.Space.small)) {
            Text(stringResource(R.string.runner_next_up_label), style = TaskChainDesignSystem.TextStyles.terminalLabel)
            upcoming.forEach { (index, task) ->
                Text(stringResource(R.string.runner_next_up_task, index + 1, task.source.title),
                    style = TaskChainDesignSystem.TextStyles.body)
            }
        }
    }
}
