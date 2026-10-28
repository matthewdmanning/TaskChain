package com.taskchain.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.taskchain.MainActivity
import com.taskchain.TaskChainApplication
import com.taskchain.R
import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import com.taskchain.domain.schedule.ReminderPlanner
import com.taskchain.domain.schedule.ReminderRequest
import com.taskchain.domain.schedule.completedSinceCycle
import com.taskchain.domain.schedule.completedRoutineSinceCycle
import com.taskchain.domain.model.SoundSettings
import com.taskchain.domain.model.SoundToken
import com.taskchain.domain.model.defaultSoundSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Android scheduling boundary kept outside routine and schedule domain models. */
interface ReminderScheduler {
    /** Use this function when enabling or changing a future routine reminder. */
    fun schedule(request: ReminderRequest)

    /** Use this function when disabling an existing routine reminder, including any snoozed delivery. */
    fun cancel(requestCode: Int)

    /** Use this function when a delivered reminder notification should leave the notification shade. */
    fun clearDelivered(requestCode: Int)
}

/** AlarmManager adapter for local routine reminders; exact when the permission allows it. */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Use this function when a domain reminder should become an Android alarm. */
    override fun schedule(request: ReminderRequest) {
        setAlarm(alarmManager, request.triggerAtEpochMillis, pendingIntent(request))
    }

    /** Use this function when a domain reminder should no longer fire. */
    override fun cancel(requestCode: Int) {
        listOf(null, ACTION_SNOOZED_DELIVERY).forEach { action ->
            val existing = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, ReminderReceiver::class.java).setAction(action),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (existing != null) alarmManager.cancel(existing)
        }
    }

    /** Use this function when a delivered reminder notification should leave the notification shade. */
    override fun clearDelivered(requestCode: Int) {
        NotificationManagerCompat.from(context).cancel(requestCode)
    }

    /** Use this function to create a stable alarm identity carrying recurrence data to the receiver. */
    private fun pendingIntent(request: ReminderRequest): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
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
            context,
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

/** Receives local alarms and displays the corresponding routine notification. */
class ReminderReceiver : BroadcastReceiver() {
    /** Use this function when Android delivers a scheduled local routine alarm. */
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
        try {
        val taskId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TASK_ID)
        val repeatMinutes = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REMIND_EVERY_MINUTES, 0)
        val completed = if (repeatMinutes > 0) runCatching {
            val container = (context.applicationContext as TaskChainApplication).container
            val routineId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_ROUTINE_ID).orEmpty()
            val cycleStartEpochMillis = intent.getLongExtra(AndroidReminderScheduler.EXTRA_CYCLE_START, 0L)
            val activeRun = container.activeRun.observeActive().first()
            val history = container.completions.observeAll().first()
            if (taskId != null) {
                completedSinceCycle(routineId, taskId, cycleStartEpochMillis, activeRun, history)
            } else {
                completedRoutineSinceCycle(routineId, cycleStartEpochMillis, activeRun, history)
            }
        }.getOrDefault(false) else false
        if (completed) {
            rearmReminder(context, intent, repeatNow = false)
            return@launch
        }
        createReminderChannel(context)
        rearmReminder(context, intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return@launch

        val title = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE)
            ?.takeIf(String::isNotBlank)
            ?: context.getString(R.string.reminder_default_title)
        val requestCode = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snoozeMinutes = context.resources.getInteger(R.integer.reminder_snooze_minutes)
        // Stays in the shade, with the launcher dot, until the user dismisses, snoozes, or starts the routine.
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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
                context.getString(R.string.reminder_action_snooze, snoozeMinutes),
                actionIntent(context, intent, ReminderActionReceiver.ACTION_SNOOZE, requestCode),
            )
            .addAction(
                0,
                context.getString(R.string.reminder_action_dismiss),
                actionIntent(context, intent, ReminderActionReceiver.ACTION_DISMISS, requestCode),
            )
            .build()
        NotificationManagerCompat.from(context).notify(requestCode, notification)
        TimerFeedback(context).fire(
            intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_SOUND_ENABLED, true),
            intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_VIBRATE_ENABLED, true),
            defaultSoundSettings(),
        )
        } finally {
            pendingResult.finish()
        }
        }
    }

    /** Use this function to build a Snooze or Dismiss button that carries the delivered alarm's full request data. */
    private fun actionIntent(context: Context, delivered: Intent, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderActionReceiver::class.java).setAction(action).putExtras(delivered),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private companion object {
        const val CHANNEL_ID = REMINDER_CHANNEL_ID
    }
}

