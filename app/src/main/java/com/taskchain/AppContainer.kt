package com.taskchain

import android.content.Context
import com.taskchain.core.data.BuiltInRoutineLibrary
import com.taskchain.core.data.CompletionRepository
import com.taskchain.core.data.DataStoreUserPreferenceRepository
import com.taskchain.core.data.FileCompletionRepository
import com.taskchain.core.data.FileRoutineRepository
import com.taskchain.core.data.FileRoutineRunRepository
import com.taskchain.core.data.RoutineRepository
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.core.data.migrateLegacyDataFiles
import com.taskchain.features.execution.recoverTerminalSession
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.features.execution.domain.RoutineRunEngine
import com.taskchain.reminder.AndroidReminderScheduler
import com.taskchain.core.platform.AndroidTaskFeedback
import com.taskchain.reminder.ReminderScheduler
import com.taskchain.reminder.RunAlerts
import com.taskchain.reminder.RunNotification
import com.taskchain.core.platform.TaskFeedback
import com.taskchain.reminder.TimerFeedback
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/** Small application composition root that wires domain ports to local Android adapters. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dataDirectory = File(appContext.filesDir, "data")

    val routines: RoutineRepository = FileRoutineRepository(File(dataDirectory, "routines"), json)
    val activeRun: RoutineRunRepository = FileRoutineRunRepository(File(dataDirectory, "sessions/active-session.json"), json)
    val completions: CompletionRepository = FileCompletionRepository(File(dataDirectory, "history/events.json"), json)
    val preferences: UserPreferenceRepository = DataStoreUserPreferenceRepository(appContext)
    val builtInLibrary = BuiltInRoutineLibrary(appContext, json)
    val runEngine = RoutineRunEngine()
    val reminders: ReminderScheduler = AndroidReminderScheduler(appContext)

    /** Serializes every change to the active run between the runner ViewModel and notification actions. */
    val runMutex = Mutex()

    /** Counts changes made to the active run outside the runner ViewModel, so a live ViewModel reloads them. */
    val runRevision = MutableStateFlow(0L)
    val runNotification = RunNotification(appContext, runEngine)

    /** True while the Run screen is visible; run alerts then come from the screen, not from an alarm. */
    @Volatile var runnerVisible = false
    val timerFeedback = TimerFeedback(appContext)
    val taskFeedback: TaskFeedback = AndroidTaskFeedback(appContext)
    val homeDayStartHour = appContext.resources.getInteger(R.integer.home_day_start_hour)
    val taskNudgeIntervalMillis = appContext.resources
        .getInteger(R.integer.run_nudge_interval_seconds)
        .toLong() * 1000L
    val untimedReminderLimitMillis = appContext.resources
        .getInteger(R.integer.run_untimed_reminder_limit_minutes)
        .toLong() * 60_000L
    val runAlerts = RunAlerts(appContext, runEngine, taskNudgeIntervalMillis, untimedReminderLimitMillis)

    /** Use this function before UI loads so files saved before the task/subtask rename use current keys. */
    suspend fun migrateLegacyData() = withContext(Dispatchers.IO) { migrateLegacyDataFiles(dataDirectory, json) }

    /** Use this function before UI loads so an interrupted terminal run reaches append-only history. */
    suspend fun recoverTerminalRun() = recoverTerminalSession(activeRun, completions, runEngine)

    /**
     * Use this function before UI editing to validate stored records and recover interrupted completion.
     * Inputs: none. Dependencies: migration, routine/history/preference repositories, terminal recovery.
     */
    suspend fun initializeUserData() {
        migrateLegacyData()
        routines.observeAll().first()
        completions.observeAll().first()
        preferences.observe().first()
        recoverTerminalRun()
    }

    /** Use this function when creating a new persistent routine identity offline. */
    fun newRoutineId(): RoutineId = RoutineId(UUID.randomUUID().toString())

    /** Use this function when creating a new routine-task identity offline. */
    fun newTaskId(): RoutineTaskId = RoutineTaskId(UUID.randomUUID().toString())

    /** Use this function when starting a distinct routine execution offline. */
    fun newRunId(): RoutineRunId = RoutineRunId(UUID.randomUUID().toString())

    /** Use this function wherever domain state needs wall-clock epoch milliseconds. */
    fun now(): Long = System.currentTimeMillis()
}
