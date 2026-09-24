package com.taskchain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.taskchain.ui.TaskChainApp
import kotlinx.coroutines.launch

/** Hosts the single Compose activity and the local application composition root. */
class MainActivity : ComponentActivity() {
    private val container by lazy { AppContainer(this) }

    /** Use this function when Android creates or recreates the app activity. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            container.recoverTerminalRun()
            setContent { TaskChainApp(container) }
        }
    }
}
