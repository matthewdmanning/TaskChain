package com.taskchain.features.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskchain.core.data.CompletionRepository
import com.taskchain.features.progress.domain.ProgressSummary
import com.taskchain.features.progress.domain.projectProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val EMPTY_PROGRESS = ProgressSummary(0, 0, 0, 0, 0, emptyList())

/** Projects local completion events into the small progress summary; trend days begin at [dayStartHour]. */
class ProgressViewModel(
    completions: CompletionRepository,
    private val now: () -> Long,
    private val dayStartHour: Int,
) : ViewModel() {
    val state: StateFlow<ProgressSummary> = completions.observeAll()
        .map { events -> projectProgress(events, now(), dayStartHour) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EMPTY_PROGRESS)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
