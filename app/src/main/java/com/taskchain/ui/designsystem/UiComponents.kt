package com.taskchain.ui.designsystem

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.example.cyberpunkandroid.components.CyberButton
import com.example.cyberpunkandroid.components.CyberCard
import com.taskchain.ui.pressAction

/**
 * Library-independent composable action contract consumed by TaskChain screens.
 *
 * @param onClick User action, wrapped by the provider's press-feedback behavior.
 * @param modifier Layout, interaction, and accessibility modifier supplied by the caller.
 * @param enabled Whether interaction is allowed.
 * @param content Button content executed inside the active composition; no RowScope guarantee.
 */
typealias UiButton = @Composable (
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable () -> Unit,
) -> Unit

/**
 * Renders a state-driven toggle; a parent may own the sole accessible row target.
 *
 * @param checked Current toggle value.
 * @param onCheckedChange Change callback, or null when a parent handles input.
 * @param modifier Caller-owned layout and semantics.
 */
typealias UiSwitch = @Composable (
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
) -> Unit

/**
 * Library-neutral content container with caller-owned layout.
 *
 * @param modifier Layout and accessibility modifier.
 * @param content Composable child content; no implementation-specific scope is exposed.
 */
typealias UiCard = @Composable (modifier: Modifier, content: @Composable ColumnScope.() -> Unit) -> Unit

/**
 * Identifies a component implementation independently of the light/dark theme mode.
 *
 * @property id Stable persisted identifier.
 */
enum class UiProvider(val id: String) {
    CYBERPUNK("cyberpunk"),
    MATERIAL("material");

    companion object {
        /** Resolves legacy or unrecognized stored values to the existing Cyberpunk appearance. */
        fun fromId(id: String): UiProvider = entries.firstOrNull { it.id == id } ?: CYBERPUNK
    }
}

/**
 * Collection of swappable UI primitives. Extend by semantic use case, not library API count.
 *
 * @property button Action implementation preserving the app's press feedback.
 * @property switch Toggle implementation with externally owned state.
 * @property card Non-interactive semantic content container.
 */
data class UiComponents(
    val button: UiButton,
    val switch: UiSwitch,
    val card: UiCard,
)

/** Existing Cyberpunk appearance; the toggle remains Material-backed under CyberTheme. */
val CyberpunkUiComponents = UiComponents(
    button = { onClick, modifier, enabled, content ->
        CyberButton(onClick = pressAction(onClick), modifier = modifier, enabled = enabled, content = content)
    },
    switch = { checked, onCheckedChange, modifier ->
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    },
    card = { modifier, content -> CyberCard(modifier = modifier, content = content) },
)

/** Material 3 primitive implementation; a full Material theme is not yet installed. */
val MaterialUiComponents = UiComponents(
    button = { onClick, modifier, enabled, content ->
        Button(onClick = pressAction(onClick), modifier = modifier, enabled = enabled, content = content)
    },
    switch = { checked, onCheckedChange, modifier ->
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier)
    },
    card = { modifier, content -> Card(modifier = modifier, content = content) },
)

/** Returns the implementation associated with a persisted [UiProvider] choice. */
fun componentsFor(provider: UiProvider): UiComponents = when (provider) {
    UiProvider.CYBERPUNK -> CyberpunkUiComponents
    UiProvider.MATERIAL -> MaterialUiComponents
}

/** Current UI implementation scoped to a Compose subtree; defaults to the legacy appearance. */
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

/**
 * Displays a non-interactive content card with the active provider while retaining
 * Compose's [ColumnScope] for child layout. Domain state remains owned by the caller.
 *
 * @param modifier Caller-owned layout and accessibility modifier.
 * @param content Card contents, evaluated inside the active composition.
 */
@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    LocalUiComponents.current.card(modifier, content)
}
