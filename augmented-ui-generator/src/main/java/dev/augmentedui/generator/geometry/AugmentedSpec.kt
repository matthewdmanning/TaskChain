package dev.augmentedui.generator.geometry

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Represents a border stroke that follows an augmented perimeter. */
@Immutable
data class BorderSpec(val width: Dp = 0.dp, val color: Color = Color.Transparent)

/** Represents the uniformly inset fill contour inside an augmented perimeter. */
@Immutable
data class InlaySpec(val inset: Dp = 0.dp, val fill: Color = Color.Transparent)

/** Represents the complete immutable augmented-ui geometry and rendering intent. */
@Immutable
data class CyberAugSpec(
    val topLeft: Cut = Cut.None,
    val topRight: Cut = Cut.None,
    val bottomRight: Cut = Cut.None,
    val bottomLeft: Cut = Cut.None,
    val topEdge: EdgeCut = EdgeCut.None,
    val rightEdge: EdgeCut = EdgeCut.None,
    val bottomEdge: EdgeCut = EdgeCut.None,
    val leftEdge: EdgeCut = EdgeCut.None,
    val border: BorderSpec? = null,
    val inlay: InlaySpec? = null,
)

/** Represents one of the grammar's corner contour primitives. */
sealed interface Cut {
    /** Represents an untouched corner. */
    data object None : Cut
    /** Represents a symmetric diagonal corner clip. */
    data class Clip(val size: Dp) : Cut
    /** Represents the guide's readable alias for a symmetric diagonal clip. */
    data class Chamfer(val size: Dp) : Cut
    /** Represents a horizontally elongated corner clip. */
    data class ClipX(val size: Dp) : Cut
    /** Represents a vertically elongated corner clip. */
    data class ClipY(val size: Dp) : Cut
    /** Represents a rounded concave corner scoop. */
    data class Scoop(val size: Dp) : Cut
    /** Represents a horizontally elongated corner scoop. */
    data class ScoopX(val size: Dp) : Cut
    /** Represents a vertically elongated corner scoop. */
    data class ScoopY(val size: Dp) : Cut
    /** Represents a right-angle corner notch. */
    data class Rect(val size: Dp) : Cut
    /** Represents a horizontally elongated right-angle notch. */
    data class RectX(val size: Dp) : Cut
    /** Represents a vertically elongated right-angle notch. */
    data class RectY(val size: Dp) : Cut
    /** Represents a two-tier staircase corner notch. */
    data class Step(val size: Dp) : Cut
}

/** Represents one of the grammar's edge notch primitives. */
sealed interface EdgeCut {
    /** Represents an untouched edge. */
    data object None : EdgeCut
    /** Represents a centered rectangular edge pocket. */
    data class Rect(val size: Dp) : EdgeCut
    /** Represents a rectangular edge pocket elongated along its axis. */
    data class RectX(val size: Dp) : EdgeCut
    /** Represents a rectangular edge pocket elongated perpendicular to its axis. */
    data class RectY(val size: Dp) : EdgeCut
    /** Represents an edge notch elongated along its axis. */
    data class ClipX(val size: Dp) : EdgeCut
    /** Represents an edge notch elongated perpendicular to its axis. */
    data class ClipY(val size: Dp) : EdgeCut
    /** Represents a rounded edge scoop elongated along its axis. */
    data class ScoopX(val size: Dp) : EdgeCut
    /** Represents a rounded edge scoop elongated perpendicular to its axis. */
    data class ScoopY(val size: Dp) : EdgeCut
    /** Represents a centered rounded edge scoop. */
    data class Scoop(val size: Dp) : EdgeCut
    /** Represents the guide's explicit centered notch with independent width and depth. */
    data class CenterNotch(val width: Dp, val depth: Dp) : EdgeCut
}
