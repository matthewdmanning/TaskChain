package com.taskchain.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskchain.core.model.SoundToken
import com.taskchain.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

/** DataStore-backed user preferences kept separate from device permission state. */
class DataStoreUserPreferenceRepository(private val context: Context) : UserPreferenceRepository {
    /** Use this function when app theme or timer behavior must update reactively. */
    override fun observe(): Flow<UserPreferences> = context.userPreferencesDataStore.data.map { values ->
        UserPreferences(
            selectedTheme = values[SELECTED_THEME] ?: DEFAULT_THEME,
            selectedUiProvider = values[SELECTED_UI_PROVIDER] ?: DEFAULT_UI_PROVIDER,
            continueTimerPastZero = values[CONTINUE_PAST_ZERO] ?: true,
            vibrationIntensity = values[VIBRATION_INTENSITY] ?: 1f,
            screenTransitionsEnabled = values[SCREEN_TRANSITIONS_ENABLED] ?: true,
            showSubtaskTimeRemaining = values[SHOW_SUBTASK_TIME_REMAINING] ?: false,
            defaultSoundUris = SoundToken.entries.mapNotNull { token ->
                values[defaultSoundKey(token)]?.let { token to it }
            }.toMap(),
        )
    }

    /** Use this function when settings chooses a theme supported by the design system. */
    override suspend fun setTheme(theme: String) {
        context.userPreferencesDataStore.edit { it[SELECTED_THEME] = theme }
    }

    /** Persists a UI provider ID; unknown IDs safely resolve to the default at the theme boundary. */
    override suspend fun setUiProvider(providerId: String) {
        context.userPreferencesDataStore.edit { it[SELECTED_UI_PROVIDER] = providerId }
    }

    /** Use this function when settings changes whether countdowns show overtime. */
    override suspend fun setContinueTimerPastZero(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[CONTINUE_PAST_ZERO] = enabled }
    }

    /** Use this function when settings changes the strength of haptic feedback. */
    override suspend fun setVibrationIntensity(intensity: Float) {
        require(intensity in 0f..1f)
        context.userPreferencesDataStore.edit { it[VIBRATION_INTENSITY] = intensity }
    }

    /** Use this function when settings changes completion transition presentation. */
    override suspend fun setScreenTransitionsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[SCREEN_TRANSITIONS_ENABLED] = enabled }
    }

    /** Use this function when Settings changes whether the active subtask shows remaining time. */
    override suspend fun setShowSubtaskTimeRemaining(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[SHOW_SUBTASK_TIME_REMAINING] = enabled }
    }

    /** Use this function when Settings picks or clears a default device sound for one token. */
    override suspend fun setDefaultSoundUri(token: SoundToken, uri: String?) {
        context.userPreferencesDataStore.edit {
            if (uri == null) it.remove(defaultSoundKey(token)) else it[defaultSoundKey(token)] = uri
        }
    }

    private companion object {
        fun defaultSoundKey(token: SoundToken) = stringPreferencesKey("default_sound_${token.name}")
        const val DEFAULT_THEME = "system"
        const val DEFAULT_UI_PROVIDER = "cyberpunk"
        val SELECTED_UI_PROVIDER = stringPreferencesKey("selected_ui_provider")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")
        val CONTINUE_PAST_ZERO = booleanPreferencesKey("continue_timer_past_zero")
        val VIBRATION_INTENSITY = floatPreferencesKey("vibration_intensity")
        val SCREEN_TRANSITIONS_ENABLED = booleanPreferencesKey("screen_transitions_enabled")
        val SHOW_SUBTASK_TIME_REMAINING = booleanPreferencesKey("show_subtask_time_remaining")
    }
}
