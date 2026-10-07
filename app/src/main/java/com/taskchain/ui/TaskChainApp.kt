@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.animation.ValueAnimator
import android.widget.NumberPicker
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import java.util.Calendar
import java.util.Date
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import com.example.cyberpunkandroid.effects.cyberOverload
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.UserPreferences
import com.taskchain.ui.designsystem.TaskChainDesignSystem
import com.taskchain.ui.designsystem.RunnerMotion
import com.taskchain.ui.designsystem.Spacing
import com.taskchain.ui.designsystem.TaskChainTheme
import com.taskchain.ui.designsystem.ThemeCatalog
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.components.CyberCard
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.effects.cyberRadarSweep
import com.example.cyberpunkandroid.effects.cyberRadialPulse
import com.example.cyberpunkandroid.effects.cyberGlowBorder
import com.example.cyberpunkandroid.effects.rememberCyberRadarSweep
import com.example.cyberpunkandroid.effects.rememberCyberRadialPulse
import com.example.cyberpunkandroid.components.GlowingText
import com.example.cyberpunkandroid.components.CyberDialTicks
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.components.CyberSectorRim
import com.example.cyberpunkandroid.icons.SemanticIcons
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
import androidx.compose.ui.unit.Dp
import com.example.cyberpunkandroid.theme.CyberTheme
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.first

/** Top-level tab choices retained while deeper builder and runner routes are open. */
private enum class HomeTab(@param:StringRes val label: Int) {
    HOME(R.string.tab_home),
    ROUTINES(R.string.tab_routines),
    PROGRESS(R.string.tab_progress),
    SETTINGS(R.string.tab_settings),
}

/** Retains the current Cyberpunk Android theme supplied by [TaskChainTheme]. */
@Composable
private fun CyberAppearance(content: @Composable () -> Unit) = content()

/** Use this function as the Compose application entry point. */
@Composable
fun TaskChainApp(container: AppContainer, openActiveRun: Boolean = false) {
    val preferences by container.preferences.observe().collectAsStateWithLifecycle(UserPreferences())
    TaskChainTheme(preferences.selectedTheme) {
        CompositionLocalProvider(LocalVibrationIntensity provides preferences.vibrationIntensity) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            LaunchedEffect(openActiveRun) {
                if (openActiveRun) {
                    container.activeRun.observeActive().first()
                        ?.takeIf { it.status == RunStatus.ACTIVE }
                        ?.let { navController.navigate(runnerRoute(it.routineId)) }
                }
            }
            var selectedTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }
            NavHost(navController = navController, startDestination = HOME_ROUTE) {
                composable(HOME_ROUTE) {
                    HomeShell(
                        container = container,
                        selectedTab = selectedTab,
                        onSelectedTabChange = { selectedTab = it },
                        onCreate = { navController.navigate(builderRoute(null)) },
                        onEdit = { navController.navigate(builderRoute(it.id)) },
                        onStart = { navController.navigate(runnerRoute(it.id)) },
                        onResume = { navController.navigate(runnerRoute(it)) },
                    )
                }
                composable(
                    route = BUILDER_ROUTE,
                    arguments = listOf(navArgument(ROUTINE_ID_ARGUMENT) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }),
                ) { entry ->
                    CyberAppearance {
                        RoutineBuilderRoute(
                            container = container,
                            routineId = entry.arguments?.getString(ROUTINE_ID_ARGUMENT)?.let(::RoutineId),
                            onClose = navController::popBackStack,
                            onSaved = {
                                selectedTab = HomeTab.ROUTINES
                                navController.popBackStack()
                            },
                        )
                    }
                }
                composable(
                    route = RUNNER_ROUTE,
                    arguments = listOf(navArgument(ROUTINE_ID_ARGUMENT) { type = NavType.StringType }),
                ) { entry ->
                    CyberAppearance {
                        RoutineRunnerRoute(
                            container = container,
                            preferences = preferences,
                            routineId = RoutineId(requireNotNull(entry.arguments?.getString(ROUTINE_ID_ARGUMENT))),
                            onFinished = navController::popBackStack,
                        )
                    }
                }
            }
        }
    }
}

}

