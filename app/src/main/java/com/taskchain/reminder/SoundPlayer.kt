package com.taskchain.reminder

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.taskchain.domain.model.SoundSetting
import com.taskchain.domain.model.SoundToken

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
        if (settings.assetPath == null) {
            if (token != SoundToken.TimerExpired) return
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
                try {
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, TIMER_TONE_DURATION_MILLIS)
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

    private companion object {
        const val TIMER_TONE_DURATION_MILLIS = 300
    }
}
