# Cyberpunk Performance Estimates

## Estimated Costs

Component | Execution Layer | Estimated Frame Cost | Performance Impact & Bottlenecks
Chamfered Shapes (Path) | CPU (Outline) / GPU (Clip) | < 0.1 ms | Negligible. Calculating a 5-point vector path takes fractions of a microsecond. The GPU rasterizes the polygon clip natively.
AGSL Shaders (RuntimeShader) | GPU (Fragment Shader) | ~0.2 – 0.5 ms | Very Fast. Runs directly on the GPU hardware pipeline. Handles full-screen glitch or chromatic aberration at 60/120 FPS without CPU overhead. (Requires API 33+)
Hardware Blur Glows (RenderEffect) | GPU (RenderThread) | ~0.5 – 1.5 ms | "Low to Moderate. Highly optimized via Skia on the GPU | but scales with blur radius and screen area. Stacking multiple full-screen blurs will increase fill-rate pressure. (Requires API 31+)"
Iterated Scanlines (drawLine loop) | CPU (DisplayList) / GPU (Draw) | ~0.5 – 2.0 ms (Unoptimized) | Moderate. Running a while loop with hundreds of drawLine calls forces the CPU to push hundreds of draw commands to the render queue every time the node redraws.

## Optimization Steps

Critical Performance Optimization for Cyberpunk UI
Phase Isolation: Ensure visual animations (pulsing neon, flicker, scanline offsets) are written inside Modifier.graphicsLayer { ... } or Modifier.drawWithContent { ... }. Passing dynamic values into lambdas bypasses both the Recomposition and Layout phases, pushing updates straight to the Draw phase.

Scanline Shader vs. Loop: Replace the procedural drawLine loop with a single GPU gradient fill using a repeating shader brush to reduce CPU overhead to near zero:

```Kotlin
val scanlineBrush = Brush.verticalGradient(
0.0f to Color.Transparent,
0.5f to Color(0x1A00F0FF),
1.0f to Color.Transparent,
startY = 0f,
endY = 4f,
tileMode = TileMode.Repeated
)
Modifier.drawBehind { drawRect(brush = scanlineBrush) }
```

Path Caching: Avoid creating a new Path() inside createOutline on every invocation if size remains static; instantiate or mutate an existing path to avoid minor garbage collection churn.
