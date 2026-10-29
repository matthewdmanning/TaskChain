package com.taskchain.features.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.taskchain.R
import com.taskchain.core.model.ScheduleFrequency
import com.taskchain.core.model.ScheduleRule
import com.taskchain.core.model.defaultSoundSettings
import com.taskchain.core.platform.PlatformIntents
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.core.platform.TimerFeedback
import com.taskchain.core.platform.setAlarm
import com.taskchain.core.schedule.ReminderPlanner
import com.taskchain.core.schedule.ReminderRequest
import com.taskchain.core.schedule.completedRoutineSinceCycle
import com.taskchain.core.schedule.completedSinceCycle
import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.RoutineRunRepository
import kotlinx.coroutines.flow.first

/** Stable routine reminder notification actions shared by the feature and legacy receiver adapter. */
object ReminderActions {
    const val SNOOZE = "com.taskchain.reminder.SNOOZE"
    const val DISMISS = "com.taskchain.reminder.DISMISS"
}

/** Android alarm adapter for local routine reminders; exact when the permission allows it. */
class AndroidReminderScheduler(
    context: Context,
    private val intents: PlatformIntents,
) : ReminderScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    /** Use this function when a domain reminder should become an Android alarm. */
    override fun schedule(request: ReminderRequest) {
        setAlarm(alarmManager, request.triggerAtEpochMillis, pendingIntent(request))
    }

    /** Use this function when a domain reminder should no longer fire. */
    override fun cancel(requestCode: Int) {
        listOf(null, ACTION_SNOOZED_DELIVERY).forEach { action ->
            val existing = PendingIntent.getBroadcast(
                appContext,
                requestCode,
                intents.reminderDelivery().setAction(action),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (existing != null) alarmManager.cancel(existing)
        }
    }

    /** Use this function when a delivered reminder notification should leave the notification shade. */
    override fun clearDelivered(requestCode: Int) {
        NotificationManagerCompat.from(appContext).cancel(requestCode)
    }

    /** Use this function to create a stable alarm identity carrying recurrence data to the receiver. */
    private fun pendingIntent(request: ReminderRequest): PendingIntent {
        val intent = intents.reminderDelivery().apply {
            putExtra(EXTRA_REQUEST_CODE, request.requestCode)
            putExtra(EXTRA_ROUTINE_ID, request.routineId)
            putExtra(EXTRA_TITLE, request.title)
            putExtra(EXTRA_TASK_ID, request.taskId)
            putExtra(EXTRA_REMIND_EVERY_MINUTES, request.remindEveryMinutes ?: 0)
            request.cycleStartEpochMillis?.let { putExtra(EXTRA_CYCLE_START, it) }
            putExtra(EXTRA_SOUND_ENABLED, request.soundEnabled)
            putExtra(EXTRA_VIBRATE_ENABLED, request.vibrateEnabled)
            request.schedule?.let {
                putExtra(EXTRA_FREQUENCY, it.frequency.name)
                putExtra(EXTRA_LOCAL_HOUR, it.localHour)
                putExtra(EXTRA_LOCAL_MINUTE, it.localMinute)
                putExtra(EXTRA_DAYS_OF_WEEK, it.daysOfWeek.toIntArray())
                it.oneTimeEpochMillis?.let { value -> putExtra(EXTRA_ONCE_EPOCH_MILLIS, value) }
            }
        }
        return PendingIntent.getBroadcast(
            appContext,
            request.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        /** Distinguishes the alarm a Snooze action sets from the routine's regular alarm with the same request code. */
        const val ACTION_SNOOZED_DELIVERY = "com.taskchain.reminder.SNOOZED_DELIVERY"
        const val EXTRA_ROUTINE_ID = "routine_id"
        const val EXTRA_REQUEST_CODE = "request_code"
        const val EXTRA_TITLE = "title"
        // Keeps the pre-rename value so alarms scheduled by older versions still deliver their task id.
        const val EXTRA_TASK_ID = "step_id"
        const val EXTRA_REMIND_EVERY_MINUTES = "remind_every_minutes"
        const val EXTRA_CYCLE_START = "cycle_start"
        const val EXTRA_SOUND_ENABLED = "sound_enabled"
        const val EXTRA_VIBRATE_ENABLED = "vibrate_enabled"
        const val EXTRA_FREQUENCY = "frequency"
        const val EXTRA_LOCAL_HOUR = "local_hour"
        const val EXTRA_LOCAL_MINUTE = "local_minute"
        const val EXTRA_DAYS_OF_WEEK = "days_of_week"
        const val EXTRA_ONCE_EPOCH_MILLIS = "once_epoch_millis"
    }
}

/** Coordinates Android delivery, action, and restore behavior for routine reminders. */
class ReminderHandlers(
    context: Context,
    private val routines: RoutineRepository,
    private val activeRun: RoutineRunRepository,
    private val completions: CompletionRepository,
    private val reminders: ReminderScheduler,
    private val intents: PlatformIntents,
    private val timerFeedback: TimerFeedback,
) {
    private val appContext = context.applicationContext

    /** Use this function when Android delivers a scheduled local routine alarm. */
    suspend fun deliver(intent: Intent) {
        val taskId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TASK_ID)
        val repeatMinutes = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REMIND_EVERY_MINUTES, 0)
        val completed = if (repeatMinutes > 0) runCatching {
            val routineId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_ROUTINE_ID).orEmpty()
            val cycleStartEpochMillis = intent.getLongExtra(AndroidReminderScheduler.EXTRA_CYCLE_START, 0L)
            val active = activeRun.observeActive().first()
            val history = completions.observeAll().first()
            if (taskId != null) {
                completedSinceCycle(routineId, taskId, cycleStartEpochMillis, active, history)
            } else {
                completedRoutineSinceCycle(routineId, cycleStartEpochMillis, active, history)
            }
        }.getOrDefault(false) else false
        if (completed) {
            rearmReminder(intent, repeatNow = false)
            return
        }
        createReminderChannel()
        rearmReminder(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val title = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE)
            ?.takeIf(String::isNotBlank)
            ?: appContext.getString(R.string.reminder_default_title)
        val requestCode = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0)
        val openApp = PendingIntent.getActivity(
            appContext,
            0,
            intents.openApp(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeMinutes = appContext.resources.getInteger(R.integer.reminder_snooze_minutes)
        // Stays in the shade, with the launcher dot, until the user dismisses, snoozes, or starts the routine.
        val notification = NotificationCompat.Builder(appContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .addAction(
                0,
                appContext.getString(R.string.reminder_action_snooze, snoozeMinutes),
                actionIntent(intent, ReminderActions.SNOOZE, requestCode),
            )
            .addAction(
                0,
                appContext.getString(R.string.reminder_action_dismiss),
                actionIntent(intent, ReminderActions.DISMISS, requestCode),
            )
            .build()
        NotificationManagerCompat.from(appContext).notify(requestCode, notification)
        timerFeedback.fire(
            intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_SOUND_ENABLED, true),
            intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_VIBRATE_ENABLED, true),
            defaultSoundSettings(),
        )
    }

    /** Use this function when the user presses Snooze or Dismiss on a reminder notification. */
    suspend fun action(intent: Intent) {
        val action = intent.action
        if (action != ReminderActions.SNOOZE && action != ReminderActions.DISMISS) return
        val requestCode = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0)
        reminders.clearDelivered(requestCode)
        // Stop this cycle's pending repeat; the next regular occurrence stays armed.
        if (!rearmReminder(intent, repeatNow = false)) reminders.cancel(requestCode)
        if (action == ReminderActions.SNOOZE) scheduleSnoozedDelivery(intent, requestCode)
    }

    /** Use this function when Android reports a reboot or time-zone change requiring alarm restoration. */
    suspend fun restore(intent: Intent) {
        try {
            val now = System.currentTimeMillis()
            routines.observeAll().first().forEach { routine ->
                val reminderPlan = ReminderPlanner.forRestore(routine, now)
                reminderPlan.cancelRequestCodes.forEach(reminders::cancel)
                reminderPlan.requests.forEach(reminders::schedule)
            }
        } catch (_: Exception) {
            // A damaged or concurrently replaced local routine file must not crash a system broadcast.
        }
    }

    /** Use this function to build a Snooze or Dismiss button that carries the delivered alarm's full request data. */
    private fun actionIntent(delivered: Intent, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intents.reminderAction().setAction(action).putExtras(delivered),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Use this function to deliver the same reminder again after the configured snooze time. */
    private fun scheduleSnoozedDelivery(source: Intent, requestCode: Int) {
        val snoozeMillis = appContext.resources.getInteger(R.integer.reminder_snooze_minutes) * 60_000L
        val operation = PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intents.reminderDelivery()
                .setAction(AndroidReminderScheduler.ACTION_SNOOZED_DELIVERY)
                .putExtras(source),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(appContext.getSystemService(AlarmManager::class.java), System.currentTimeMillis() + snoozeMillis, operation)
    }

    /** Use this function after delivery or a dismiss to schedule the next occurrence of a recurring reminder. */
    private suspend fun rearmReminder(intent: Intent, repeatNow: Boolean = true): Boolean {
        val frequency = intent.getStringExtra(AndroidReminderScheduler.EXTRA_FREQUENCY)
            ?.let { runCatching { ScheduleFrequency.valueOf(it) }.getOrNull() } ?: return false
        val minutes = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REMIND_EVERY_MINUTES, 0).takeIf { it > 0 }
        val rule = ScheduleRule(
            frequency = frequency,
            localHour = intent.getIntExtra(AndroidReminderScheduler.EXTRA_LOCAL_HOUR, -1),
            localMinute = intent.getIntExtra(AndroidReminderScheduler.EXTRA_LOCAL_MINUTE, -1),
            daysOfWeek = intent.getIntArrayExtra(AndroidReminderScheduler.EXTRA_DAYS_OF_WEEK)?.toSet() ?: emptySet(),
            oneTimeEpochMillis = intent.getLongExtra(AndroidReminderScheduler.EXTRA_ONCE_EPOCH_MILLIS, 0L).takeIf { it != 0L },
        )
        val now = System.currentTimeMillis()
        val next = ReminderPlanner.afterDelivery(
            ReminderRequest(
                requestCode = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0),
                routineId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_ROUTINE_ID).orEmpty(),
                title = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE).orEmpty(),
                triggerAtEpochMillis = now,
                schedule = rule,
                taskId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TASK_ID),
                remindEveryMinutes = minutes,
                cycleStartEpochMillis = intent.getLongExtra(AndroidReminderScheduler.EXTRA_CYCLE_START, 0L).takeIf { it > 0 },
                soundEnabled = intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_SOUND_ENABLED, true),
                vibrateEnabled = intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_VIBRATE_ENABLED, true),
            ),
            now,
            repeatNow,
        ) ?: return false
        reminders.schedule(next)
        return true
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
