package com.taskchain.core.platform

import android.content.Intent

/** Provides Android entry-point handlers without exposing feature implementations to receiver adapters. */
interface PlatformEntryPointProvider {
    /** Use this property from a legacy Android receiver to dispatch its delivered intent. */
    val platformEntryPoints: PlatformEntryPoints
}

/** Holds the five suspend handlers used by the historical Android receiver identities. */
class PlatformEntryPoints(
    /** Handles a delivered routine reminder alarm. */
    val deliver: suspend (Intent) -> Unit,
    /** Handles a routine reminder notification action. */
    val action: suspend (Intent) -> Unit,
    /** Handles routine reminder restoration after a system event. */
    val restore: suspend (Intent) -> Unit,
    /** Handles a run notification action. */
    val runAction: suspend (Intent) -> Unit,
    /** Handles a background run alert alarm. */
    val runAlert: suspend (Intent) -> Unit,
)

/** Root-owned factories for platform operations that feature adapters must address. */
class PlatformIntents(
    /** Creates the normal app launch intent for notification content. */
    val openApp: () -> Intent,
    /** Creates the app launch intent that requests the active run screen. */
    val openActiveRun: () -> Intent,
    /** Creates the routine reminder delivery receiver target. */
    val reminderDelivery: () -> Intent,
    /** Creates the routine reminder action receiver target. */
    val reminderAction: () -> Intent,
    /** Creates the run notification action receiver target. */
    val runAction: () -> Intent,
    /** Creates the background run alert receiver target. */
    val runAlert: () -> Intent,
)
