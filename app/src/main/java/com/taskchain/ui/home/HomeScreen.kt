@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.ui.home
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.ui.designsystem.TaskChainDesignSystem
import com.taskchain.ui.designsystem.Spacing
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.components.CyberCard
import com.taskchain.ui.designsystem.TaskCard
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.effects.cyberGlowBorder
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.icons.SemanticIcons
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.ui.Button
import com.taskchain.ui.CyberButton
import com.taskchain.ui.SECONDS_PER_MINUTE
import com.taskchain.ui.TextButton
import com.taskchain.ui.progress.ProgressRoute
import com.taskchain.ui.progress.ProgressViewModel
import com.taskchain.ui.settings.SettingsRoute
import com.taskchain.ui.settings.SettingsViewModel

/** Top-level tab choices retained while deeper builder and runner routes are open. */
internal enum class HomeTab(@param:StringRes val label: Int) {
    HOME(R.string.tab_home),
    ROUTINES(R.string.tab_routines),
    PROGRESS(R.string.tab_progress),
    SETTINGS(R.string.tab_settings),
}

/** Use this function to render persistent tabs and preserve the selected tab across recomposition. */
@Composable
internal fun HomeShell(
    container: AppContainer,
    selectedTab: HomeTab,
    onSelectedTabChange: (HomeTab) -> Unit,
    onCreate: () -> Unit,
    onEdit: (RoutineTemplate) -> Unit,
    onStart: (RoutineTemplate) -> Unit,
    onResume: (RoutineId) -> Unit,
) {
    val todayListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val routinesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val routinesViewModel: RoutinesViewModel = viewModel { RoutinesViewModel(container) }
    val progressViewModel: ProgressViewModel = viewModel { ProgressViewModel(container) }
    val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel(container) }
    val activeRun by routinesViewModel.activeRun.collectAsStateWithLifecycle()
    val blockedRoutine by routinesViewModel.blockedRoutine.collectAsStateWithLifecycle()
    val pendingStart by routinesViewModel.pendingStart.collectAsStateWithLifecycle()
    LaunchedEffect(pendingStart) {
        pendingStart?.let {
            onStart(it)
            routinesViewModel.startHandled()
        }
    }
    val startOrExplain: (RoutineTemplate) -> Unit = routinesViewModel::requestStart
    val screen: @Composable () -> Unit = {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(selectedTab.label)) }) },
            bottomBar = {
                NavigationBar {
                    HomeTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { onSelectedTabChange(tab) },
                            icon = {},
                            label = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelSmall,
                                softWrap = false) },
                        )
                    }
                }
            },
        ) { padding ->
            when (selectedTab) {
                HomeTab.HOME -> TodayRoute(
                    routinesViewModel,
                    padding,
                    todayListState,
                    onCreate,
                    startOrExplain,
                    activeRun,
                    onResume = { run -> onResume(run.routineId) },
                )
                HomeTab.ROUTINES -> RoutinesRoute(routinesViewModel, padding, routinesListState, onCreate, onEdit, startOrExplain)
                HomeTab.PROGRESS -> ProgressRoute(progressViewModel, padding)
                HomeTab.SETTINGS -> SettingsRoute(settingsViewModel, padding)
            }
        }
        blockedRoutine?.let {
            val run = activeRun
            if (run != null) {
                AlertDialog(
                    onDismissRequest = routinesViewModel::dismissBlockedStart,
                    title = { Text(stringResource(R.string.active_run_in_progress_title)) },
                    text = { Text(stringResource(R.string.active_run_in_progress_message, run.routineTitle)) },
                    confirmButton = {
                        Button(onClick = {
                            routinesViewModel.dismissBlockedStart()
                            onResume(run.routineId)
                        }) {
                            Text(stringResource(R.string.resume_routine, run.routineTitle))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = routinesViewModel::dismissBlockedStart) {
                            Text(stringResource(R.string.keep_browsing))
                        }
                    },
                )
            }
        }
    }
    screen()
}

/** Use this function to bind Today UI to the routine-list ViewModel. */
@Composable
private fun TodayRoute(
    viewModel: RoutinesViewModel,
    padding: PaddingValues,
    listState: LazyListState,
    onCreate: () -> Unit,
    onStart: (RoutineTemplate) -> Unit,
    activeRun: com.taskchain.domain.model.RoutineRun?,
    onResume: (com.taskchain.domain.model.RoutineRun) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val projection = state.todayRoutines
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
            activeRun?.let { run ->
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
                        Text(
                            run.routineTitle,
                            style = CyberTheme.typography.display,
                            color = CyberTheme.colors.textPrimary,
                        )
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
                    TaskCard(modifier = Modifier.fillMaxWidth()) {
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
                            brush = Brush.linearGradient(
                                listOf(CyberTheme.colors.primary, CyberTheme.semantics.colors.info),
                            ),
                        ),
                    )
                }
                if (routines.isEmpty()) {
                    item {
                        Spacer(
                            Modifier.fillMaxWidth().height(
                                CyberPrimitives.IconSizes.dp48 + CyberPrimitives.Spacing.dp32,
                            ),
                        )
                    }
                }
                items(routines, key = { it.id.value }) { routine ->
                    CompactRoutineRow(
                        routine = routine,
                        isCompleted = isCompleted,
                        isSkipped = routine.id in projection.skipped,
                        onUndoSkip = { undoTarget = routine },
                        onSkip = { viewModel.skip(routine) }.takeIf { canSkip && routine.schedule != null },
                    ) { onStart(routine) }
                }
            }
            section(R.string.home_scheduled, projection.scheduled, canSkip = true)
            section(R.string.home_manual, projection.manual)
            section(R.string.home_completed, projection.completed, isCompleted = true)
        }
    }
}

