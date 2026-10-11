package com.taskchain

import android.app.Application

/** Owns the shared AppContainer and its Android adapters for the lifetime of the app process. */
class TaskChainApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
good