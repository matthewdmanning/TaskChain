package com.taskchain.core.browsing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.components.CyberCard
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.effects.cyberGlowBorder
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.core.designsystem.Button
import com.taskchain.core.designsystem.CyberButton
import com.taskchain.core.designsystem.Spacing
import com.taskchain.core.designsystem.TaskCard
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.core.designsystem.TextButton
import com.taskchain.core.model.RoutineTemplate
import com.taskchain.core.ui.SECONDS_PER_MINUTE

/** Use this function to bind the routine library UI to its collection ViewModel. */
@Composable
internal fun RoutinesRoute(
    viewModel: RoutinesViewModel,
    padding: PaddingValues,
    listState: LazyListState,
    onCreate: () -> Unit,
    onEdit: (RoutineTemplate) -> Unit,
    onStart: (RoutineTemplate) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .background(CyberTheme.colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = dimensionResource(R.dimen.content_max_width)).fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            if (state.routines.isNotEmpty()) {
                items(state.routines, key = { it.id.value }) { routine ->
                    RoutineCard(routine, onEdit = { onEdit(routine) }, onStart = { onStart(routine) })
                }
            }
            if (state.builtIns.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.starter_routines), style = CyberTheme.typography.display, color = CyberTheme.colors.textPrimary)
                }
                items(state.builtIns, key = { it.id.value }) { routine ->
                    RoutineCard(routine, onEdit = { onEdit(routine) }, onStart = { onStart(routine) })
                }
            }
            item {
                CyberButton(
                    modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                    onClick = onCreate,
                    size = CyberButtonSize.Large,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), verticalAlignment = Alignment.CenterVertically) {
                        CyberIcon(iconRes = CyberIcons.Plus, contentDescription = null)
                        Text(stringResource(R.string.new_routine), style = CyberTheme.typography.body)
                    }
                }
            }
        }
    }
}

/** Use this function to render one routine consistently in collection lists. */
@Composable
private fun RoutineCard(routine: RoutineTemplate, onEdit: (() -> Unit)?, onStart: () -> Unit) {
    val spacing = TaskChainDesignSystem.spacing()
    val shape = CyberTheme.shapes.cyberCutCornerShape
    Box(
        modifier = Modifier.fillMaxWidth().padding(spacing.small)
            .background(CyberTheme.colors.surfaceSecondary, shape)
            .cyberGlowBorder(CyberTheme.colors.primary.copy(alpha = 0.45f), shape, CyberPrimitives.Spacing.dp8, CyberPrimitives.BorderWidths.dp1)
            .padding(spacing.small),
    ) {
        TaskCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                Text(routine.title, style = CyberTheme.typography.display, color = CyberTheme.colors.textPrimary)
                if (routine.description.isNotBlank()) Text(routine.description, style = CyberTheme.typography.body, color = CyberTheme.colors.textSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatRoutineItemCount(routine), color = CyberTheme.colors.textSecondary)
                    formatRoutineTotalTime(routine)?.let { Text(it, color = CyberTheme.colors.textSecondary) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    onEdit?.let {
                        CyberButton(modifier = Modifier.weight(1f).heightIn(min = CyberPrimitives.IconSizes.dp48), onClick = it, style = CyberButtonStyle.Outline, size = CyberButtonSize.Small) {
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), verticalAlignment = Alignment.CenterVertically) {
                                CyberIcon(iconRes = CyberIcons.Edit, contentDescription = null, size = spacing.medium)
                                Text(stringResource(R.string.edit_routine))
                            }
                        }
                    }
                    CyberButton(modifier = Modifier.weight(1f).heightIn(min = CyberPrimitives.IconSizes.dp48), onClick = onStart, size = CyberButtonSize.Small) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small), verticalAlignment = Alignment.CenterVertically) {
                            CyberIcon(iconRes = CyberIcons.Play, contentDescription = null, size = spacing.medium)
                            Text(stringResource(R.string.start_routine))
                        }
                    }
                }
            }
        }
    }
}

/** Use this function to format the total time of a routine template, or null if untimed. */
internal fun formatRoutineTotalTime(routine: RoutineTemplate): String? {
    if (routine.tasks.none { it.durationSeconds != null }) return null
    val totalSeconds = routine.tasks.mapNotNull { it.durationSeconds }
        .fold(0L) { total, seconds -> total + seconds.coerceIn(0L, Long.MAX_VALUE - total) }
    return if (totalSeconds >= 3600) {
        String.format(java.util.Locale.ROOT, "Total Time: %d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
    } else {
        String.format(java.util.Locale.ROOT, "Total Time: %d:%02d", totalSeconds / 60, totalSeconds % 60)
    }
}

/** Use this function to format the number of tasks shown for a routine. */
internal fun formatRoutineItemCount(routine: RoutineTemplate): String =
    "${routine.tasks.size} ${if (routine.tasks.size == 1) "Item" else "Items"}"

/** Use this function to split a persisted duration into builder minutes and seconds. */
internal fun routineDurationParts(totalSeconds: Long): Pair<Long, Long> {
    val normalizedSeconds = totalSeconds.coerceAtLeast(0L)
    return normalizedSeconds / SECONDS_PER_MINUTE to normalizedSeconds % SECONDS_PER_MINUTE
}

/** Use this function to render a titled routine list with an empty state. */
@Composable
internal fun RoutineList(
    routines: List<RoutineTemplate>,
    padding: PaddingValues,
    listState: LazyListState,
    emptyText: String,
    onCreate: () -> Unit,
    onStart: (RoutineTemplate) -> Unit,
) {
    val spacing: Spacing = TaskChainDesignSystem.spacing()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        state = listState,
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        if (routines.isEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Text(emptyText)
                Button(onClick = onCreate) { Text(stringResource(R.string.new_routine)) }
            }
        }
        items(routines, key = { it.id.value }) { routine -> RoutineCard(routine, null) { onStart(routine) } }
    }
}
