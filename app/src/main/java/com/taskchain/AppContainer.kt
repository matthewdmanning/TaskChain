package com.taskchain

import android.content.Context
import android.content.Intent
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
import com.taskchain.core.model.RoutineId
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineTaskId
import com.taskchain.core.platform.AndroidTaskFeedback
import com.taskchain.core.platform.PlatformEntryPoints
import com.taskchain.core.platform.PlatformIntents
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.core.platform.TaskFeedback
import com.taskchain.core.platform.TimerFeedback
import com.taskchain.features.authoring.AuthoringDependencies
import com.taskchain.features.keepscreenon.KeepScreenOnDependencies
import com.taskchain.features.notifications.NotificationDependencies
import com.taskchain.features.progress.ProgressDependencies
import com.taskchain.features.reminders.AndroidReminderScheduler
import com.taskchain.features.reminders.ReminderHandlers
import com.taskchain.features.running.RunEngine
import com.taskchain.features.running.RunningDependencies
import com.taskchain.features.running.recoverTerminalSession
import com.taskchain.features.scheduling.SchedulingDependencies
import com.taskchain.features.settings.SettingsDependencies
import com.taskchain.reminder.ReminderActionReceiver
import com.taskchain.reminder.ReminderReceiver
import com.taskchain.reminder.RunActionReceiver
import com.taskchain.reminder.RunAlertReceiver
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Small application composition root that wires Core adapters into each feature's public entry.
 * Public members are what root navigation, startup and the receiver adapters need; features receive
 * only their bounded dependency objects.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val dataDirectory = File(appContext.filesDir, "data")

    val routines: RoutineRepository = FileRoutineRepository(File(dataDirectory, "routines"), json)
    private val activeRun: RoutineRunRepository =
        FileRoutineRunRepository(File(dataDirectory, "sessions/active-session.json"), json)
    val completions: CompletionRepository = FileCompletionRepository(File(dataDirectory, "history/events.json"), json)
    val preferences: UserPreferenceRepository = DataStoreUserPreferenceRepository(appContext)
    val builtInLibrary = BuiltInRoutineLibrary(appContext, json)
    private val runEngine = RunEngine()
    private val platformIntents = PlatformIntents(
        openApp = { Intent(appContext, MainActivity::class.java) },
        openActiveRun = {
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_ACTIVE_RUN, true)
            }
        },
        reminderDelivery = { Intent(appContext, ReminderReceiver::class.java) },
        reminderAction = { Intent(appContext, ReminderActionReceiver::class.java) },
        runAction = { Intent(appContext, RunActionReceiver::class.java) },
        runAlert = { Intent(appContext, RunAlertReceiver::class.java) },
    )
    private val runMutex = Mutex()
    private val runRevision = MutableStateFlow(0L)
    private val timerFeedback = TimerFeedback(appContext)
    private val taskFeedback: TaskFeedback = AndroidTaskFeedback(appContext)
    private val homeDayStartHour = appContext.resources.getInteger(R.integer.home_day_start_hour)
    private val taskNudgeIntervalMillis = appContext.resources
        .getInteger(R.integer.run_nudge_interval_seconds)
        .toLong() * 1000L
    private val untimedReminderLimitMillis = appContext.resources
        .getInteger(R.integer.run_untimed_reminder_limit_minutes)
        .toLong() * 60_000L

    /** Bounded background notification runtime; reads runner visibility from [running] when an alert fires. */
    private val notifications: NotificationDependencies = NotificationDependencies(
        context = appContext,
        intents = platformIntents,
        activeRun = activeRun,
        preferences = preferences,
        runRules = notificationRunRules(runEngine),
        runMutex = runMutex,
        runRevision = runRevision,
        isRunnerVisible = { running.runnerVisible },
        timerFeedback = timerFeedback,
        taskFeedback = taskFeedback,
        nudgeIntervalMillis = taskNudgeIntervalMillis,
        untimedIdleLimitMillis = untimedReminderLimitMillis,
        now = ::now,
    )
    private val reminders: ReminderScheduler =
        AndroidReminderScheduler(appContext, platformIntents, notifications.reminderNotification::cancel)

    /** Bounded Running runtime shared by the runner UI and Android background entry points. */
    val running: RunningDependencies = RunningDependencies(
        routines = routines,
        activeRun = activeRun,
        completions = completions,
        preferences = preferences,
        builtInLibrary = builtInLibrary,
        runEngine = runEngine,
        reminders = reminders,
        runMutex = runMutex,
        runRevision = runRevision,
        runnerVisible = false,
        timerFeedback = timerFeedback,
        taskFeedback = taskFeedback,
        syncRunNotification = notifications.runNotification::sync,
        syncRunAlerts = notifications.runAlerts::sync,
        remindersExhausted = notifications.runAlerts::remindersExhausted,
        dayStartHour = homeDayStartHour,
        taskNudgeIntervalMillis = taskNudgeIntervalMillis,
        newRunId = ::newRunId,
        now = ::now,
        recoverTerminalRun = { recoverTerminalSession(activeRun, completions, runEngine) },
    )

    /** Routine builder inputs and ViewModel factory. */
    val authoring = AuthoringDependencies(
        routines = routines,
        loadBuiltIns = builtInLibrary::load,
        reminders = reminders,
        now = ::now,
        newRoutineId = ::newRoutineId,
        newTaskId = ::newTaskId,
    )

    /** Today list inputs and ViewModel factory. */
    val scheduling = SchedulingDependencies(
        routines = routines,
        activeRun = activeRun,
        completions = completions,
        builtInLibrary = builtInLibrary,
        reminders = reminders,
        now = ::now,
        dayStartHour = homeDayStartHour,
        newRunId = ::newRunId,
    )

    /** Progress tab inputs and ViewModel factory. */
    val progress = ProgressDependencies(completions = completions, now = ::now, dayStartHour = homeDayStartHour)

    /** Settings tab ViewModel factory. */
    val settings = SettingsDependencies(preferences)

    /** Keep-screen-on Settings row ViewModel factory. */
    val keepScreenOn = KeepScreenOnDependencies(preferences)

    private val reminderHandlers = ReminderHandlers(
        context = appContext,
        routines = routines,
        activeRun = activeRun,
        completions = completions,
        reminders = reminders,
        intents = platformIntents,
        timerFeedback = timerFeedback,
        showDelivered = { showReminderNotification(appContext, platformIntents, notifications, it) },
    )

    /** Root-owned handlers dispatched by the historical Android receiver identities. */
    val platformEntryPoints = PlatformEntryPoints(
        deliver = reminderHandlers::deliver,
        action = reminderHandlers::action,
        restore = reminderHandlers::restore,
        runAction = notifications::onRunAction,
        runAlert = notifications::onRunAlert,
    )

    /** Use this function before UI loads so files saved before the task/subtask rename use current keys. */
    suspend fun migrateLegacyData() = withContext(Dispatchers.IO) { migrateLegacyDataFiles(dataDirectory, json) }

    /** Use this function when creating a new persistent routine identity offline. */
    private fun newRoutineId(): RoutineId = RoutineId(UUID.randomUUID().toString())

    /** Use this function when creating a new routine-task identity offline. */
    private fun newTaskId(): RoutineTaskId = RoutineTaskId(UUID.randomUUID().toString())

    /** Use this function when starting a distinct routine run offline. */
    private fun newRunId(): RoutineRunId = RoutineRunId(UUID.randomUUID().toString())

    /** Use this function wherever domain state needs wall-clock epoch milliseconds. */
    fun now(): Long = System.currentTimeMillis()
}