/** Use this function to render persistent tabs and preserve the selected tab across recomposition. */
@Composable
private fun HomeShell(
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
    val activeRun by container.activeRun.observeActive().collectAsStateWithLifecycle(null)
    var blockedRoutine by remember { mutableStateOf<RoutineTemplate?>(null) }
    val startOrExplain: (RoutineTemplate) -> Unit = { routine ->
        val currentRun = activeRun?.takeIf { it.status == RunStatus.ACTIVE }
        if (currentRun != null && currentRun.routineId != routine.id) {
            blockedRoutine = routine
        } else {
            onStart(routine)
        }
    }
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
                    activeRun?.takeIf { it.status == RunStatus.ACTIVE },
                    onResume = { run -> onResume(run.routineId) },
                )
                HomeTab.ROUTINES -> RoutinesRoute(routinesViewModel, padding, routinesListState, onCreate, onEdit, startOrExplain)
                HomeTab.PROGRESS -> ProgressRoute(progressViewModel, padding)
                HomeTab.SETTINGS -> SettingsRoute(settingsViewModel, padding)
            }
        }
        blockedRoutine?.let {
            val run = activeRun?.takeIf { current -> current.status == RunStatus.ACTIVE }
            if (run != null) {
                AlertDialog(
                    onDismissRequest = { blockedRoutine = null },
                    title = { Text(stringResource(R.string.active_run_in_progress_title)) },
                    text = { Text(stringResource(R.string.active_run_in_progress_message, run.routineTitle)) },
                    confirmButton = {
                        Button(onClick = {
                            blockedRoutine = null
                            onResume(run.routineId)
                        }) {
                            Text(stringResource(R.string.resume_routine, run.routineTitle))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { blockedRoutine = null }) {
                            Text(stringResource(R.string.keep_browsing))
                        }
                    },
                )
            }
        }
    }
    if (selectedTab == HomeTab.HOME || selectedTab == HomeTab.ROUTINES) CyberAppearance(screen) else screen()
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
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.today_empty), style = CyberTheme.typography.body)
                        Spacer(Modifier.height(CyberPrimitives.Spacing.dp16))
                        CyberButton(onClick = onCreate) { Text(stringResource(R.string.new_routine)) }
                    }
                }
            }
            fun section(title: Int, routines: List<RoutineTemplate>, isCompleted: Boolean = false) {
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
                    CompactRoutineRow(routine = routine, isCompleted = isCompleted) { onStart(routine) }
                }
            }
            section(R.string.home_scheduled, projection.scheduled)
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
        if (isCompleted) {
            CyberIcon(
                iconRes = SemanticIcons.Success,
                contentDescription = stringResource(R.string.status_completed),
                tint = CyberTheme.colors.textPrimary,
            )
        } else {
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
        CyberCard(modifier = Modifier.fillMaxWidth()) {
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
    if (routine.steps.none { it.durationSeconds != null }) return null
    val totalSeconds = routine.steps.mapNotNull { it.durationSeconds }
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
    return "${routine.steps.size} ${if (routine.steps.size == 1) "Item" else "Items"}"
}

/** Use this function to split a non-negative step duration into minutes and seconds for builder display.
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
@Composable
private fun RoutineBuilderRoute(
    container: AppContainer,
    routineId: RoutineId?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel: RoutineBuilderViewModel = viewModel(key = "builder-${routineId?.value}") {
        RoutineBuilderViewModel(container, routineId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    val context = LocalContext.current
    val addStepDescription = stringResource(R.string.add_step)
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var expandedStepId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingNameStepId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.savedRoutineId) { if (state.savedRoutineId != null) onSaved() }
    val requestClose = {
        if (state.hasUnsavedChanges) showDiscardDialog = true else onClose()
    }
    BackHandler { requestClose() }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.builder_title), style = CyberTheme.typography.display) },
            )
        },
    ) { innerPadding ->
    Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)
            .imePadding().background(CyberTheme.colors.background),
        contentAlignment = Alignment.TopCenter,
    ) {
    LazyColumn(
        modifier = Modifier.widthIn(max = dimensionResource(R.dimen.content_max_width)).fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.medium,
            top = spacing.medium,
            end = spacing.medium,
            bottom = spacing.large * 2,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                    com.example.cyberpunkandroid.components.CyberField(
                        label = stringResource(R.string.routine_name),
                        errorText = if (BuilderValidationError.ROUTINE_NAME_REQUIRED in state.validationErrors) {
                            stringResource(R.string.validation_routine_name_required)
                        } else null,
                        isRequired = true,
                    ) { isError ->
                        com.example.cyberpunkandroid.components.CyberTextField(
                            value = state.title,
                            onValueChange = viewModel::setTitle,
                            isError = isError,
                        )
                    }
                    com.example.cyberpunkandroid.components.CyberField(
                        label = stringResource(R.string.routine_description),
                    ) { isError ->
                        com.example.cyberpunkandroid.components.CyberTextArea(
                            value = state.description,
                            onValueChange = viewModel::setDescription,
                            isError = isError,
                        )
                    }
                }
            }
        }
        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
                    Text(
                        stringResource(R.string.routine_settings),
                        style = CyberTheme.typography.display,
                        color = CyberTheme.colors.textPrimary,
                    )
                    ScheduleEditor(state, viewModel, spacing)
                    LabeledSwitchRow(stringResource(R.string.authoring_sound), state.soundEnabled, viewModel::setSoundEnabled)
                    LabeledSwitchRow(stringResource(R.string.authoring_vibrate), state.vibrateEnabled, viewModel::setVibrateEnabled)
                }
            }
        }
        item {
            Text(
                stringResource(R.string.authoring_steps),
                style = CyberTheme.typography.display,
                color = CyberTheme.colors.textPrimary,
            )
            if (BuilderValidationError.STEP_REQUIRED in state.validationErrors) {
                Text(stringResource(R.string.validation_step_required), color = CyberTheme.semantics.colors.danger)
            }
        }
        itemsIndexed(state.steps, key = { _, step -> step.id.value }) { index, step ->
            var dragOffset by remember(step.id) { mutableStateOf(0f) }
            var isDragging by remember(step.id) { mutableStateOf(false) }
            val expanded = expandedStepId == step.id.value
            val dragScale by animateFloatAsState(
                if (isDragging) 1.03f else 1f,
                animationSpec = tween(DRAG_TRANSITION_MILLIS, easing = FastOutSlowInEasing),
                label = "StepDragScale",
            )
            val dragElevation by animateDpAsState(
                if (isDragging) spacing.small else 0.dp,
                animationSpec = tween(DRAG_TRANSITION_MILLIS, easing = FastOutSlowInEasing),
                label = "StepDragElevation",
            )
            val moveUp = stringResource(R.string.authoring_move_up)
            val moveDown = stringResource(R.string.authoring_move_down)
            val changeExpansion: (Boolean) -> Unit = { shouldExpand ->
                if (shouldExpand) {
                    val currentIndex = viewModel.state.value.steps.indexOfFirst { it.id == step.id }
                    if (currentIndex >= 0) {
                        viewModel.editStep(currentIndex)
                        if (viewModel.state.value.editingStepIndex == currentIndex) {
                            expandedStepId = step.id.value
                            editingNameStepId = null
                        }
                    }
                } else if (expandedStepId == step.id.value) {
                    expandedStepId = null
                    editingNameStepId = null
                }
            }
            com.example.cyberpunkandroid.components.CyberAccordion(
                title = step.title.ifBlank { stringResource(R.string.step_name) },
                expanded = expanded,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(placementSpec = tween(DRAG_TRANSITION_MILLIS, easing = FastOutSlowInEasing))
                    .zIndex(if (isDragging) 10f else 0f)
                    .graphicsLayer {
                        translationY = dragOffset
                        scaleX = dragScale
                        scaleY = dragScale
                        shadowElevation = dragElevation.toPx()
                    }
                    .semantics {
                        customActions = buildList {
                            if (index > 0) add(CustomAccessibilityAction(moveUp) { viewModel.moveStep(index, -1); true })
                            if (index < state.steps.lastIndex) add(CustomAccessibilityAction(moveDown) { viewModel.moveStep(index, 1); true })
                        }
                    },
                borderColor = if (isDragging) CyberTheme.colors.primary else CyberTheme.colors.border,
                headerContent = { isExpanded ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48)
                            .clickable(role = Role.Button) { changeExpansion(!isExpanded) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    CyberIcon(
                        iconRes = CyberIcons.Drag,
                        contentDescription = stringResource(R.string.authoring_drag_handle),
                        tint = CyberTheme.colors.textSecondary,
                        size = CyberPrimitives.IconSizes.dp48,
                        modifier = Modifier
                            .widthIn(min = CyberPrimitives.IconSizes.dp48)
                            .pointerInput(step.id.value) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = { dragOffset = 0f; isDragging = false },
                                    onDragCancel = { dragOffset = 0f; isDragging = false },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        if (dragOffset.absoluteValue >= DRAG_REORDER_THRESHOLD_PX) {
                                            val currentIndex = viewModel.state.value.steps.indexOfFirst {
                                                it.id.value == step.id.value
                                            }
                                            if (currentIndex >= 0) {
                                                viewModel.moveStep(currentIndex, if (dragOffset > 0) 1 else -1)
                                            }
                                            dragOffset = 0f
                                        }
                                    },
                                )
                            },
                    )
                    Box(Modifier.weight(1f)) {
                    if (isExpanded && editingNameStepId == step.id.value) {
                        com.example.cyberpunkandroid.components.CyberTextField(
                            value = state.pendingStepTitle,
                            placeholder = stringResource(R.string.step_name),
                            isError = BuilderValidationError.STEP_NAME_REQUIRED in state.validationErrors,
                            onValueChange = viewModel::setPendingStepTitle,
                        )
                    } else {
                        Text(
                            text = step.title.ifBlank { stringResource(R.string.step_name) },
                            style = CyberTheme.typography.body,
                            color = if (step.title.isBlank()) CyberTheme.colors.textSecondary else CyberTheme.colors.textPrimary,
                            modifier = if (isExpanded) {
                                Modifier.wrapContentWidth().clickable { editingNameStepId = step.id.value }
                            } else {
                                Modifier
                            },
                        )
                    }
                    }
                    if (!isExpanded) step.durationSeconds?.let { seconds ->
                        val duration = routineDurationParts(seconds)
                        Text(
                            stringResource(
                                R.string.authoring_duration_compact,
                                duration.first,
                                duration.second,
                            ),
                            color = CyberTheme.colors.textSecondary,
                        )
                    }
                    }
                },
                onExpandedChange = changeExpansion,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    if (BuilderValidationError.STEP_NAME_REQUIRED in state.validationErrors && state.editingStepIndex == index) {
                        Text(stringResource(R.string.validation_step_name_required), color = CyberTheme.semantics.colors.danger)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val seconds = step.durationSeconds ?: 0L
                        val duration = routineDurationParts(seconds)
                        CyberButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                            enabled = step.cues.isEmpty(),
                            onClick = {
                                val currentMinutes = (seconds / SECONDS_PER_MINUTE)
                                    .coerceIn(0L, Int.MAX_VALUE.toLong())
                                    .toInt()
                                val minutesPicker = NumberPicker(context).apply {
                                    minValue = 0
                                    maxValue = maxOf(1440, currentMinutes)
                                    value = currentMinutes
                                }
                                val secondsPicker = NumberPicker(context).apply {
                                    minValue = 0
                                    maxValue = (SECONDS_PER_MINUTE - 1).toInt()
                                    value = (seconds % SECONDS_PER_MINUTE).toInt()
                                }
                                val pickers = android.widget.LinearLayout(context).apply {
                                    orientation = android.widget.LinearLayout.HORIZONTAL
                                    addView(minutesPicker)
                                    addView(secondsPicker)
                                }
                                android.app.AlertDialog.Builder(context)
                                    .setTitle(R.string.authoring_duration)
                                    .setView(pickers)
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton(android.R.string.ok) { _, _ ->
                                        val totalSeconds = minutesPicker.value * SECONDS_PER_MINUTE + secondsPicker.value
                                        val currentIndex = viewModel.state.value.steps.indexOfFirst { it.id.value == step.id.value }
                                        if (currentIndex >= 0) {
                                            viewModel.editStep(currentIndex)
                                            if (viewModel.state.value.editingStepIndex == currentIndex) {
                                                viewModel.setPendingTimerSeconds(if (totalSeconds == 0L) "" else totalSeconds.toString())
                                            }
                                        }
                                    }
                                    .show()
                            },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Text(stringResource(R.string.authoring_duration_value, duration.first, duration.second))
                        }
                    }
                    if (BuilderValidationError.STEP_TIMER_MUST_BE_POSITIVE in state.validationErrors && state.editingStepIndex == index) {
                        Text(stringResource(R.string.validation_timer_positive), color = CyberTheme.semantics.colors.danger)
                    }
                    BuilderCueEditor(
                        cues = step.cues,
                        durationText = { cue -> state.pendingCueDurations["${step.id.value}:${cue.id.value}"]
                            ?: cue.durationSeconds.toString() },
                        durationErrorIds = state.invalidCueDurationIds,
                        onTitleChange = { cueId, title -> viewModel.setCueTitle(step.id, cueId, title) },
                        onDurationChange = { cueId, value -> viewModel.setCueDuration(step.id, cueId, value) },
                        onAddCue = { viewModel.addCue(step.id) },
                        onRemoveCue = { viewModel.removeCue(step.id, it) },
                        onMoveCue = { cueId, offset -> viewModel.moveCue(step.id, cueId, offset) },
                    )
                    if (BuilderValidationError.CUE_FIELDS_INVALID in state.validationErrors && step.cues.isNotEmpty()) {
                        Text(stringResource(R.string.cue_duration_invalid), color = CyberTheme.semantics.colors.danger)
                    }
                    CyberButton(
                        modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                        onClick = {
                            viewModel.removeStep(index)
                            if (expandedStepId == step.id.value) expandedStepId = null
                        },
                        style = CyberButtonStyle.Outline,
                        size = CyberButtonSize.Small,
                    ) {
                        Text(stringResource(R.string.remove_step))
                    }
                }
            }
        }
        item {
            CyberButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                onClick = {
                    viewModel.addStep("")
                    expandedStepId = viewModel.state.value.steps.lastOrNull()?.id?.value
                },
                style = CyberButtonStyle.Outline,
                size = CyberButtonSize.Large,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CyberIcon(iconRes = CyberIcons.Plus, contentDescription = null)
                    Text(addStepDescription)
                }
            }
        }
        item {
            val missingHighlighted = state.validationErrors.any {
                it in setOf(
                    BuilderValidationError.ROUTINE_NAME_REQUIRED,
                    BuilderValidationError.STEP_NAME_REQUIRED,
                    BuilderValidationError.SELECTED_DAY_REQUIRED,
                    BuilderValidationError.SCHEDULE_DATE_REQUIRED,
                )
            }
            if (missingHighlighted) {
                Text(stringResource(R.string.validation_fix_fields), color = CyberTheme.semantics.colors.danger)
            }
            if (BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE in state.validationErrors) {
                Text(stringResource(R.string.validation_routine_setting_exclusive), color = CyberTheme.semantics.colors.danger)
            }
            if (BuilderValidationError.LEGACY_SETTINGS_CONFLICT in state.validationErrors) {
                Text(stringResource(R.string.validation_legacy_settings_conflict), color = CyberTheme.semantics.colors.danger)
            }
            if (BuilderValidationError.SAVE_FAILED in state.validationErrors) {
                Text(stringResource(R.string.validation_save_failed), color = CyberTheme.semantics.colors.danger)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                OutlinedButton(
                    onClick = requestClose,
                    modifier = Modifier.weight(1f).heightIn(min = CyberPrimitives.IconSizes.dp48),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTheme.semantics.colors.warning),
                ) {
                    Text(stringResource(R.string.discard))
                }
                CyberButton(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f).heightIn(min = CyberPrimitives.IconSizes.dp48),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CyberIcon(iconRes = CyberIcons.Save, contentDescription = null, size = spacing.medium)
                        Text(stringResource(if (state.isSaving) R.string.saving else R.string.save))
                    }
                }
            }
        }
    }
    }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                Button(
                    onClick = { showDiscardDialog = false; onClose() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberTheme.semantics.colors.warning,
                        contentColor = CyberTheme.colors.background,
                    ),
                ) {
                    Text(stringResource(R.string.discard_changes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}

/** Use this function to edit the optional local recurrence attached to a routine draft. */
@Composable
private fun ScheduleEditor(state: BuilderState, viewModel: RoutineBuilderViewModel, spacing: Spacing) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setScheduleEnabled(granted)
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        LabeledSwitchRow(label = stringResource(R.string.authoring_repeating), checked = state.scheduleEnabled, onCheckedChange = { enabled ->
                if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) viewModel.setScheduleEnabled(enabled)
                else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
        if (state.scheduleEnabled) {
            val recurringFrequencies = listOf(
                ScheduleFrequency.DAILY,
                ScheduleFrequency.WEEKDAYS,
                ScheduleFrequency.SELECTED_DAYS,
            )
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                recurringFrequencies.forEach { frequency ->
                    val label = frequencyLabel(frequency)
                    if (state.scheduleFrequency == frequency) Button(onClick = { viewModel.setScheduleFrequency(frequency) }) { Text(label) }
                    else OutlinedButton(onClick = { viewModel.setScheduleFrequency(frequency) }) { Text(label) }
                }
            }
            if (state.scheduleFrequency == ScheduleFrequency.SELECTED_DAYS) {
                WeekdaySelector(
                    selectedDays = state.scheduleDaysOfWeek,
                    onDayToggle = { day ->
                        val selected = day in state.scheduleDaysOfWeek
                        viewModel.setScheduleDays(if (selected) state.scheduleDaysOfWeek - day else state.scheduleDaysOfWeek + day)
                    },
                )
                if (BuilderValidationError.SELECTED_DAY_REQUIRED in state.validationErrors) {
                    Text(stringResource(R.string.validation_selected_day_required), color = CyberTheme.semantics.colors.danger)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { showTimePicker(context, state.scheduleHour, state.scheduleMinute, viewModel::setScheduleTime) }) {
                    Text(
                        text = formatScheduleTime(context, state.scheduleHour, state.scheduleMinute),
                        style = CyberTheme.typography.display,
                    )
                }
            }
            LabeledSwitchRow(
                label = stringResource(R.string.authoring_remind_every),
                checked = state.remindEveryMinutes != null,
                onCheckedChange = { enabled ->
                    viewModel.setRemindEveryMinutes(if (enabled) context.resources.getInteger(R.integer.authoring_default_repeat_minutes) else null)
                },
            )
            state.remindEveryMinutes?.let { minutes ->
                TextButton(onClick = {
                    val picker = NumberPicker(context).apply {
                        minValue = 1
                        maxValue = 1440
                        value = minutes
                        wrapSelectorWheel = true
                    }
                    android.app.AlertDialog.Builder(context)
                        .setTitle(R.string.authoring_remind_every)
                        .setView(picker)
                        .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.setRemindEveryMinutes(picker.value) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }) { Text(stringResource(R.string.authoring_remind_every_minutes, minutes)) }
            }
        }
    }
}

