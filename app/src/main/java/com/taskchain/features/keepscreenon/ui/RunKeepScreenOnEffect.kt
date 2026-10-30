package com.taskchain.features.keepscreenon.ui

import androidx.compose.runtime.Composable
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.ui.KeepScreenOnEffect
import com.taskchain.features.keepscreenon.domain.runKeepsScreenOn

/** Applies the keep-screen-on preference to the run currently presented by Execution. */
@Composable
internal fun RunKeepScreenOnEffect(run: RoutineRun?, preferenceEnabled: Boolean) {
    KeepScreenOnEffect(enabled = runKeepsScreenOn(run, preferenceEnabled))
}
