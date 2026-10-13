# Free Android Java/Kotlin UI library evaluation

## Selection criteria

Do **not** restrict candidates to Jetpack Compose. Evaluate free libraries usable from Java/Kotlin on Android that can coexist with or integrate into a Material 3 app. Distinguish (a) Android/Compose runtime interoperability, (b) Material 3 visual token compatibility, and (c) direct generic widget coverage. A complete routine editor or timer engine is **not** a library component requirement.

## Candidate shortlist (not a popularity ranking)

| Library | Technology | Material 3 relationship | Relevant generic capability | Limitation |
| --- | --- | --- | --- | --- |
| [Material Components for Android](https://github.com/material-components/material-components-android) | Android Views, Java/Kotlin | Material-themed View components | Buttons, text fields, cards, navigation, dialogs | View-to-Compose adapter needed; verify maintenance/version |
| [MaterialDrawer](https://github.com/mikepenz/MaterialDrawer) | Android Views, Java/Kotlin | Material-themed navigation | Drawer, navigation items, badges | Specialized, not a broad control catalog |
| [Epoxy](https://github.com/airbnb/epoxy) | RecyclerView models, Java/Kotlin | Hosts Material-styled views | Reusable list/item rendering | Composition framework, not button/accordion catalog |
| [Balloon](https://github.com/skydoves/Balloon) | Kotlin; APIs vary by release | Styleable overlays | Tooltip/popover/anchored overlay | Specialized; verify Compose vs View version |
| [Litho](https://github.com/facebook/litho) | Java/Kotlin declarative Android UI | Can coexist with Material-themed app | Component/layout composition | Independent rendering system; high integration cost |

These are **candidate examples**, not a verified top-five ranking, dependency recommendation, or assurance of compatibility with TaskChain's pinned Android/Compose/Kotlin versions. Inspect current licenses, releases, supported Android API levels, maintenance, and exact artifact APIs before adoption. A View library can be hosted via `AndroidView`; its theme is not automatically synchronized with Compose `MaterialTheme`.

## Compare primitives, not features

| Generic control | Match criterion | Exclude from component assessment |
| --- | --- | --- |
| Disclosure/accordion | Controlled expansion, slots, semantics | Step editing rules |
| Text/numeric input | Value, focus, keyboard, errors | Duration parsing and validation policy |
| Card | Content slot, click/selection semantics | Routine totals and start logic |
| Button | Click, enabled state, native content scope | Substep creation |
| Reorderable item | Gesture and proposed movement | Permitted order and persistence |
| Progress | Render supplied progress | Timer progression and audio |

## Evaluation gates

1. License and maintenance verified for a pinned release.
2. Compatible with TaskChain's Android API, Kotlin, Gradle and Compose versions; compile a minimal adapter.
3. Material 3 color/typography/shape mapping evaluated, not assumed.
4. Generic contract preserves callback count, accessibility, focus and touch targets.
5. Feature behavior regression tests pass unchanged when switching providers.

This document corrects the earlier Compose-only and feature-editor-based library comparison. It does not assert that any candidate has been integrated.