/** Use this function when a labeled setting should expose one accessible switch target for its whole row. */
@Composable
internal fun LabeledSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = CyberPrimitives.Spacing.dp8))
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
    }
}

/** Use this function to render selectable weekdays with visible and semantic selected state. */
@Composable
internal fun WeekdaySelector(selectedDays: Set<Int>, onDayToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.spacing().small),
    ) {
        (1..7).forEach { day ->
            FilterChip(
                selected = day in selectedDays,
                onClick = { onDayToggle(day) },
                label = { Text(dayLabel(day)) },
            )
        }
    }
}

/** Use this function to format a selected device date and time for builder controls. */
private fun formatDateTime(context: android.content.Context, epochMillis: Long): String {
    val date = Date(epochMillis)
    return "${DateFormat.getDateFormat(context).format(date)} ${DateFormat.getTimeFormat(context).format(date)}"
}

/** Use this function to format a recurring schedule time with the device 12/24-hour preference. */
private fun formatScheduleTime(context: android.content.Context, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

/** Use this function to map recurrence values to localized schedule labels. */
@Composable
private fun frequencyLabel(frequency: ScheduleFrequency): String = stringResource(
    when (frequency) {
        ScheduleFrequency.ONCE -> R.string.authoring_frequency_once
        ScheduleFrequency.DAILY -> R.string.authoring_frequency_daily
        ScheduleFrequency.WEEKDAYS -> R.string.authoring_frequency_weekdays
        ScheduleFrequency.SELECTED_DAYS -> R.string.authoring_frequency_custom
    },
)

/** Use this function to show weekday names instead of storage integers. */
@Composable
private fun dayLabel(day: Int): String = stringResource(
    when (day) {
        1 -> R.string.authoring_day_monday
        2 -> R.string.authoring_day_tuesday
        3 -> R.string.authoring_day_wednesday
        4 -> R.string.authoring_day_thursday
        5 -> R.string.authoring_day_friday
        6 -> R.string.authoring_day_saturday
        else -> R.string.authoring_day_sunday
    },
)

/** Use this function to let Android's native date and time pickers select an epoch deadline. */
private fun showDateTimePicker(context: android.content.Context, onSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            onSelected(Calendar.getInstance().apply { set(year, month, day, hour, minute, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(context)).show()
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
}

/** Use this function when a schedule needs a native Android scrolling time selection. */
private fun showTimePicker(context: android.content.Context, hour: Int, minute: Int, onSelected: (Int, Int) -> Unit) {
    TimePickerDialog(
        context,
        android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
        { _, selectedHour, selectedMinute -> onSelected(selectedHour, selectedMinute) },
        hour,
        minute,
        DateFormat.is24HourFormat(context),
    ).show()
}

/** Use this function to bind the persisted run state to the stateless runner screen. */
@Composable
private fun RoutineRunnerRoute(
    container: AppContainer,
    preferences: UserPreferences,
    routineId: RoutineId,
    onFinished: () -> Unit,
) {
    val viewModel: RoutineRunnerViewModel = viewModel(key = "runner-${routineId.value}") {
        RoutineRunnerViewModel(container, routineId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    foreground = true
                    viewModel.setForeground(true)
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    viewModel.setForeground(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.setForeground(foreground)
        onDispose {
            foreground = false
            viewModel.setForeground(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val presentation = rememberRunnerPresentation(
        run = state.run,
        animationsEnabled = preferences.screenTransitionsEnabled && runnerAnimationsEnabled(LocalContext.current),
        foreground = foreground,
    )
    LaunchedEffect(state.finished, presentation.holdingCompletion) {
        if (state.finished && !presentation.holdingCompletion) onFinished()
    }
    val animationsEnabled = preferences.screenTransitionsEnabled && runnerAnimationsEnabled(LocalContext.current)
    LaunchedEffect(animationsEnabled) { viewModel.setTransitionsEnabled(animationsEnabled) }
    RoutineRunnerScreen(
        state = state,
        preferences = preferences,
        presentation = presentation,
        foreground = foreground,
        animationsEnabled = animationsEnabled,
        remaining = presentation.run?.let { container.runEngine.remainingMillis(it, state.nowEpochMillis) },
        cueRemaining = presentation.run?.let { container.runEngine.cueRemainingMillis(it, state.nowEpochMillis) },
        cueElapsed = presentation.run?.let { run ->
            run.steps[run.currentStepIndex].source.cues.map { cue ->
                container.runEngine.cueElapsedMillis(run, cue.id, state.nowEpochMillis)
            }
        }.orEmpty(),
        behindCueSchedule = presentation.run?.let {
            container.runEngine.isBehindCueSchedule(it, state.nowEpochMillis)
        } ?: false,
        unfinishedStepIndexes = presentation.run?.let(container.runEngine::unfinishedStepIndexes).orEmpty(),
        onComplete = { viewModel.complete() },
        onSkip = { viewModel.skip() },
        onBack = { viewModel.back() },
        onAdvance = { viewModel.advanceToNextFinishedStep() },
        onPause = { viewModel.pause() },
        onResume = { viewModel.resume() },
        onContinue = { viewModel.continueRun() },
        onSelectStep = { viewModel.selectStep(it) },
        onConfirmComplete = { viewModel.confirmComplete() },
        onAbort = { viewModel.abort() },
        onRetryHistorySave = viewModel::retryHistorySave,
    )
}

/** Use this function to render the runner from immutable VM/presentation state and emit user intent.
 * Inputs: state and presentation snapshots, preferences, lifecycle/motion gates, derived timer/unfinished tasks,
 * and action callbacks. Dependencies: RunnerState, RunnerPresentation, RunCountdownDial, and runnerGestures.
 */
@Composable
private fun RoutineRunnerScreen(
    state: RunnerState,
    preferences: UserPreferences,
    presentation: RunnerPresentation,
    foreground: Boolean,
    animationsEnabled: Boolean,
    remaining: Long?,
    cueRemaining: Long?,
    cueElapsed: List<Long>,
    behindCueSchedule: Boolean,
    unfinishedStepIndexes: List<Int>,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onAdvance: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onContinue: () -> Unit,
    onSelectStep: (Int) -> Unit,
    onConfirmComplete: () -> Unit,
    onAbort: () -> Unit,
    onRetryHistorySave: () -> Unit,
) {
    val run = presentation.run
    if (run == null) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.runner_title)) }) },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }
    BackHandler(enabled = run.status == RunStatus.ACTIVE || presentation.holdingCompletion) {
        if (!presentation.holdingCompletion) {
            if (run.abortConfirmationRequested || run.finishConfirmationRequested) {
                onContinue()
            } else {
                onBack()
            }
        }
    }

    val spacing = TaskChainDesignSystem.spacing()
    val current = run.steps[run.currentStepIndex]
    val configuration = LocalConfiguration.current
    val smallerDimensionDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp).dp
    val controlMinWidth = 112.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale
    val dialDiameter = (smallerDimensionDp - CyberPrimitives.Spacing.dp32 * 2)
        .coerceAtLeast(CyberPrimitives.IconSizes.dp48)
        .coerceAtMost(RunnerMotion.dialDiameter)
    val entrance = remember(run.currentStepIndex) { Animatable(0f) }
    LaunchedEffect(run.currentStepIndex, animationsEnabled) {
        if (animationsEnabled) {
            entrance.animateTo(1f, tween(RunnerMotion.durationMillis, easing = RunnerMotion.easing))
        } else {
            entrance.snapTo(1f)
        }
    }

    val hasTimer = current.source.durationSeconds != null
    val timerString = if (hasTimer) {
        formatRunnerTimer(remaining, current.actualDurationMillis, preferences.continueTimerPastZero)
    } else {
        ""
    }
    val transitionColor = CyberTheme.semantics.colors.warning
    Scaffold(
        modifier = Modifier.drawWithContent {
            drawContent()
            if (presentation.readyLabel != null) {
                val phase = (presentation.transitionElapsedMillis % 1000L) / 1000f
                drawRect(transitionColor.copy(alpha = 0.15f * (1f - kotlin.math.abs(phase * 2f - 1f))))
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(run.routineTitle, style = CyberTheme.typography.display)
            })
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .background(CyberTheme.colors.background)
                .runnerGestureTracking(
                    paused = current.pausedAtEpochMillis != null || current.status == RunStepStatus.SKIPPED,
                    onAdvance = { if (!presentation.holdingCompletion) onAdvance() },
                    onSkip = { if (!presentation.holdingCompletion) onSkip() },
                    onPause = { if (!presentation.holdingCompletion) onPause() },
                    onResume = { if (!presentation.holdingCompletion) onResume() },
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .widthIn(max = dimensionResource(R.dimen.content_max_width))
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = CyberPrimitives.Spacing.dp24),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp24),
                ) {
                    Text(
                        text = current.source.title,
                        style = MaterialTheme.typography.displayMedium,
                        color = if (presentation.readyLabel != null) CyberTheme.semantics.colors.warning else CyberTheme.colors.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberOverload(enabled = presentation.readyLabel != null &&
                                presentation.transitionElapsedMillis % 1000L < 300L, animationSpec = tween(300))
                            .offset(y = RunnerMotion.titleStartOffset +
                                (RunnerMotion.titleEndOffset - RunnerMotion.titleStartOffset) * entrance.value),
                    )

                    RunnerCueList(current, cueRemaining, preferences.showCueTimeRemaining)

                    key(run.currentStepIndex, presentation.holdingCompletion) {
                        Box(modifier = Modifier.graphicsLayer { alpha = entrance.value }) {
                            RunnerCueDial(current, cueElapsed, dialDiameter, behindCueSchedule) { innerDiameter ->
                            RunCountdownDial(
                                timer = if (presentation.readyLabel == "Get Ready") stringResource(R.string.punch_get_ready)
                                    else presentation.readyLabel ?: timerString,
                                progress = countdownProgress(current.source.durationSeconds, remaining),
                                remainingMillis = remaining,
                                diameter = innerDiameter,
                                behindCueSchedule = behindCueSchedule,
                                status = current.status,
                                paused = current.pausedAtEpochMillis != null,
                                completedPulse = presentation.holdingCompletion && presentation.readyLabel == null,
                                foreground = foreground,
                                animationsEnabled = animationsEnabled && presentation.readyLabel == null,
                            )
                            }
                        }
                    }

                    val completeShape = remember {
                        androidx.compose.foundation.shape.GenericShape { size, _ ->
                            moveTo(size.width * 0.03f, 0f)
                            lineTo(size.width * 0.97f, 0f)
                            lineTo(size.width, size.height * 0.16f)
                            lineTo(size.width * 0.88f, size.height * 0.84f)
                            lineTo(size.width * 0.85f, size.height * 0.84f)
                            lineTo(size.width * 0.85f, size.height)
                            lineTo(size.width * 0.15f, size.height)
                            lineTo(size.width * 0.15f, size.height * 0.84f)
                            lineTo(size.width * 0.12f, size.height * 0.84f)
                            lineTo(0f, size.height * 0.16f)
                            close()
                        }
                    }
                    CyberButton(
                        modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp64)
                            .clip(completeShape)
                            .background(Brush.verticalGradient(listOf(CyberTheme.semantics.colors.success.copy(alpha = 0.7f),
                                CyberTheme.semantics.colors.success.copy(alpha = 0.1f))))
                            .border(2.dp, CyberTheme.semantics.colors.success, completeShape),
                        enabled = !presentation.holdingCompletion,
                        onClick = { if (!presentation.holdingCompletion) onComplete() },
                        size = CyberButtonSize.Large,
                        style = CyberButtonStyle.Ghost,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = CyberPrimitives.Spacing.dp16, vertical = CyberPrimitives.Spacing.dp12),
                            horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp16),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CyberIcon(
                                iconRes = SemanticIcons.Success,
                                contentDescription = null,
                                size = CyberPrimitives.IconSizes.dp48,
                                tint = CyberTheme.semantics.colors.success,
                            )
                            Text(
                                stringResource(R.string.complete),
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp12, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp12),
                    ) {
                        run.steps.forEachIndexed { index, step ->
                            val isCurrent = index == run.currentStepIndex
                            val stepDescription = stringResource(
                                when (step.status) {
                                    RunStepStatus.PENDING -> R.string.runner_step_status_pending
                                    RunStepStatus.COMPLETED -> R.string.runner_step_status_completed
                                    RunStepStatus.SKIPPED -> R.string.runner_step_status_skipped
                                },
                                index + 1,
                                step.source.title,
                            )
                            Box(
                                modifier = Modifier
                                    .size(CyberPrimitives.IconSizes.dp48)
                                    .border(
                                        if (isCurrent) CyberPrimitives.BorderWidths.dp2 else CyberPrimitives.BorderWidths.dp1,
                                        statusColor(step.status),
                                        CircleShape,
                                    )
                                    .background(
                                        statusColor(step.status).copy(alpha = if (isCurrent) 0.16f else 0.06f),
                                        CircleShape,
                                    )
                                    .semantics { contentDescription = stepDescription },
                                contentAlignment = Alignment.Center,
                            ) {
                                when (step.status) {
                                    RunStepStatus.PENDING -> {
                                        CyberIcon(
                                            iconRes = CyberIcons.Play,
                                            contentDescription = null,
                                            size = if (isCurrent) CyberPrimitives.IconSizes.dp24 else CyberPrimitives.IconSizes.dp16,
                                            tint = CyberTheme.semantics.colors.info,
                                        )
                                    }
                                    RunStepStatus.COMPLETED -> {
                                        CyberIcon(
                                            iconRes = SemanticIcons.Success,
                                            contentDescription = null,
                                            tint = CyberTheme.semantics.colors.success,
                                        )
                                    }
                                    RunStepStatus.SKIPPED -> {
                                        CyberIcon(
                                            iconRes = SemanticIcons.Caution,
                                            contentDescription = null,
                                            tint = CyberTheme.semantics.colors.warning,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                RunnerMainProgress(run, state.nowEpochMillis,
                    presentation.holdingCompletion && presentation.readyLabel == null, foreground, animationsEnabled)
                RunnerNextUpPreview(run)

                if (state.historySaveFailed) {
                    Text(stringResource(R.string.run_history_save_failed))
                    Button(onClick = onRetryHistorySave) { Text(stringResource(R.string.run_retry_history_save)) }
                } else if (run.status == RunStatus.ACTIVE) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = CyberPrimitives.Spacing.dp16),
                        horizontalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
                        verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
                    ) {
                        CyberButton(
                            modifier = Modifier.weight(1f).widthIn(min = controlMinWidth).heightIn(min = CyberPrimitives.IconSizes.dp48),
                            enabled = !presentation.holdingCompletion,
                            onClick = { if (!presentation.holdingCompletion) onBack() },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberIcon(iconRes = CyberIcons.ArrowLeft, contentDescription = null, size = CyberPrimitives.Spacing.dp16)
                                Text(stringResource(R.string.back))
                            }
                        }
                        CyberButton(
                            modifier = Modifier.weight(1f).widthIn(min = controlMinWidth).heightIn(min = CyberPrimitives.IconSizes.dp48),
                            enabled = !presentation.holdingCompletion,
                            onClick = {
                                if (!presentation.holdingCompletion) {
                                    if (current.pausedAtEpochMillis == null && current.status != RunStepStatus.SKIPPED) onPause() else onResume()
                                }
                            },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberIcon(
                                    iconRes = if (current.pausedAtEpochMillis == null && current.status != RunStepStatus.SKIPPED) CyberIcons.Pause else CyberIcons.Play,
                                    contentDescription = null,
                                    size = CyberPrimitives.Spacing.dp16,
                                )
                                Text(stringResource(if (current.pausedAtEpochMillis == null && current.status != RunStepStatus.SKIPPED) R.string.pause else R.string.resume))
                            }
                        }
                        CyberButton(
                            modifier = Modifier.weight(1f).widthIn(min = controlMinWidth).heightIn(min = CyberPrimitives.IconSizes.dp48),
                            enabled = !presentation.holdingCompletion,
                            onClick = { if (!presentation.holdingCompletion) onSkip() },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberIcon(
                                    iconRes = SemanticIcons.Caution,
                                    contentDescription = null,
                                    size = CyberPrimitives.Spacing.dp16,
                                    tint = CyberTheme.semantics.colors.warning,
                                )
                                Text(stringResource(R.string.skip))
                            }
                        }
                    }
                }
            }
        }
    }

    if (run.status == RunStatus.ACTIVE && run.finishConfirmationRequested && !presentation.holdingCompletion) {
        val unfinished = unfinishedStepIndexes
        AlertDialog(
            onDismissRequest = { if (!presentation.holdingCompletion) onContinue() },
            title = { Text(stringResource(R.string.confirm_complete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    if (unfinished.isNotEmpty()) Text(stringResource(R.string.unfinished_tasks))
                    unfinished.forEach { index ->
                        TextButton(onClick = { if (!presentation.holdingCompletion) onSelectStep(index) }) { Text(run.steps[index].source.title) }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { if (!presentation.holdingCompletion) onConfirmComplete() }) { Text(stringResource(R.string.confirm_complete)) }
            },
            dismissButton = {
                TextButton(onClick = { if (!presentation.holdingCompletion) onContinue() }) { Text(stringResource(R.string.continue_run)) }
            },
        )
    }
    if (run.status == RunStatus.ACTIVE && run.abortConfirmationRequested && !presentation.holdingCompletion) {
        AlertDialog(
            modifier = Modifier.border(3.dp, CyberTheme.semantics.colors.error, CyberTheme.shapes.cyberCutCornerShape),
            onDismissRequest = { if (!presentation.holdingCompletion) onContinue() },
            title = { Text(stringResource(R.string.abort_title), style = MaterialTheme.typography.headlineLarge) },
            text = { Text(stringResource(R.string.abort_run), style = MaterialTheme.typography.titleLarge) },
            confirmButton = {
                Button(
                    modifier = Modifier.background(Brush.verticalGradient(listOf(
                        CyberTheme.semantics.colors.error, CyberTheme.semantics.colors.error.copy(alpha = 0.55f))),
                        CyberTheme.shapes.cyberCutCornerShape),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = CyberTheme.colors.textPrimary),
                    onClick = { if (!presentation.holdingCompletion) onAbort() },
                ) { Text(stringResource(R.string.abort_run), style = MaterialTheme.typography.titleLarge) }
            },
            dismissButton = {
                TextButton(onClick = { if (!presentation.holdingCompletion) onContinue() }) { Text(stringResource(R.string.continue_run)) }
            },
        )
    }
}

