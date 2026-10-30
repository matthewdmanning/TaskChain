package com.taskchain.core.model

import kotlinx.serialization.Serializable

/** Semantic feedback events that can be mapped to local bundled audio assets. */
@Serializable
enum class SoundToken {
    TaskNudge,
    TimerExpired,
    TaskCompleted,
    TaskRunning,
    TaskPaused,
    SubtaskAdvanced,
}

/** One sound's persisted enablement and optional Android asset pointer. */
@Serializable
data class SoundSetting(
    val enabled: Boolean = false,
    val assetPath: String? = null,
    /** Content URI of a device sound the user picked; it takes precedence over [assetPath]. */
    val uri: String? = null,
)

/** Per-routine sound policy, with missing token entries resolved to current defaults. */
@Serializable
data class SoundSettings(
    val entries: Map<SoundToken, SoundSetting> = defaultSoundEntries(),
) {
    /** Use this function when a playback adapter needs the setting for one semantic token. */
    operator fun get(token: SoundToken): SoundSetting = entries[token] ?: defaultSoundEntries()[token] ?: SoundSetting()
}

/**
 * Use this function to give one task its own alert sound.
 * Inputs: `soundUri` — the task's picked sound, or null to keep the routine's sounds.
 * The task sound replaces the nudge and timer-expiry sounds and keeps their enabled flags.
 */
fun SoundSettings.withTaskSound(soundUri: String?): SoundSettings {
    if (soundUri == null) return this
    return SoundSettings(
        entries + mapOf(
            SoundToken.TaskNudge to this[SoundToken.TaskNudge].copy(uri = soundUri),
            SoundToken.TimerExpired to this[SoundToken.TimerExpired].copy(uri = soundUri),
        ),
    )
}

/**
 * Use this function to apply the user's default sounds from Settings to a routine's sound policy.
 * Inputs: `defaults` — picked sound URI per token. A token the routine already customized keeps its own sound.
 */
fun SoundSettings.withUserDefaults(defaults: Map<SoundToken, String>): SoundSettings {
    if (defaults.isEmpty()) return this
    val builtIn = defaultSoundSettings()
    return SoundSettings(
        SoundToken.entries.associateWith { token ->
            val setting = this[token]
            val uri = defaults[token]
            if (uri != null && setting == builtIn[token]) setting.copy(uri = uri) else setting
        },
    )
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
    SoundToken.SubtaskAdvanced to SoundSetting(enabled = true),
)
