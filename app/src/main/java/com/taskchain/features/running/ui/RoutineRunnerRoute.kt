@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.features.running.ui
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.taskchain.core.ui.navigationBackPreview
import com.taskchain.features.keepscreenon.ui.RunKeepScreenOnEffect
import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import com.example.cyberpunkandroid.effects.cyberOverload
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskchain.R
import com.taskchain.core.model.MILLIS_PER_SECOND
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.core.model.UserPreferences
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.core.designsystem.RunnerMotion
import com.taskchain.core.designsystem.Spacing
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.icons.SemanticIcons
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.key
import kotlin.math.absoluteValue
import com.taskchain.core.designsystem.Button
import com.taskchain.core.designsystem.CyberButton
import com.taskchain.core.designsystem.TextButton
import com.taskchain.core.ui.SECONDS_PER_MINUTE
import com.taskchain.features.running.RunningDependencies

@Composable
internal fun RoutineRunnerRoute(
    dependencies: RunningDependencies,
    preferences: UserPreferences,
    routineId: RoutineId,
    onFinished: () -> Unit,
    runEffects: @Composable (RoutineRun?) -> Unit = {},
) {
    val viewModel: RoutineRunnerViewModel = viewModel(key = "runner-${routineId.value}") {
        RoutineRunnerViewModel(dependencies, routineId)
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
    val animationsEnabled = preferences.screenTransitionsEnabled && runnerAnimationsEnabled(LocalContext.current)
    val presentation = rememberRunnerPresentation(
        run = state.run,
        animationsEnabled = animationsEnabled,
        foreground = foreground,
    )
    val readyPhase = TaskReadyPhase.entries.firstOrNull { it.label == presentation.readyLabel }
    LaunchedEffect(readyPhase) {
        if (readyPhase?.isCountdownNumber() == true) viewModel.countdownPulse()
    }
    LaunchedEffect(state.finished, presentation.holdingCompletion) {
        if (state.finished && !presentation.holdingCompletion) onFinished()
    }
    LaunchedEffect(animationsEnabled) { viewModel.setTransitionsEnabled(animationsEnabled) }
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val run = presentation.run
    val timing = viewModel.timingFor(run, state.nowEpochMillis)
    runEffects(run)
    RunKeepScreenOnEffect(run, preferences.keepScreenOnDuringRun)
    NavigationBackHandler(
        state = backState,
        isBackEnabled = run?.status == RunStatus.ACTIVE || presentation.holdingCompletion,
        onBackCompleted = { if (!presentation.holdingCompletion) viewModel.systemBack() },
    )
    RoutineRunnerScreen(
        modifier = Modifier.navigationBackPreview(
            backState.transitionState,
            enabled = animationsEnabled && !presentation.holdingCompletion &&
                run?.abortConfirmationRequested == false && !run.finishConfirmationRequested,
        ),
        state = state,
        preferences = preferences,
        presentation = presentation,
        foreground = foreground,
        animationsEnabled = animationsEnabled,
        remaining = timing.remainingMillis,
        subtaskRemaining = timing.subtaskRemainingMillis,
        subtaskElapsed = timing.subtaskElapsedMillis,
        behindSubtaskSchedule = timing.behindSubtaskSchedule,
        unfinishedTaskIndexes = timing.unfinishedTaskIndexes,
        onComplete = { viewModel.complete() },
        onSkip = { viewModel.skip() },
        onBack = { viewModel.back() },
        onPrevious = { viewModel.goToPreviousTask() },
        onPause = { viewModel.pause() },
        onResume = { viewModel.resume() },
        onContinue = { viewModel.continueRun() },
        onSelectTask = { viewModel.selectTask(it) },
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
    modifier: Modifier,
    state: RunnerState,
    preferences: UserPreferences,
    presentation: RunnerPresentation,
    foreground: Boolean,
    animationsEnabled: Boolean,
    remaining: Long?,
    subtaskRemaining: Long?,
    subtaskElapsed: List<Long>,
    behindSubtaskSchedule: Boolean,
    unfinishedTaskIndexes: List<Int>,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onContinue: () -> Unit,
    onSelectTask: (Int) -> Unit,
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

    val spacing = TaskChainDesignSystem.spacing()
    val current = run.tasks[run.currentTaskIndex]
    val configuration = LocalConfiguration.current
    val smallerDimensionDp = minOf(configuration.screenWidthDp, configuration.screenHeightDp).dp
    val controlMinWidth = TaskChainDesignSystem.Sizes.actionMinWidth * androidx.compose.ui.platform.LocalDensity.current.fontScale
    val dialDiameter = (smallerDimensionDp - TaskChainDesignSystem.Dial.screenInset * 2)
        .coerceAtLeast(TaskChainDesignSystem.Dial.minDiameter)
        .coerceAtMost(RunnerMotion.dialDiameter)
    val entrance = remember(run.currentTaskIndex) { Animatable(0f) }
    LaunchedEffect(run.currentTaskIndex, animationsEnabled) {
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
    val transitionColor = TaskChainDesignSystem.Colors.warning
    val gestureState = rememberRunnerGestureState()
    // A paused or skipped task offers Resume; a running one offers Pause.
    val offersResume = current.pausedAtEpochMillis != null || current.status == RunTaskStatus.SKIPPED
    val gestureActions = Modifier.runnerGestureActions(
        paused = offersResume,
        onPrevious = { if (!presentation.holdingCompletion) onPrevious() },
        onSkip = { if (!presentation.holdingCompletion) onSkip() },
        onPause = { if (!presentation.holdingCompletion) onPause() },
        onResume = { if (!presentation.holdingCompletion) onResume() },
    )
    val swipeGesture = Modifier.runnerSwipeGesture(
        gestureState,
        onPrevious = { if (!presentation.holdingCompletion) onPrevious() },
        onSkip = { if (!presentation.holdingCompletion) onSkip() },
    )
    val toggleGesture = Modifier.runnerDoubleTapGesture {
        if (!presentation.holdingCompletion) {
            gestureState.flashToggle(showPause = !offersResume)
            if (offersResume) onResume() else onPause()
        }
    }
    Scaffold(
        modifier = modifier.drawWithContent {
            drawContent()
            if (presentation.readyLabel != null) {
                val phase = (presentation.transitionElapsedMillis % 1000L) / 1000f
                drawRect(transitionColor.copy(alpha = 0.15f * (1f - kotlin.math.abs(phase * 2f - 1f))))
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(run.routineTitle, style = TaskChainDesignSystem.TextStyles.display)
            })
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .background(TaskChainDesignSystem.Colors.background)
                .then(gestureActions)
                .then(swipeGesture),
            contentAlignment = Alignment.TopCenter,
        ) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .widthIn(max = dimensionResource(R.dimen.content_max_width))
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .followRunnerSwipe(gestureState)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TaskChainDesignSystem.Space.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.Space.large),
                ) {
                    Text(
                        text = current.source.title,
                        style = TaskChainDesignSystem.TextStyles.hero,
                        color = if (presentation.readyLabel != null) TaskChainDesignSystem.Colors.warning else TaskChainDesignSystem.Colors.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberOverload(enabled = presentation.readyLabel != null &&
                                presentation.transitionElapsedMillis % 1000L < 300L, animationSpec = tween(300))
                            .offset(y = RunnerMotion.titleStartOffset +
                                (RunnerMotion.titleEndOffset - RunnerMotion.titleStartOffset) * entrance.value),
                    )

                    RunnerSubtaskList(current, subtaskRemaining, preferences.showSubtaskTimeRemaining)

                    key(run.currentTaskIndex, presentation.holdingCompletion) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer { alpha = entrance.value }
                                .then(toggleGesture),
                        ) {
                            RunnerSubtaskDial(current, subtaskElapsed, dialDiameter, behindSubtaskSchedule) { innerDiameter ->
                            RunCountdownDial(
                                timer = if (presentation.readyLabel == "Get Ready") stringResource(R.string.punch_get_ready)
                                    else presentation.readyLabel ?: timerString,
                                progress = countdownProgress(current.source.durationSeconds, remaining),
                                remainingMillis = remaining,
                                diameter = innerDiameter,
                                behindSubtaskSchedule = behindSubtaskSchedule,
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = TaskChainDesignSystem.Sizes.prominentTouchTarget)
                            .clip(completeShape)
                            .background(Brush.verticalGradient(listOf(TaskChainDesignSystem.Colors.success.copy(alpha = 0.7f),
                                TaskChainDesignSystem.Colors.success.copy(alpha = 0.1f))))
                            .border(TaskChainDesignSystem.Strokes.primaryActionBorder, TaskChainDesignSystem.Colors.success, completeShape),
                        enabled = !presentation.holdingCompletion,
                        onClick = { if (!presentation.holdingCompletion) onComplete() },
                        size = CyberButtonSize.Large,
                        style = CyberButtonStyle.Ghost,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = TaskChainDesignSystem.Space.medium, vertical = TaskChainDesignSystem.Space.smallMedium),
                            horizontalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.Space.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CyberIcon(
                                iconRes = SemanticIcons.Success,
                                contentDescription = null,
                                size = TaskChainDesignSystem.Sizes.iconLarge,
                                tint = TaskChainDesignSystem.Colors.success,
                            )
                            Text(
                                stringResource(R.string.complete),
                                style = TaskChainDesignSystem.TextStyles.actionLabel,
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.Space.smallMedium, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.Space.smallMedium),
                    ) {
                        run.tasks.forEachIndexed { index, task ->
                            val isCurrent = index == run.currentTaskIndex
                            val taskDescription = stringResource(
                                when (task.status) {
                                    RunTaskStatus.PENDING -> R.string.runner_task_status_pending
                                    RunTaskStatus.COMPLETED -> R.string.runner_task_status_completed
                                    RunTaskStatus.SKIPPED -> R.string.runner_task_status_skipped
                                },
                                index + 1,
                                task.source.title,
                            )
                            Box(
                                modifier = Modifier
                                    .size(TaskChainDesignSystem.Sizes.touchTarget)
                                    .border(
                                        if (isCurrent) TaskChainDesignSystem.Strokes.emphasis else TaskChainDesignSystem.Strokes.hairline,
                                        statusColor(task.status),
                                        CircleShape,
                                    )
                                    .background(
                                        statusColor(task.status).copy(alpha = if (isCurrent) TASK_DOT_CURRENT_FILL_ALPHA else TASK_DOT_FILL_ALPHA),
                                        CircleShape,
                                    )
                                    .semantics { contentDescription = taskDescription },
                                contentAlignment = Alignment.Center,
                            ) {
                                when (task.status) {
                                    RunTaskStatus.PENDING -> {
                                        CyberIcon(
                                            iconRes = CyberIcons.Play,
                                            contentDescription = null,
                                            size = if (isCurrent) TaskChainDesignSystem.Sizes.iconMedium else TaskChainDesignSystem.Sizes.iconSmall,
                                            tint = TaskChainDesignSystem.Colors.info,
                                        )
                                    }
                                    RunTaskStatus.COMPLETED -> {
                                        CyberIcon(
                                            iconRes = SemanticIcons.Success,
                                            contentDescription = null,
                                            tint = TaskChainDesignSystem.Colors.success,
                                        )
                                    }
                                    RunTaskStatus.SKIPPED -> {
                                        CyberIcon(
                                            iconRes = SemanticIcons.Caution,
                                            contentDescription = null,
                                            tint = TaskChainDesignSystem.Colors.warning,
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
                            .padding(bottom = TaskChainDesignSystem.Space.medium),
                        horizontalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.Space.small),
                        verticalArrangement = Arrangement.spacedBy(TaskChainDesignSystem.Space.small),
                    ) {
                        CyberButton(
                            modifier = Modifier.weight(1f).widthIn(min = controlMinWidth).heightIn(min = TaskChainDesignSystem.Sizes.touchTarget),
                            enabled = !presentation.holdingCompletion,
                            onClick = { if (!presentation.holdingCompletion) onBack() },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberIcon(iconRes = CyberIcons.ArrowLeft, contentDescription = null, size = TaskChainDesignSystem.Sizes.inlineIcon)
                                Text(stringResource(R.string.back))
                            }
                        }
                        CyberButton(
                            modifier = Modifier.weight(1f).widthIn(min = controlMinWidth).heightIn(min = TaskChainDesignSystem.Sizes.touchTarget),
                            enabled = !presentation.holdingCompletion,
                            onClick = {
                                if (!presentation.holdingCompletion) {
                                    if (offersResume) onResume() else onPause()
                                }
                            },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberIcon(
                                    iconRes = if (offersResume) CyberIcons.Play else CyberIcons.Pause,
                                    contentDescription = null,
                                    size = TaskChainDesignSystem.Sizes.inlineIcon,
                                )
                                Text(stringResource(if (offersResume) R.string.resume else R.string.pause))
                            }
                        }
                        CyberButton(
                            modifier = Modifier.weight(1f).widthIn(min = controlMinWidth).heightIn(min = TaskChainDesignSystem.Sizes.touchTarget),
                            enabled = !presentation.holdingCompletion,
                            onClick = { if (!presentation.holdingCompletion) onSkip() },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberIcon(
                                    iconRes = SemanticIcons.Caution,
                                    contentDescription = null,
                                    size = TaskChainDesignSystem.Sizes.inlineIcon,
                                    tint = TaskChainDesignSystem.Colors.warning,
                                )
                                Text(stringResource(R.string.skip))
                            }
                        }
                    }
                }
            }
        }
            RunnerSwipeCue(gestureState)
            RunnerToggleFlash(gestureState)
        }
    }

    if (run.status == RunStatus.ACTIVE && run.finishConfirmationRequested && !presentation.holdingCompletion) {
        val unfinished = unfinishedTaskIndexes
        AlertDialog(
            onDismissRequest = { if (!presentation.holdingCompletion) onContinue() },
            title = { Text(stringResource(R.string.confirm_complete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    if (unfinished.isNotEmpty()) Text(stringResource(R.string.unfinished_tasks))
                    unfinished.forEach { index ->
                        TextButton(onClick = { if (!presentation.holdingCompletion) onSelectTask(index) }) { Text(run.tasks[index].source.title) }
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
            modifier = Modifier.border(TaskChainDesignSystem.Strokes.alertBorder, TaskChainDesignSystem.Colors.error, TaskChainDesignSystem.Shapes.panel),
            onDismissRequest = { if (!presentation.holdingCompletion) onContinue() },
            title = { Text(stringResource(R.string.abort_title), style = TaskChainDesignSystem.TextStyles.heading) },
            text = { Text(stringResource(R.string.abort_run), style = TaskChainDesignSystem.TextStyles.title) },
            confirmButton = {
                Button(
                    modifier = Modifier.background(Brush.verticalGradient(listOf(
                        TaskChainDesignSystem.Colors.error, TaskChainDesignSystem.Colors.error.copy(alpha = 0.55f))),
                        TaskChainDesignSystem.Shapes.panel),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = TaskChainDesignSystem.Colors.textPrimary),
                    onClick = { if (!presentation.holdingCompletion) onAbort() },
                ) { Text(stringResource(R.string.abort_run), style = TaskChainDesignSystem.TextStyles.title) }
            },
            dismissButton = {
                TextButton(onClick = { if (!presentation.holdingCompletion) onContinue() }) { Text(stringResource(R.string.continue_run)) }
            },
        )
    }
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

/** Use this function to map each runner task state to its semantic status color. */
@Composable
private fun statusColor(status: RunTaskStatus) = when (status) {
    RunTaskStatus.PENDING -> TaskChainDesignSystem.Colors.info
    RunTaskStatus.COMPLETED -> TaskChainDesignSystem.Colors.success
    RunTaskStatus.SKIPPED -> TaskChainDesignSystem.Colors.warning
}

/** Use this function when deciding whether decorative runner motion should honor the system animator setting. */
private fun runnerAnimationsEnabled(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

/** Fill opacity of the current and other task markers. */
private const val TASK_DOT_CURRENT_FILL_ALPHA = 0.16f
private const val TASK_DOT_FILL_ALPHA = 0.06f
