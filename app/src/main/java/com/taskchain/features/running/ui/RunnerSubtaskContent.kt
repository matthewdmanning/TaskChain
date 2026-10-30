package com.taskchain.features.running.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.R
import com.taskchain.core.model.RoutineRunTask

/** Use this function to display the current subtask beneath its main task.
 * Inputs: task snapshot, derived active-subtask remaining time, and the display preference.
 * Dependencies: RoutineRunTask, TaskChainDesignSystem tokens, and the existing timer formatter.
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
        verticalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.Space.small),
    ) {
        Text(
            text = subtask.title,
            style = TaskChainDesignSystem.TextStyles.subheading,
            color = if (advanced) TaskChainDesignSystem.Colors.success else TaskChainDesignSystem.Colors.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = subtaskState },
        )
        if (active && showRemaining) {
            Text(
                formatRunnerTimer(remainingMillis, null, true),
                style = TaskChainDesignSystem.TextStyles.title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
