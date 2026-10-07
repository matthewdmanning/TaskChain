package com.taskchain.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import com.example.cyberpunkandroid.components.CyberButtonSize
import com.example.cyberpunkandroid.components.CyberButtonStyle

internal val LocalVibrationIntensity = staticCompositionLocalOf { 1f }

/** Use this function to add press feedback to an action; input is the action, dependencies are Android vibration and LocalVibrationIntensity. */
@Composable
private fun pressAction(action: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val intensity = LocalVibrationIntensity.current
    return {
        if (intensity > 0f) runCatching {
            val vibrator = context.getSystemService(android.os.Vibrator::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(20, (intensity * 255).toInt().coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20L)
            }
        }
        action()
    }
}

/** Use this function for Cyber buttons with press feedback; inputs mirror the library button, dependencies are pressAction and CyberButton. */
@Composable
internal fun CyberButton(modifier: Modifier = Modifier, style: CyberButtonStyle = CyberButtonStyle.Primary,
    size: CyberButtonSize = CyberButtonSize.Medium, enabled: Boolean = true,
    onClick: () -> Unit, content: @Composable () -> Unit) {
    com.example.cyberpunkandroid.components.CyberButton(modifier = modifier, style = style, size = size,
        enabled = enabled, onClick = pressAction(onClick), content = content)
}

/** Use this function for filled buttons with press feedback; inputs are action, layout, enabled state, colors and content, dependencies are pressAction and Material Button. */
@Composable
internal fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(), content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.Button(onClick = pressAction(onClick), modifier = modifier,
        enabled = enabled, colors = colors, content = content)
}

/** Use this function for outlined buttons with press feedback; inputs are action, layout, enabled state and content, dependencies are pressAction and Material OutlinedButton. */
@Composable
internal fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.OutlinedButton(onClick = pressAction(onClick), modifier = modifier,
        enabled = enabled, colors = colors, content = content)
}

/** Use this function for text buttons with press feedback; inputs are action, layout, enabled state and content, dependencies are pressAction and Material TextButton. */
@Composable
internal fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    androidx.compose.material3.TextButton(onClick = pressAction(onClick), modifier = modifier,
        enabled = enabled, content = content)
}
