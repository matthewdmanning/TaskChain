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
import com.taskchain.core.platform.PlatformEntryPoints
import com.taskchain.core.platform.PlatformIntents
import com.taskchain.core.platform.ReminderScheduler
import com.taskchain.features.execution.domain.RoutineRunEngine
import com.taskchain.core.platform.TaskFeedback
import com.taskchain.core.platform.TimerFeedback
import com.taskchain.features.notifications.platform.ReminderNotification
import com.taskchain.features.notifications.NotificationDependencies
import com.taskchain.features.notifications.domain.RunAlertPolicy
import com.taskchain.features.execution.ExecutionDependencies
import com.taskchain.features.notifications.platform.RunAlerts
import com.taskchain.features.notifications.platform.RunNotification
import com.taskchain.features.notifications.platform.handleRunAction
import com.taskchain.features.notifications.platform.handleRunAlert
import com.taskchain.features.reminders.AndroidReminderScheduler
import com.taskchain.features.reminders.ReminderHandlers
import com.taskchain.reminder.ReminderReceiver
import com.taskchain.reminder.ReminderActionReceiver
import com.taskchain.reminder.RunActionReceiver
import com.taskchain.reminder.RunAlertReceiver
import com.taskchain.core.platform.AndroidTaskFeedback
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import com.taskchain.features.execution.recoverTerminalSession

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
    private val reminderNotification = ReminderNotification(appContext)
    val reminders: ReminderScheduler = AndroidReminderScheduler(appContext, platformIntents, reminderNotification::cancel)
    private val runMutex = Mutex()
    private val runRevision = MutableStateFlow(0L)
    private val notificationRules = notificationRunRules(runEngine)
    private val runNotification = RunNotification(appContext, notificationRules, platformIntents)
    private val timerFeedback = TimerFeedback(appContext)
    private val taskFeedback: TaskFeedback = AndroidTaskFeedback(appContext)
    val homeDayStartHour = appContext.resources.getInteger(R.integer.home_day_start_hour)
    private val taskNudgeIntervalMillis = appContext.resources
        .getInteger(R.integer.run_nudge_interval_seconds)
        .toLong() * 1000L
    private val untimedReminderLimitMillis = appContext.resources
        .getInteger(R.integer.run_untimed_reminder_limit_minutes)
        .toLong() * 60_000L
    private val runAlerts = RunAlerts(
        appContext,
        notificationRules,
        platformIntents,
        taskNudgeIntervalMillis,
        untimedReminderLimitMillis,
    )

    /** Bounded execution runtime shared by the runner UI and Android background entry points. */
    val execution = ExecutionDependencies(
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
        syncRunNotification = runNotification::sync,
        syncRunAlerts = runAlerts::sync,
        remindersExhausted = { run, now ->
            RunAlertPolicy.remindersExhausted(run, notificationRules, now, untimedReminderLimitMillis)
        },
        taskNudgeIntervalMillis = taskNudgeIntervalMillis,
        newRunId = ::newRunId,
        now = ::now,
        recoverTerminalRun = { recoverTerminalSession(activeRun, completions, runEngine) },
    )

    private val notifications = NotificationDependencies(
        activeRun = activeRun,
        preferences = preferences,
        runRules = notificationRules,
        runMutex = runMutex,
        runRevision = runRevision,
        isRunnerVisible = { execution.runnerVisible },
        timerFeedback = timerFeedback,
        taskFeedback = taskFeedback,
        runNotification = runNotification,
        runAlerts = runAlerts,
        taskNudgeIntervalMillis = taskNudgeIntervalMillis,
        untimedReminderLimitMillis = untimedReminderLimitMillis,
        now = ::now,
    )

    private val reminderHandlers = ReminderHandlers(
        context = appContext,
        routines = routines,
        activeRun = activeRun,
        completions = completions,
        reminders = reminders,
        intents = platformIntents,
        timerFeedback = timerFeedback,
        showDelivered = { showReminderNotification(appContext, platformIntents, reminderNotification, it) },
    )

    /** Root-owned handlers dispatched by the historical Android receiver identities. */
    val platformEntryPoints = PlatformEntryPoints(
        deliver = reminderHandlers::deliver,
        action = reminderHandlers::action,
        restore = reminderHandlers::restore,
        runAction = { intent -> handleRunAction(intent, notifications) },
        runAlert = { intent -> handleRunAlert(intent, notifications) },
    )

    /** Use this function before UI loads so files saved before the task/subtask rename use current keys. */
    suspend fun migrateLegacyData() = withContext(Dispatchers.IO) { migrateLegacyDataFiles(dataDirectory, json) }

    /** Use this function when creating a new persistent routine identity offline. */
    fun newRoutineId(): RoutineId = RoutineId(UUID.randomUUID().toString())

    /** Use this function when creating a new routine-task identity offline. */
    fun newTaskId(): RoutineTaskId = RoutineTaskId(UUID.randomUUID().toString())

    /** Use this function when starting a distinct routine execution offline. */
    fun newRunId(): RoutineRunId = RoutineRunId(UUID.randomUUID().toString())

    /** Use this function wherever domain state needs wall-clock epoch milliseconds. */
    fun now(): Long = System.currentTimeMillis()
}
