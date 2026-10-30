@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.features.settings.ui
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskchain.R
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.core.designsystem.ThemeCatalog
import com.taskchain.core.designsystem.UiProvider
import com.taskchain.core.designsystem.LocalUiComponents
import com.taskchain.core.designsystem.Button
import com.taskchain.core.ui.LabeledSwitchRow
import com.taskchain.core.designsystem.OutlinedButton
import com.taskchain.core.ui.SoundPickerRow
import com.taskchain.core.model.SoundToken

/** Use this function to bind persisted appearance and timer behavior to Settings UI. */
@Composable
internal fun SettingsRoute(
    viewModel: SettingsViewModel,
    padding: PaddingValues,
    additionalSettings: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    val context = LocalContext.current
    val ui = LocalUiComponents.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Text(stringResource(R.string.theme), style = TaskChainDesignSystem.TextStyles.heading)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.small), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                ThemeCatalog.options(context).forEach { option ->
                    if (state.selectedTheme == option.id) {
                        Button(onClick = { viewModel.setTheme(option.id) }) { Text(stringResource(option.label)) }
                    } else {
                        OutlinedButton(onClick = { viewModel.setTheme(option.id) }) { Text(stringResource(option.label)) }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.ui_component_style), style = TaskChainDesignSystem.TextStyles.title)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                UiProvider.entries.forEach { provider ->
                    ui.button(
                        { viewModel.setUiProvider(provider.id) },
                        Modifier,
                        state.selectedUiProvider != provider.id,
                    ) {
                        Text(when (provider) {
                            UiProvider.CYBERPUNK -> stringResource(R.string.ui_provider_cyberpunk)
                            UiProvider.MATERIAL -> stringResource(R.string.ui_provider_material)
                        })
                    }
                }
            }
        }
        item {
            LabeledSwitchRow(
                label = stringResource(R.string.continue_past_zero),
                checked = state.continueTimerPastZero,
                onCheckedChange = viewModel::setContinuePastZero,
            )
        }
        item {
            LabeledSwitchRow(
                label = stringResource(R.string.show_subtask_time_remaining),
                checked = state.showSubtaskTimeRemaining,
                onCheckedChange = viewModel::setShowSubtaskTimeRemaining,
            )
        }
        item {
            Text(stringResource(R.string.punch_vibration_intensity), style = TaskChainDesignSystem.TextStyles.title)
            androidx.compose.material3.Slider(value = state.vibrationIntensity,
                onValueChange = viewModel::setVibrationIntensity, valueRange = 0f..1f, steps = 3)
        }
        item { additionalSettings() }
        item {
            Text(stringResource(R.string.settings_default_sounds), style = TaskChainDesignSystem.TextStyles.title)
        }
        items(SoundToken.entries) { token ->
            SoundPickerRow(
                label = stringResource(soundTokenLabel(token)),
                uri = state.defaultSoundUris[token],
                onPicked = { viewModel.setDefaultSound(token, it) },
            )
        }
        item {
            LabeledSwitchRow(label = stringResource(R.string.punch_screen_transitions),
                checked = state.screenTransitionsEnabled, onCheckedChange = viewModel::setScreenTransitionsEnabled)
        }
    }
}

/** Use this function to name a sound token in Settings. */
private fun soundTokenLabel(token: SoundToken): Int = when (token) {
    SoundToken.TaskNudge -> R.string.sound_token_nudge
    SoundToken.TimerExpired -> R.string.sound_token_timer_expired
    SoundToken.TaskCompleted -> R.string.sound_token_task_completed
    SoundToken.TaskRunning -> R.string.sound_token_task_running
    SoundToken.TaskPaused -> R.string.sound_token_task_paused
    SoundToken.SubtaskAdvanced -> R.string.sound_token_subtask_advanced
}
