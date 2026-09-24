package dev.augmentedui.generator.geometry

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Focused regression checks for deterministic, bounded augmented contours. */
class AugmentedShapeTest {
    /** Use this function to verify orientation, asymmetric sizing, and inset clamping without Android graphics. */
    @Test
    fun resolvesNotchesInwardAndClampsInset() {
        val density = Density(2f)
        val topStart = ContourPoint(0f, 0f)
        val topEnd = ContourPoint(200f, 0f)
        val bottomStart = ContourPoint(200f, 100f)
        val bottomEnd = ContourPoint(0f, 100f)

        val wide = requireNotNull(
            resolveEdgeNotch(topStart, topEnd, EdgeCut.ClipX(10.dp), EdgeSide.Top, density.density, 50f),
        )
        val deep = requireNotNull(
            resolveEdgeNotch(topStart, topEnd, EdgeCut.ClipY(10.dp), EdgeSide.Top, density.density, 50f),
        )
        val bottom = requireNotNull(
            resolveEdgeNotch(
                bottomStart,
                bottomEnd,
                EdgeCut.CenterNotch(width = 40.dp, depth = 6.dp),
                EdgeSide.Bottom,
                density.density,
                50f,
            ),
        )

        assertTrue(wide.boundaryEnd.x - wide.boundaryStart.x > deep.boundaryEnd.x - deep.boundaryStart.x)
        assertTrue(deep.control.y > wide.control.y)
        assertTrue(bottom.control.y < 100f)
        assertEquals(bottom, resolveEdgeNotch(
            bottomStart,
            bottomEnd,
            EdgeCut.CenterNotch(width = 40.dp, depth = 6.dp),
            EdgeSide.Bottom,
            density.density,
            50f,
        ))
        assertEquals(50f, resolveInset(Size(200f, 100f), density, 1000.dp))
    }
}
