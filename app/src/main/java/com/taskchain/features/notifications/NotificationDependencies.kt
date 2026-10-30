package com.taskchain.features.notifications

import android.content.Context
import android.content.Intent
import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.core.platform.PlatformIntents
import com.taskchain.core.platform.TaskFeedback
import com.taskchain.core.platform.TimerFeedback
import com.taskchain.features.notifications.platform.ReminderNotification
import com.taskchain.features.notifications.platform.RunAlerts
import com.taskchain.features.notifications.platform.RunNotification
import com.taskchain.features.notifications.platform.handleRunAction
import com.taskchain.features.notifications.platform.handleRunAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Public entry of the Notifications feature: the bounded background notification runtime root wires once.
 * It builds the feature's Android adapters from [context], [runRules] and [intents]; lock and revision are
 * shared with the runner by root wiring. Background alert cadence and reminder limit belong to [runAlerts].
 */
class NotificationDependencies(
    context: Context,
    intents: PlatformIntents,
    val activeRun: RoutineRunRepository,
    val preferences: UserPreferenceRepository,
    val runRules: NotificationRunRules,
    val runMutex: Mutex,
    val runRevision: MutableStateFlow<Long>,
    private val isRunnerVisible: () -> Boolean,
    val timerFeedback: TimerFeedback,
    val taskFeedback: TaskFeedback,
    nudgeIntervalMillis: Long,
    untimedIdleLimitMillis: Long,
    val now: () -> Long,
) {
    /** Ongoing run notification; root binds its `sync` into Running. */
    val runNotification = RunNotification(context, runRules, intents)

    /** Background run alarm; root binds its `sync` and `remindersExhausted` into Running. */
    val runAlerts = RunAlerts(context, runRules, intents, nudgeIntervalMillis, untimedIdleLimitMillis)

    /** Routine reminder shade presentation; root binds it to Reminders delivery and cancellation. */
    val reminderNotification = ReminderNotification(context)

    val runnerVisible: Boolean get() = isRunnerVisible()

    /** Use this function from the root run-action receiver adapter for the notification's Skip and Complete. */
    suspend fun onRunAction(intent: Intent): Unit = handleRunAction(intent, this)

    /** Use this function from the root run-alert receiver adapter when the background alert alarm fires. */
    suspend fun onRunAlert(intent: Intent): Unit = handleRunAlert(intent, this)
}

/** Intent identities the root receiver adapters filter on before dispatching; values are the historical ones. */
object NotificationIntents {
    const val RUN_ACTION_SKIP = RunNotification.ACTION_SKIP
    const val RUN_ACTION_COMPLETE = RunNotification.ACTION_COMPLETE
    const val RUN_EXTRA_RUN_ID = RunNotification.EXTRA_RUN_ID
    const val RUN_ALERT = RunAlerts.ACTION_ALERT
}