/** Paint elapsed countdown progress clockwise over the library's dial ticks with time-responsive effects and styles. */
@Composable
private fun RunCountdownDial(
    timer: String,
    progress: Float,
    remainingMillis: Long?,
    diameter: Dp,
    behindCueSchedule: Boolean,
    status: RunStepStatus,
    paused: Boolean,
    completedPulse: Boolean,
    foreground: Boolean,
    animationsEnabled: Boolean,
) {
    val painted by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        animationSpec = if (animationsEnabled) tween(800) else snap(),
        label = "CountdownTicks",
    )

    val isOvertime = remainingMillis != null && remainingMillis < 0L
    val remainingRatio = (1f - progress).coerceIn(0f, 1f)
    val isUrgent = isOvertime || remainingRatio <= 0.15f
    val isWarning = !isUrgent && remainingRatio <= 0.35f

    val targetAccentColor = when {
        status == RunStepStatus.COMPLETED -> CyberTheme.semantics.colors.success
        status == RunStepStatus.SKIPPED -> CyberTheme.semantics.colors.warning
        behindCueSchedule -> CyberTheme.semantics.colors.warning
        isOvertime -> CyberTheme.semantics.colors.error
        isUrgent || isWarning -> CyberTheme.semantics.colors.warning
        else -> CyberTheme.semantics.colors.info
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = if (animationsEnabled) tween(600) else snap(),
        label = "DialColorAnimation",
    )

    val pulseAlpha = if (foreground && animationsEnabled && isUrgent && status == RunStepStatus.PENDING && !paused) {
        val infiniteTransition = rememberInfiniteTransition(label = "DialPulseTransition")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (isOvertime) 300 else 600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "DialPulseAlpha",
        )
        alpha
    } else 1f

    val currentGlowColor = if (isUrgent) animatedAccentColor.copy(alpha = pulseAlpha) else animatedAccentColor
    val glowRadius = if (isUrgent) CyberPrimitives.Spacing.dp16 else CyberPrimitives.Spacing.dp8
    val radialModifier = when {
        !foreground || !animationsEnabled -> Modifier
        completedPulse -> Modifier.cyberRadialPulse(
            rememberCyberRadialPulse(animationSpec = tween(RunnerMotion.completionDurationMillis)),
            color = CyberTheme.semantics.colors.success,
        )
        status == RunStepStatus.PENDING && paused -> Modifier.cyberRadialPulse(
            rememberCyberRadialPulse(animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing))),
            color = CyberTheme.semantics.colors.warning,
        )
        // The sweep head follows the same elapsed fraction as the ticks rather than an independent loop.
        status == RunStepStatus.PENDING -> Modifier.drawWithContent {
            drawArc(animatedAccentColor.copy(alpha = 0.15f), -90f, 360f * painted, useCenter = true)
            drawLine(animatedAccentColor, center, center + androidx.compose.ui.geometry.Offset(
                kotlin.math.cos((painted * 360f - 90f) * kotlin.math.PI / 180).toFloat() * size.width / 2,
                kotlin.math.sin((painted * 360f - 90f) * kotlin.math.PI / 180).toFloat() * size.height / 2,
            ), strokeWidth = 2.dp.toPx())
            drawContent()
        }
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(if (behindCueSchedule) CyberTheme.semantics.colors.warning.copy(alpha = 0.3f)
                else androidx.compose.ui.graphics.Color.Transparent)
            .then(radialModifier)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        CyberSectorRim(
            size = diameter,
            color = CyberTheme.colors.border,
            thickness = CyberPrimitives.BorderWidths.dp2 * 3,
            sectorAngles = listOf(80f, 80f, 80f, 80f),
            gapAngle = 10f,
        )

        CyberSectorRim(
            modifier = Modifier.drawWithContent {
                val elapsedArea = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(Rect(0f, 0f, size.width, size.height), -90f, 360f * painted, false)
                    close()
                }
                clipPath(elapsedArea) { this@drawWithContent.drawContent() }
            },
            size = diameter,
            color = animatedAccentColor.copy(alpha = pulseAlpha),
            thickness = CyberPrimitives.BorderWidths.dp2 * 3,
            sectorAngles = listOf(80f, 80f, 80f, 80f),
            gapAngle = 10f,
        )

        CyberDialTicks(
            size = diameter * 0.92f,
            color = CyberTheme.colors.border.copy(alpha = 0.35f),
            tickLength = CyberPrimitives.Spacing.dp12,
            majorTickLength = CyberPrimitives.Spacing.dp24,
            strokeWidth = CyberPrimitives.BorderWidths.dp2 * 3,
        )

        CyberDialTicks(
            modifier = Modifier.drawWithContent {
                val paintedArea = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(Rect(0f, 0f, size.width, size.height), -90f, 360f * painted, false)
                    close()
                }
                clipPath(paintedArea) { this@drawWithContent.drawContent() }
            },
            size = diameter * 0.92f,
            color = animatedAccentColor.copy(alpha = if (isUrgent) pulseAlpha else 1.0f),
            tickLength = CyberPrimitives.Spacing.dp12,
            majorTickLength = CyberPrimitives.Spacing.dp24,
            strokeWidth = CyberPrimitives.BorderWidths.dp2 * 3,
        )

        if (timer.isNotEmpty()) {
            GlowingText(
                text = timer,
                glowRadius = glowRadius,
                glowColor = currentGlowColor,
                textColor = CyberTheme.colors.textPrimary,
                fontSize = with(androidx.compose.ui.platform.LocalDensity.current) {
                    (diameter.toPx() * 0.68f / (timer.length.coerceAtLeast(1) * 0.65f)).toSp().value.coerceAtMost(64f)
                }.let { androidx.compose.ui.unit.TextUnit(it, androidx.compose.ui.unit.TextUnitType.Sp) },
            )
        } else {
            CyberIcon(
                iconRes = when {
                    status == RunStepStatus.COMPLETED -> SemanticIcons.Success
                    status == RunStepStatus.SKIPPED -> SemanticIcons.Caution
                    paused -> CyberIcons.Pause
                    else -> CyberIcons.Play
                },
                contentDescription = null,
                size = CyberPrimitives.IconSizes.dp48,
                tint = animatedAccentColor,
            )
        }
    }
}

