package com.taskchain.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskchain.TaskChainApplication
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.SoundToken
import com.taskchain.core.model.withTaskSound
import com.taskchain.features.execution.domain.RoutineRunEngine
import com.taskchain.features.execution.domain.RunAlertPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/**
 * Keeps one alarm for the next run alert (periodic reminder or timer expiry) while the Run screen is not visible,
 * so sound and haptics continue with the app in the background or the screen off or locked.
 * Timing comes from [RunAlertPolicy]; this class only maps it to [AlarmManager].
 */
class RunAlerts(
    context: Context,
    private val engine: RoutineRunEngine,
    private val nudgeIntervalMillis: Long,
    private val untimedIdleLimitMillis: Long,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    /**
     * Use this function after any persisted run change, and when the Run screen appears or disappears.
     * The alarm exists only while the Run screen is hidden and an alert can still occur.
     */
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
        Intent(appContext, RunAlertReceiver::class.java).setAction(ACTION_ALERT),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val ACTION_ALERT = "com.taskchain.run.ALERT"
        private const val REQUEST_ALERT = 0x5448
    }
}

/** Plays due run alerts when the alarm fires without the Run screen, then arms the next alarm. */
class RunAlertReceiver : BroadcastReceiver() {
    /** Use this function when the run alert alarm fires. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RunAlerts.ACTION_ALERT) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var played = false
            try {
                val container = (context.applicationContext as TaskChainApplication).container
                val intensity = container.preferences.observe().first().vibrationIntensity
                // The runner ViewModel shares this lock, so an alert is acknowledged and played exactly once.
                container.runMutex.withLock {
                    val run = container.activeRun.observeActive().first()
                    val now = container.now()
                    if (run == null || container.runnerVisible) {
                        container.runAlerts.sync(run, now, container.runnerVisible)
                        return@withLock
                    }
                    val due = RunAlertPolicy.takeDue(
                        run,
                        container.runEngine,
                        now,
                        container.taskNudgeIntervalMillis,
                        container.untimedReminderLimitMillis,
                    )
                    if (due.run != run) {
                        container.activeRun.saveActive(due.run)
                        container.runRevision.update { it + 1 }
                    }
                    val task = due.run.tasks[due.run.currentTaskIndex].source
                    val sounds = due.run.soundSettings.withTaskSound(task.soundUri)
                    if (due.nudge) {
                        container.taskFeedback.fire(
                            token = SoundToken.TaskNudge,
                            soundEnabled = due.run.routineSoundEnabled && task.soundEnabled && sounds[SoundToken.TaskNudge].enabled,
                            vibrateEnabled = due.run.routineVibrateEnabled && task.vibrateEnabled,
                            soundSettings = sounds,
                            intensity = intensity,
                        )
                        played = true
                    }
                    if (due.timerExpired) {
                        container.timerFeedback.fire(
                            soundEnabled = due.run.routineSoundEnabled && task.soundEnabled,
                            vibrateEnabled = due.run.routineVibrateEnabled && task.vibrateEnabled,
                            soundSettings = sounds,
                            intensity = intensity,
                        )
                        played = true
                    }
                    container.runAlerts.sync(due.run, now, runnerVisible = false)
                }
                // Let asynchronous audio start before the broadcast ends.
                if (played) delay(PLAYBACK_GRACE_MILLIS)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A damaged or concurrently replaced run file must not crash a system broadcast.
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val PLAYBACK_GRACE_MILLIS = 3_000L
    }
}
