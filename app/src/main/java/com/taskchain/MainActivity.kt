package com.taskchain

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.taskchain.domain.model.RunStatus
import com.taskchain.reminder.RunBubble
import com.taskchain.ui.TaskChainApp
import com.taskchain.ui.StartupScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Hosts the single Compose activity and the local application composition root. */
class MainActivity : ComponentActivity() {
    private val container get() = (application as TaskChainApplication).container
    private val runBubble by lazy { RunBubble(this) }
    private var bubbleJob: Job? = null
    private var startupJob: Job? = null
    private var startupReady by mutableStateOf(false)
    private var startupFailed by mutableStateOf(false)

    /** Use this function when Android creates or recreates the app activity. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            if (startupReady) {
                TaskChainApp(container, openActiveRun = intent.getBooleanExtra(RunBubble.EXTRA_OPEN_ACTIVE_RUN, false))
            } else {
                StartupScreen(failed = startupFailed, onRetry = ::loadUserData)
            }
        }
        loadUserData()
    }

    /**
     * Use this function on startup or Retry to open saved data before exposing editable screens.
     * Inputs: none. Dependencies: AppContainer.initializeUserData, startup state, lifecycleScope.
     */
    private fun loadUserData() {
        if (startupJob?.isActive == true) return
        startupFailed = false
        startupJob = lifecycleScope.launch {
            try {
                container.initializeUserData()
                startupReady = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.e("TaskChain", "Unable to open saved data", error)
                startupFailed = true
            }
        }
    }

    /** Removes the minimize bubble before the normal Home resume flow is shown. */
    override fun onStart() {
        super.onStart()
        bubbleJob?.cancel()
        if (!launchedFromBubble()) runBubble.cancel()
    }

    /** Offers the native bubble only for an opted-in active run after the activity leaves foreground. */
    override fun onStop() {
        super.onStop()
        if (!startupReady || isChangingConfigurations || launchedFromBubble()) return
        bubbleJob = lifecycleScope.launch {
            val preferences = container.preferences.observe().first()
            if (!preferences.bubbleOnMinimize) return@launch
            container.activeRun.observeActive().first()
                ?.takeIf { it.status == RunStatus.ACTIVE }
                ?.let(runBubble::show)
        }
    }

    /** Use this function to preserve the native bubble while its activity is expanded. */
    private fun launchedFromBubble(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isLaunchedFromBubble
}
