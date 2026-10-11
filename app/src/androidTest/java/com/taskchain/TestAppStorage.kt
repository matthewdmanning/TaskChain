package com.taskchain

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Use this function to discard only the isolated instrumentation application's test-owned state.
 * The package guard makes persisted routines in this disposable sandbox test-owned while preserving built-in IDs.
 * Inputs: target application context. Dependencies: testing package suffix, AppContainer repositories, and built-in routine asset.
 */
internal fun resetTestAppState(context: Context) {
    check(context.packageName == "com.taskchain.testapp") {
        "Instrumentation state cleanup requires the isolated testing package: ${context.packageName}"
    }
    val container = (context.applicationContext as TaskChainApplication).container
    runBlocking {
        val builtInIds = container.builtInLibrary.load(container.now()).map { it.id }.toSet()
        container.routines.observeAll().first()
            .filterNot { it.id in builtInIds }
            .forEach { container.routines.delete(it.id) }
        container.activeRun.clearActive()
    }
}

/**
 * Use this function to resolve the process-shared test container after verifying package isolation.
 * Inputs: target application context. Dependencies: TaskChainApplication and testing application ID suffix.
 */
internal fun testAppContainer(context: Context): AppContainer {
    check(context.packageName == "com.taskchain.testapp") {
        "Instrumentation state mutation requires the isolated testing package: ${context.packageName}"
    }
    return (context.applicationContext as TaskChainApplication).container
}
