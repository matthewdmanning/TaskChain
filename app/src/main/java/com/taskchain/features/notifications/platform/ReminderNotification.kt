package com.taskchain.features.notifications.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.taskchain.R

/** Owns routine-reminder shade content; recurrence and button behavior are injected by root wiring. */
class ReminderNotification(context: Context) {
    private val appContext = context.applicationContext

    /** Posts the delivered reminder and reports whether permission allowed its feedback. */
    fun show(title: String?, requestCode: Int, openApp: PendingIntent, snooze: PendingIntent, dismiss: PendingIntent): Boolean {
        createReminderChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val displayTitle = title
            ?.takeIf(String::isNotBlank)
            ?: appContext.getString(R.string.reminder_default_title)
        val snoozeMinutes = appContext.resources.getInteger(R.integer.reminder_snooze_minutes)
        // Stays in the shade, with the launcher dot, until the user dismisses, snoozes, or starts the routine.
        val notification = NotificationCompat.Builder(appContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayTitle)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .addAction(
                0,
                appContext.getString(R.string.reminder_action_snooze, snoozeMinutes),
                snooze,
            )
            .addAction(
                0,
                appContext.getString(R.string.reminder_action_dismiss),
                dismiss,
            )
            .build()
        NotificationManagerCompat.from(appContext).notify(requestCode, notification)
        return true
    }

    /** Removes a delivered reminder when answered by a button or by starting its routine. */
    fun cancel(requestCode: Int) {
        NotificationManagerCompat.from(appContext).cancel(requestCode)
    }

    /** Use this function before posting reminders on Android 8 or newer. */
    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            appContext.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.reminder_channel_description)
            setShowBadge(true)
        }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val REMINDER_CHANNEL_ID = "routine_reminders"
    }
}
