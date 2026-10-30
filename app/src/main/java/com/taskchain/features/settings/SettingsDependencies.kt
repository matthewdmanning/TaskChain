package com.taskchain.features.settings

import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.features.settings.ui.SettingsViewModel

/** Public entry of the Settings feature: the preference store it edits and the Settings tab's ViewModel factory. */
class SettingsDependencies(private val preferences: UserPreferenceRepository) {
    /** Use this function as the root ViewModel factory for the Settings tab. */
    fun settingsViewModel(): SettingsViewModel = SettingsViewModel(preferences)
}
