package com.taskchain

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.taskchain.core.platform.PlatformIntents
import com.taskchain.features.running.domain.RoutineRunEngine
import com.taskchain.features.running.domain.RunFeedbackPolicy
import com.taskchain.features.notifications.domain.NotificationNudge
import com.taskchain.features.notifications.domain.NotificationRunRules
import com.taskchain.features.notifications.platform.ReminderNotification
import com.taskchain.features.reminders.AndroidReminderScheduler
import com.taskchain.features.reminders.ReminderActions

/** Binds notification capabilities to the single Running engine without feature-to-feature imports. */
internal fun notificationRunRules(engine: RoutineRunEngine) = NotificationRunRules(
    remainingMillis = engine::remainingMillis,
    subtaskRemainingMillis = engine::subtaskRemainingMillis,
    elapsedMillis = engine::elapsedMillis,
    plannedRunMillis = engine::plannedRunMillis,
    skipCurrent = engine::skipCurrent,
    completeCurrent = engine::completeCurrent,
    needsTimerFeedback = engine::needsTimerFeedback,
    acknowledgeTimerFeedback = engine::acknowledgeTimerFeedback,
    evaluateNudge = { run, now, interval, emit ->
        val decision = RunFeedbackPolicy.evaluateNudge(run, now, interval, emit)
        NotificationNudge(decision.run, decision.shouldFire)
    },
    nextNudgeAt = { run, interval -> RunFeedbackPolicy.nextNudgeAt(run, interval) },
)

/** Maps Reminder delivery protocol to Notifications presentation while preserving pending-intent identities. */
internal fun showReminderNotification(
    context: Context,
    intents: PlatformIntents,
    notification: ReminderNotification,
    delivered: Intent,
): Boolean {
    val requestCode = delivered.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0)
    fun action(action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intents.reminderAction().setAction(action).putExtras(delivered),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return notification.show(
        title = delivered.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE),
        requestCode = requestCode,
        openApp = PendingIntent.getActivity(
            context, 0, intents.openApp(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
        snooze = action(ReminderActions.SNOOZE),
        dismiss = action(ReminderActions.DISMISS),
    )
}
