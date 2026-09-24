@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.NumberPicker
import android.widget.LinearLayout
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.taskchain.ui.designsystem.Spacing
import com.taskchain.ui.designsystem.TaskChainTheme
import com.taskchain.ui.designsystem.ThemeCatalog
import kotlin.math.absoluteValue

/** Top-level tab choices retained while deeper builder and runner routes are open. */
private enum class HomeTab(@param:StringRes val label: Int) {
    HOME(R.string.tab_home),
    ROUTINES(R.string.tab_routines),
    PROGRESS(R.string.tab_progress),
    SETTINGS(R.string.tab_settings),
}

/** Use this function as the Compose application entry point. */
@Composable
fun TaskChainApp(container: AppContainer) {
    val preferences by container.preferences.observe().collectAsStateWithLifecycle(UserPreferences())
    TaskChainTheme(preferences.selectedTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
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
                composable(
                    route = RUNNER_ROUTE,
                    arguments = listOf(navArgument(ROUTINE_ID_ARGUMENT) { type = NavType.StringType }),
                ) { entry ->
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

/** Use this function to render persistent tabs and preserve the selected tab across recomposition. */
@Composable
private fun HomeShell(
    container: AppContainer,
    selectedTab: HomeTab,
    onSelectedTabChange: (HomeTab) -> Unit,
    onCreate: () -> Unit,
    onEdit: (RoutineTemplate) -> Unit,
    onStart: (RoutineTemplate) -> Unit,
) {
    val todayListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val routinesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val routinesViewModel: RoutinesViewModel = viewModel { RoutinesViewModel(container) }
    val progressViewModel: ProgressViewModel = viewModel { ProgressViewModel(container) }
    val settingsViewModel: SettingsViewModel = viewModel { SettingsViewModel(container) }
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
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        when (selectedTab) {
            HomeTab.HOME -> TodayRoute(routinesViewModel, padding, todayListState, onCreate, onStart)
            HomeTab.ROUTINES -> RoutinesRoute(routinesViewModel, padding, routinesListState, onCreate, onEdit, onStart)
            HomeTab.PROGRESS -> ProgressRoute(progressViewModel, padding)
            HomeTab.SETTINGS -> SettingsRoute(settingsViewModel, padding)
        }
    }
}

/** Use this function to bind Today UI to the routine-list ViewModel. */
@Composable
private fun TodayRoute(
    viewModel: RoutinesViewModel,
    padding: PaddingValues,
    listState: LazyListState,
    onCreate: () -> Unit,
    onStart: (RoutineTemplate) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    val projection = state.todayRoutines
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        state = listState,
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        if (projection.scheduled.isEmpty() && projection.manual.isEmpty() && projection.completed.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text(stringResource(R.string.today_empty))
                    Button(onClick = onCreate) { Text(stringResource(R.string.new_routine)) }
                }
            }
        }
        fun section(title: Int, routines: List<RoutineTemplate>, isCompleted: Boolean = false) {
            item { Text(stringResource(title), style = MaterialTheme.typography.titleLarge) }
            items(routines, key = { it.id.value }) { routine ->
                CompactRoutineRow(routine = routine, isCompleted = isCompleted) { onStart(routine) }
            }
        }
        section(R.string.home_scheduled, projection.scheduled)
        section(R.string.home_manual, projection.manual)
        section(R.string.home_completed, projection.completed, isCompleted = true)
    }
}

