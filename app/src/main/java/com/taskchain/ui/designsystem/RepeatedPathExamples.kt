package com.taskchain.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.taskchain.R

/** Creates the normalized angle used by the chevron examples. */
private fun anglePath(size: Float): Path = Path().apply {
    moveTo(0f, 0f)
    lineTo(size, size)
    lineTo(0f, size * 2f)
}

/** Demonstrates parallel translated copies of one unchanged angle path. */
@Composable
fun ParallelChevronExample(modifier: Modifier = Modifier) {
    val spacing = dimensionResource(R.dimen.space_medium).value
    val stroke = dimensionResource(R.dimen.repeated_path_stroke).value
    val extent = dimensionResource(R.dimen.repeated_path_example_extent).value
    val color = colorResource(R.color.dark_primary)
    RepeatedPath(
        sourcePath = anglePath(extent),
        placements = translatedPlacements(
            count = integerResourceValue(R.integer.repeated_path_parallel_count),
            translation = Offset(spacing, 0f),
        ),
        brush = Brush.linearGradient(listOf(color.copy(alpha = 0.45f), color)),
        strokeWidth = stroke,
        modifier = modifier,
    )
}

/** Demonstrates the same angle distributed radially without changing its geometry. */
@Composable
fun RadialChevronExample(modifier: Modifier = Modifier) {
    val radius = dimensionResource(R.dimen.repeated_path_radial_radius).value
    val stroke = dimensionResource(R.dimen.repeated_path_stroke).value
    val extent = dimensionResource(R.dimen.repeated_path_radial_extent).value
    val start = colorResource(R.color.dark_primary)
    val end = colorResource(R.color.semantic_warning)
    RepeatedPath(
        sourcePath = anglePath(extent),
        placements = radialPlacements(
            count = integerResourceValue(R.integer.repeated_path_radial_count),
            center = Offset(radius, radius),
            radius = radius,
            startDegrees = 0f,
            sweepDegrees = 360f,
        ),
        brush = Brush.sweepGradient(listOf(start, end, start)),
        strokeWidth = stroke,
        modifier = modifier,
    )
}

/** Reads an integer resource while keeping example counts out of Kotlin literals. */
@Composable
private fun integerResourceValue(id: Int): Int =
    androidx.compose.ui.platform.LocalContext.current.resources.getInteger(id)

@Preview(showBackground = true)
@Composable
private fun ParallelChevronPreview() {
    Box(Modifier.fillMaxSize()) {
        ParallelChevronExample(Modifier.fillMaxSize())
    }
}

@Preview(showBackground = true)
@Composable
private fun RadialChevronPreview() {
    Box(Modifier.fillMaxSize()) {
        RadialChevronExample(Modifier.fillMaxSize())
    }
}