/** Use this function to render each routine on Home as a compact row with its action. */
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(CyberPrimitives.Spacing.dp4)
            .cyberGlowBorder(
                color = CyberTheme.colors.primary.copy(alpha = 0.45f),
                shape = shape,
                glowRadius = CyberPrimitives.Spacing.dp8,
                width = CyberPrimitives.BorderWidths.dp1,
            )
            .clip(shape)
            .background(CyberTheme.colors.surfaceSecondary, shape)
            .border(CyberPrimitives.BorderWidths.dp1, CyberTheme.colors.border, shape)
            .padding(CyberPrimitives.Spacing.dp16)
            .heightIn(min = CyberPrimitives.IconSizes.dp48),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = routine.title,
            style = CyberTheme.typography.body,
            color = CyberTheme.colors.textPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(end = CyberPrimitives.Spacing.dp12),
        )
        if (isSkipped) {
            TextButton(onClick = onUndoSkip) { Text(stringResource(R.string.skipped_flag).uppercase()) }
        } else if (isCompleted) {
            CyberIcon(
                iconRes = SemanticIcons.Success,
                contentDescription = stringResource(R.string.status_completed),
                tint = CyberTheme.colors.textPrimary,
            )
        } else {
            onSkip?.let {
                TextButton(onClick = it) { Text(stringResource(R.string.skip_routine)) }
            }
            CyberButton(
                modifier = Modifier.size(CyberPrimitives.IconSizes.dp48),
                onClick = onStart,
                style = CyberButtonStyle.Outline,
                size = CyberButtonSize.Small,
            ) {
                CyberIcon(
                    iconRes = CyberIcons.Play,
                    contentDescription = stringResource(R.string.start_routine),
                )
            }
        }
    }
}

/** Use this function to bind the routine library UI to its ViewModel. */
@Composable
private fun RoutinesRoute(
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
                    Text(
                        stringResource(R.string.starter_routines),
                        style = CyberTheme.typography.display,
                        color = CyberTheme.colors.textPrimary,
                    )
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CyberIcon(iconRes = CyberIcons.Plus, contentDescription = null)
                        Text(stringResource(R.string.new_routine), style = CyberTheme.typography.body)
                    }
                }
            }
        }
    }
}

/** Use this function to render one routine consistently in Today and Routines. */
@Composable
private fun RoutineCard(routine: RoutineTemplate, onEdit: (() -> Unit)?, onStart: () -> Unit) {
    val spacing = TaskChainDesignSystem.spacing()
    val shape = CyberTheme.shapes.cyberCutCornerShape
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.small)
            .background(CyberTheme.colors.surfaceSecondary, shape)
            .cyberGlowBorder(
                color = CyberTheme.colors.primary.copy(alpha = 0.45f),
                shape = shape,
                glowRadius = CyberPrimitives.Spacing.dp8,
                width = CyberPrimitives.BorderWidths.dp1,
            )
            .padding(spacing.small),
    ) {
        TaskCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = routine.title,
                style = CyberTheme.typography.display,
                color = CyberTheme.colors.textPrimary,
            )
            if (routine.description.isNotBlank()) {
                Text(
                    text = routine.description,
                    style = CyberTheme.typography.body,
                    color = CyberTheme.colors.textSecondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(formatRoutineItemCount(routine), color = CyberTheme.colors.textSecondary)
                formatRoutineTotalTime(routine)?.let { totalTime ->
                    Text(totalTime, color = CyberTheme.colors.textSecondary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                onEdit?.let {
                    CyberButton(
                        modifier = Modifier.weight(1f).heightIn(min = CyberPrimitives.IconSizes.dp48),
                        onClick = it,
                        style = CyberButtonStyle.Outline,
                        size = CyberButtonSize.Small,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CyberIcon(iconRes = CyberIcons.Edit, contentDescription = null, size = spacing.medium)
                            Text(stringResource(R.string.edit_routine))
                        }
                    }
                }
                CyberButton(
                    modifier = Modifier.weight(1f).heightIn(min = CyberPrimitives.IconSizes.dp48),
                    onClick = onStart,
                    size = CyberButtonSize.Small,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        String.format(java.util.Locale.ROOT, "Total Time: %d:%02d:%02d", hours, minutes, secs)
    } else {
        val minutes = totalSeconds / 60
        val secs = totalSeconds % 60
        String.format(java.util.Locale.ROOT, "Total Time: %d:%02d", minutes, secs)
    }
}

/** Use this function to format the item count of a routine template. */
internal fun formatRoutineItemCount(routine: RoutineTemplate): String {
    return "${routine.tasks.size} ${if (routine.tasks.size == 1) "Item" else "Items"}"
}

/** Use this function to split a non-negative task duration into minutes and seconds for builder display.
 * Inputs: `totalSeconds` — the persisted duration in seconds.
 * Dependencies: `SECONDS_PER_MINUTE`.
 */
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
    val spacing = TaskChainDesignSystem.spacing()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        state = listState,
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        if (routines.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text(emptyText)
                    Button(onClick = onCreate) { Text(stringResource(R.string.new_routine)) }
                }
            }
        }
        items(routines, key = { it.id.value }) { routine -> RoutineCard(routine, null) { onStart(routine) } }
    }
}

/** Use this function to bind an editable routine draft to builder fields and actions. */
