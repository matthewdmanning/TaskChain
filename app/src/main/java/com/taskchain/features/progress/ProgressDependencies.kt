package com.taskchain.features.progress

import com.taskchain.core.data.CompletionRepository
import com.taskchain.features.progress.ui.ProgressViewModel

/**
 * Public entry of the Progress feature: shared history, clock and day start (`home_day_start_hour`)
 * plus the Progress tab's ViewModel factory.
 */
class ProgressDependencies(
    private val completions: CompletionRepository,
    private val now: () -> Long,
    private val dayStartHour: Int,
) {
    /** Use this function as the root ViewModel factory for the Progress tab. */
    fun progressViewModel(): ProgressViewModel = ProgressViewModel(completions, now, dayStartHour)
}
