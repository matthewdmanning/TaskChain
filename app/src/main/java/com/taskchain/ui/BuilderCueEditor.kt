package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cyberpunkandroid.components.CyberButton
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.components.CyberTextField
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.domain.model.RoutineCue
import com.taskchain.domain.model.RoutineCueId

/**
 * Renders editable cue fields and accessible list controls without owning draft state.
 * Inputs: `cues` — ordered cue values; `durationText` — raw duration text for each cue; callbacks — plain cue IDs and
 * field values for the owning ViewModel; `durationErrorIds` — cues whose raw duration is invalid.
 * Dependencies: `CyberTextField`, `CyberButton`, `CyberTheme`, and the caller's state holder.
 */
@Composable
fun BuilderCueEditor(
    cues: List<RoutineCue>,
    durationText: (RoutineCue) -> String = { it.durationSeconds.toString() },
    durationErrorIds: Set<RoutineCueId> = emptySet(),
    onTitleChange: (RoutineCueId, String) -> Unit,
    onDurationChange: (RoutineCueId, String) -> Unit,
    onAddCue: () -> Unit,
    onRemoveCue: (RoutineCueId) -> Unit,
    onMoveCue: (RoutineCueId, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
    ) {
        cues.forEachIndexed { index, cue ->
            key(cue.id.value) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
                ) {
                Column(modifier = Modifier.weight(1f)) {
                    CyberTextField(
                        value = cue.title,
                        placeholder = androidx.compose.ui.res.stringResource(R.string.cue_title),
                        isError = cue.title.isBlank(),
                        onValueChange = { onTitleChange(cue.id, it) },
                    )
                    Spacer(Modifier.height(CyberPrimitives.Spacing.dp8))
                    CyberTextField(
                        value = durationText(cue),
                        placeholder = androidx.compose.ui.res.stringResource(R.string.cue_duration_seconds),
                        isError = cue.id in durationErrorIds,
                        onValueChange = { onDurationChange(cue.id, it) },
                    )
                    if (cue.id in durationErrorIds) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.cue_duration_invalid),
                            color = CyberTheme.semantics.colors.danger,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8)) {
                    CyberButton(
                        modifier = Modifier.heightIn(min = CyberPrimitives.IconSizes.dp48),
                        enabled = index > 0,
                        onClick = { onMoveCue(cue.id, -1) },
                        style = CyberButtonStyle.Outline,
                        size = CyberButtonSize.Small,
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.cue_move_up))
                    }
                    CyberButton(
                        modifier = Modifier.heightIn(min = CyberPrimitives.IconSizes.dp48),
                        enabled = index < cues.lastIndex,
                        onClick = { onMoveCue(cue.id, 1) },
                        style = CyberButtonStyle.Outline,
                        size = CyberButtonSize.Small,
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.cue_move_down))
                    }
                    CyberButton(
                        modifier = Modifier.heightIn(min = CyberPrimitives.IconSizes.dp48),
                        onClick = { onRemoveCue(cue.id) },
                        style = CyberButtonStyle.Outline,
                        size = CyberButtonSize.Small,
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.cue_remove))
                    }
                }
                }
            }
        }
        CyberButton(
            modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
            onClick = onAddCue,
            style = CyberButtonStyle.Outline,
            size = CyberButtonSize.Large,
        ) {
            Text(androidx.compose.ui.res.stringResource(R.string.cue_add))
        }
    }
}
