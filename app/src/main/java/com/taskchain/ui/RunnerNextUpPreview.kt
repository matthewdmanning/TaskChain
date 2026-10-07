package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunkandroid.components.CyberCard
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStepStatus

/** Use this function to preview pending main tasks from the same snapshot displayed by the runner.
 * Inputs: run snapshot. Dependencies: RoutineRun and the configured preview-count resource.
 */
@Composable
internal fun RunnerNextUpPreview(run: RoutineRun) {
    val count = integerResource(R.integer.runner_next_up_preview_count).coerceAtLeast(0)
    val upcoming = run.steps.withIndex().drop(run.currentStepIndex + 1)
        .filter { it.value.status == RunStepStatus.PENDING }.take(count)
    if (upcoming.isEmpty()) return
    CyberCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.runner_next_up_label), style = CyberTheme.typography.terminal)
            upcoming.forEach { (index, step) ->
                Text(stringResource(R.string.runner_next_up_step, index + 1, step.source.title),
                    style = CyberTheme.typography.body)
            }
        }
    }
}