internal fun countdownProgress(timerSeconds: Long?, remainingMillis: Long?): Float {
    if (timerSeconds == null || timerSeconds <= 0 || remainingMillis == null) return 0f
    return (1.0 - remainingMillis.toDouble() / (timerSeconds.toDouble() * MILLIS_PER_SECOND))
        .coerceIn(0.0, 1.0).toFloat()
}

/** Use this function to format the runner timer digits for countdown, overtime, elapsed, and untimed displays. */
@Composable
internal fun formatRunnerTimer(remainingMillis: Long?, actualDurationMillis: Long?, continuePastZero: Boolean): String {
    if (remainingMillis == null && actualDurationMillis == null) return ""
    if (actualDurationMillis != null) {
        val seconds = actualDurationMillis / MILLIS_PER_SECOND
        return String.format("%d:%02d", seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }
    if (remainingMillis == null) return ""
    val displayMillis = if (continuePastZero) remainingMillis else remainingMillis.coerceAtLeast(0)
    val seconds = displayMillis.absoluteValue / MILLIS_PER_SECOND
    val prefix = if (displayMillis < 0) "+" else ""
    return String.format("%s%d:%02d", prefix, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

/** Use this function to bind completion-history projections to Progress UI. */
@Composable
private fun ProgressRoute(viewModel: ProgressViewModel, padding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item { Text(stringResource(R.string.completed_runs, state.completedRuns)) }
        item { Text(stringResource(R.string.aborted_runs, state.abortedRuns)) }
        item { Text(stringResource(R.string.progress_actual_duration, state.actualDurationMillis / MILLIS_PER_SECOND)) }
        item { Text(stringResource(R.string.progress_step_adherence, state.stepAdherencePercent)) }
        item { Text(stringResource(R.string.progress_skipped_steps, state.skippedStepPercent)) }
        item { Text(stringResource(R.string.progress_seven_day_trend)) }
        items(state.lastSevenDays) { day ->
            Text(stringResource(R.string.progress_daily_count, DateFormat.format("EEE M/d", day.dayStartEpochMillis), day.completedRuns))
        }
    }
}

/** Use this function to bind persisted appearance and timer behavior to Settings UI. */
@Composable
private fun SettingsRoute(viewModel: SettingsViewModel, padding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Text(stringResource(R.string.theme), style = CyberTheme.typography.display)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.small), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                ThemeCatalog.options(context).forEach { option ->
                    if (state.selectedTheme == option.id) {
                        Button(onClick = { viewModel.setTheme(option.id) }) { Text(stringResource(option.label)) }
                    } else {
                        OutlinedButton(onClick = { viewModel.setTheme(option.id) }) { Text(stringResource(option.label)) }
                    }
                }
            }
        }
        item {
            LabeledSwitchRow(
                label = stringResource(R.string.continue_past_zero),
                checked = state.continueTimerPastZero,
                onCheckedChange = viewModel::setContinuePastZero,
            )
        }
        item {
            LabeledSwitchRow(
                label = stringResource(R.string.show_cue_time_remaining),
                checked = state.showCueTimeRemaining,
                onCheckedChange = viewModel::setShowCueTimeRemaining,
            )
        }
        item {
            Text(stringResource(R.string.punch_vibration_intensity), style = MaterialTheme.typography.titleLarge)
            androidx.compose.material3.Slider(value = state.vibrationIntensity,
                onValueChange = viewModel::setVibrationIntensity, valueRange = 0f..1f, steps = 3)
        }
        item {
            LabeledSwitchRow(label = stringResource(R.string.punch_screen_transitions),
                checked = state.screenTransitionsEnabled, onCheckedChange = viewModel::setScreenTransitionsEnabled)
        }
        item {
            LabeledSwitchRow(label = stringResource(R.string.punch_bubble_minimize),
                checked = state.bubbleOnMinimize, onCheckedChange = { enabled ->
                    viewModel.setBubbleOnMinimize(enabled)
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                })
            if (state.bubbleOnMinimize) {
                TextButton(onClick = {
                    context.startActivity(android.content.Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text(stringResource(R.string.punch_bubble_system_settings)) }
            }
        }
    }
}

