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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
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
import com.taskchain.domain.model.RoutineSubtask
import com.taskchain.domain.model.RoutineSubtaskId
import kotlin.math.absoluteValue

/**
 * Renders editable subtask fields and accessible list controls without owning draft state.
 * Inputs: `subtasks` — ordered subtask values; `durationText` — raw duration text for each subtask; callbacks — plain subtask IDs and
 * field values for the owning ViewModel; `durationErrorIds` — subtasks whose raw duration is invalid;
 * `showTitleErrors` — true after a save attempt found a blank subtask title.
 * Dependencies: `CyberTextField`, `CyberButton`, `CyberTheme`, and the caller's state holder.
 */
@Composable
fun BuilderSubtaskEditor(
    subtasks: List<RoutineSubtask>,
    durationText: (RoutineSubtask) -> String = { it.durationSeconds.toString() },
    durationErrorIds: Set<RoutineSubtaskId> = emptySet(),
    showTitleErrors: Boolean = false,
    onTitleChange: (RoutineSubtaskId, String) -> Unit,
    onDurationChange: (RoutineSubtaskId, String) -> Unit,
    onAddSubtask: () -> Unit,
    onRemoveSubtask: (RoutineSubtaskId) -> Unit,
    onMoveSubtask: (RoutineSubtaskId, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
    ) {
        subtasks.forEachIndexed { index, subtask ->
            key(subtask.id.value) {
                var dragOffset by remember(subtask.id) { mutableStateOf(0f) }
                var isDragging by remember(subtask.id) { mutableStateOf(false) }
                val moveUp = stringResource(R.string.subtask_move_up)
                val moveDown = stringResource(R.string.subtask_move_down)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction(moveUp) { onMoveSubtask(subtask.id, -1); true })
                                if (index < subtasks.lastIndex) add(CustomAccessibilityAction(moveDown) { onMoveSubtask(subtask.id, 1); true })
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
                ) {
                    CyberIcon(
                        iconRes = CyberIcons.Drag,
                        contentDescription = stringResource(R.string.subtask_drag_handle),
                        tint = if (isDragging) CyberTheme.colors.primary else CyberTheme.colors.textSecondary,
                        size = CyberPrimitives.IconSizes.dp48,
                        modifier = Modifier
                            .heightIn(min = CyberPrimitives.IconSizes.dp48)
                            .pointerInput(subtask.id.value) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = { dragOffset = 0f; isDragging = false },
                                    onDragCancel = { dragOffset = 0f; isDragging = false },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        if (dragOffset.absoluteValue >= SUBTASK_DRAG_REORDER_THRESHOLD_PX) {
                                            onMoveSubtask(subtask.id, if (dragOffset > 0) 1 else -1)
                                            dragOffset = 0f
                                        }
                                    },
                                )
                            },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        val titleError = stringResource(R.string.subtask_title_required)
                            .takeIf { showTitleErrors && subtask.title.isBlank() }
                        CyberTextField(
                            value = subtask.title,
                            placeholder = stringResource(R.string.subtask_title),
                            isError = titleError != null,
                            modifier = Modifier.fieldError(titleError),
                            onValueChange = { onTitleChange(subtask.id, it) },
                        )
                        titleError?.let { BuilderErrorText(it) }
                        Spacer(Modifier.height(CyberPrimitives.Spacing.dp8))
                        val durationError = stringResource(R.string.subtask_duration_invalid)
                            .takeIf { subtask.id in durationErrorIds }
                        CyberTextField(
                            value = durationText(subtask),
                            placeholder = stringResource(R.string.subtask_duration_seconds),
                            isError = durationError != null,
                            modifier = Modifier.fieldError(durationError),
                            onValueChange = { onDurationChange(subtask.id, it) },
                        )
                        durationError?.let { BuilderErrorText(it) }
                        Spacer(Modifier.height(CyberPrimitives.Spacing.dp8))
                        CyberButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                            onClick = { onRemoveSubtask(subtask.id) },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Text(stringResource(R.string.subtask_remove))
                        }
                    }
                }
            }
        }
        CyberButton(
            modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
            onClick = onAddSubtask,
            style = CyberButtonStyle.Outline,
            size = CyberButtonSize.Large,
        ) {
            Text(androidx.compose.ui.res.stringResource(R.string.subtask_add))
        }
    }
}

private const val SUBTASK_DRAG_REORDER_THRESHOLD_PX = 48f

/** Use this function to show one builder error that screen readers announce when it appears. */
@Composable
internal fun BuilderErrorText(text: String) {
    Text(
        text = text,
        color = CyberTheme.semantics.colors.danger,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Use this function to give a field the error that screen readers read with it; null means no error. */
internal fun Modifier.fieldError(message: String?): Modifier =
    if (message == null) this else semantics { error(message) }
