package com.taskchain.features.notifications

import com.taskchain.core.data.RoutineRunRepository
import com.taskchain.core.data.UserPreferenceRepository
import com.taskchain.core.platform.TaskFeedback
import com.taskchain.core.platform.TimerFeedback
import com.taskchain.features.notifications.domain.NotificationRunRules
import com.taskchain.features.notifications.platform.RunAlerts
import com.taskchain.features.notifications.platform.RunNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Bounded background notification runtime; lock and revision are shared with the runner by root wiring.
 * Background alert cadence and reminder limit belong to [runAlerts].
 */
class NotificationDependencies(
    val activeRun: RoutineRunRepository,
    val preferences: UserPreferenceRepository,
    val runRules: NotificationRunRules,
    val runMutex: Mutex,
    val runRevision: MutableStateFlow<Long>,
    private val isRunnerVisible: () -> Boolean,
    val timerFeedback: TimerFeedback,
    val taskFeedback: TaskFeedback,
    val runNotification: RunNotification,
    val runAlerts: RunAlerts,
    val now: () -> Long,
) {
    val runnerVisible: Boolean get() = isRunnerVisible()
}
