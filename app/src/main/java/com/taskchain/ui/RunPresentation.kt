package com.taskchain.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStepStatus
import com.taskchain.ui.designsystem.RunnerMotion
import kotlinx.coroutines.delay

/** Represents the run snapshot and completion gate currently shown by the runner route. */
internal data class RunnerPresentation(
    val run: RoutineRun?,
    val holdingCompletion: Boolean,
)

/**
 * Use this function when the VM advances a completed step so the completed snapshot stays visible during its effect.
 *
 * @param run Latest persisted run state from the runner ViewModel.
 * @param animationsEnabled Whether system animator settings permit the visual hold.
 * @param foreground Whether the runner route is currently resumed.
 * @return The snapshot to render and whether completion actions must remain gated.
 */
@Composable
internal fun rememberRunnerPresentation(
    run: RoutineRun?,
    animationsEnabled: Boolean,
    foreground: Boolean,
): RunnerPresentation {
    var presentedRun by remember { mutableStateOf(run) }
    var holdingCompletion by remember { mutableStateOf(false) }
    var completionSnapshot by remember { mutableStateOf<RoutineRun?>(null) }
    val latestRun by rememberUpdatedState(run)
    val completionKey = completionPresentationKey(presentedRun, run)
    val synchronousSnapshot = completedSnapshot(presentedRun, run)
    val canHold = animationsEnabled && foreground && presentedRun?.id == run?.id

    SideEffect {
        if (!holdingCompletion && completionKey == null) presentedRun = run
    }

    LaunchedEffect(completionKey, animationsEnabled, foreground) {
        if (completionKey != null && animationsEnabled && foreground && synchronousSnapshot != null) {
            completionSnapshot = synchronousSnapshot
            holdingCompletion = true
            delay(RunnerMotion.completionDurationMillis.toLong())
            presentedRun = latestRun
        } else if (run != null) {
            presentedRun = run
        }
        completionSnapshot = null
        holdingCompletion = false
    }

    return RunnerPresentation(
        run = when {
            holdingCompletion && canHold -> completionSnapshot ?: synchronousSnapshot ?: presentedRun
            completionKey != null && animationsEnabled && foreground -> synchronousSnapshot ?: presentedRun
            else -> run
        },
        holdingCompletion = canHold && (holdingCompletion || completionKey != null),
    )
}

/**
 * Use this function to identify an active-run transition that completed the displayed step.
 *
 * @param previous Snapshot currently shown by the route.
 * @param next Latest snapshot after the domain transition.
 * @return True only when the same run completed the previous current step.
 */
internal fun shouldHoldCompletedPresentation(previous: RoutineRun?, next: RoutineRun?): Boolean {
    if (previous == null || next == null || previous.id != next.id || previous.routineId != next.routineId) return false
    val previousStep = previous.steps.getOrNull(previous.currentStepIndex) ?: return false
    val nextStep = next.steps.getOrNull(previous.currentStepIndex) ?: return false
    return previousStep.status != RunStepStatus.COMPLETED && nextStep.status == RunStepStatus.COMPLETED
}

/** Use this function to derive the completed-step identity that owns one visual hold from two run snapshots. */
private fun completionPresentationKey(previous: RoutineRun?, next: RoutineRun?): String? {
    if (!shouldHoldCompletedPresentation(previous, next)) return null
    val step = next!!.steps[previous!!.currentStepIndex]
    return "${next.id.value}:${previous.currentStepIndex}:${step.finishedAtEpochMillis}"
}

/** Use this function to display the just-completed step while the next step remains persisted and ready. */
private fun completedSnapshot(previous: RoutineRun?, next: RoutineRun?): RoutineRun? {
    if (!shouldHoldCompletedPresentation(previous, next)) return null
    return next!!.copy(currentStepIndex = previous!!.currentStepIndex)
}