/** Handles the Snooze and Dismiss buttons on a delivered routine reminder notification. */
class ReminderActionReceiver : BroadcastReceiver() {
    /** Use this function when the user presses Snooze or Dismiss on a reminder notification. */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_SNOOZE && action != ACTION_DISMISS) return
        val requestCode = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = (context.applicationContext as TaskChainApplication).container.reminders
                reminders.clearDelivered(requestCode)
                // Stop this cycle's pending repeat; the next regular occurrence stays armed.
                if (!rearmReminder(context, intent, repeatNow = false)) reminders.cancel(requestCode)
                if (action == ACTION_SNOOZE) scheduleSnoozedDelivery(context, intent, requestCode)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Use this function to deliver the same reminder again after the configured snooze time. */
    private fun scheduleSnoozedDelivery(context: Context, source: Intent, requestCode: Int) {
        val snoozeMillis = context.resources.getInteger(R.integer.reminder_snooze_minutes) * 60_000L
        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java)
                .setAction(AndroidReminderScheduler.ACTION_SNOOZED_DELIVERY)
                .putExtras(source),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        setAlarm(context.getSystemService(AlarmManager::class.java), System.currentTimeMillis() + snoozeMillis, operation)
    }

    companion object {
        const val ACTION_SNOOZE = "com.taskchain.reminder.SNOOZE"
        const val ACTION_DISMISS = "com.taskchain.reminder.DISMISS"
    }
}

/** Use this function to set one alarm; exact when the permission allows it, because inexact alarms drift in Doze. */
internal fun setAlarm(alarmManager: AlarmManager, triggerAtEpochMillis: Long, operation: PendingIntent) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, operation)
    } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, operation)
    }
}

/**
 * Use this function after delivery or a dismiss to schedule the next occurrence of a recurring reminder.
 * Inputs: `intent` — the delivered alarm's extras; `repeatNow` — whether another repeat inside the window is wanted.
 * Returns false when the reminder has no further occurrence to schedule.
 */
private fun rearmReminder(context: Context, intent: Intent, repeatNow: Boolean = true): Boolean {
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
    (context.applicationContext as TaskChainApplication).container.reminders.schedule(next)
    return true
}

/** Use this function before posting reminders on Android 8 or newer. */
private fun createReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        REMINDER_CHANNEL_ID,
        context.getString(R.string.reminder_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.reminder_channel_description)
        setShowBadge(true)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private const val REMINDER_CHANNEL_ID = "routine_reminders"

/** Restores saved routine alarms after Android clears alarms during reboot or local-time changes. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    /** Use this function when Android reports a reboot or time-zone change requiring alarm restoration. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
            )
        ) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as TaskChainApplication).container
                val now = System.currentTimeMillis()
                container.routines.observeAll().first().forEach { routine ->
                    val reminderPlan = ReminderPlanner.forRestore(routine, now)
                    reminderPlan.cancelRequestCodes.forEach(container.reminders::cancel)
                    reminderPlan.requests.forEach(container.reminders::schedule)
                }
            } catch (_: Exception) {
                // A damaged or concurrently replaced local routine file must not crash a system broadcast.
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Provides one-shot audio and haptic feedback when a task timer reaches zero. */
class TimerFeedback(
    private val context: Context,
    private val soundPlayer: SoundPlayer = AndroidAssetSoundPlayer(context),
    private val vibrateWithAmplitude: ((Long, Int) -> Unit)? = null,
) {

    /** Use this function exactly once when the domain reports an unacknowledged timer expiry. */
    fun fire(
        soundEnabled: Boolean = true,
        vibrateEnabled: Boolean = true,
        soundSettings: SoundSettings = defaultSoundSettings(),
        intensity: Float = 1f,
    ) {
        val setting = soundSettings[SoundToken.TimerExpired]
        if (soundEnabled && setting.enabled) runCatching { soundPlayer.play(SoundToken.TimerExpired, setting) }
        if (!vibrateEnabled) return
        val amplitude = vibrationAmplitude(intensity) ?: return
        runCatching {
            if (vibrateWithAmplitude != null) {
                vibrateWithAmplitude.invoke(VIBRATION_DURATION_MILLIS, amplitude)
            } else {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MILLIS, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(VIBRATION_DURATION_MILLIS)
                }
            }
        }
    }

    private companion object {
        const val VIBRATION_DURATION_MILLIS = 300L
    }
}
