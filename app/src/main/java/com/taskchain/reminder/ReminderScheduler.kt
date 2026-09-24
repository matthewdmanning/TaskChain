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
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.taskchain.MainActivity
import com.taskchain.AppContainer
import com.taskchain.R
import com.taskchain.domain.model.ScheduleFrequency
import com.taskchain.domain.model.ScheduleRule
import com.taskchain.domain.schedule.NextTriggerCalculator
import com.taskchain.domain.model.CompletionEvent
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RoutineTemplate
import com.taskchain.domain.model.RunStatus
import com.taskchain.domain.model.RunStepStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Platform-neutral request to prompt the user about one routine. */
data class ReminderRequest(
    val requestCode: Int,
    val routineId: String,
    val title: String,
    val triggerAtEpochMillis: Long,
    val schedule: ScheduleRule? = null,
    val stepId: String? = null,
    val remindEveryMinutes: Int? = null,
    val cycleStartEpochMillis: Long? = null,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
)

/** Maps one routine occurrence to the complete request understood by the Android reminder adapter. */
internal fun RoutineTemplate.toReminderRequest(triggerAtEpochMillis: Long): ReminderRequest = ReminderRequest(
    requestCode = id.value.hashCode(),
    routineId = id.value,
    title = title,
    triggerAtEpochMillis = triggerAtEpochMillis,
    schedule = schedule,
    remindEveryMinutes = remindEveryMinutes,
    cycleStartEpochMillis = triggerAtEpochMillis.takeIf { schedule != null },
    soundEnabled = soundEnabled,
    vibrateEnabled = vibrateEnabled,
)

/** Use this function when a repeated task alert must honor a completion persisted in an active run or history. */
internal fun completedSinceCycle(
    routineId: String,
    stepId: String,
    cycleStartEpochMillis: Long,
    activeRun: RoutineRun?,
    history: List<CompletionEvent>,
): Boolean {
    if (cycleStartEpochMillis <= 0) return false
    val activeCompleted = activeRun?.takeIf { it.routineId.value == routineId }?.steps?.any { step ->
        step.source.id.value == stepId && step.status == RunStepStatus.COMPLETED &&
            (step.completedAtEpochMillis ?: step.finishedAtEpochMillis ?: Long.MIN_VALUE) >= cycleStartEpochMillis
    } == true
    return activeCompleted || history.any { event ->
        event.routineId.value == routineId && event.steps.any { step ->
            step.source.id.value == stepId && step.status == RunStepStatus.COMPLETED &&
                (step.completedAtEpochMillis ?: step.finishedAtEpochMillis ?: Long.MIN_VALUE) >= cycleStartEpochMillis
        }
    }
}

/** Use this function when a repeated routine prompt must stop after a completed run in its cycle. */
internal fun completedRoutineSinceCycle(
    routineId: String,
    cycleStartEpochMillis: Long,
    activeRun: RoutineRun?,
    history: List<CompletionEvent>,
): Boolean {
    if (cycleStartEpochMillis <= 0) return false
    val activeCompleted = activeRun?.takeIf {
        it.routineId.value == routineId && it.status == RunStatus.COMPLETED
    }?.endedAtEpochMillis?.let { it >= cycleStartEpochMillis } == true
    return activeCompleted || history.any { event ->
        event.routineId.value == routineId && event.status == RunStatus.COMPLETED &&
            event.endedAtEpochMillis >= cycleStartEpochMillis
    }
}

/** Android scheduling boundary kept outside routine and schedule domain models. */
interface ReminderScheduler {
    /** Use this function when enabling or changing a future routine reminder. */
    fun schedule(request: ReminderRequest)

    /** Use this function when disabling an existing routine reminder. */
    fun cancel(requestCode: Int)
}

