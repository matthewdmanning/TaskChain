package com.taskchain.features.notifications.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.SoundToken
import com.taskchain.core.model.withTaskSound
import com.taskchain.core.platform.PlatformIntents
import com.taskchain.core.platform.setAlarm
import com.taskchain.features.notifications.NotificationDependencies
import com.taskchain.features.notifications.domain.NotificationRunRules
import com.taskchain.features.notifications.domain.RunAlertPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.withLock

/** Keeps one background alarm for the next run nudge or timer expiry. */
class RunAlerts(
    context: Context,
    private val engine: NotificationRunRules,
    private val intents: PlatformIntents,
    private val nudgeIntervalMillis: Long,
    private val untimedIdleLimitMillis: Long,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    /** Use this function after a persisted run change, and when the Run screen appears or disappears. */
    fun sync(run: RoutineRun?, nowEpochMillis: Long, runnerVisible: Boolean) {
        val at = if (run == null || runnerVisible) {
            null
        } else {
            RunAlertPolicy.nextAlertAt(run, engine, nowEpochMillis, nudgeIntervalMillis, untimedIdleLimitMillis)
        }
        if (at == null) {
            cancel()
        } else {
            runCatching { setAlarm(alarmManager, at, alarmIntent()) }
        }
    }

    /** Use this function when no alert can occur. */
    fun cancel() {
        alarmManager.cancel(alarmIntent())
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext,
        REQUEST_ALERT,
        intents.runAlert().setAction(ACTION_ALERT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_ALERT = "com.taskchain.run.ALERT"
        private const val REQUEST_ALERT = 0x5448
    }
}

/** Handles a background run alert and re-arms the next one under the shared run lock. */
suspend fun handleRunAlert(intent: Intent, dependencies: NotificationDependencies) {
    if (intent.action != RunAlerts.ACTION_ALERT) return
    var played = false
    try {
        val intensity = dependencies.preferences.observe().first().vibrationIntensity
        // The runner ViewModel shares this lock, so an alert is acknowledged and played exactly once.
        dependencies.runMutex.withLock {
            val run = dependencies.activeRun.observeActive().first()
            val now = dependencies.now()
            if (run == null || dependencies.runnerVisible) {
                dependencies.runAlerts.sync(run, now, dependencies.runnerVisible)
                return@withLock
            }
            val due = RunAlertPolicy.takeDue(
                run,
                dependencies.runRules,
                now,
                dependencies.taskNudgeIntervalMillis,
                dependencies.untimedReminderLimitMillis,
            )
            if (due.run != run) {
                dependencies.activeRun.saveActive(due.run)
                dependencies.runRevision.update { it + 1 }
            }
            val task = due.run.tasks[due.run.currentTaskIndex].source
            val sounds = due.run.soundSettings.withTaskSound(task.soundUri)
            if (due.nudge) {
                dependencies.taskFeedback.fire(
                    token = SoundToken.TaskNudge,
                    soundEnabled = due.run.routineSoundEnabled && task.soundEnabled && sounds[SoundToken.TaskNudge].enabled,
                    vibrateEnabled = due.run.routineVibrateEnabled && task.vibrateEnabled,
                    soundSettings = sounds,
                    intensity = intensity,
                )
                played = true
            }
            if (due.timerExpired) {
                dependencies.timerFeedback.fire(
                    soundEnabled = due.run.routineSoundEnabled && task.soundEnabled,
                    vibrateEnabled = due.run.routineVibrateEnabled && task.vibrateEnabled,
                    soundSettings = sounds,
                    intensity = intensity,
                )
                played = true
            }
            dependencies.runAlerts.sync(due.run, now, runnerVisible = false)
        }
        // Let asynchronous audio start before the broadcast ends.
        if (played) delay(PLAYBACK_GRACE_MILLIS)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        // A damaged or concurrently replaced run file must not crash a system broadcast.
    }
}

private const val PLAYBACK_GRACE_MILLIS = 3_000L
