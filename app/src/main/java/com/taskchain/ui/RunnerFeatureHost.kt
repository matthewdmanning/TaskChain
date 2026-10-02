package com.taskchain.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.theme.CyberColors
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.AppContainer
import com.taskchain.domain.model.RoutineRun
import com.taskchain.domain.model.RunStatus
import kotlinx.coroutines.flow.collect

/**
 * Keeps the existing application and runner intact while layering one branch-specific runner enhancement.
 * The overlay is shown only after the active-run repository writes again after initial composition, which
 * corresponds to the private runner route creating its ViewModel for a new or resumed run.
 */
@Composable
fun RunnerFeatureApp(container: AppContainer) {
    var overlayVisible by rememberSaveable { mutableStateOf(false) }
    var activeRun by remember { mutableStateOf<RoutineRun?>(null) }

    LaunchedEffect(container) {
        var firstEmission = true
        // #fallback: TaskChainApp owns its NavController privately; repository writes are the available
        // route-entry signal without duplicating navigation or the existing runner implementation.
        container.activeRun.observeActive().collect { run ->
            activeRun = run
            if (firstEmission) {
                firstEmission = false
                if (run?.status != RunStatus.ACTIVE) overlayVisible = false
            } else {
                overlayVisible = run?.status == RunStatus.ACTIVE
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TaskChainApp(container)
        val run = activeRun
        if (overlayVisible && run?.status == RunStatus.ACTIVE) {
            RunnerFeatureCyberAppearance {
                RunnerFeatureOverlay(
                    container = container,
                    run = run,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Uses the same cyberpunkAndroid palette as the existing runner route. */
@Composable
private fun RunnerFeatureCyberAppearance(content: @Composable () -> Unit) {
    val palette = CyberPrimitives.Colors
    CyberTheme(
        colors = CyberColors(
            primary = palette.Chrome100,
            secondary = palette.Chrome200,
            background = palette.Void500,
            surface = palette.Void100,
            textPrimary = palette.Chrome100,
            textSecondary = palette.Chrome300,
            border = palette.Chrome600,
        ),
        content = content,
    )
}
