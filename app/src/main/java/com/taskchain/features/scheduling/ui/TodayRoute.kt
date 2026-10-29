package com.taskchain.features.scheduling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.icons.SemanticIcons
import com.example.cyberpunkandroid.components.CyberCard
import com.example.cyberpunkandroid.effects.cyberGlowBorder
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.designsystem.Button
import com.taskchain.core.designsystem.CyberButton
import com.taskchain.core.designsystem.TextButton

/** Use this function to render Today occurrences from Scheduling-owned state. */
@Composable
internal fun TodayRoute(
    viewModel: TodayViewModel,
    padding: PaddingValues,
    listState: LazyListState,
    onCreate: () -> Unit,
    onStart: (RoutineTemplate) -> Unit,
    onResume: (com.taskchain.core.model.RoutineRun) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val projection = state.projection
    var undoTarget by remember { mutableStateOf<RoutineTemplate?>(null) }
    undoTarget?.let { routine ->
        AlertDialog(
            onDismissRequest = { undoTarget = null },
            title = { Text(stringResource(R.string.undo_skip_title)) },
            text = { Text(stringResource(R.string.undo_skip_message, routine.title)) },
            confirmButton = {
                Button(onClick = { viewModel.undoSkip(routine); undoTarget = null }) {
                    Text(stringResource(R.string.undo_skip))
                }
            },
            dismissButton = { TextButton(onClick = { undoTarget = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .background(CyberTheme.colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = dimensionResource(R.dimen.content_max_width)).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(CyberPrimitives.Spacing.dp16),
            verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
        ) {
            state.activeRun?.let { run ->
                item {
                    CyberCard(
                        modifier = Modifier.fillMaxWidth(),
                        header = {
                            Text(
                                stringResource(R.string.active_run_label).uppercase(),
                                style = CyberTheme.typography.terminal,
                                color = CyberTheme.colors.textSecondary,
                            )
                        },
                    ) {
                        Text(run.routineTitle, style = CyberTheme.typography.display, color = CyberTheme.colors.textPrimary)
                        Spacer(Modifier.height(CyberPrimitives.Spacing.dp16))
                        CyberButton(onClick = { onResume(run) }, size = CyberButtonSize.Large) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CyberIcon(iconRes = CyberIcons.Play, contentDescription = null)
                                Text(stringResource(R.string.resume_routine, run.routineTitle))
                            }
                        }
                    }
                }
            }
            if (projection.scheduled.isEmpty() && projection.manual.isEmpty() && projection.completed.isEmpty()) {
                item {
                    com.taskchain.core.designsystem.TaskCard(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.today_empty), style = CyberTheme.typography.body)
                        Spacer(Modifier.height(CyberPrimitives.Spacing.dp16))
                        CyberButton(onClick = onCreate) { Text(stringResource(R.string.new_routine)) }
                    }
                }
            }
            fun section(title: Int, routines: List<RoutineTemplate>, isCompleted: Boolean = false, canSkip: Boolean = false) {
                item {
                    Text(
                        stringResource(title).uppercase(),
                        modifier = Modifier.padding(top = CyberPrimitives.Spacing.dp24, bottom = CyberPrimitives.Spacing.dp8),
                        style = CyberTheme.typography.display.copy(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(CyberTheme.colors.primary, CyberTheme.semantics.colors.info),
                            ),
                        ),
                    )
                }
                if (routines.isEmpty()) {
                    item { Spacer(Modifier.fillMaxWidth().height(CyberPrimitives.IconSizes.dp48 + CyberPrimitives.Spacing.dp32)) }
                }
                items(routines, key = { it.id.value }) { routine ->
                    CompactRoutineRow(
                        routine = routine,
                        isCompleted = isCompleted,
                        isSkipped = routine.id in projection.skipped,
                        onUndoSkip = { undoTarget = routine },
                        onSkip = { viewModel.skip(routine) }.takeIf { canSkip && routine.schedule != null },
                        onStart = { onStart(routine) },
                    )
                }
            }
            section(R.string.home_scheduled, projection.scheduled, canSkip = true)
            section(R.string.home_manual, projection.manual)
            section(R.string.home_completed, projection.completed, isCompleted = true)
        }
    }
}

/** Use this function to render one Today occurrence row and its available action. */
@Composable
private fun CompactRoutineRow(
    routine: RoutineTemplate,
    isCompleted: Boolean,
    isSkipped: Boolean = false,
    onUndoSkip: () -> Unit = {},
    onSkip: (() -> Unit)? = null,
    onStart: () -> Unit,
) {
    val shape = CyberTheme.shapes.cyberCutCornerShape
    Row(
        modifier = Modifier.fillMaxWidth().padding(CyberPrimitives.Spacing.dp4)
            .cyberGlowBorder(CyberTheme.colors.primary.copy(alpha = 0.45f), shape, CyberPrimitives.Spacing.dp8, CyberPrimitives.BorderWidths.dp1)
            .clip(shape).background(CyberTheme.colors.surfaceSecondary, shape)
            .border(CyberPrimitives.BorderWidths.dp1, CyberTheme.colors.border, shape)
            .padding(CyberPrimitives.Spacing.dp16).heightIn(min = CyberPrimitives.IconSizes.dp48),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(routine.title, style = CyberTheme.typography.body, color = CyberTheme.colors.textPrimary,
            modifier = Modifier.weight(1f).padding(end = CyberPrimitives.Spacing.dp12))
        if (isSkipped) {
            TextButton(onClick = onUndoSkip) { Text(stringResource(R.string.skipped_flag).uppercase()) }
        } else if (isCompleted) {
            CyberIcon(iconRes = SemanticIcons.Success, contentDescription = stringResource(R.string.status_completed), tint = CyberTheme.colors.textPrimary)
        } else {
            onSkip?.let { TextButton(onClick = it) { Text(stringResource(R.string.skip_routine)) } }
            CyberButton(modifier = Modifier.size(CyberPrimitives.IconSizes.dp48), onClick = onStart, style = CyberButtonStyle.Outline, size = CyberButtonSize.Small) {
                CyberIcon(iconRes = CyberIcons.Play, contentDescription = stringResource(R.string.start_routine))
            }
        }
    }
}
