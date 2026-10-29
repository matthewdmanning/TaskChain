package com.taskchain.core.ui

import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.navigationevent.NavigationEventTransitionState

/**
 * Use this function to preview an intercepted Back gesture from its transition state, gated by enabled.
 * Dependencies: NavigationEventTransitionState; replace this graphics layer for future fades or wipes.
 */
@Composable
internal fun Modifier.navigationBackPreview(
    transitionState: NavigationEventTransitionState,
    enabled: Boolean = true,
): Modifier {
    val context = LocalContext.current
    val motionEnabled = enabled && if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    return graphicsLayer {
        val progress = if (motionEnabled) {
            (transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent?.progress ?: 0f
        } else 0f
        scaleX = 1f - progress.coerceIn(0f, 1f) * BACK_PREVIEW_SCALE_REDUCTION
        scaleY = scaleX
    }
}

/** Small reversible cue; no domain or navigation state changes until Back completes. */
private const val BACK_PREVIEW_SCALE_REDUCTION = 0.02f
