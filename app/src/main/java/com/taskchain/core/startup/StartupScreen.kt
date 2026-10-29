package com.taskchain.core.startup

import com.taskchain.core.designsystem.Button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.taskchain.R
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.core.designsystem.TaskChainTheme

/**
 * Use this function while opening saved data, or to offer Retry after a storage failure.
 * Inputs: failed — whether loading failed; onRetry — reload request. Dependencies: TaskChainTheme, Button.
 */
@Composable
internal fun StartupScreen(failed: Boolean, onRetry: () -> Unit) {
    TaskChainTheme("system") {
        val spacing = TaskChainDesignSystem.spacing()
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium, Alignment.CenterVertically),
            ) {
                Text(stringResource(if (failed) R.string.saved_data_unavailable else R.string.opening_saved_data))
                if (failed) {
                    Text(stringResource(R.string.saved_data_retry_message))
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }
}
