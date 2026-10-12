package com.taskchain.ui.designsystem

import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.example.cyberpunkandroid.components.CyberButton

/**
 * Library-independent composable actions consumed by TaskChain screens.
 *
 * Arguments remain ordinary Compose state and callbacks so screens never need
 * to import an implementation library.
 */
typealias UiButton = @Composable (
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable () -> Unit,
) -> Unit

/** Renders a state-driven toggle; the parent can own the accessible row target. */
typealias UiSwitch = @Composable (
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
) -> Unit

/** Collection of swappable UI primitives. Extend by semantic use case, not library API count. */
data class UiComponents(
    val button: UiButton,
    val switch: UiSwitch,
)

/** Existing Cyberpunk appearance, retaining the current Material-backed switch. */
val CyberpunkUiComponents = UiComponents(
    button = { onClick, modifier, enabled, content ->
        CyberButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    },
    switch = { checked, onCheckedChange, modifier ->
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    },
)

/** Material 3 implementation of the same component contracts. */
val MaterialUiComponents = UiComponents(
    button = { onClick, modifier, enabled, content ->
        Button(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
    },
    switch = { checked, onCheckedChange, modifier ->
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    },
)

/** Current UI implementation, scoped to a Compose subtree. */
val LocalUiComponents = staticCompositionLocalOf { CyberpunkUiComponents }

/**
 * Installs a component implementation without precomposing UI outside the active composition.
 *
 * @param components Composable function implementations to expose to descendants.
 * @param content Screen hierarchy using the selected components.
 */
@Composable
fun ProvideUiComponents(
    components: UiComponents,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalUiComponents provides components, content = content)
}
