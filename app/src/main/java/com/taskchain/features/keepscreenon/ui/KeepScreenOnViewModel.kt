package com.taskchain.features.keepscreenon.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.core.model.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Owns observation and edits of the keep-screen-on preference. */
class KeepScreenOnViewModel(private val preferences: UserPreferenceRepository) : ViewModel() {
    val state = preferences.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserPreferences())

    /** Saves whether an active run keeps the display awake, including while paused. */
    fun setKeepScreenOnDuringRun(enabled: Boolean) {
        viewModelScope.launch { preferences.setKeepScreenOnDuringRun(enabled) }
    }
}
