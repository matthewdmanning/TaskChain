package com.taskchain.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cyberpunkandroid.theme.CyberTheme
import com.taskchain.R
import com.taskchain.domain.model.RoutineRunStep

/** Use this function to display ordered cues beneath their main task.
 * Inputs: step snapshot, derived active-cue remaining time, and the display preference.
 * Dependencies: RoutineRunStep and the existing semantic theme and timer formatter.
 */
@Composable
internal fun RunnerCueList(step: RoutineRunStep, remainingMillis: Long?, showRemaining: Boolean) {
    if (step.source.cues.isEmpty()) return
    var expanded by rememberSaveable(step.source.id.value) { mutableStateOf(true) }
    val visibleCues = if (expanded) step.source.cues else
        step.source.cues.filter { it.id == step.activeCueId }.ifEmpty { step.source.cues.takeLast(1) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.runner_cues_collapse else R.string.runner_cues_expand))
        }
        visibleCues.forEach { cue ->
            val active = cue.id == step.activeCueId
            val advanced = step.cueAdvancements.any { it.cueId == cue.id }
            Text(
                text = cue.title,
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    advanced -> CyberTheme.semantics.colors.success
                    active -> CyberTheme.colors.secondary
                    else -> CyberTheme.colors.textSecondary
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (active && showRemaining) {
                Text(
                    stringResource(R.string.cue_remaining, formatRunnerTimer(remainingMillis, null, true)),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
