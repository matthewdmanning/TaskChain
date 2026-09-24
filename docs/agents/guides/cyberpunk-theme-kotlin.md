# Cyberpunk Kotlin Implementation Guide

## Architecture for a Full UI Setup

A functional UI setup requires a cohesive design system architecture rather than isolated canvas modifiers:

### Theme & Token Architecture

A production setup must define custom theme containers via CompositionLocalProvider (e.g., @Immutable data class CyberpunkColors, CyberpunkTypography, CyberpunkShapes) rather than relying on Google's MaterialTheme, which enforces rounded corners and Material styling tokens.

### Component Primitives

It must have reusable implementations for interactive controls (angled buttons, clipped card containers, HUD telemetry text fields, and custom ripple/indication states).

### API Compatibility Fallbacks

Both RenderEffect (API 31+) and AGSL RuntimeShader (API 33+) will crash or fail to render on older Android versions. A complete setup requires API branching to handle legacy fallbacks (e.g., multi-layered offset text for glitching, static 9-patch assets for glows).

## Load-Bearing Performance Safeguards

The performance guide [cyberpunk-performance.md] covered phase isolation and basic loop avoidance, but omitted the primary failure modes that cause dropped frames on mobile GPUs:

### Offscreen Buffer Allocation (FBO Spikes)

Applying RenderEffect or setting an alpha < 1f inside graphicsLayer forces Compose and the Skia rendering engine to allocate an offscreen buffer (Frame Buffer Object) per element. Applying blurs across multiple nested cards will exhaust GPU memory bandwidth and cause severe frame drops on mid-tier hardware.

### GPU Overdraw & Fill-Rate Saturation

Stacking semi-transparent card panels (rgba), backdrop blurs, scanline overlays, and glow borders forces the GPU to shade the same screen pixels 4 to 6 times per frame. In Jetpack Compose, this must be mitigated by removing redundant backgrounds on parent containers and strictly bounding translucent modifiers with Modifier.clip().

### Zero-Allocation Draw Phase Rule

Creating objects (Brush, Path, Paint, or lambdas) inside drawBehind or drawWithContent causes allocations on every single frame during animations. All geometry and brushes must be hoisted into a remember block or managed inside a custom Node modifier (DrawModifierNode) to prevent GC churn and hitching.

### Shader Compilation Overhead

AGSL RuntimeShader parsing incurs CPU compilation overhead. The shader string must be instantiated once at the singleton or class level, rather than parsed inside a Composable recomposition scope.

## augmented-ui (augmented-ui.com)

A pure CSS library designed to generate angular, cybernetic container geometry. It uses HTML data-augmented-ui attributes to construct chamfered corners, scoop cuts, clipped borders, and embedded insets without requiring manual SVG masks or canvas rendering.

To achieve this aesthetic natively in Kotlin—specifically with Jetpack Compose—you recreate the core visual primitives using Compose's geometry, canvas, and graphics APIs.

## Native Jetpack Compose Equivalents

### Chamfered / Angled Containers (Replaces augmented-ui)

Create a custom Shape using Compose Path to cut 45° chamfers or notches on container corners:

```Kotlin
fun Modifier.scanlines(stepPx: Float = 6f, color: Color = Color(0x1A00F0FF)) = this.drawWithContent {
    drawContent()
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += stepPx
    }
}
```

### Glitch & Chromatic Aberration

Use AGSL (Android Graphics Shading Language) with RuntimeShader on API 33+ (Android 13+) to apply real-time RGB split shaders directly to any Composable.

For lower API targets, duplicate text elements with an animated Offset and alpha channel in Cyan and Magenta behind the primary text layer.

### Hardware Borders & Reticles

Apply thin, high-contrast borders directly with Modifier.border(1.dp, Color(0xFF00F0FF), shape) using your custom shapes.
