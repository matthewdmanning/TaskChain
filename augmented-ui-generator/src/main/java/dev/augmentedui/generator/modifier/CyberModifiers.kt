package dev.augmentedui.generator.modifier

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.augmentedui.generator.geometry.AugmentedShape
import dev.augmentedui.generator.geometry.CyberAugSpec

private const val ChromaticShiftShader = """
    uniform shader composable;
    uniform float intensity;

    half4 main(float2 coordinate) {
        half4 left = composable.eval(coordinate - float2(intensity, 0));
        half4 center = composable.eval(coordinate);
        half4 right = composable.eval(coordinate + float2(intensity, 0));
        return half4(left.r, center.g, right.b, center.a);
    }
"""

/** Use this function to add a hardware-layer neon shadow whose intensity can animate without layout. */
fun Modifier.cyberGlow(
    color: Color,
    radius: Dp,
    intensityState: State<Float>,
    shape: Shape = RectangleShape,
): Modifier = graphicsLayer {
    this.shape = shape
    shadowElevation = radius.toPx().coerceAtLeast(0f) * intensityState.value.coerceIn(0f, 1f)
    ambientShadowColor = color
    spotShadowColor = color
}

/** Use this function to overlay cached repeating scanlines while animation remains in the draw phase. */
fun Modifier.cyberScanlines(
    color: Color,
    step: Dp = 4.dp,
    offsetState: State<Float>? = null,
): Modifier = drawWithCache {
    val stepPx = step.toPx().coerceAtLeast(1f)
    val brush = Brush.verticalGradient(
        0f to Color.Transparent,
        0.5f to color,
        1f to Color.Transparent,
        startY = 0f,
        endY = stepPx,
        tileMode = TileMode.Repeated,
    )

    onDrawWithContent {
        drawContent()
        val offset = offsetState?.value?.rem(stepPx) ?: 0f
        translate(top = offset - stepPx) {
            drawRect(brush = brush, size = size.copy(height = size.height + stepPx * 2f))
        }
    }
}

/** Use this function to draw the border and inlay declared by a deterministic augmented-ui spec. */
fun Modifier.augmentedFrame(spec: CyberAugSpec): Modifier = drawWithCache {
    val shape = AugmentedShape(spec)
    val outerPath = shape.createPath(size = size, density = this)
    val borderWidth = spec.border?.width?.toPx()?.coerceAtLeast(0f) ?: 0f
    val inlayPath = spec.inlay?.let { shape.createPath(size = size, density = this, inset = it.inset) }

    onDrawWithContent {
        spec.inlay?.let { inlay ->
            inlayPath?.let { drawPath(path = it, color = inlay.fill) }
        }
        drawContent()
        spec.border?.let { border ->
            drawPath(path = outerPath, color = border.color, style = Stroke(width = borderWidth))
        }
    }
}

/** Use this function for API-safe RGB splitting without recompiling the AGSL program during recomposition. */
fun Modifier.cyberChromaticShift(intensityState: State<Float>): Modifier = composed {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        this
    } else {
        with(AgslApi33) { chromaticShift(intensityState) }
    }
}

/** Owns Android 13 shader APIs so older devices never resolve unsupported classes. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object AgslApi33 {
    /** Use this function after the platform guard to create one remembered shader-backed layer. */
    @Composable
    fun Modifier.chromaticShift(intensityState: State<Float>): Modifier {
        val shader = remember { RuntimeShader(ChromaticShiftShader) }
        val effect = remember(shader) {
            AndroidRenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
        }

        return graphicsLayer {
            shader.setFloatUniform("intensity", intensityState.value.coerceAtLeast(0f))
            renderEffect = effect
        }
    }
}
