package com.taskchain.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.domain.model.RoutineStep
import com.taskchain.domain.model.RoutineStepRole

/**
 * Render secondary steps beneath an expanded main step using one-step-smaller typography and icons.
 * `onPickDuration` must invoke the shared duration picker and return the selected seconds string.
 */
@Composable
internal fun SubstepEditor(
    parentIndex: Int,
    parent: RoutineStep,
    steps: List<RoutineStep>,
    viewModel: RoutineBuilderViewModel,
    onPickDuration: (Long?, (String) -> Unit) -> Unit,
) {
    val substeps = steps.mapIndexedNotNull { index, step ->
        (index to step).takeIf {
            step.role == RoutineStepRole.SECONDARY && step.parentStepId == parent.id
        }
    }
    var newTitle by remember(parent.id) { mutableStateOf("") }
    var newTimerSeconds by remember(parent.id) { mutableStateOf("") }

    substeps.forEach { (index, substep) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(CyberPrimitives.Spacing.dp24)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .border(
                        CyberPrimitives.BorderWidths.dp1,
                        CyberTheme.colors.border,
                        androidx.compose.foundation.shape.CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CyberIcon(
                    iconRes = CyberIcons.Plus,
                    contentDescription = null,
                    size = CyberPrimitives.Spacing.dp16,
                    tint = CyberTheme.colors.textSecondary,
                )
            }
            OutlinedTextField(
                value = substep.title,
                onValueChange = { viewModel.setSubstepTitle(index, it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = {
                    onPickDuration(substep.timerSeconds) { value -> viewModel.setSubstepTimerSeconds(index, value) }
                },
            ) {
                val seconds = substep.timerSeconds ?: 0L
                Text(
                    text = "%d:%02d".format(seconds / 60, seconds % 60),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = { viewModel.removeSubstep(index) }) {
                Text("×", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            modifier = Modifier
                .size(32.dp)
                .border(
                    CyberPrimitives.BorderWidths.dp1,
                    CyberTheme.colors.primary,
                    androidx.compose.foundation.shape.CircleShape,
                ),
            onClick = {
                viewModel.addSubstep(parentIndex, newTitle, newTimerSeconds)
                if (newTitle.isNotBlank()) {
                    newTitle = ""
                    newTimerSeconds = ""
                }
            },
        ) {
            CyberIcon(
                iconRes = CyberIcons.Plus,
                contentDescription = "Add substep",
                size = CyberPrimitives.Spacing.dp16,
            )
        }
        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Create a new substep", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        OutlinedButton(
            onClick = {
                onPickDuration(newTimerSeconds.toLongOrNull()) { newTimerSeconds = it }
            },
        ) {
            val seconds = newTimerSeconds.toLongOrNull() ?: 0L
            Text(
                text = "%d:%02d".format(seconds / 60, seconds % 60),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
