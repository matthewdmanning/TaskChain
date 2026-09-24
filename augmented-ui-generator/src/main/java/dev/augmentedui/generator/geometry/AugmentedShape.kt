package dev.augmentedui.generator.geometry

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.min

/** A deterministic Compose shape whose positions remain physical in both layout directions. */
class AugmentedShape(val spec: CyberAugSpec) : Shape {
    /** Use this function to obtain the exact contour for drawing, clipping, or caching. */
    fun createPath(size: Size, density: Density, inset: Dp = 0.dp): Path =
        GeometryPathBuilder(spec, size, density, inset).build()

    /** Use this function when Compose needs the outline used for clipping. */
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(createPath(size, density))
}

/** Represents the two-cut compatibility shape requested by the implementation plan. */
class ChamferedShape(val topLeftCut: Dp, val bottomRightCut: Dp) : Shape {
    /** Use this function when Compose needs a two-corner chamfer outline. */
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(
            AugmentedShape(
                CyberAugSpec(
                    topLeft = Cut.Chamfer(topLeftCut),
                    bottomRight = Cut.Chamfer(bottomRightCut),
                ),
            ).createPath(size, density),
        )
}

/** Represents one resolved point in the geometry engine's pixel coordinate space. */
internal data class ContourPoint(val x: Float, val y: Float)

/** Names a physical edge and its inward-facing unit normal. */
internal enum class EdgeSide(val normalX: Float, val normalY: Float) {
    Top(0f, 1f),
    Right(-1f, 0f),
    Bottom(0f, -1f),
    Left(1f, 0f),
}

/** Represents the resolved points and style for one centered edge cut. */
internal data class EdgeNotch(
    val boundaryStart: ContourPoint,
    val innerStart: ContourPoint,
    val innerEnd: ContourPoint,
    val boundaryEnd: ContourPoint,
    val control: ContourPoint,
    val style: EdgeStyle,
)

/** Names the minimal drawing operation used by a resolved edge cut. */
internal enum class EdgeStyle { Rect, Clip, Scoop }

/** Use this function to clamp an inset before it can invert a small contour. */
internal fun resolveInset(size: Size, density: Density, inset: Dp): Float =
    with(density) { inset.toPx() }
        .coerceIn(0f, min(size.width, size.height).coerceAtLeast(0f) / 2f)

/** Use this function to resolve an edge primitive into bounded, orientation-correct points. */
internal fun resolveEdgeNotch(
    start: ContourPoint,
    end: ContourPoint,
    cut: EdgeCut,
    side: EdgeSide,
    density: Float,
    maxDepth: Float,
): EdgeNotch? {
    if (cut == EdgeCut.None) return null

    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = hypot(dx, dy)
    if (length == 0f) return null

    val base = edgeSize(cut).coerceAtLeast(0f) * density
    val horizontal = side == EdgeSide.Top || side == EdgeSide.Bottom
    val xBiased = cut is EdgeCut.RectX || cut is EdgeCut.ClipX || cut is EdgeCut.ScoopX
    val yBiased = cut is EdgeCut.RectY || cut is EdgeCut.ClipY || cut is EdgeCut.ScoopY
    val requestedHalfWidth = when (cut) {
        is EdgeCut.CenterNotch -> cut.width.value.coerceAtLeast(0f) * density / 2f
        else -> base * if ((horizontal && xBiased) || (!horizontal && yBiased)) 2f else 1f
    }
    val requestedDepth = when (cut) {
        is EdgeCut.CenterNotch -> cut.depth.value.coerceAtLeast(0f) * density
        else -> base * if ((horizontal && yBiased) || (!horizontal && xBiased)) 2f else 1f
    }
    val halfWidth = requestedHalfWidth.coerceAtMost(length / 2f)
    val depth = requestedDepth.coerceAtMost(maxDepth.coerceAtLeast(0f))
    if (halfWidth == 0f || depth == 0f) return null

    val tangentX = dx / length
    val tangentY = dy / length
    val mid = ContourPoint((start.x + end.x) / 2f, (start.y + end.y) / 2f)
    val boundaryStart = mid.offset(-tangentX * halfWidth, -tangentY * halfWidth)
    val boundaryEnd = mid.offset(tangentX * halfWidth, tangentY * halfWidth)
    val style = when (cut) {
        is EdgeCut.ClipX, is EdgeCut.ClipY -> EdgeStyle.Clip
        is EdgeCut.Scoop, is EdgeCut.ScoopX, is EdgeCut.ScoopY -> EdgeStyle.Scoop
        else -> EdgeStyle.Rect
    }
    val innerHalfWidth = if (style == EdgeStyle.Clip) halfWidth / 2f else halfWidth
    val inwardX = side.normalX * depth
    val inwardY = side.normalY * depth

    return EdgeNotch(
        boundaryStart = boundaryStart,
        innerStart = mid.offset(-tangentX * innerHalfWidth + inwardX, -tangentY * innerHalfWidth + inwardY),
        innerEnd = mid.offset(tangentX * innerHalfWidth + inwardX, tangentY * innerHalfWidth + inwardY),
        boundaryEnd = boundaryEnd,
        control = mid.offset(inwardX, inwardY),
        style = style,
    )
}

