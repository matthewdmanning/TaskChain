package com.taskchain.features.notifications.domain

import com.taskchain.core.model.RoutineRun
import com.taskchain.features.notifications.NotificationRunRules
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.RunTaskStatus

/** Holds the run after due alerts were acknowledged and which alerts the caller must now play. */
data class DueRunAlerts(
    val run: RoutineRun,
    val nudge: Boolean,
    val timerExpired: Boolean,
)

/**
 * Decides when the run needs its next audio and haptic alert while the Run screen is not visible,
 * and which alerts are due. Platform-neutral: the Android alarm adapter only maps these times to alarms.
 * Dependencies: [NotificationRunRules], injected nudge timing and acknowledgement rules.
 */
object RunAlertPolicy {
    /**
     * Use this function to decide whether periodic reminders must stop for this run.
     * The limit is the sum of all configured task times, fixed when the run starts. A run with no timer
     * uses `untimedIdleLimitMillis`. It counts from the latest navigation, pause, resume, skip, or complete,
     * or from the run start before any of those. Time remaining or passed in tasks does not change it.
     */
    fun remindersExhausted(
        run: RoutineRun,
        engine: NotificationRunRules,
        nowEpochMillis: Long,
        untimedIdleLimitMillis: Long,
    ): Boolean {
        val limitMillis = engine.plannedRunMillis(run).takeIf { it > 0L } ?: untimedIdleLimitMillis
        val since = run.lastInteractionAtEpochMillis ?: run.startedAtEpochMillis
        return nowEpochMillis - since >= limitMillis
    }

    /**
     * Use this function to find the wall-clock time of the next alert, or null when none can occur
     * (not active, paused, in confirmation, or reminders exhausted and no timer expiry pending).
     * The result is never earlier than `nowEpochMillis`.
     */
    fun nextAlertAt(
        run: RoutineRun,
        engine: NotificationRunRules,
        nowEpochMillis: Long,
        nudgeIntervalMillis: Long,
        untimedIdleLimitMillis: Long,
    ): Long? {
        require(nudgeIntervalMillis > 0L)
        if (run.status != RunStatus.ACTIVE || run.finishConfirmationRequested || run.abortConfirmationRequested) return null
        val task = run.tasks.getOrNull(run.currentTaskIndex) ?: return null
        if (task.status != RunTaskStatus.PENDING || task.pausedAtEpochMillis != null) return null
        if (task.startedAtEpochMillis == null) return null
        val candidates = mutableListOf<Long>()
        if (!remindersExhausted(run, engine, nowEpochMillis, untimedIdleLimitMillis)) {
            // Running owns the nudge bucket rule, so the alarm fires exactly when the bucket comes due.
            engine.nextNudgeAt(run, nudgeIntervalMillis)?.let { candidates += it }
        }
        if (task.timerFeedbackAtEpochMillis == null) {
            engine.remainingMillis(run, nowEpochMillis)?.let { candidates += nowEpochMillis + it }
        }
        return candidates.minOrNull()?.coerceAtLeast(nowEpochMillis)
    }

    /**
     * Use this function when the alert alarm fires, or a screen-off tick runs, to acknowledge what is due.
     * A due nudge bucket is always acknowledged, but only reported when reminders are not exhausted.
     */
    fun takeDue(
        run: RoutineRun,
        engine: NotificationRunRules,
        nowEpochMillis: Long,
        nudgeIntervalMillis: Long,
        untimedIdleLimitMillis: Long,
    ): DueRunAlerts {
        if (run.status != RunStatus.ACTIVE || run.finishConfirmationRequested || run.abortConfirmationRequested) {
            return DueRunAlerts(run, nudge = false, timerExpired = false)
        }
        val task = run.tasks.getOrNull(run.currentTaskIndex)
        if (task == null || task.status != RunTaskStatus.PENDING || task.pausedAtEpochMillis != null) {
            return DueRunAlerts(run, nudge = false, timerExpired = false)
        }
        val exhausted = remindersExhausted(run, engine, nowEpochMillis, untimedIdleLimitMillis)
        val nudge = engine.evaluateNudge(run, nowEpochMillis, nudgeIntervalMillis, !exhausted)
        var updated = nudge.run
        val timerExpired = engine.needsTimerFeedback(updated, nowEpochMillis)
        if (timerExpired) updated = engine.acknowledgeTimerFeedback(updated, nowEpochMillis)
        return DueRunAlerts(updated, nudge = nudge.shouldFire, timerExpired = timerExpired)
    }
}
