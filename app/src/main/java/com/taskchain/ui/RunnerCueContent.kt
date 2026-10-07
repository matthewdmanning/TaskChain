package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.cyberpunkandroid.config.CyberPrimitives
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.domain.model.RoutineRunStep

/** Use this function to display the current cue beneath its main task.
 * Inputs: step snapshot, derived active-cue remaining time, and the display preference.
 * Dependencies: RoutineRunStep, CyberPrimitives, and the existing semantic theme and timer formatter.
 */
@Composable
internal fun RunnerCueList(step: RoutineRunStep, remainingMillis: Long?, showRemaining: Boolean) {
    if (step.source.cues.isEmpty()) return
    val cue = step.activeCueId?.let { activeCueId ->
        step.source.cues.firstOrNull { it.id == activeCueId }
    } ?: step.source.cues.lastOrNull().takeIf {
        step.cueAdvancements.size >= step.source.cues.size
    } ?: return
    val active = cue.id == step.activeCueId
    val advanced = step.cueAdvancements.any { it.cueId == cue.id }
    val cueState = stringResource(if (advanced) R.string.cue_advanced else R.string.cue_active)
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CyberPrimitives.Spacing.dp8),
    ) {
        Text(
            text = cue.title,
            style = MaterialTheme.typography.headlineMedium,
            color = if (advanced) CyberTheme.semantics.colors.success else CyberTheme.colors.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = cueState },
        )
        if (active && showRemaining) {
            Text(
                formatRunnerTimer(remainingMillis, null, true),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}
