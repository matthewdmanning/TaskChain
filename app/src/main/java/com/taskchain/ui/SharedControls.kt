@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.ui
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.taskchain.ui.designsystem.TaskChainTheme
import com.taskchain.ui.designsystem.LocalUiComponents

/** Retains the current Cyberpunk Android theme supplied by [TaskChainTheme]. */
@Composable
internal fun CyberAppearance(content: @Composable () -> Unit) = content()

/** Use this function when a labeled setting should expose one accessible switch target for its whole row. */
@Composable
internal fun LabeledSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val ui = LocalUiComponents.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = CyberPrimitives.Spacing.dp8))
        ui.switch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
    }
}

internal const val SECONDS_PER_MINUTE = 60L