/** AlarmManager adapter for local, inexact routine reminders. */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Use this function when a domain reminder should become an Android alarm. */
    override fun schedule(request: ReminderRequest) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            request.triggerAtEpochMillis,
            pendingIntent(request),
        )
    }

    /** Use this function when a domain reminder should no longer fire. */
    override fun cancel(requestCode: Int) {
        val existing = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(existing)
    }

    /** Use this function to create a stable alarm identity carrying recurrence data to the receiver. */
    private fun pendingIntent(request: ReminderRequest): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REQUEST_CODE, request.requestCode)
            putExtra(EXTRA_ROUTINE_ID, request.routineId)
            putExtra(EXTRA_TITLE, request.title)
            putExtra(EXTRA_STEP_ID, request.stepId)
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
        const val EXTRA_ROUTINE_ID = "routine_id"
        const val EXTRA_REQUEST_CODE = "request_code"
        const val EXTRA_TITLE = "title"
        const val EXTRA_STEP_ID = "step_id"
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
        val stepId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_STEP_ID)
        val repeatMinutes = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REMIND_EVERY_MINUTES, 0)
        val completed = if (repeatMinutes > 0) runCatching {
            val container = AppContainer(context.applicationContext)
            val routineId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_ROUTINE_ID).orEmpty()
            val cycleStartEpochMillis = intent.getLongExtra(AndroidReminderScheduler.EXTRA_CYCLE_START, 0L)
            val activeRun = container.activeRun.observeActive().first()
            val history = container.completions.observeAll().first()
            if (stepId != null) {
                completedSinceCycle(routineId, stepId, cycleStartEpochMillis, activeRun, history)
            } else {
                completedRoutineSinceCycle(routineId, cycleStartEpochMillis, activeRun, history)
            }
        }.getOrDefault(false) else false
        if (completed) {
            rearm(context, intent, repeatNow = false)
            return@launch
        }
        createChannel(context)
        rearm(context, intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return@launch

        val title = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE)
            ?.takeIf(String::isNotBlank)
            ?: context.getString(R.string.reminder_default_title)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .build()
        NotificationManagerCompat.from(context).notify(
            intent.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0),
            notification,
        )
        TimerFeedback(context).fire(
            intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_SOUND_ENABLED, true),
            intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_VIBRATE_ENABLED, true),
        )
        } finally {
            pendingResult.finish()
        }
        }
    }

    /** Use this function after delivery to schedule the next occurrence of a recurring reminder. */
    private fun rearm(context: Context, intent: Intent, repeatNow: Boolean = true) {
        val frequency = intent.getStringExtra(AndroidReminderScheduler.EXTRA_FREQUENCY)
            ?.let { runCatching { ScheduleFrequency.valueOf(it) }.getOrNull() } ?: return
        val minutes = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REMIND_EVERY_MINUTES, 0).takeIf { it > 0 }
        if (frequency == ScheduleFrequency.ONCE && minutes == null) return
        val rule = ScheduleRule(
            frequency = frequency,
            localHour = intent.getIntExtra(AndroidReminderScheduler.EXTRA_LOCAL_HOUR, -1),
            localMinute = intent.getIntExtra(AndroidReminderScheduler.EXTRA_LOCAL_MINUTE, -1),
            daysOfWeek = intent.getIntArrayExtra(AndroidReminderScheduler.EXTRA_DAYS_OF_WEEK)?.toSet() ?: emptySet(),
            oneTimeEpochMillis = intent.getLongExtra(AndroidReminderScheduler.EXTRA_ONCE_EPOCH_MILLIS, 0L).takeIf { it != 0L },
        )
        val now = System.currentTimeMillis()
        val regular = NextTriggerCalculator.nextTriggerEpochMillis(rule, now)
        val next = if (repeatNow) NextTriggerCalculator.nextRepeatedTriggerEpochMillis(rule, now, minutes ?: 0)
            else regular
        if (next == null) return
        AndroidReminderScheduler(context).schedule(
            ReminderRequest(
                requestCode = intent.getIntExtra(AndroidReminderScheduler.EXTRA_REQUEST_CODE, 0),
                routineId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_ROUTINE_ID).orEmpty(),
                title = intent.getStringExtra(AndroidReminderScheduler.EXTRA_TITLE).orEmpty(),
                triggerAtEpochMillis = next,
                schedule = rule,
                stepId = intent.getStringExtra(AndroidReminderScheduler.EXTRA_STEP_ID),
                remindEveryMinutes = minutes,
                cycleStartEpochMillis = if (!repeatNow || next == regular) next else intent.getLongExtra(AndroidReminderScheduler.EXTRA_CYCLE_START, 0L).takeIf { it > 0 } ?: now,
                soundEnabled = intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_SOUND_ENABLED, true),
                vibrateEnabled = intent.getBooleanExtra(AndroidReminderScheduler.EXTRA_VIBRATE_ENABLED, true),
            ),
        )
    }

    /** Use this function before posting reminders on Android 8 or newer. */
    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "routine_reminders"
    }
}

/** Restores saved routine alarms after Android clears alarms during reboot or local-time changes. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    /** Use this function when Android reports a reboot or time-zone change requiring alarm restoration. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = AppContainer(context.applicationContext)
                val now = System.currentTimeMillis()
                container.routines.observeAll().first().forEach { routine ->
                    val routineRequestCode = routine.id.value.hashCode()
                    val hasRoutineSettings = routine.schedule != null || routine.deadlineEpochMillis != null ||
                        routine.reminderAtEpochMillis != null || routine.remindEveryMinutes != null
                    if (hasRoutineSettings) {
                        val trigger = routine.schedule?.let { NextTriggerCalculator.nextTriggerEpochMillis(it, now) }
                            ?: routine.reminderAtEpochMillis?.takeIf { it > now }
                        if (trigger == null) container.reminders.cancel(routineRequestCode)
                        else container.reminders.schedule(
                            routine.toReminderRequest(trigger),
                        )
                        routine.steps.forEach { step ->
                            container.reminders.cancel("step:${step.id.value}".hashCode())
                        }
                    } else {
                        container.reminders.cancel(routineRequestCode)
                        routine.steps.forEach { step ->
                            val requestCode = "step:${step.id.value}".hashCode()
                            val trigger = step.schedule?.let { NextTriggerCalculator.nextTriggerEpochMillis(it, now) }
                                ?: step.reminderAtEpochMillis?.takeIf { it > now }
                            if (trigger == null) container.reminders.cancel(requestCode)
                            else container.reminders.schedule(
                                ReminderRequest(
                                    requestCode = requestCode,
                                    routineId = routine.id.value,
                                    title = step.title,
                                    triggerAtEpochMillis = trigger,
                                    schedule = step.schedule,
                                    stepId = step.id.value,
                                    remindEveryMinutes = step.remindEveryMinutes,
                                    cycleStartEpochMillis = if (step.schedule != null) trigger else null,
                                    soundEnabled = step.soundEnabled,
                                    vibrateEnabled = step.vibrateEnabled,
                                ),
                            )
                        }
                    }
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
class TimerFeedback(private val context: Context) {
    private val tone = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)

    /** Use this function exactly once when the domain reports an unacknowledged timer expiry. */
    fun fire(soundEnabled: Boolean = true, vibrateEnabled: Boolean = true) {
        if (soundEnabled) tone.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_DURATION_MILLIS)
        if (!vibrateEnabled) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_DURATION_MILLIS)
        }
    }

    private companion object {
        const val TONE_DURATION_MILLIS = 300
        const val VIBRATION_DURATION_MILLIS = 300L
    }
}
