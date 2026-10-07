package com.taskchain.domain.model

import kotlinx.serialization.Serializable

/** Semantic feedback events that can be mapped to local bundled audio assets. */
@Serializable
enum class SoundToken {
    TaskNudge,
    TimerExpired,
    TaskCompleted,
    TaskRunning,
    TaskPaused,
}

/** One sound's persisted enablement and optional Android asset pointer. */
@Serializable
data class SoundSetting(
    val enabled: Boolean = false,
    val assetPath: String? = null,
)

/** Per-routine sound policy, with missing token entries resolved to current defaults. */
@Serializable
data class SoundSettings(
    val entries: Map<SoundToken, SoundSetting> = defaultSoundEntries(),
) {
    /** Use this function when a playback adapter needs the setting for one semantic token. */
    operator fun get(token: SoundToken): SoundSetting = entries[token] ?: defaultSoundEntries()[token] ?: SoundSetting()
}

/** Use this function when creating a routine with the backward-compatible sound policy. */
fun defaultSoundSettings(): SoundSettings = SoundSettings()

/** Use this function to provide the default mapping for every semantic token. */
private fun defaultSoundEntries(): Map<SoundToken, SoundSetting> = mapOf(
    SoundToken.TaskNudge to SoundSetting(assetPath = "sounds/task_nudge.ogg"),
    SoundToken.TimerExpired to SoundSetting(enabled = true),
    SoundToken.TaskCompleted to SoundSetting(assetPath = "sounds/task_completed.ogg"),
    SoundToken.TaskRunning to SoundSetting(assetPath = "sounds/task_running.ogg"),
    SoundToken.TaskPaused to SoundSetting(assetPath = "sounds/task_paused.ogg"),
)
