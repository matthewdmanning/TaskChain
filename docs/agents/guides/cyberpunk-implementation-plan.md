# Cyberpunk Theme Implementation Plan

## Slice 1: Core Theme & Tokens

- **Boundary:** Pure data definitions for colors, typography, and spacing. This layer contains no UI logic, rendering commands, or state management.
- **Tasks:** Define immutable data classes for the neon color palette, setup monospaced typography, and establish the `CompositionLocalProvider` to distribute these values.
- **Contract:**
- `@Immutable data class CyberTheme(val cyanGlow: Color, val surfaceDark: Color...)`
- Must be accessed strictly via `LocalCyberTheme.current`. Components cannot contain hardcoded hex values.

## Slice 2: Geometry Engine (Shapes)

- **Boundary:** Mathematical path generation and bounding. It dictates form and clipping operations, remaining completely unaware of colors, borders, or application state.
- **Tasks:** Implement the Compose `Shape` interface to recreate the `augmented-ui` features, including chamfered corners, notch cutouts, and asymmetric angles.
- **Contract:**
- Classes must implement `androidx.compose.ui.graphics.Shape`.
- Must output `Outline.Generic(path)`.
- Signature: `class ChamferedShape(val topLeftCut: Dp, val bottomRightCut: Dp) : Shape`

## Slice 3: VFX & Render Pipeline (Modifiers)

- **Boundary:** Visual effects—such as glows, scanlines, borders, and shaders—applied over the geometry defined in Slice 2. It does not manage layout or business logic.
- **Tasks:** Write custom `Modifier` extensions encapsulating `drawBehind`, `graphicsLayer`, and AGSL `RuntimeShader` operations. Hoist all object allocations outside the draw phase.
- **Contract:**
- Extension functions strictly on `Modifier`.
- Parameters must accept primitive types or `State<Float>` to allow hardware-accelerated animation without triggering CPU layout passes.
- Signature: `fun Modifier.cyberGlow(color: Color, radius: Dp, intensityState: State<Float>): Modifier`

## Slice 4: Component Widgets (Interactive UI)

- **Boundary:** Stateful, interactive UI elements. This layer acts as the consumer, assembling Slices 1, 2, and 3 into usable interfaces like buttons and cards.
- **Tasks:** Build reusable `CyberButton`, `TelemetryCard`, and `HexAvatar` composables. Handle touch feedback, state changes, and accessibility semantics.
- **Contract:**
- Standard Composable signatures that must accept an optional `Modifier` as the first optional parameter to allow parent overrides.
- Signature: `@Composable fun CyberButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit)`
