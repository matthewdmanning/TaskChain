# Augmented UI Generator

A standalone Jetpack Compose library for turning a compact Kotlin DSL into cyberpunk geometry, effects, and accessible widgets. It has no dependency on TaskChain modules or application state.

## Requirements

- Android `minSdk 24`, `compileSdk 36`
- Java 21 for Gradle; library bytecode remains Java 17 compatible
- Jetpack Compose via the Compose BOM

## Deterministic shape DSL

```kotlin
val prototypeCardSpec = CyberAugSpec(
    topLeft = Cut.Chamfer(16.dp),
    bottomRight = Cut.Scoop(20.dp),
    topEdge = EdgeCut.CenterNotch(width = 40.dp, depth = 6.dp),
    border = BorderSpec(width = 1.dp, color = Color(0xFF00F0FF)),
    inlay = InlaySpec(inset = 4.dp, fill = Color(0x2200F0FF)),
)

val shape = AugmentedShape(prototypeCardSpec)

Box(
    Modifier
        .clip(shape)
        .augmentedFrame(prototypeCardSpec),
)
```

The DSL covers every corner and edge primitive in `augmented-ui-grammar.yaml`. Positions are physical (`topLeft` remains top-left in RTL), and oversized cuts/insets are clamped to their bounds.

## Theme and widgets

```kotlin
CyberThemeProvider {
    val theme = LocalCyberTheme.current

    TelemetryCard(title = "SYSTEM") {
        CyberButton(onClick = ::startRun) {
            BasicText(
                text = "EXECUTE",
                style = theme.typography.label.copy(color = theme.textPrimary),
            )
        }
    }
}
```

Available widgets are `CyberButton`, `TelemetryCard`, and `HexAvatar`. Components read colors, typography, and spacing only through `LocalCyberTheme.current`.

## Effects

- `cyberGlow` reads animated intensity in the graphics phase.
- `cyberScanlines` uses one cached repeating gradient.
- `augmentedFrame` draws cached inlay and border paths from the same spec as clipping.
- `cyberChromaticShift` remembers one AGSL shader on API 33+ and safely becomes a no-op on older Android versions.

## Shape fixtures

Reusable shape inputs are versioned JSON fixtures:

- `fixtures/manual/` contains human-authored inputs.
- `fixtures/agent/` contains AI-authored inputs.
- `build/fixture-output/<fixture-id>/` contains derived galleries, screenshots, and logs.

Both input origins use the same explicit corner, edge, border, and inlay vocabulary. Run the dependency-free contract test before using a fixture:

```powershell
node --test tools\fixture-contract.test.mjs
```

See [docs/testing.md](docs/testing.md) for the JSON contract, naming rules, output placement, agent workflow, and verification boundaries.

## Verify

For fixture-only changes:

```powershell
node --test tools\fixture-contract.test.mjs
```

For library changes:

```powershell
.\gradle-agent.cmd compileDebugKotlin
.\gradle-agent.cmd testDebugUnitTest
.\gradle-agent.cmd lintDebug
```

The checked-in wrapper and build files are contained in this directory, so the library can be moved or versioned independently.
