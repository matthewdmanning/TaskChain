@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.ui.progress
import android.text.format.DateFormat
import java.util.Date
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskchain.R
import com.taskchain.domain.model.MILLIS_PER_SECOND
import com.taskchain.ui.designsystem.TaskChainDesignSystem

/** Use this function to bind completion-history projections to Progress UI. */
@Composable
internal fun ProgressRoute(viewModel: ProgressViewModel, padding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item { Text(stringResource(R.string.completed_runs, state.completedRuns)) }
        item { Text(stringResource(R.string.aborted_runs, state.abortedRuns)) }
        item { Text(stringResource(R.string.progress_actual_duration, state.actualDurationMillis / MILLIS_PER_SECOND)) }
        item { Text(stringResource(R.string.progress_task_adherence, state.taskAdherencePercent)) }
        item { Text(stringResource(R.string.progress_skipped_tasks, state.skippedTaskPercent)) }
        item { Text(stringResource(R.string.progress_seven_day_trend)) }
        items(state.lastSevenDays) { day ->
            Text(stringResource(R.string.progress_daily_count, DateFormat.format("EEE M/d", day.dayStartEpochMillis), day.completedRuns))
        }
    }
}
