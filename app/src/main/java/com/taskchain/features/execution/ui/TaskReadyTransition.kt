package com.taskchain.features.execution.ui

import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus
import com.taskchain.core.designsystem.RunnerMotion

/** Represents the visible label during the five-second transition into the next routine task. */
internal enum class TaskReadyPhase(val label: String) {
    GET_READY("Get Ready"),
    THREE("3"),
    TWO("2"),
    ONE("1"),
}

/** Use this function to tell the numbered countdown steps (3, 2, 1), which pulse, from the Get Ready lead-in. */
internal fun TaskReadyPhase.isCountdownNumber(): Boolean = this != TaskReadyPhase.GET_READY

/** Use this function to map elapsed transition time to the label shown before a task starts.
 * Inputs: `elapsedMillis` — elapsed time since the next task became current.
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

/** Use this function to identify a completed-task transition that has a pending next task to prepare.
 * Inputs: `previous` — the displayed run before completion; `next` — the persisted run after completion.
 * Dependencies: `shouldHoldCompletedPresentation`, `RunStatus`, and `RunTaskStatus`.
 */
internal fun shouldHoldTaskReadyTransition(previous: RoutineRun?, next: RoutineRun?): Boolean {
    if (!shouldHoldCompletedPresentation(previous, next)) return false
    if (next!!.status != RunStatus.ACTIVE || next.currentTaskIndex <= previous!!.currentTaskIndex) return false
    return next.tasks.getOrNull(next.currentTaskIndex)?.status == RunTaskStatus.PENDING
}