/** Use this function to render each routine on Home as a compact row with its action. */
@Composable
private fun CompactRoutineRow(
    routine: RoutineTemplate,
    isCompleted: Boolean,
    onStart: () -> Unit,
) {
    val spacing = TaskChainDesignSystem.spacing()
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium, vertical = spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = routine.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = spacing.small),
            )
            if (isCompleted) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.status_completed),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Surface(
                    onClick = onStart,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play),
                            contentDescription = stringResource(R.string.start_routine),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        state = listState,
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        items(state.routines, key = { it.id.value }) { routine ->
            RoutineCard(routine, onEdit = { onEdit(routine) }, onStart = { onStart(routine) })
        }
        if (state.builtIns.isNotEmpty()) {
            items(state.builtIns, key = { it.id.value }) { routine ->
                RoutineCard(routine, onEdit = { onEdit(routine) }, onStart = { onStart(routine) })
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.small),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onCreate,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.new_routine),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = routine.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    onEdit?.let {
                        IconButton(onClick = it) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.edit_routine),
                            )
                        }
                    }
                    Surface(
                        onClick = onStart,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_play),
                                contentDescription = stringResource(R.string.start_routine),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
            if (routine.description.isNotBlank()) {
                Text(
                    text = "  ${routine.description}",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                )
            }
            val itemCountText = formatRoutineItemCount(routine)
            val totalTimeText = formatRoutineTotalTime(routine)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = itemCountText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (totalTimeText != null) {
                    Text(
                        text = totalTimeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Use this function to format the total time of a routine template, or null if untimed. */
internal fun formatRoutineTotalTime(routine: RoutineTemplate): String? {
    if (routine.steps.none { it.timerSeconds != null }) return null
    val totalSeconds = routine.steps.mapNotNull { it.timerSeconds }.sum()
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
    val newTaskTitle = stringResource(R.string.authoring_new_task_default)
    val addStepDescription = stringResource(R.string.add_step)
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.savedRoutineId) { if (state.savedRoutineId != null) onSaved() }
    val requestClose = {
        if (state.hasUnsavedChanges) showDiscardDialog = true else onClose()
    }
    BackHandler { requestClose() }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.builder_title)) },
            )
        },
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .imePadding(),
        contentPadding = PaddingValues(
            start = spacing.medium,
            top = spacing.medium,
            end = spacing.medium,
            bottom = spacing.large * 2,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text(stringResource(R.string.routine_name)) },
                modifier = Modifier.fillMaxWidth(),
                isError = BuilderValidationError.ROUTINE_NAME_REQUIRED in state.validationErrors,
                supportingText = {
                    if (BuilderValidationError.ROUTINE_NAME_REQUIRED in state.validationErrors) {
                        Text(stringResource(R.string.validation_routine_name_required))
                    }
                },
            )
        }
        item {
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text(stringResource(R.string.routine_description)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                ScheduleEditor(state, viewModel, spacing)
                LabeledSwitchRow(stringResource(R.string.authoring_sound), state.soundEnabled, viewModel::setSoundEnabled)
                LabeledSwitchRow(stringResource(R.string.authoring_vibrate), state.vibrateEnabled, viewModel::setVibrateEnabled)
            }
        }
        item {
            Text(stringResource(R.string.authoring_steps), style = MaterialTheme.typography.titleLarge)
            if (BuilderValidationError.STEP_REQUIRED in state.validationErrors) {
                Text(stringResource(R.string.validation_step_required), color = MaterialTheme.colorScheme.error)
            }
        }
        itemsIndexed(state.steps, key = { _, step -> step.id.value }) { index, step ->
            var dragOffset by remember(step.id) { mutableStateOf(0f) }
            val isDragging = dragOffset != 0f
            val moveUp = stringResource(R.string.authoring_move_up)
            val moveDown = stringResource(R.string.authoring_move_down)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 10f else 0f)
                    .graphicsLayer {
                        translationY = dragOffset
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 1.03f else 1f
                        shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                    }
                    .semantics {
                        customActions = buildList {
                            if (index > 0) add(CustomAccessibilityAction(moveUp) { viewModel.moveStep(index, -1); true })
                            if (index < state.steps.lastIndex) add(CustomAccessibilityAction(moveDown) { viewModel.moveStep(index, 1); true })
                        }
                    }
                    .pointerInput(step.id, index, state.steps.size) {
                        detectDragGesturesAfterLongPress(
                            onDragEnd = { dragOffset = 0f },
                            onDragCancel = { dragOffset = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                if (dragOffset.absoluteValue >= DRAG_REORDER_THRESHOLD_PX) {
                                    viewModel.moveStep(index, if (dragOffset > 0) 1 else -1)
                                    dragOffset = 0f
                                }
                            },
                        )
                    },
                border = if (isDragging) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_drag_handle),
                            contentDescription = stringResource(R.string.authoring_drag_handle),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        val seconds = step.timerSeconds ?: 0L
                        Text(
                            text = stringResource(R.string.authoring_step_duration, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { viewModel.editStep(index) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.authoring_edit_step),
                            )
                        }
                    }
                    if (state.editingStepIndex == index) {
                        OutlinedTextField(
                            state.pendingStepTitle,
                            viewModel::setPendingStepTitle,
                            label = { Text(stringResource(R.string.step_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = BuilderValidationError.STEP_NAME_REQUIRED in state.validationErrors,
                            supportingText = {
                                if (BuilderValidationError.STEP_NAME_REQUIRED in state.validationErrors) {
                                    Text(stringResource(R.string.validation_step_name_required))
                                }
                            },
                        )
                        OutlinedButton(onClick = {
                            showDurationPicker(context, state.pendingTimerSeconds.toLongOrNull(), viewModel::setPendingTimerSeconds)
                        }) {
                            val seconds = state.pendingTimerSeconds.toLongOrNull() ?: 0L
                            Text(stringResource(R.string.authoring_duration_value, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE))
                        }
                        if (BuilderValidationError.STEP_TIMER_MUST_BE_POSITIVE in state.validationErrors) {
                            Text(stringResource(R.string.validation_timer_positive), color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { viewModel.removeStep(index) }) { Text(stringResource(R.string.remove_step)) }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = { viewModel.addStep(newTaskTitle) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = addStepDescription,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
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
                Text(stringResource(R.string.validation_fix_fields), color = MaterialTheme.colorScheme.error)
            }
            if (BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE in state.validationErrors) {
                Text(stringResource(R.string.validation_routine_setting_exclusive), color = MaterialTheme.colorScheme.error)
            }
            if (BuilderValidationError.LEGACY_SETTINGS_CONFLICT in state.validationErrors) {
                Text(stringResource(R.string.validation_legacy_settings_conflict), color = MaterialTheme.colorScheme.error)
            }
            if (BuilderValidationError.SAVE_FAILED in state.validationErrors) {
                Text(stringResource(R.string.validation_save_failed), color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = requestClose,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.semantic_warning),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(stringResource(R.string.discard))
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(if (state.isSaving) R.string.saving else R.string.save))
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
                        containerColor = colorResource(R.color.semantic_warning),
                        contentColor = Color.White,
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
                    Text(stringResource(R.string.validation_selected_day_required), color = MaterialTheme.colorScheme.error)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = { showTimePicker(context, state.scheduleHour, state.scheduleMinute, viewModel::setScheduleTime) }) {
                    Text(
                        text = formatScheduleTime(context, state.scheduleHour, state.scheduleMinute),
                        style = MaterialTheme.typography.titleLarge,
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

/** Use this function to select elapsed minutes and seconds without presenting a time-of-day clock. */
private fun showDurationPicker(
    context: android.content.Context,
    currentSeconds: Long?,
    onSelected: (String) -> Unit,
) {
    val total = currentSeconds?.coerceAtLeast(0) ?: 0
    val minutes = NumberPicker(context).apply {
        minValue = 0
        maxValue = 1440
        value = (total / SECONDS_PER_MINUTE).coerceAtMost(1440).toInt()
        contentDescription = context.getString(R.string.authoring_duration_minutes)
    }
    val seconds = NumberPicker(context).apply {
        minValue = 0
        maxValue = 59
        value = (total % SECONDS_PER_MINUTE).toInt()
        contentDescription = context.getString(R.string.authoring_duration_seconds)
    }
    val pickers = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER
        addView(minutes)
        addView(seconds)
    }
    android.app.AlertDialog.Builder(context)
        .setTitle(R.string.authoring_duration)
        .setView(pickers)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            val selected = minutes.value * SECONDS_PER_MINUTE + seconds.value
            onSelected(if (selected == 0L) "" else selected.toString())
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
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
        Text(label)
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
    LaunchedEffect(state.finished) { if (state.finished) onFinished() }
    val run = state.run
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
    BackHandler(enabled = run.status == RunStatus.ACTIVE) {
        if (run.abortConfirmationRequested || run.finishConfirmationRequested) {
            viewModel.continueRun()
        } else {
            viewModel.back()
        }
    }

    val spacing = TaskChainDesignSystem.spacing()
    val current = run.steps[run.currentStepIndex]
    val remaining = container.runEngine.remainingMillis(run, state.nowEpochMillis)
    val configuration = LocalConfiguration.current
    val smallerDimensionDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp).dp
    val targetTimerWidthDp = smallerDimensionDp / 3
    val targetTimerWidthPx = with(LocalDensity.current) { targetTimerWidthDp.toPx() }

    val hasTimer = current.source.timerSeconds != null
    val timerString = if (hasTimer) {
        formatRunnerTimer(remaining, current.actualDurationMillis, preferences.continueTimerPastZero)
    } else {
        ""
    }
    val textMeasurer = rememberTextMeasurer()
    val baseStyle = MaterialTheme.typography.displayLarge.copy(fontSize = 100.sp)
    val measured = textMeasurer.measure(timerString, baseStyle)
    val calculatedFontSize = if (measured.size.width > 0) {
        (100f * (targetTimerWidthPx / measured.size.width)).sp
    } else {
        MaterialTheme.typography.displayLarge.fontSize
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { CenterAlignedTopAppBar(title = { Text(run.routineTitle, style = MaterialTheme.typography.headlineMedium) }) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.medium),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        text = current.source.title,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.medium),
                    )

                    if (timerString.isNotEmpty()) {
                        Text(
                            text = timerString,
                            fontSize = calculatedFontSize,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = calculatedFontSize),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    Button(
                        onClick = viewModel::complete,
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    ) {
                        Text(
                            stringResource(R.string.complete),
                            style = MaterialTheme.typography.headlineLarge,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val stepIconSize = 64.dp
                        val stepBorderWidth = 4.dp
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
                                    .size(76.dp)
                                    .then(
                                        if (isCurrent) {
                                            Modifier
                                                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                when (step.status) {
                                    RunStepStatus.PENDING -> {
                                        Box(
                                            modifier = Modifier
                                                .size(stepIconSize)
                                                .border(stepBorderWidth, MaterialTheme.colorScheme.outline, CircleShape)
                                                .semantics { contentDescription = stepDescription },
                                        )
                                    }
                                    RunStepStatus.COMPLETED -> {
                                        Box(
                                            modifier = Modifier
                                                .size(stepIconSize)
                                                .background(colorResource(R.color.semantic_completed), CircleShape)
                                                .semantics { contentDescription = stepDescription },
                                        )
                                    }
                                    RunStepStatus.SKIPPED -> {
                                        Box(
                                            modifier = Modifier
                                                .size(stepIconSize)
                                                .background(colorResource(R.color.semantic_warning), CircleShape)
                                                .semantics { contentDescription = stepDescription },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (state.historySaveFailed) {
                    Text(stringResource(R.string.run_history_save_failed))
                    Button(onClick = viewModel::retryHistorySave) { Text(stringResource(R.string.run_retry_history_save)) }
                } else if (run.status == RunStatus.ACTIVE) {
                    val actionButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = viewModel::back,
                            contentPadding = actionButtonPadding,
                        ) {
                            Text(
                                stringResource(R.string.back),
                                style = MaterialTheme.typography.headlineLarge,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        OutlinedButton(
                            onClick = viewModel::skip,
                            contentPadding = actionButtonPadding,
                        ) {
                            Text(
                                stringResource(R.string.skip),
                                style = MaterialTheme.typography.headlineLarge,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }
    }

    if (run.status == RunStatus.ACTIVE && run.finishConfirmationRequested) {
        val unfinished = container.runEngine.unfinishedStepIndexes(run)
        AlertDialog(
            onDismissRequest = viewModel::continueRun,
            title = { Text(stringResource(R.string.confirm_complete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    if (unfinished.isNotEmpty()) Text(stringResource(R.string.unfinished_tasks))
                    unfinished.forEach { index ->
                        TextButton(onClick = { viewModel.selectStep(index) }) { Text(run.steps[index].source.title) }
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmComplete) { Text(stringResource(R.string.confirm_complete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::continueRun) { Text(stringResource(R.string.continue_run)) }
            },
        )
    }
    if (run.status == RunStatus.ACTIVE && run.abortConfirmationRequested) {
        AlertDialog(
            onDismissRequest = viewModel::continueRun,
            title = { Text(stringResource(R.string.abort_title)) },
            confirmButton = { Button(onClick = viewModel::abort) { Text(stringResource(R.string.abort_run)) } },
            dismissButton = {
                TextButton(onClick = viewModel::continueRun) { Text(stringResource(R.string.continue_run)) }
            },
        )
    }
}

/** Use this function to format the runner timer digits for countdown, overtime, elapsed, and untimed displays. */
@Composable
private fun formatRunnerTimer(remainingMillis: Long?, actualDurationMillis: Long?, continuePastZero: Boolean): String {
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
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
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
    }
}

/** Use this function when navigating to a new or existing builder route. */
private fun builderRoute(routineId: RoutineId?): String = routineId?.let { "builder?routineId=${it.value}" } ?: "builder"

/** Use this function when starting a routine by stable identity. */
private fun runnerRoute(routineId: RoutineId): String = "runner/${routineId.value}"

private const val HOME_ROUTE = "home"
private const val ROUTINE_ID_ARGUMENT = "routineId"
private const val BUILDER_ROUTE = "builder?routineId={$ROUTINE_ID_ARGUMENT}"
private const val RUNNER_ROUTE = "runner/{$ROUTINE_ID_ARGUMENT}"
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val DRAG_REORDER_THRESHOLD_PX = 48f
