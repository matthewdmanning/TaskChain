package com.taskchain.core.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.taskchain.core.model.SoundSetting
import com.taskchain.core.model.SoundToken

/** Plays one enabled semantic sound or the native timer default without exposing Android APIs to domain code. */
interface SoundPlayer {
    /** Use this function when a domain event requests one configured sound. */
    fun play(token: SoundToken, settings: SoundSetting)
}

/** Resolves configured asset paths to one-shot players and contains audio construction, playback, and release failures. */
class AndroidAssetSoundPlayer(private val context: Context) : SoundPlayer {
    /** Use this function when a semantic sound should play without allowing asset failures to escape. */
    override fun play(token: SoundToken, settings: SoundSetting) {
        if (!settings.enabled) return
        settings.uri?.takeIf { it.isNotBlank() }?.let {
            playPickedSound(it)
            return
        }
        if (settings.assetPath == null) {
            val toneType = when (token) {
                SoundToken.TimerExpired -> ToneGenerator.TONE_PROP_BEEP
                SoundToken.SubtaskAdvanced -> ToneGenerator.TONE_PROP_ACK
                else -> return
            }
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
                try {
                    tone.startTone(toneType, TIMER_TONE_DURATION_MILLIS)
                    val released = Handler(Looper.getMainLooper()).postDelayed(
                        { runCatching { tone.release() } },
                        TIMER_TONE_DURATION_MILLIS.toLong(),
                    )
                    if (!released) tone.release()
                } catch (_: Exception) {
                    runCatching { tone.release() }
                }
            }
            return
        }
        if (settings.assetPath.isBlank()) return
        var player: MediaPlayer? = null
        try {
            val activePlayer = MediaPlayer()
            player = activePlayer
            context.assets.openFd(settings.assetPath).use { descriptor ->
                activePlayer.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            activePlayer.setOnCompletionListener { runCatching { it.release() } }
            activePlayer.setOnErrorListener { mediaPlayer, _, _ ->
                runCatching { mediaPlayer.release() }
                true
            }
            activePlayer.prepare()
            activePlayer.start()
        } catch (_: Exception) {
            runCatching { player?.release() }
        }
    }

    /** Use this function to play a device sound the user picked, stopping it after a short cap so ringtones stay brief. */
    private fun playPickedSound(uri: String) {
        var player: MediaPlayer? = null
        try {
            val activePlayer = MediaPlayer()
            player = activePlayer
            activePlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            activePlayer.setDataSource(context, Uri.parse(uri))
            activePlayer.setOnCompletionListener { runCatching { it.release() } }
            activePlayer.setOnErrorListener { mediaPlayer, _, _ ->
                runCatching { mediaPlayer.release() }
                true
            }
            activePlayer.prepare()
            activePlayer.start()
            Handler(Looper.getMainLooper()).postDelayed(
                { runCatching { activePlayer.release() } },
                PICKED_SOUND_MAX_MILLIS,
            )
        } catch (_: Exception) {
            runCatching { player?.release() }
        }
    }

    private companion object {
        const val TIMER_TONE_DURATION_MILLIS = 300
        const val PICKED_SOUND_MAX_MILLIS = 4_000L
    }
}
