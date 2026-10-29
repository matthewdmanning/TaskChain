@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.features.authoring.ui
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.NumberPicker
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import java.util.Calendar
import java.util.Date
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.taskchain.core.ui.navigationBackPreview
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.core.designsystem.Spacing
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.components.CyberCard
import com.taskchain.core.designsystem.TaskCard
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
import androidx.compose.ui.unit.Dp
import com.example.cyberpunkandroid.theme.CyberTheme
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.first
import com.taskchain.core.designsystem.Button
import com.taskchain.core.designsystem.CyberButton
import com.taskchain.core.ui.LabeledSwitchRow
import com.taskchain.core.designsystem.OutlinedButton
import com.taskchain.core.designsystem.TextButton
import com.taskchain.core.ui.SECONDS_PER_MINUTE
import com.taskchain.core.ui.SoundPickerRow
import com.taskchain.core.browsing.routineDurationParts

@Composable
internal fun RoutineBuilderRoute(
    container: AppContainer,
    routineId: RoutineId?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    animationsEnabled: Boolean,
) {
    val viewModel: RoutineBuilderViewModel = viewModel(key = "builder-${routineId?.value}") {
        RoutineBuilderViewModel(container, routineId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    val context = LocalContext.current
    val addTaskDescription = stringResource(R.string.add_task)
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.savedRoutineId) { if (state.savedRoutineId != null) onSaved() }
    // After a failed save, scroll to the section that the ViewModel chose to reveal.
    LaunchedEffect(state.failedSaveCount) {
        val targetItem = when (val target = state.revealTarget) {
            null -> return@LaunchedEffect
            BuilderRevealTarget.RoutineName -> BUILDER_ROUTINE_ITEM
            BuilderRevealTarget.RoutineSettings -> BUILDER_SETTINGS_ITEM
            BuilderRevealTarget.TaskHeader -> BUILDER_TASK_HEADER_ITEM
            is BuilderRevealTarget.Task -> BUILDER_FIRST_TASK_ITEM + target.index
        }
        listState.animateScrollToItem(targetItem)
    }
    val requestClose = {
        if (state.hasUnsavedChanges) showDiscardDialog = true else onClose()
    }
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(state = backState, onBackCompleted = { requestClose() })
    Scaffold(
        modifier = Modifier.navigationBackPreview(backState.transitionState, enabled = animationsEnabled && !showDiscardDialog),
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
        state = listState,
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
            TaskCard(modifier = Modifier.fillMaxWidth()) {
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
                            modifier = Modifier.fieldError(
                                stringResource(R.string.validation_routine_name_required).takeIf { isError },
                            ),
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
            TaskCard(modifier = Modifier.fillMaxWidth()) {
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
                stringResource(R.string.authoring_tasks),
                style = CyberTheme.typography.display,
                color = CyberTheme.colors.textPrimary,
            )
            if (BuilderValidationError.TASK_REQUIRED in state.validationErrors) {
                BuilderErrorText(stringResource(R.string.validation_task_required))
            }
        }
        itemsIndexed(state.tasks, key = { _, task -> task.id.value }) { index, task ->
            var dragOffset by remember(task.id) { mutableStateOf(0f) }
            var isDragging by remember(task.id) { mutableStateOf(false) }
            val expanded = state.expandedTaskId == task.id
            val taskErrors = state.taskErrorsFor(index)
            val dragScale by animateFloatAsState(
                if (isDragging) 1.03f else 1f,
                animationSpec = tween(DRAG_TRANSITION_MILLIS, easing = FastOutSlowInEasing),
                label = "TaskDragScale",
            )
            val dragElevation by animateDpAsState(
                if (isDragging) spacing.small else 0.dp,
                animationSpec = tween(DRAG_TRANSITION_MILLIS, easing = FastOutSlowInEasing),
                label = "TaskDragElevation",
            )
            val moveUp = stringResource(R.string.authoring_move_up)
            val moveDown = stringResource(R.string.authoring_move_down)
            val changeExpansion: (Boolean) -> Unit = { shouldExpand ->
                if (shouldExpand) viewModel.expandTask(task.id) else viewModel.collapseTask(task.id)
            }
            com.example.cyberpunkandroid.components.CyberAccordion(
                title = task.title.ifBlank { stringResource(R.string.task_name) },
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
                            if (index > 0) add(CustomAccessibilityAction(moveUp) { viewModel.moveTask(task.id, -1); true })
                            if (index < state.tasks.lastIndex) add(CustomAccessibilityAction(moveDown) { viewModel.moveTask(task.id, 1); true })
                        }
                    },
                borderColor = when {
                    isDragging -> CyberTheme.colors.primary
                    taskErrors.isNotEmpty() -> CyberTheme.semantics.colors.danger
                    else -> CyberTheme.colors.border
                },
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
                            .pointerInput(task.id.value) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = { dragOffset = 0f; isDragging = false },
                                    onDragCancel = { dragOffset = 0f; isDragging = false },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        if (dragOffset.absoluteValue >= TaskChainDesignSystem.reorderDragDistance.toPx()) {
                                            viewModel.moveTask(task.id, if (dragOffset > 0) 1 else -1)
                                            dragOffset = 0f
                                        }
                                    },
                                )
                            },
                    )
                    Box(Modifier.weight(1f)) {
                    if (isExpanded && state.editingNameTaskId == task.id) {
                        val nameError = stringResource(R.string.validation_task_name_required)
                            .takeIf { BuilderValidationError.TASK_NAME_REQUIRED in state.taskErrorsFor(index) }
                        com.example.cyberpunkandroid.components.CyberTextField(
                            value = state.pendingTaskTitle,
                            placeholder = stringResource(R.string.task_name_required_placeholder),
                            isError = nameError != null,
                            modifier = Modifier.fieldError(nameError),
                            onValueChange = viewModel::setPendingTaskTitle,
                        )
                    } else {
                        Text(
                            text = task.title.ifBlank { stringResource(R.string.task_name) },
                            style = CyberTheme.typography.body,
                            color = if (task.title.isBlank()) CyberTheme.colors.textSecondary else CyberTheme.colors.textPrimary,
                            modifier = if (isExpanded) {
                                Modifier.wrapContentWidth().clickable { viewModel.startTaskNameEdit(task.id) }
                            } else {
                                Modifier
                            },
                        )
                    }
                    }
                    if (!isExpanded && taskErrors.isNotEmpty()) {
                        BuilderErrorText(stringResource(R.string.validation_task_has_errors))
                    } else if (!isExpanded) task.durationSeconds?.let { seconds ->
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
                    if (BuilderValidationError.TASK_NAME_REQUIRED in state.validationErrors && state.editingTaskIndex == index) {
                        BuilderErrorText(stringResource(R.string.validation_task_name_required))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val seconds = task.durationSeconds ?: 0L
                        val duration = routineDurationParts(seconds)
                        CyberButton(
                            modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                            enabled = task.subtasks.isEmpty(),
                            onClick = {
                                val pickerSeconds = task.timerSeconds ?: DEFAULT_TASK_TIMER_SECONDS
                                val currentMinutes = (pickerSeconds / SECONDS_PER_MINUTE)
                                    .coerceIn(0L, Int.MAX_VALUE.toLong())
                                    .toInt()
                                val minutesPicker = NumberPicker(context).apply {
                                    minValue = 0
                                    maxValue = maxOf(TASK_TIMER_MAX_MINUTES, currentMinutes)
                                    value = currentMinutes
                                }
                                val secondsPicker = NumberPicker(context).apply {
                                    minValue = 0
                                    maxValue = (SECONDS_PER_MINUTE - 1).toInt()
                                    value = (pickerSeconds % SECONDS_PER_MINUTE).toInt()
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
                                        viewModel.setTaskTimer(task.id, taskTimerFromPicker(minutesPicker.value, secondsPicker.value))
                                    }
                                    .show()
                            },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Text(stringResource(R.string.authoring_duration_value, duration.first, duration.second))
                        }
                    }
                    SoundPickerRow(
                        label = stringResource(R.string.authoring_task_sound),
                        uri = task.soundUri,
                        onPicked = { viewModel.setTaskSound(task.id, it) },
                    )
                    BuilderSubtaskEditor(
                        subtasks = task.subtasks,
                        durationText = { subtask -> state.pendingSubtaskDurations["${task.id.value}:${subtask.id.value}"]
                            ?: subtask.durationSeconds.toString() },
                        durationErrorIds = state.invalidSubtaskDurationIds,
                        showTitleErrors = BuilderValidationError.SUBTASK_TITLE_REQUIRED in state.validationErrors,
                        onTitleChange = { subtaskId, title -> viewModel.setSubtaskTitle(task.id, subtaskId, title) },
                        onDurationChange = { subtaskId, value -> viewModel.setSubtaskDuration(task.id, subtaskId, value) },
                        onAddSubtask = { viewModel.addSubtask(task.id) },
                        onRemoveSubtask = { viewModel.removeSubtask(task.id, it) },
                        onMoveSubtask = { subtaskId, offset -> viewModel.moveSubtask(task.id, subtaskId, offset) },
                    )
                    val shownSubtaskErrors = state.subtaskErrorsFor(task) intersect state.validationErrors
                    if (BuilderValidationError.SUBTASK_TOTAL_TOO_LONG in shownSubtaskErrors) {
                        BuilderErrorText(stringResource(R.string.subtask_total_too_long))
                    }
                    if (BuilderValidationError.SUBTASK_ID_DUPLICATED in shownSubtaskErrors) {
                        BuilderErrorText(stringResource(R.string.subtask_id_duplicated))
                    }
                    CyberButton(
                        modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                        onClick = { viewModel.removeTask(task.id) },
                        style = CyberButtonStyle.Outline,
                        size = CyberButtonSize.Small,
                    ) {
                        Text(stringResource(R.string.remove_task))
                    }
                }
            }
        }
        item {
            CyberButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = CyberPrimitives.IconSizes.dp48),
                onClick = { viewModel.addTask("") },
                style = CyberButtonStyle.Outline,
                size = CyberButtonSize.Large,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CyberIcon(iconRes = CyberIcons.Plus, contentDescription = null)
                    Text(addTaskDescription)
                }
            }
        }
        item {
            val missingHighlighted = state.validationErrors.any { it !in SAVE_AREA_ERRORS }
            if (missingHighlighted) {
                BuilderErrorText(stringResource(R.string.validation_fix_fields))
            }
            if (BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE in state.validationErrors) {
                BuilderErrorText(stringResource(R.string.validation_routine_setting_exclusive))
            }
            if (BuilderValidationError.LEGACY_SETTINGS_CONFLICT in state.validationErrors) {
                BuilderErrorText(stringResource(R.string.validation_legacy_settings_conflict))
            }
            if (BuilderValidationError.SAVE_FAILED in state.validationErrors) {
                BuilderErrorText(stringResource(R.string.validation_save_failed))
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
                    BuilderErrorText(stringResource(R.string.validation_selected_day_required))
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

// Builder LazyColumn item positions, used to scroll to the first error after a failed save.
private const val BUILDER_ROUTINE_ITEM = 0
private const val BUILDER_SETTINGS_ITEM = 1
private const val BUILDER_TASK_HEADER_ITEM = 2
private const val BUILDER_FIRST_TASK_ITEM = 3

/** Errors that have their own message next to Save instead of a highlighted field. */
private val SAVE_AREA_ERRORS = setOf(
    BuilderValidationError.SCHEDULE_REMINDER_EXCLUSIVE,
    BuilderValidationError.LEGACY_SETTINGS_CONFLICT,
    BuilderValidationError.SAVE_FAILED,
)
private const val DRAG_TRANSITION_MILLIS = 250

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