/** Use this function to translate a contour point without mutating cached geometry. */
private fun ContourPoint.offset(dx: Float, dy: Float): ContourPoint = ContourPoint(x + dx, y + dy)

/** Use this function to read the primary edge size while preserving every sealed subtype. */
private fun edgeSize(cut: EdgeCut): Float = when (cut) {
    EdgeCut.None, is EdgeCut.CenterNotch -> 0f
    is EdgeCut.Rect -> cut.size.value
    is EdgeCut.RectX -> cut.size.value
    is EdgeCut.RectY -> cut.size.value
    is EdgeCut.ClipX -> cut.size.value
    is EdgeCut.ClipY -> cut.size.value
    is EdgeCut.Scoop -> cut.size.value
    is EdgeCut.ScoopX -> cut.size.value
    is EdgeCut.ScoopY -> cut.size.value
}

/** Builds one contour while keeping all intermediate geometry in pixel coordinates. */
private class GeometryPathBuilder(
    private val spec: CyberAugSpec,
    private val size: Size,
    private val density: Density,
    inset: Dp,
) {
    private val insetPx = resolveInset(size, density, inset)
    private val left = insetPx
    private val top = insetPx
    private val right = (size.width - insetPx).coerceAtLeast(left)
    private val bottom = (size.height - insetPx).coerceAtLeast(top)
    private val width = right - left
    private val height = bottom - top

    /** Use this function to build one bounded contour from the immutable specification. */
    fun build(): Path {
        val path = Path()
        val topLeft = corner(spec.topLeft, 0)
        val topRight = corner(spec.topRight, 1)
        val bottomRight = corner(spec.bottomRight, 2)
        val bottomLeft = corner(spec.bottomLeft, 3)

        path.moveTo(topLeft.first.x, topLeft.first.y)
        cornerTo(path, topLeft.first, topLeft.second, 0, spec.topLeft)
        edgeTo(path, topLeft.second, topRight.first, spec.topEdge, EdgeSide.Top)
        cornerTo(path, topRight.first, topRight.second, 1, spec.topRight)
        edgeTo(path, topRight.second, bottomRight.first, spec.rightEdge, EdgeSide.Right)
        cornerTo(path, bottomRight.first, bottomRight.second, 2, spec.bottomRight)
        edgeTo(path, bottomRight.second, bottomLeft.first, spec.bottomEdge, EdgeSide.Bottom)
        cornerTo(path, bottomLeft.first, bottomLeft.second, 3, spec.bottomLeft)
        edgeTo(path, bottomLeft.second, topLeft.first, spec.leftEdge, EdgeSide.Left)
        path.close()
        return path
    }

    /** Use this function to resolve a corner primitive into clockwise perimeter endpoints. */
    private fun corner(cut: Cut, index: Int): Pair<ContourPoint, ContourPoint> {
        val base = cutSize(cut).coerceAtLeast(0f) * density.density
        val xBias = cut is Cut.ClipX || cut is Cut.ScoopX || cut is Cut.RectX
        val yBias = cut is Cut.ClipY || cut is Cut.ScoopY || cut is Cut.RectY
        val alongX = (base * if (xBias) 2f else 1f).coerceAtMost(width / 2f)
        val alongY = (base * if (yBias) 2f else 1f).coerceAtMost(height / 2f)

        return when (index) {
            0 -> ContourPoint(left, top + alongY) to ContourPoint(left + alongX, top)
            1 -> ContourPoint(right - alongX, top) to ContourPoint(right, top + alongY)
            2 -> ContourPoint(right, bottom - alongY) to ContourPoint(right - alongX, bottom)
            else -> ContourPoint(left + alongX, bottom) to ContourPoint(left, bottom - alongY)
        }
    }

    /** Use this function to read the primary corner size while preserving every sealed subtype. */
    private fun cutSize(cut: Cut): Float = when (cut) {
        Cut.None -> 0f
        is Cut.Clip -> cut.size.value
        is Cut.Chamfer -> cut.size.value
        is Cut.ClipX -> cut.size.value
        is Cut.ClipY -> cut.size.value
        is Cut.Scoop -> cut.size.value
        is Cut.ScoopX -> cut.size.value
        is Cut.ScoopY -> cut.size.value
        is Cut.Rect -> cut.size.value
        is Cut.RectX -> cut.size.value
        is Cut.RectY -> cut.size.value
        is Cut.Step -> cut.size.value
    }

    /** Use this function to emit one corner as a clip, concave scoop, notch, or stair. */
    private fun cornerTo(path: Path, start: ContourPoint, end: ContourPoint, index: Int, cut: Cut) {
        val vertex = cornerVertex(index)
        val interior = cornerInterior(index, start, end)
        when (cut) {
            is Cut.Scoop, is Cut.ScoopX, is Cut.ScoopY ->
                path.quadraticTo(interior.x, interior.y, end.x, end.y)
            is Cut.Rect, is Cut.RectX, is Cut.RectY -> {
                path.lineTo(interior.x, interior.y)
                path.lineTo(end.x, end.y)
            }
            is Cut.Step -> {
                val startVector = ContourPoint(start.x - vertex.x, start.y - vertex.y)
                val endVector = ContourPoint(end.x - vertex.x, end.y - vertex.y)
                path.lineTo(vertex.x + startVector.x / 2f, vertex.y + startVector.y / 2f)
                path.lineTo(
                    vertex.x + (startVector.x + endVector.x) / 2f,
                    vertex.y + (startVector.y + endVector.y) / 2f,
                )
                path.lineTo(vertex.x + endVector.x / 2f, vertex.y + endVector.y / 2f)
                path.lineTo(end.x, end.y)
            }
            else -> path.lineTo(end.x, end.y)
        }
    }

    /** Use this function to retrieve the physical vertex for one clockwise corner index. */
    private fun cornerVertex(index: Int): ContourPoint = when (index) {
        0 -> ContourPoint(left, top)
        1 -> ContourPoint(right, top)
        2 -> ContourPoint(right, bottom)
        else -> ContourPoint(left, bottom)
    }

    /** Use this function to find the inward intersection of a corner's two cut endpoints. */
    private fun cornerInterior(index: Int, start: ContourPoint, end: ContourPoint): ContourPoint =
        if (index == 0 || index == 2) ContourPoint(end.x, start.y) else ContourPoint(start.x, end.y)

    /** Use this function to emit a straight edge or an orientation-correct centered cut. */
    private fun edgeTo(path: Path, start: ContourPoint, end: ContourPoint, cut: EdgeCut, side: EdgeSide) {
        val maxDepth = if (side == EdgeSide.Top || side == EdgeSide.Bottom) height / 2f else width / 2f
        val notch = resolveEdgeNotch(start, end, cut, side, density.density, maxDepth)
        if (notch == null) {
            path.lineTo(end.x, end.y)
            return
        }

        path.lineTo(notch.boundaryStart.x, notch.boundaryStart.y)
        when (notch.style) {
            EdgeStyle.Rect, EdgeStyle.Clip -> {
                path.lineTo(notch.innerStart.x, notch.innerStart.y)
                path.lineTo(notch.innerEnd.x, notch.innerEnd.y)
                path.lineTo(notch.boundaryEnd.x, notch.boundaryEnd.y)
            }
            EdgeStyle.Scoop -> path.quadraticTo(
                notch.control.x,
                notch.control.y,
                notch.boundaryEnd.x,
                notch.boundaryEnd.y,
            )
        }
        path.lineTo(end.x, end.y)
    }
}
