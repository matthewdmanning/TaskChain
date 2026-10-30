package com.taskchain.features.notifications.domain

import com.taskchain.core.model.RoutineRun
import com.taskchain.features.notifications.NotificationRunRules
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineSubtaskId
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus

/** Time display for the ongoing run notification; anchors are epoch milliseconds the platform clock can tick from. */
sealed interface RunNotificationClock {
    /** No time applies, for example a revisited finished task or an open confirmation. */
    data object Hidden : RunNotificationClock

    /** The displayed task is paused; [remainingMillis] is the frozen allowance, or null when untimed. */
    data class Paused(val remainingMillis: Long?) : RunNotificationClock

    /** A timed step that reaches zero at [endAtEpochMillis]. */
    data class CountDown(val endAtEpochMillis: Long) : RunNotificationClock

    /** An untimed step that started at [sinceEpochMillis]. */
    data class CountUp(val sinceEpochMillis: Long) : RunNotificationClock
}

/** The step a notification action was built for, used to ignore a tap that arrives after the step changed. */
data class RunNotificationTarget(
    val runId: RoutineRunId,
    val taskIndex: Int,
    val subtaskId: RoutineSubtaskId?,
)

/**
 * Platform-neutral content of the ongoing run notification.
 * [title] is the active subtask name, or the main task name when there is no active subtask.
 * [taskTitle] is the main task name and is set only while a subtask is the title.
 */
data class RunNotificationContent(
    val target: RunNotificationTarget,
    val title: String,
    val taskTitle: String?,
    val routineTitle: String,
    val clock: RunNotificationClock,
    val skipNeedsApp: Boolean,
    val completeNeedsApp: Boolean,
)

/** The two run transitions the notification can apply without opening the app. */
enum class RunNotificationAction { SKIP, COMPLETE }

/**
 * Derives the run notification and applies its actions through injected [NotificationRunRules].
 * Used by notification adapters and root-injected runner integration; it holds no Android state.
 */
object RunNotificationPolicy {
    /**
     * Use this function to build notification content for the displayed step.
     * Inputs: `run` — the persisted run; `engine` — the run rules; `nowEpochMillis` — the wall-clock sample.
     * Returns null when the run is not active, so the caller removes the notification.
     * The clock anchors do not change while the step keeps running, so equal content means no repost.
     */
    fun content(run: RoutineRun, engine: NotificationRunRules, nowEpochMillis: Long): RunNotificationContent? {
        if (run.status != RunStatus.ACTIVE) return null
        val task = run.tasks.getOrNull(run.currentTaskIndex) ?: return null
        val subtasks = task.source.subtasks
        val subtask = subtasks.firstOrNull { it.id == task.activeSubtaskId }
        val inConfirmation = run.finishConfirmationRequested || run.abortConfirmationRequested

        val remaining = if (subtask != null) {
            engine.subtaskRemainingMillis(run, nowEpochMillis)
        } else {
            engine.remainingMillis(run, nowEpochMillis)
        }
        val startedAt = task.startedAtEpochMillis
        val clock = when {
            inConfirmation || task.status != RunTaskStatus.PENDING -> RunNotificationClock.Hidden
            task.pausedAtEpochMillis != null -> RunNotificationClock.Paused(remaining)
            startedAt == null -> RunNotificationClock.Hidden
            // Start plus elapsed plus remaining is constant while running and also covers a start delayed into the future.
            remaining != null -> RunNotificationClock.CountDown(
                startedAt + engine.elapsedMillis(run, nowEpochMillis) + remaining,
            )
            else -> RunNotificationClock.CountUp(startedAt)
        }

        val isLastTask = run.currentTaskIndex == run.tasks.lastIndex
        val isFinalSubtask = subtask == null || subtasks.last().id == subtask.id
        return RunNotificationContent(
            target = RunNotificationTarget(run.id, run.currentTaskIndex, subtask?.id),
            title = subtask?.title ?: task.source.title,
            taskTitle = task.source.title.takeIf { subtask != null },
            routineTitle = run.routineTitle,
            clock = clock,
            // Ending the last task completes the run or opens the finish dialog, which only the app can present.
            skipNeedsApp = inConfirmation || isLastTask,
            completeNeedsApp = inConfirmation || (isLastTask && isFinalSubtask),
        )
    }

    /**
     * Use this function to apply a notification action to the persisted run.
     * Inputs: `run` — the persisted run; `action` — the pressed button; `target` — the step the button was built for.
     * Returns the updated active run, or null when the action must be ignored because the run or step changed,
     * or because the transition needs the app.
     */
    fun applyAction(
        run: RoutineRun,
        engine: NotificationRunRules,
        action: RunNotificationAction,
        target: RunNotificationTarget,
        nowEpochMillis: Long,
    ): RoutineRun? {
        val content = content(run, engine, nowEpochMillis) ?: return null
        if (content.target != target) return null
        return when (action) {
            RunNotificationAction.SKIP ->
                if (content.skipNeedsApp) null else engine.skipCurrent(run, nowEpochMillis)
            RunNotificationAction.COMPLETE ->
                if (content.completeNeedsApp) null else engine.completeCurrent(run, nowEpochMillis)
        }
    }
}
