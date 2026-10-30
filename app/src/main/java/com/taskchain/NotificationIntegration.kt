package com.taskchain

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.taskchain.core.platform.PlatformIntents
import com.taskchain.features.notifications.NotificationDependencies
import com.taskchain.features.notifications.NotificationNudge
import com.taskchain.features.notifications.NotificationRunRules
import com.taskchain.features.reminders.AndroidReminderScheduler
import com.taskchain.features.reminders.ReminderActions
import com.taskchain.features.running.RunEngine
import com.taskchain.features.running.evaluateRunNudge
import com.taskchain.features.running.nextRunNudgeAt

/**
 * Binds notification capabilities to the single Running engine and nudge rules without feature-to-feature imports.
 * Covered by the root system test `NotificationIntegrationTest`.
 */
internal fun notificationRunRules(engine: RunEngine) = NotificationRunRules(
    remainingMillis = engine::remainingMillis,
    subtaskRemainingMillis = engine::subtaskRemainingMillis,
    elapsedMillis = engine::elapsedMillis,
    plannedRunMillis = engine::plannedRunMillis,
    skipCurrent = engine::skipCurrent,
    completeCurrent = engine::completeCurrent,
    needsTimerFeedback = engine::needsTimerFeedback,
    acknowledgeTimerFeedback = engine::acknowledgeTimerFeedback,
    evaluateNudge = { run, now, interval, emit ->
        val decision = evaluateRunNudge(run, now, interval, emit)
        NotificationNudge(decision.run, decision.shouldFire)
    },
    nextNudgeAt = { run, interval -> nextRunNudgeAt(run, interval) },
)

/** Maps Reminder delivery protocol to Notifications presentation while preserving pending-intent identities. */
internal fun showReminderNotification(
    context: Context,
    intents: PlatformIntents,
    notifications: NotificationDependencies,
    delivered: Intent,
): Boolean {
    val requestCode = delivered.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0)
    fun action(action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intents.reminderAction().setAction(action).putExtras(delivered),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    return notifications.reminderNotification.show(
        title = delivered.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE),
        requestCode = requestCode,
        openApp = PendingIntent.getActivity(
            context, 0, intents.openApp(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ),
        snooze = action(ReminderActions.SNOOZE),
        dismiss = action(ReminderActions.DISMISS),
    )
}
