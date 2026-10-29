package com.taskchain.features.execution.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.taskchain.R
import com.taskchain.core.model.RoutineRun
import com.taskchain.core.model.RoutineRunId
import com.taskchain.core.model.RoutineSubtaskId
import com.taskchain.core.platform.PlatformIntents
import com.taskchain.features.execution.ExecutionDependencies
import com.taskchain.features.execution.domain.RoutineRunEngine
import com.taskchain.features.execution.domain.RunNotificationAction
import com.taskchain.features.execution.domain.RunNotificationClock
import com.taskchain.features.execution.domain.RunNotificationContent
import com.taskchain.features.execution.domain.RunNotificationPolicy
import com.taskchain.features.execution.domain.RunNotificationTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/** Posts the ongoing Android notification for the active run and its two action buttons. */
class RunNotification(
    context: Context,
    private val engine: RoutineRunEngine,
    private val intents: PlatformIntents,
) {
    private val appContext = context.applicationContext
    private val notifications = NotificationManagerCompat.from(appContext)

    /** Use this function after a persisted run change; a missing or non-active run removes the notification. */
    fun sync(run: RoutineRun?, nowEpochMillis: Long) {
        show(run?.let { RunNotificationPolicy.content(it, engine, nowEpochMillis) })
    }

    /** Use this function to post or update the notification; null content removes it. */
    fun show(content: RunNotificationContent?) {
        if (content == null) {
            cancel()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel()
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(
                content.taskTitle?.let { appContext.getString(R.string.run_notification_task_and_routine, it, content.routineTitle) }
                    ?: content.routineTitle,
            )
            .setContentIntent(openActiveRun())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
        when (val clock = content.clock) {
            is RunNotificationClock.CountDown -> builder
                .setShowWhen(true)
                .setWhen(clock.endAtEpochMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
            is RunNotificationClock.CountUp -> builder
                .setShowWhen(true)
                .setWhen(clock.sinceEpochMillis)
                .setUsesChronometer(true)
            is RunNotificationClock.Paused -> builder.setSubText(pausedText(clock.remainingMillis))
            RunNotificationClock.Hidden -> Unit
        }
        builder
            .addAction(
                0,
                appContext.getString(R.string.run_notification_skip),
                if (content.skipNeedsApp) openActiveRun() else actionIntent(ACTION_SKIP, content.target),
            )
            .addAction(
                0,
                appContext.getString(R.string.run_notification_complete),
                if (content.completeNeedsApp) openActiveRun() else actionIntent(ACTION_COMPLETE, content.target),
            )
        try {
            notifications.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Notification permission may be revoked between the check and posting.
        }
    }

    /** Use this function when the run ends or no active run exists. */
    fun cancel() {
        notifications.cancel(NOTIFICATION_ID)
    }

    /** Use this function to describe a paused step; a negative allowance means it ran past its planned time. */
    private fun pausedText(remainingMillis: Long?): String {
        if (remainingMillis == null) return appContext.getString(R.string.run_notification_paused)
        val time = DateUtils.formatElapsedTime(abs(remainingMillis) / 1000)
        return appContext.getString(
            R.string.run_notification_paused_remaining,
            if (remainingMillis < 0) "-$time" else time,
        )
    }

    /** Use this function for a button that applies one transition without opening the app. */
    private fun actionIntent(action: String, target: RunNotificationTarget): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            if (action == ACTION_SKIP) REQUEST_SKIP else REQUEST_COMPLETE,
            intents.runAction()
                .setAction(action)
                .putExtra(EXTRA_RUN_ID, target.runId.value)
                .putExtra(EXTRA_TASK_INDEX, target.taskIndex)
                .putExtra(EXTRA_SUBTASK_ID, target.subtaskId?.value),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Use this function for the notification body and for buttons whose transition needs the app. */
    private fun openActiveRun(): PendingIntent = PendingIntent.getActivity(
        appContext,
        REQUEST_OPEN,
        intents.openActiveRun(),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Use this function before posting on Android 8 or newer; the run notification shows no launcher dot. */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.run_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = appContext.getString(R.string.run_notification_channel_description)
            setShowBadge(false)
        }
        runCatching { appContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel) }
    }

    companion object {
        const val CHANNEL_ID = "run_progress"
        const val NOTIFICATION_ID = 0x5444
        const val ACTION_SKIP = "com.taskchain.run.SKIP"
        const val ACTION_COMPLETE = "com.taskchain.run.COMPLETE"
        const val EXTRA_RUN_ID = "run_id"
        const val EXTRA_TASK_INDEX = "task_index"
        const val EXTRA_SUBTASK_ID = "subtask_id"
        private const val REQUEST_OPEN = 0x5445
        private const val REQUEST_SKIP = 0x5446
        private const val REQUEST_COMPLETE = 0x5447
    }
}

/** Applies a validated run notification action to the persisted run without opening the app. */
suspend fun handleRunAction(intent: Intent, dependencies: ExecutionDependencies) {
    val action = when (intent.action) {
        RunNotification.ACTION_SKIP -> RunNotificationAction.SKIP
        RunNotification.ACTION_COMPLETE -> RunNotificationAction.COMPLETE
        else -> return
    }
    val target = RunNotificationTarget(
        runId = RoutineRunId(intent.getStringExtra(RunNotification.EXTRA_RUN_ID) ?: return),
        taskIndex = intent.getIntExtra(RunNotification.EXTRA_TASK_INDEX, -1),
        subtaskId = intent.getStringExtra(RunNotification.EXTRA_SUBTASK_ID)?.let(::RoutineSubtaskId),
    )
    try {
        dependencies.runMutex.withLock {
            val run = dependencies.activeRun.observeActive().first()
            val now = dependencies.now()
            val updated = run?.let {
                RunNotificationPolicy.applyAction(it, dependencies.runEngine, action, target, now)
            }
            if (updated != null) {
                dependencies.activeRun.saveActive(updated)
                dependencies.runRevision.update { it + 1 }
            }
            dependencies.runNotification.sync(updated ?: run, now)
            dependencies.runAlerts.sync(updated ?: run, now, dependencies.runnerVisible)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        // A damaged or concurrently replaced run file must not crash a notification action.
    }
}
