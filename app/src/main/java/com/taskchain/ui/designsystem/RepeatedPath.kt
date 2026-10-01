package com.taskchain.ui.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Placement for each copy of a source path. */
data class PathPlacement(
    val offset: Offset,
    val rotationDegrees: Float = 0f,
)

/**
 * Creates parallel placements by repeatedly translating the original path.
 *
 * @param count number of copies to produce.
 * @param translation displacement applied between adjacent copies.
 */
fun translatedPlacements(count: Int, translation: Offset): List<PathPlacement> =
    List(count.coerceAtLeast(0)) { index ->
        PathPlacement(offset = translation * index.toFloat())
    }

/**
 * Creates placements around an arc while preserving the source path geometry.
 * Rotation changes orientation only; it never deforms the source path.
 *
 * @param count number of copies to produce.
 * @param center center of the radial placement path.
 * @param radius distance of each copy from [center].
 * @param startDegrees starting polar angle.
 * @param sweepDegrees angular distance covered by all copies.
 * @param rotateWithPath whether copies follow the tangent of the arc.
 */
fun radialPlacements(
    count: Int,
    center: Offset,
    radius: Float,
    startDegrees: Float,
    sweepDegrees: Float,
    rotateWithPath: Boolean = true,
): List<PathPlacement> {
    if (count <= 0) return emptyList()
    val divisor = (count - 1).coerceAtLeast(1)
    return List(count) { index ->
        val degrees = startDegrees + sweepDegrees * index / divisor
        val radians = degrees * PI.toFloat() / 180f
        PathPlacement(
            offset = center + Offset(cos(radians) * radius, sin(radians) * radius),
            rotationDegrees = if (rotateWithPath) degrees + 90f else 0f,
        )
    }
}

/**
 * Draws an unchanged source [Path] at each requested placement.
 *
 * The caller owns geometry, placement, stroke width, and brush so this primitive contains no
 * feature-specific dimensions, colors, typography, or other presentation constants.
 *
 * @param sourcePath original path reused for every copy.
 * @param placements translation/rotation transforms for each copy.
 * @param brush shading applied along each path; use a gradient brush for progressive shading.
 * @param strokeWidth width of the rendered path in pixels.
 */
@Composable
fun RepeatedPath(
    sourcePath: Path,
    placements: List<PathPlacement>,
    brush: Brush,
    strokeWidth: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        placements.forEach { placement ->
            withTransform({
                translate(placement.offset.x, placement.offset.y)
                rotate(placement.rotationDegrees)
            }) {
                drawPath(
                    path = sourcePath,
                    brush = brush,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.cornerPathEffect(strokeWidth),
                    ),
                )
            }
        }
    }
}

/** Convenience brush for a caller that wants a solid semantic color. */
fun solidPathBrush(color: Color): Brush = Brush.linearGradient(listOf(color, color))
