package com.taskchain.features.keepscreenon.domain

import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RunStatus

/**
 * Use this function to decide whether the Run screen must keep the display awake.
 *
 * @param run The run snapshot shown by the runner route, or null while it loads.
 * @param preferenceEnabled The user's "Keep screen on during a run" setting.
 * @return True only while the setting is on and the run is active; pause does not change the result.
 */
internal fun runKeepsScreenOn(run: RoutineRun?, preferenceEnabled: Boolean): Boolean =
    preferenceEnabled && run?.status == RunStatus.ACTIVE
