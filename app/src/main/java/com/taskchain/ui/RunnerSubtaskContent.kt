package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.domain.model.RoutineRunTask

/** Use this function to display the current subtask beneath its main task.
 * Inputs: task snapshot, derived active-subtask remaining time, and the display preference.
 * Dependencies: RoutineRunTask, CyberPrimitives, and the existing semantic theme and timer formatter.
 */
@Composable
internal fun RunnerSubtaskList(task: RoutineRunTask, remainingMillis: Long?, showRemaining: Boolean) {
    if (task.source.subtasks.isEmpty()) return
    val subtask = task.activeSubtaskId?.let { activeSubtaskId ->
        task.source.subtasks.firstOrNull { it.id == activeSubtaskId }
    } ?: task.source.subtasks.lastOrNull().takeIf {
        task.subtaskAdvancements.size >= task.source.subtasks.size
    } ?: return
    val active = subtask.id == task.activeSubtaskId
    val advanced = task.subtaskAdvancements.any { it.subtaskId == subtask.id }
    val subtaskState = stringResource(if (advanced) R.string.subtask_advanced else R.string.subtask_active)
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
    ) {
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.headlineMedium,
            color = if (advanced) CyberTheme.semantics.colors.success else CyberTheme.colors.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = subtaskState },
        )
        if (active && showRemaining) {
            Text(
                formatRunnerTimer(remainingMillis, null, true),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
