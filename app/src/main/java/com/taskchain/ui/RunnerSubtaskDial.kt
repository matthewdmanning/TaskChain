package com.taskchain.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.taskchain.R
import com.taskchain.domain.model.RoutineRunTask
import com.taskchain.domain.model.RoutineTask
import com.taskchain.ui.designsystem.TaskChainDesignSystem
import kotlin.math.cos
import kotlin.math.sin

/** Use this function to place durable subtask rims around the existing main countdown dial.
 * Inputs: task snapshot, projected subtask active times, diameter, schedule warning, and main-dial content.
 * Dependencies: persisted advancement markers, subtask geometry, and the design-system color seed.
 */
@Composable
internal fun RunnerSubtaskDial(
    task: RoutineRunTask,
    subtaskElapsedMillis: List<Long>,
    diameter: Dp,
    behindSchedule: Boolean,
    content: @Composable (Dp) -> Unit,
) {
    if (task.source.subtasks.isEmpty()) {
        content(diameter)
        return
    }
    val rimSpace = (8.dp * (task.source.subtasks.size + 1)).coerceAtMost(diameter * 0.28f)
    val innerDiameter = diameter - rimSpace * 2
    val colors = task.source.subtasks.indices.map { TaskChainDesignSystem.subtaskColor(it) }
    val warningLabel = stringResource(R.string.behind_subtask_schedule)
    Box(Modifier.size(diameter).semantics {
        if (behindSchedule) stateDescription = warningLabel
    }, contentAlignment = Alignment.Center) {
        content(innerDiameter)
        Canvas(Modifier.size(diameter)) {
            val innerRadius = innerDiameter.toPx() / 2
            val spacing = rimSpace.toPx() / (task.source.subtasks.size + 1)
            task.source.subtasks.forEachIndexed { index, subtask ->
                val radius = innerRadius + spacing * (index + 1)
                val bounds = Size(radius * 2, radius * 2)
                val origin = center - Offset(radius, radius)
                val start = subtaskRimStartDegrees(task.source, index)
                val plannedSweep = subtaskRimSweepDegrees(subtask.durationSeconds * 1_000, task.source.durationSeconds!!)
                val actualSweep = subtaskRimSweepDegrees(subtaskElapsedMillis.getOrElse(index) { 0 }, task.source.durationSeconds!!)
                val stroke = spacing.coerceAtMost(3.dp.toPx()).coerceAtLeast(1f)
                drawArc(colors[index].copy(alpha = 0.2f), start, plannedSweep, false, origin, bounds, style = Stroke(stroke))
                drawArc(colors[index], start, actualSweep, false, origin, bounds, style = Stroke(stroke))
                if (task.subtaskAdvancements.any { it.subtaskId == subtask.id }) {
                    val angle = (start + actualSweep) * Math.PI / 180
                    val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val from = center + direction * (radius - spacing / 2)
                    val to = center + direction * (radius + spacing / 2)
                    drawLine(colors[index].copy(alpha = 0.3f), from, to, stroke * 4)
                    drawLine(colors[index], from, to, stroke)
                }
            }
        }
    }
}

/** Use this function to anchor a subtask rim at its proportional planned start.
 * Inputs: main task and subtask index. Dependencies: validated subtask duration totals.
 */
internal fun subtaskRimStartDegrees(task: RoutineTask, index: Int): Float =
    -90f + (task.subtasks.take(index).sumOf { it.durationSeconds }.toDouble() /
        requireNotNull(task.durationSeconds) * 360).toFloat()

/** Use this function to draw actual subtask time, including beyond its proportional end.
 * Inputs: elapsed active milliseconds and total main duration in seconds. Dependencies: None.
 */
internal fun subtaskRimSweepDegrees(elapsedMillis: Long, totalSeconds: Long): Float =
    if (totalSeconds <= 0) 0f else (elapsedMillis.coerceAtLeast(0).toDouble() / (totalSeconds.toDouble() * 1_000) * 360).toFloat()
