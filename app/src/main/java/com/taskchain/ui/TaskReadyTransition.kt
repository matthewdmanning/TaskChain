package com.taskchain.ui

import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
import com.taskchain.ui.designsystem.RunnerMotion

/** Represents the visible label during the five-second transition into the next routine step. */
internal enum class TaskReadyPhase(val label: String) {
    GET_READY("Get Ready"),
    THREE("3"),
    TWO("2"),
    ONE("1"),
}

/** Use this function to map elapsed transition time to the label shown before a step starts.
 * Inputs: `elapsedMillis` — elapsed time since the next step became current.
 * Dependencies: `RunnerMotion.taskReadyGetReadyDurationMillis`, `RunnerMotion.taskReadyPhaseDurationMillis`, and
 * `RunnerMotion.taskReadyTransitionDurationMillis`.
 */
internal fun taskReadyPhase(elapsedMillis: Long): TaskReadyPhase? = when {
    elapsedMillis < 0L -> TaskReadyPhase.GET_READY
    elapsedMillis < RunnerMotion.taskReadyGetReadyDurationMillis -> TaskReadyPhase.GET_READY
    elapsedMillis < RunnerMotion.taskReadyGetReadyDurationMillis + RunnerMotion.taskReadyPhaseDurationMillis -> TaskReadyPhase.THREE
    elapsedMillis < RunnerMotion.taskReadyGetReadyDurationMillis + RunnerMotion.taskReadyPhaseDurationMillis * 2 -> TaskReadyPhase.TWO
    elapsedMillis < RunnerMotion.taskReadyTransitionDurationMillis -> TaskReadyPhase.ONE
    else -> null
}

/** Use this function to identify a completed-step transition that has a pending next step to prepare.
 * Inputs: `previous` — the displayed run before completion; `next` — the persisted run after completion.
 * Dependencies: `shouldHoldCompletedPresentation`, `RunStatus`, and `RunStepStatus`.
 */
internal fun shouldHoldTaskReadyTransition(previous: RoutineRun?, next: RoutineRun?): Boolean {
    if (!shouldHoldCompletedPresentation(previous, next)) return false
    if (next!!.status != RunStatus.ACTIVE || next.currentStepIndex <= previous!!.currentStepIndex) return false
    return next.steps.getOrNull(next.currentStepIndex)?.status == RunStepStatus.PENDING
}
