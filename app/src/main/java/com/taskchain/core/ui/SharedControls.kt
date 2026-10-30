@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.taskchain.core.ui
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
import com.taskchain.core.designsystem.TaskChainDesignSystem
import com.taskchain.core.designsystem.LocalUiComponents

/**
 * Renders a labeled setting as a single accessible toggle row using the active UI provider.
 *
 * @param label Visible and accessible setting name.
 * @param checked Current externally owned state.
 * @param onCheckedChange Callback invoked when the row is toggled.
 */
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
        Text(label, modifier = Modifier.weight(1f).padding(end = TaskChainDesignSystem.spacing().small))
        ui.switch(checked, null, Modifier.clearAndSetSemantics {})
    }
}

internal const val SECONDS_PER_MINUTE = 60L
