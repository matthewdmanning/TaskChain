package com.taskchain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.taskchain.ui.TaskChainApp
import com.taskchain.ui.StartupScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Hosts the single Compose activity and the local application composition root. */
class MainActivity : ComponentActivity() {
    private val container get() = (application as TaskChainApplication).container
    private var startupJob: Job? = null
    private var startupReady by mutableStateOf(false)
    private var startupFailed by mutableStateOf(false)

    /** Use this function when Android creates or recreates the app activity. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            if (startupReady) {
                TaskChainApp(container, openActiveRun = intent.getBooleanExtra(EXTRA_OPEN_ACTIVE_RUN, false))
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

    companion object {
        /** Intent extra that asks the app to open the persisted active run, for example from the run notification. */
        const val EXTRA_OPEN_ACTIVE_RUN = "open_active_run"
    }
}
