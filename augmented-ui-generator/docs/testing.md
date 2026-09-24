---
title: Testing and fixture conventions
applies_to: augmented-ui-generator
---

# Testing and fixture conventions

Use this note when adding a shape case, accepting AI-generated shape definitions, producing a gallery, or verifying library behavior. Related: [[README]] and [[AGENTS]].

## Current test layers

| Layer | Purpose | Command |
|---|---|---|
| Fixture contract | JSON structure, naming, origins, primitives, dimensions, and colors | `node --test tools\fixture-contract.test.mjs` |
| JVM unit tests | Geometry rules that can run without Android graphics | `gradle-agent.cmd testDebugUnitTest` |
| Kotlin compilation | Compose and Android integration diagnostics | `gradle-agent.cmd compileDebugKotlin` |
| Android lint | Android and Kotlin static diagnostics | `gradle-agent.cmd lintDebug` |

The module uses JUnit 4. It has no dependency injection, mocking, Robolectric, UI-test, screenshot-test, or end-to-end framework. Do not add one merely to validate fixture files.

Fixture validation is structural. It does not prove that a browser gallery rendered or that Compose produced the intended pixels. Compose remains the authoritative geometry implementation; browser output is approximate and requires separate human review.

## Placement

```text
augmented-ui-generator/
|-- fixtures/
|   |-- manual/       # Human-authored JSON inputs
|   `-- agent/        # AI-authored JSON inputs
|-- tools/            # Dependency-free fixture checks
|-- src/main/         # Supported library implementation only
|-- src/test/         # Load-bearing JUnit tests only
`-- build/
    `-- fixture-output/<fixture-id>/  # Derived galleries, screenshots, and logs
```

Do not put generated families, preview catalogs, galleries, screenshots, or run logs in `src/main/` or at the module root. Do not put JSON inputs under `build/`; inputs must remain reviewable and reproducible.

## Naming

- Manual fixture: `fixtures/manual/<purpose>.shape.json`
- Agent fixture: `fixtures/agent/<yyyy-mm-dd>-<producer>-<purpose>.shape.json`
- Fixture `id`: exactly the filename without `.shape.json`
- Shape `id`: unique within its fixture and written in kebab-case
- Derived output directory: `build/fixture-output/<fixture-id>/`

Examples:

```text
fixtures/manual/basic-chamfer.shape.json
fixtures/agent/2026-09-18-codex-gutter-punk-family-4.shape.json
build/fixture-output/2026-09-18-codex-gutter-punk-family-4/shape-gallery.html
```

Use a stable producer name such as `codex`, `claude`, or a project agent profile name. Never include a user name, machine path, session secret, or credential in fixture metadata.

## JSON contract

Both origins use schema version `1` and the same shape vocabulary. Agent fixtures additionally require `producer` and `createdOn`.

```json
{
  "schemaVersion": 1,
  "id": "basic-chamfer",
  "origin": "manual",
  "description": "Why this fixture exists.",
  "shapes": [
    {
      "id": "prototype-card",
      "corners": {
        "topLeft": { "type": "chamfer", "sizeDp": 16 },
        "topRight": { "type": "none" },
        "bottomRight": { "type": "none" },
        "bottomLeft": { "type": "none" }
      },
      "edges": {
        "top": { "type": "center-notch", "widthDp": 40, "depthDp": 6 },
        "right": { "type": "none" },
        "bottom": { "type": "none" },
        "left": { "type": "none" }
      },
      "border": { "widthDp": 1, "color": "#FF00F0FF" },
      "inlay": null
    }
  ]
}
```

Rules enforced by the contract test:

- IDs use lowercase kebab-case.
- Every shape declares all four physical corners and edges; use `none` explicitly.
- Dp values are finite and non-negative.
- Colors use Compose-style `#AARRGGBB`.
- Corner and edge type names mirror `Cut` and `EdgeCut` using kebab-case.
- Agent IDs begin with their `createdOn` date and `producer`.

## Agent workflow

1. Choose `manual` or `agent` from the author of the input, not from who runs the test.
2. Copy the closest fixture and give the new file and root `id` the required name.
3. Keep all requested shapes in that fixture; do not create production Kotlin declarations for a test run.
4. Run `node --test tools\fixture-contract.test.mjs`.
5. Run `gradle-agent.cmd testDebugUnitTest` when the library implementation changed.
6. Put derived artifacts under `build/fixture-output/<fixture-id>/`.
7. Report fixture-contract, JVM, browser, Compose preview, and device evidence separately. Never infer one layer from another.
