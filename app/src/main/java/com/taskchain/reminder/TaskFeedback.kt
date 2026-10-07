package com.taskchain.reminder

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.taskchain.domain.model.SoundSettings
import com.taskchain.domain.model.SoundToken

/** Receives one run feedback event with independent sound and vibration gates. */
interface TaskFeedback {
    /**
     * Use this function when a run event should deliver its configured feedback.
     * Inputs: `token` — semantic event; `soundEnabled` — caller sound gate; `vibrateEnabled` — caller haptic gate;
     * `soundSettings` — per-token sound policy. Dependencies: `SoundSettings`.
     */
    fun fire(
        token: SoundToken,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean,
        soundSettings: SoundSettings,
    )
}

/**
 * Resolves semantic sound settings and contains independent audio and haptic failures for run feedback.
 * Constructor inputs: `soundPlayer` — semantic audio adapter; `vibrate` — haptic dispatch callback.
 * Dependencies: `SoundPlayer`, Android vibration services in the context constructor.
 */
class AndroidTaskFeedback(
    private val soundPlayer: SoundPlayer,
    private val vibrate: (Long) -> Unit,
) : TaskFeedback {
    /**
     * Use this constructor when production feedback should use the app context and native Android services.
     * Inputs: `context` — application context used for sound assets and vibration services.
     * Dependencies: `AndroidAssetSoundPlayer`, `Vibrator`, `VibratorManager`.
     */
    constructor(context: Context) : this(
        soundPlayer = AndroidAssetSoundPlayer(context),
        vibrate = { durationMillis ->
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMillis)
            }
        },
    )

    /**
     * Use this function when a run event should honor its token setting and the caller's independent gates.
     * Inputs: `token` — semantic event; `soundEnabled` — caller sound gate; `vibrateEnabled` — caller haptic gate;
     * `soundSettings` — per-token sound policy. Dependencies: `SoundPlayer`, `vibrate`.
     */
    override fun fire(
        token: SoundToken,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean,
        soundSettings: SoundSettings,
    ) {
        val setting = soundSettings[token]
        if (soundEnabled && setting.enabled) {
            runCatching { soundPlayer.play(token, setting) }
        }
        if (token == SoundToken.TaskNudge && vibrateEnabled) {
            runCatching { vibrate(NUDGE_VIBRATION_DURATION_MILLIS) }
        }
    }

    private companion object {
        const val NUDGE_VIBRATION_DURATION_MILLIS = 300L
    }
}
