package com.taskchain.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the display awake while the requesting content is in composition.
 * The effect sets `View.keepScreenOn` on the host view and restores the previous value
 * when [enabled] changes or the screen leaves composition.
 *
 * @param enabled Whether the display must stay on.
 */
@Composable
internal fun KeepScreenOnEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
}
