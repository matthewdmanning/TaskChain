package com.taskchain.ui.runner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStatus

/**
 * Use this function to decide whether the Run screen must keep the display awake.
 *
 * @param run The run snapshot shown by the runner route, or null while it loads.
 * @return True only while the run is active; pause does not change the result.
 */
internal fun runKeepsScreenOn(run: RoutineRun?): Boolean = run?.status == RunStatus.ACTIVE

/**
 * Use this function to keep the display awake while the Run screen is in composition.
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
