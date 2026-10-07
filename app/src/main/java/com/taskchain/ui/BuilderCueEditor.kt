package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.example.cyberpunkandroid.components.CyberButton
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.components.CyberTextField
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.domain.model.RoutineCue
import com.taskchain.domain.model.RoutineCueId
import kotlin.math.absoluteValue

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
                var dragOffset by remember(cue.id) { mutableStateOf(0f) }
                var isDragging by remember(cue.id) { mutableStateOf(false) }
                val moveUp = stringResource(R.string.cue_move_up)
                val moveDown = stringResource(R.string.cue_move_down)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction(moveUp) { onMoveCue(cue.id, -1); true })
                                if (index < cues.lastIndex) add(CustomAccessibilityAction(moveDown) { onMoveCue(cue.id, 1); true })
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
                ) {
                    CyberIcon(
                        iconRes = CyberIcons.Drag,
                        contentDescription = stringResource(R.string.cue_drag_handle),
                        tint = if (isDragging) CyberTheme.colors.primary else CyberTheme.colors.textSecondary,
                        size = CyberPrimitives.IconSizes.dp48,
                        modifier = Modifier
                            .heightIn(min = CyberPrimitives.IconSizes.dp48)
                            .pointerInput(cue.id.value) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = { dragOffset = 0f; isDragging = false },
                                    onDragCancel = { dragOffset = 0f; isDragging = false },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        if (dragOffset.absoluteValue >= CUE_DRAG_REORDER_THRESHOLD_PX) {
                                            onMoveCue(cue.id, if (dragOffset > 0) 1 else -1)
                                            dragOffset = 0f
                                        }
                                    },
                                )
                            },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        CyberTextField(
                            value = cue.title,
                            placeholder = stringResource(R.string.cue_title),
                            isError = cue.title.isBlank(),
                            onValueChange = { onTitleChange(cue.id, it) },
                        )
                        Spacer(Modifier.height(CyberPrimitives.Spacing.dp8))
                        CyberTextField(
                            value = durationText(cue),
                            placeholder = stringResource(R.string.cue_duration_seconds),
                            isError = cue.id in durationErrorIds,
                            onValueChange = { onDurationChange(cue.id, it) },
                        )
                        if (cue.id in durationErrorIds) {
                            Text(
                                text = stringResource(R.string.cue_duration_invalid),
                                color = CyberTheme.semantics.colors.danger,
                            )
                        }
                        Spacer(Modifier.height(CyberPrimitives.Spacing.dp8))
                        CyberButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                            onClick = { onRemoveCue(cue.id) },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Text(stringResource(R.string.cue_remove))
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

private const val CUE_DRAG_REORDER_THRESHOLD_PX = 48f
