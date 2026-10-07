package com.taskchain.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person as CompatPerson
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.taskchain.MainActivity
import com.taskchain.R
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStatus

/** Posts the native Android bubble for an active run when the user and platform allow it. */
class RunBubble(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)
    private val notifications = NotificationManagerCompat.from(appContext)

    /**
     * Use this function before rendering the bubble setting or posting a bubble. It checks the
     * Android version, notification permission, app notification state, global bubble state, and
     * the dedicated channel's user-controlled bubble state.
     */
    fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        createChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!notifications.areNotificationsEnabled() || notificationManager?.areBubblesAllowed() != true) return false
        return notificationManager?.getNotificationChannel(CHANNEL_ID)?.canBubble() == true
    }

    /**
     * Use this function when the activity leaves the foreground. It posts one silent ongoing
     * bubble for [run], using the active-run channel, shortcut, conversation person, and native
     * Android permission checks; denied settings are handled as a no-op.
     */
    fun show(run: RoutineRun) {
        if (run.status != RunStatus.ACTIVE || !isAvailable()) return
        createShortcut(run.routineTitle)
        val openApp = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_ACTIVE_RUN, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentMutability(),
        )
        val currentStep = run.steps.getOrNull(run.currentStepIndex)?.source?.title
        val conversationPerson = CompatPerson.Builder()
            .setName(run.routineTitle)
            .setKey(SHORTCUT_ID)
            .setBot(true)
            .build()
        val bubbleIcon = IconCompat.createWithResource(appContext, R.drawable.ic_notification)
        val bubble = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            NotificationCompat.BubbleMetadata.Builder(SHORTCUT_ID)
                .setAutoExpandBubble(false)
                .setSuppressNotification(false)
                .build()
        } else {
            NotificationCompat.BubbleMetadata.Builder(openApp, bubbleIcon)
                .setAutoExpandBubble(false)
                .setSuppressNotification(false)
                .build()
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(run.routineTitle)
            .setContentText(currentStep ?: appContext.getString(R.string.settings_punch_bubble_content))
            .setContentIntent(openApp)
            .setShortcutId(SHORTCUT_ID)
            .setBubbleMetadata(bubble)
            .setStyle(
                NotificationCompat.MessagingStyle(conversationPerson)
                    .setConversationTitle(run.routineTitle)
                    .addMessage(
                        currentStep ?: appContext.getString(R.string.settings_punch_bubble_content),
                        System.currentTimeMillis(),
                        conversationPerson,
                    ),
            )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try {
            notifications.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission may be revoked between the availability check and posting.
        }
    }

    /** Use this function when the activity returns to foreground; [notifications] owns the bubble. */
    fun cancel() {
        notifications.cancel(NOTIFICATION_ID)
    }

    /** Use this function to route a settings affordance to Android's app bubble controls. */
    fun settingsIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bubbleSettings = Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            if (appContext.packageManager.resolveActivity(bubbleSettings, 0) != null) return bubbleSettings
        }
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
    }

    /** Use this function when Android reports the active-run channel itself has bubbles disabled. */
    fun channelSettingsIntent(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
    } else {
        settingsIntent()
    }

    /** Use this function before posting a bubble to create its user-controlled channel. */
    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.settings_punch_bubble_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = appContext.getString(R.string.settings_punch_bubble_channel_description)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setAllowBubbles(true)
        }
        runCatching { notificationManager?.createNotificationChannel(channel) }
    }

    /** Use this function before posting a bubble to register its long-lived conversation shortcut. */
    private fun createShortcut(routineTitle: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        runCatching {
            val shortcutManager = appContext.getSystemService(ShortcutManager::class.java) ?: return@runCatching
            val builder = ShortcutInfo.Builder(appContext, SHORTCUT_ID)
                .setShortLabel(appContext.getString(R.string.settings_punch_bubble_short_label))
                .setLongLabel(appContext.getString(R.string.settings_punch_bubble_long_label))
                .setIcon(Icon.createWithResource(appContext, R.drawable.ic_notification))
                .setCategories(setOf(ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION))
                .setIntent(
                    Intent(appContext, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(EXTRA_OPEN_ACTIVE_RUN, true)
                    },
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setPerson(
                    android.app.Person.Builder()
                        .setName(routineTitle)
                        .setKey(SHORTCUT_ID)
                        .setBot(true)
                        .build(),
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) builder.setLongLived(true)
            shortcutManager.addDynamicShortcuts(listOf(builder.build()))
        }
    }

    /** Use this function to satisfy Android 12's mutable bubble launch requirement safely. */
    private fun pendingIntentMutability(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_IMMUTABLE

    companion object {
        const val CHANNEL_ID = "active_run_bubble"
        const val SHORTCUT_ID = "active_run"
        const val NOTIFICATION_ID = 0x5443
        const val REQUEST_CODE = 0x5443
        const val EXTRA_OPEN_ACTIVE_RUN = "open_active_run"
    }
}
