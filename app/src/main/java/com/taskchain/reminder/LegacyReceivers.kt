package com.taskchain.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.taskchain.core.platform.PlatformEntryPointProvider
import com.taskchain.features.notifications.NotificationIntents
import com.taskchain.features.reminders.ReminderActions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Historical Android receiver identities retained for already-created alarms and pending intents. */
class ReminderReceiver : BroadcastReceiver() {
    /** Use this function when Android delivers a scheduled local routine alarm. */
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                platformEntryPoints(context).deliver(intent)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Handles the Snooze and Dismiss buttons on a delivered routine reminder notification. */
class ReminderActionReceiver : BroadcastReceiver() {
    /** Use this function when the user presses Snooze or Dismiss on a reminder notification. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE && intent.action != ACTION_DISMISS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                platformEntryPoints(context).action(intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = ReminderActions.SNOOZE
        const val ACTION_DISMISS = ReminderActions.DISMISS
    }
}

/** Restores saved routine alarms after Android clears alarms during reboot or local-time changes. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    /** Use this function when Android reports a reboot or time-zone change requiring alarm restoration. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESTORE_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                try {
                    platformEntryPoints(context).restore(intent)
                } catch (_: Exception) {
                    // A damaged or concurrently replaced local routine file must not crash a system broadcast.
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val RESTORE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

/** Applies the run notification's Skip and Complete buttons without changing the historical component name. */
class RunActionReceiver : BroadcastReceiver() {
    /** Use this function when the user presses Skip or Complete on the run notification. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationIntents.RUN_ACTION_SKIP && intent.action != NotificationIntents.RUN_ACTION_COMPLETE) return
        if (intent.getStringExtra(NotificationIntents.RUN_EXTRA_RUN_ID) == null) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                try {
                    platformEntryPoints(context).runAction(intent)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // A damaged or concurrently replaced run file must not crash a notification action.
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Plays due run alerts while the Run screen is hidden, retaining the historical component name. */
class RunAlertReceiver : BroadcastReceiver() {
    /** Use this function when the run alert alarm fires. */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationIntents.RUN_ALERT) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                try {
                    platformEntryPoints(context).runAlert(intent)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // A damaged or concurrently replaced run file must not crash a system broadcast.
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Use this function to resolve application-provided handlers without importing the concrete application class. */
private fun platformEntryPoints(context: Context) =
    (context.applicationContext as PlatformEntryPointProvider).platformEntryPoints
