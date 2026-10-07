package com.taskchain.reminder

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.taskchain.domain.model.SoundSettings
import com.taskchain.domain.model.SoundToken
import kotlin.math.roundToInt

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
        intensity: Float = 1f,
    )
}

/** Use this function to convert a user haptic intensity into an Android amplitude or suppress vibration at zero.
 * Inputs: `intensity` — user intensity normalized to the 0..1 range.
 * Dependencies: `kotlin.math.roundToInt`.
 */
internal fun vibrationAmplitude(intensity: Float): Int? {
    val normalized = intensity.coerceIn(0f, 1f)
    if (normalized <= 0f) return null
    return (normalized * 255f).roundToInt().coerceIn(1, 255)
}

/**
 * Resolves semantic sound settings and contains independent audio and haptic failures for run feedback.
 * Constructor inputs: `soundPlayer` — semantic audio adapter; `vibrate` — haptic dispatch callback.
 * Dependencies: `SoundPlayer`, Android vibration services in the context constructor.
 */
class AndroidTaskFeedback(
    private val soundPlayer: SoundPlayer,
    private val vibrate: (Long) -> Unit,
    private val vibrateWithAmplitude: ((Long, Int) -> Unit)? = null,
) : TaskFeedback {
    /**
     * Use this constructor when production feedback should use the app context and native Android services.
     * Inputs: `context` — application context used for sound assets and vibration services.
     * Dependencies: `AndroidAssetSoundPlayer`, `Vibrator`, `VibratorManager`.
     */
    constructor(context: Context) : this(
        soundPlayer = AndroidAssetSoundPlayer(context),
        vibrate = { durationMillis -> vibrateNative(context, durationMillis, 255) },
        vibrateWithAmplitude = { durationMillis, amplitude -> vibrateNative(context, durationMillis, amplitude) },
    )

    /** Use this function when a concrete feedback adapter should use the default haptic intensity.
     * Inputs: `token` — semantic event; `soundEnabled` — caller sound gate; `vibrateEnabled` — caller haptic gate;
     * `soundSettings` — per-token sound policy. Dependencies: the intensity-aware `fire` overload.
     */
    fun fire(
        token: SoundToken,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean,
        soundSettings: SoundSettings,
    ) = fire(token, soundEnabled, vibrateEnabled, soundSettings, intensity = 1f)

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
        intensity: Float,
    ) {
        val setting = soundSettings[token]
        if (soundEnabled && setting.enabled) {
            runCatching { soundPlayer.play(token, setting) }
        }
        if (token == SoundToken.TaskNudge && vibrateEnabled) {
            val amplitude = vibrationAmplitude(intensity) ?: return
            runCatching {
                vibrateWithAmplitude?.invoke(NUDGE_VIBRATION_DURATION_MILLIS, amplitude)
                    ?: vibrate(NUDGE_VIBRATION_DURATION_MILLIS)
            }
        }
    }

    private companion object {
        const val NUDGE_VIBRATION_DURATION_MILLIS = 300L
    }
}

/** Use this function to dispatch one haptic pulse through the Android vibrator service.
 * Inputs: `context` — application context; `durationMillis` — pulse duration; `amplitude` — Android amplitude.
 * Dependencies: `VibratorManager`, `Vibrator`, and `VibrationEffect`.
 */
private fun vibrateNative(context: Context, durationMillis: Long, amplitude: Int) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, amplitude))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMillis)
    }
}
