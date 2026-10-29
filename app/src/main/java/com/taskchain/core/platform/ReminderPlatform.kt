package com.taskchain.core.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.taskchain.core.model.SoundSettings
import com.taskchain.core.model.SoundToken
import com.taskchain.core.model.defaultSoundSettings
import com.taskchain.core.schedule.ReminderRequest

/** Android scheduling boundary shared by routine reminders and their feature adapter. */
interface ReminderScheduler {
    /** Use this function when enabling or changing a future routine reminder. */
    fun schedule(request: ReminderRequest)

    /** Use this function when disabling an existing routine reminder, including any snoozed delivery. */
    fun cancel(requestCode: Int)

    /** Use this function when a delivered reminder notification should leave the notification shade. */
    fun clearDelivered(requestCode: Int)
}

/** Use this function to set one alarm; exact when the permission allows it, because inexact alarms drift in Doze. */
internal fun setAlarm(alarmManager: AlarmManager, triggerAtEpochMillis: Long, operation: PendingIntent) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, operation)
    } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMillis, operation)
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
