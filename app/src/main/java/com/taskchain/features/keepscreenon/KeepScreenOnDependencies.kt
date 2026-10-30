package com.taskchain.features.keepscreenon

import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.features.keepscreenon.ui.KeepScreenOnViewModel

/**
 * Public entry of the Keep screen on feature: the preference store and the ViewModel factory for its Settings row.
 * Root injects the row into Settings and the run effect into the Run route.
 */
class KeepScreenOnDependencies(private val preferences: UserPreferenceRepository) {
    /** Use this function as the root ViewModel factory for the feature's Settings row. */
    fun settingsViewModel(): KeepScreenOnViewModel = KeepScreenOnViewModel(preferences)
}
