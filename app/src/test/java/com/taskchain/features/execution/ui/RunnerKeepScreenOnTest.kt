package com.taskchain.features.execution.ui

import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineRunTask
import com.taskchain.core.model.RoutineTask
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.model.RunStatus
import com.taskchain.core.model.UserPreferences
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies when the Run screen keeps the display awake. */
class RunnerKeepScreenOnTest {
    /** Use this function to verify the display stays on while a run is active and the setting is on.
     * Inputs: none. Dependencies: `runKeepsScreenOn`.
     */
    @Test
    fun activeRunKeepsScreenOnWhenSettingIsOn() {
        assertTrue(runKeepsScreenOn(run(RunStatus.ACTIVE), preferenceEnabled = true))
    }

    /** Use this function to verify the setting turns the behavior off for an active run.
     * Inputs: none. Dependencies: `runKeepsScreenOn`.
     */
    @Test
    fun settingOffAllowsScreenTimeout() {
        assertFalse(runKeepsScreenOn(run(RunStatus.ACTIVE), preferenceEnabled = false))
    }

    /** Use this function to verify a loading screen, and ended runs, let the display sleep.
     * Inputs: none. Dependencies: `runKeepsScreenOn`.
     */
    @Test
    fun loadingOrEndedRunAllowsScreenTimeout() {
        assertFalse(runKeepsScreenOn(null, preferenceEnabled = true))
        assertFalse(runKeepsScreenOn(run(RunStatus.COMPLETED), preferenceEnabled = true))
        assertFalse(runKeepsScreenOn(run(RunStatus.ABORTED), preferenceEnabled = true))
    }

    /** Use this function to verify the setting defaults to on, including for saved preferences without it.
     * Inputs: none. Dependencies: `UserPreferences` and kotlinx serialization.
     */
    @Test
    fun settingDefaultsToOn() {
        assertTrue(UserPreferences().keepScreenOnDuringRun)
        val legacy = Json { ignoreUnknownKeys = true }.decodeFromString<UserPreferences>("""{"selectedTheme":"system"}""")
        assertTrue(legacy.keepScreenOnDuringRun)
    }

    /** Use this function to create the smallest run snapshot needed by the assertions. */
    private fun run(status: RunStatus): RoutineRun =
        RoutineRun(
            id = RoutineRunId("run"),
            routineId = RoutineId("routine"),
            routineTitle = "Routine",
            tasks = listOf(RoutineRunTask(RoutineTask(RoutineTaskId("task"), "Task"))),
            currentTaskIndex = 0,
            startedAtEpochMillis = 1_000,
            status = status,
        )
}
