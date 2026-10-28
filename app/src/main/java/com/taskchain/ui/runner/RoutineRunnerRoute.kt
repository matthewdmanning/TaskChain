@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.ui.runner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.taskchain.ui.navigationBackPreview
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipPath
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
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.MILLIS_PER_SECOND
import com.taskchain.domain.model.RoutineId
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunTaskStatus
import com.taskchain.domain.model.UserPreferences
import com.taskchain.ui.designsystem.TaskChainDesignSystem
import com.taskchain.ui.designsystem.RunnerMotion
import com.taskchain.ui.designsystem.Spacing
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.effects.cyberRadialPulse
import com.example.cyberpunkandroid.effects.rememberCyberRadialPulse
import com.example.cyberpunkandroid.components.GlowingText
import com.example.cyberpunkandroid.components.CyberDialTicks
import com.example.cyberpunkandroid.icons.CyberIcon
import com.example.cyberpunkandroid.icons.CyberIcons
import com.example.cyberpunkandroid.components.CyberSectorRim
import com.example.cyberpunkandroid.icons.SemanticIcons
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import com.taskchain.ui.Button
import com.taskchain.ui.CyberButton
import com.taskchain.ui.TextButton
import com.taskchain.ui.SECONDS_PER_MINUTE

@Composable
internal fun RoutineRunnerRoute(
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
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val run = presentation.run
    KeepScreenOnEffect(enabled = runKeepsScreenOn(run, preferences.keepScreenOnDuringRun))
    NavigationBackHandler(
        state = backState,
        isBackEnabled = run?.status == RunStatus.ACTIVE || presentation.holdingCompletion,
        onBackCompleted = {
            if (!presentation.holdingCompletion && run != null) {
                if (run.abortConfirmationRequested || run.finishConfirmationRequested) {
                    viewModel.continueRun()
                } else {
                    viewModel.back()
                }
            }
        },
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
        remaining = presentation.run?.let { container.runEngine.remainingMillis(it, state.nowEpochMillis) },
        subtaskRemaining = presentation.run?.let { container.runEngine.subtaskRemainingMillis(it, state.nowEpochMillis) },
        subtaskElapsed = presentation.run?.let { run ->
            run.tasks[run.currentTaskIndex].source.subtasks.map { subtask ->
                container.runEngine.subtaskElapsedMillis(run, subtask.id, state.nowEpochMillis)
            }
        }.orEmpty(),
        behindSubtaskSchedule = presentation.run?.let {
            container.runEngine.isBehindSubtaskSchedule(it, state.nowEpochMillis)
        } ?: false,
        unfinishedTaskIndexes = presentation.run?.let(container.runEngine::unfinishedTaskIndexes).orEmpty(),
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
    val controlMinWidth = RUNNER_CONTROL_MIN_WIDTH * androidx.compose.ui.platform.LocalDensity.current.fontScale
    val dialDiameter = (smallerDimensionDp - CyberPrimitives.Spacing.dp32 * 2)
        .coerceAtLeast(CyberPrimitives.IconSizes.dp48)
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
    val transitionColor = CyberTheme.semantics.colors.warning
    val gestureState = rememberRunnerGestureState()
    val gesturePaused = current.pausedAtEpochMillis != null || current.status == RunTaskStatus.SKIPPED
    val gestureActions = Modifier.runnerGestureActions(
        paused = gesturePaused,
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
            gestureState.flashToggle(showPause = !gesturePaused)
            if (gesturePaused) onResume() else onPause()
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
                Text(run.routineTitle, style = CyberTheme.typography.display)
            })
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .background(CyberTheme.colors.background)
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
                                    .size(CyberPrimitives.IconSizes.dp48)
                                    .border(
                                        if (isCurrent) CyberPrimitives.BorderWidths.dp2 else CyberPrimitives.BorderWidths.dp1,
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
                                            size = if (isCurrent) CyberPrimitives.IconSizes.dp24 else CyberPrimitives.IconSizes.dp16,
                                            tint = CyberTheme.semantics.colors.info,
                                        )
                                    }
                                    RunTaskStatus.COMPLETED -> {
                                        CyberIcon(
                                            iconRes = SemanticIcons.Success,
                                            contentDescription = null,
                                            tint = CyberTheme.semantics.colors.success,
                                        )
                                    }
                                    RunTaskStatus.SKIPPED -> {
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
                                    if (current.pausedAtEpochMillis == null && current.status != RunTaskStatus.SKIPPED) onPause() else onResume()
                                }
                            },
                            style = CyberButtonStyle.Outline,
                            size = CyberButtonSize.Small,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CyberIcon(
                                    iconRes = if (current.pausedAtEpochMillis == null && current.status != RunTaskStatus.SKIPPED) CyberIcons.Pause else CyberIcons.Play,
                                    contentDescription = null,
                                    size = CyberPrimitives.Spacing.dp16,
                                )
                                Text(stringResource(if (current.pausedAtEpochMillis == null && current.status != RunTaskStatus.SKIPPED) R.string.pause else R.string.resume))
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
    behindSubtaskSchedule: Boolean,
    status: RunTaskStatus,
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
        status == RunTaskStatus.COMPLETED -> CyberTheme.semantics.colors.success
        status == RunTaskStatus.SKIPPED -> CyberTheme.semantics.colors.warning
        behindSubtaskSchedule -> CyberTheme.semantics.colors.warning
        isOvertime -> CyberTheme.semantics.colors.error
        isUrgent || isWarning -> CyberTheme.semantics.colors.warning
        else -> CyberTheme.semantics.colors.info
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = if (animationsEnabled) tween(600) else snap(),
        label = "DialColorAnimation",
    )

    val pulseAlpha = if (foreground && animationsEnabled && isUrgent && status == RunTaskStatus.PENDING && !paused) {
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
        status == RunTaskStatus.PENDING && paused -> Modifier.cyberRadialPulse(
            rememberCyberRadialPulse(animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing))),
            color = CyberTheme.semantics.colors.warning,
        )
        // The sweep head follows the same elapsed fraction as the ticks rather than an independent loop.
        status == RunTaskStatus.PENDING -> Modifier.drawWithContent {
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
            .background(if (behindSubtaskSchedule) CyberTheme.semantics.colors.warning.copy(alpha = BEHIND_SCHEDULE_FILL_ALPHA)
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
                    status == RunTaskStatus.COMPLETED -> SemanticIcons.Success
                    status == RunTaskStatus.SKIPPED -> SemanticIcons.Caution
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

/** Use this function to map each runner task state to the library's semantic status color. */
@Composable
private fun statusColor(status: RunTaskStatus) = when (status) {
    RunTaskStatus.PENDING -> CyberTheme.semantics.colors.info
    RunTaskStatus.COMPLETED -> CyberTheme.semantics.colors.success
    RunTaskStatus.SKIPPED -> CyberTheme.semantics.colors.warning
}

/** Use this function when deciding whether decorative runner motion should honor the system animator setting. */
private fun runnerAnimationsEnabled(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

/** Smallest runner control width at default font scale; it grows with the font scale. */
private val RUNNER_CONTROL_MIN_WIDTH = 112.dp
/** Fill opacity of the current and other task markers. */
private const val TASK_DOT_CURRENT_FILL_ALPHA = 0.16f
private const val TASK_DOT_FILL_ALPHA = 0.06f
/** Warning tint behind the dial when the subtask schedule runs late. */
private const val BEHIND_SCHEDULE_FILL_ALPHA = 0.3f