/** Use this function when navigating to a new or existing builder route. */
private fun builderRoute(routineId: RoutineId?): String = routineId?.let { "builder?routineId=${it.value}" } ?: "builder"

/** Use this function when starting a routine by stable identity. */
private fun runnerRoute(routineId: RoutineId): String = "runner/${routineId.value}"

/** Use this function to map each runner step state to the library's semantic status color. */
@Composable
private fun statusColor(status: RunStepStatus) = when (status) {
    RunStepStatus.PENDING -> CyberTheme.semantics.colors.info
    RunStepStatus.COMPLETED -> CyberTheme.semantics.colors.success
    RunStepStatus.SKIPPED -> CyberTheme.semantics.colors.warning
}

/** Use this function when deciding whether decorative runner motion should honor the system animator setting. */
private fun runnerAnimationsEnabled(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

private const val HOME_ROUTE = "home"
private const val ROUTINE_ID_ARGUMENT = "routineId"
private const val BUILDER_ROUTE = "builder?routineId={$ROUTINE_ID_ARGUMENT}"
private const val RUNNER_ROUTE = "runner/{$ROUTINE_ID_ARGUMENT}"
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val DRAG_REORDER_THRESHOLD_PX = 48f
private const val DRAG_TRANSITION_MILLIS = 250
