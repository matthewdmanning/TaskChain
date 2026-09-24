# Compact Intermediate Representation (IR)

Output prototypes solely as a structured configuration object (JSON or Kotlin DSL), never raw canvas drawing commands.

## Orthogonal Grammar for Design Space

Read [augmented-ui-grammar.yaml].

### Agent Output Example

Kotlin

```val PrototypeCardSpec = CyberAugSpec(
topLeft = Cut.Chamfer(16.dp),
bottomRight = Cut.Scoop(20.dp),
topEdge = EdgeCut.CenterNotch(width = 40.dp, depth = 6.dp),
border = BorderSpec(width = 1.dp, color = Color(0xFF00F0FF)),
inlay = InlaySpec(inset = 4.dp, fill = Color(0x2200F0FF))
)
```

## Deterministic Execution Pipeline

The agent defines intent; your native runtime handles geometry, borders, and inlays:

│ Agent Prompt (Grammar Schema) │ ----> │ Compact JSON/DSL (AugSpec Definition) │ ----> │ Native Geometry Engine (Path + Inset Math) │

## Border & Inlay Rendering

In native Compose, matching augmented-ui’s borders and inlays requires drawing concentric paths. Your pre-built runtime engine implements path:

```math
deflating:$$\text{insetPath} = \text{generatePath}(\text{spec}, \text{offset} = \text{inlayInset})$$
```

The agent just provides inlay = InlaySpec(4.dp); the engine does the perimeter math to scale the inner contour uniformly.
