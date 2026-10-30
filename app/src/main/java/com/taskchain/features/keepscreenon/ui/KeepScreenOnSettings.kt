package com.taskchain.features.keepscreenon.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskchain.R
import com.taskchain.core.ui.LabeledSwitchRow

/** Presents the feature's persisted preference inside the Settings host. */
@Composable
internal fun KeepScreenOnSettings(viewModel: KeepScreenOnViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LabeledSwitchRow(
        label = stringResource(R.string.keep_screen_on_during_run),
        checked = state.keepScreenOnDuringRun,
        onCheckedChange = viewModel::setKeepScreenOnDuringRun,
    )
}
