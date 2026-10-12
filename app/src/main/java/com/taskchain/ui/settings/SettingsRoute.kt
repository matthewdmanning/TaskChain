@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.ui.settings
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.taskchain.R
import com.taskchain.ui.designsystem.TaskChainDesignSystem
import com.taskchain.ui.designsystem.ThemeCatalog
import com.taskchain.ui.designsystem.UiProvider
import com.taskchain.ui.designsystem.LocalUiComponents
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.ui.Button
import com.taskchain.ui.LabeledSwitchRow
import com.taskchain.ui.OutlinedButton
import com.taskchain.ui.TextButton

/** Use this function to bind persisted appearance and timer behavior to Settings UI. */
@Composable
internal fun SettingsRoute(viewModel: SettingsViewModel, padding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = TaskChainDesignSystem.spacing()
    val context = LocalContext.current
    val ui = LocalUiComponents.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        contentPadding = PaddingValues(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item {
            Text(stringResource(R.string.theme), style = CyberTheme.typography.display)
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
            Text(stringResource(R.string.ui_component_style), style = MaterialTheme.typography.titleLarge)
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
            Text(stringResource(R.string.punch_vibration_intensity), style = MaterialTheme.typography.titleLarge)
            androidx.compose.material3.Slider(value = state.vibrationIntensity,
                onValueChange = viewModel::setVibrationIntensity, valueRange = 0f..1f, steps = 3)
        }
        item {
            LabeledSwitchRow(label = stringResource(R.string.punch_screen_transitions),
                checked = state.screenTransitionsEnabled, onCheckedChange = viewModel::setScreenTransitionsEnabled)
        }
        item {
            LabeledSwitchRow(label = stringResource(R.string.punch_bubble_minimize),
                checked = state.bubbleOnMinimize, onCheckedChange = { enabled ->
                    viewModel.setBubbleOnMinimize(enabled)
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                })
            if (state.bubbleOnMinimize) {
                TextButton(onClick = {
                    context.startActivity(android.content.Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text(stringResource(R.string.punch_bubble_system_settings)) }
            }
        }
    }
}
