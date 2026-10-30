package com.taskchain

import android.app.Application
import com.taskchain.core.platform.PlatformEntryPointProvider
import com.taskchain.core.platform.PlatformEntryPoints

/** Owns the shared AppContainer and its Android adapters for the lifetime of the app process. */
class TaskChainApplication : Application(), PlatformEntryPointProvider {
    val container: AppContainer by lazy { AppContainer(this) }

    /** Provides the root-wired handlers to legacy Android receiver identities. */
    override val platformEntryPoints: PlatformEntryPoints
        get() = container.platformEntryPoints
}
