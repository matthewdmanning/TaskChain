# Research reference: Swappable UI architecture in Jetpack Compose

> Historical research for `feature/swappable-compose-ui`; not a claim of implementation. **Correction:** The feature-named provider examples below conflate feature composition with reusable component contracts. Use [the scoped reference index](ui-reference/README.md), especially [separation of concerns](ui-reference/separation-of-concerns.md), as the authoritative guidance for issue #35.

## Executive conclusion

Use **composition over comprehensive abstraction**. Keep TaskChain's core functionality and state transitions independent of visual libraries. Provide a small set of stable semantic contracts for interactions that must remain consistent across providers, and implement complex components as assembled Compose surfaces using Material 3, CyberpunkAndroid, or existing third-party libraries.

Material 3 must be a first-class provider: it should be usable directly within its surface, not reduced to the least-common-denominator API of another library. A Material-themed accordion assembled from standard Compose primitives is a valid implementation even when Material 3 has no direct equivalent.

## Five complementary patterns

| Pattern | Mechanism | Appropriate use |
| --- | --- | --- |
| Theme-driven components | Shared components read colors, typography, shapes, and tokens | Light/dark and other variations within one design system |
| Wrapped library components | Application composables delegate to Material 3 or third-party controls | App defaults, semantics, and haptics around standard controls |
| Swappable implementations | Stable semantic contract dispatches to different component implementations | Runtime switching between materially different design systems |
| Feature composition (not provider contracts) | Application screens assemble generic UI primitives | Routine editor, substep editor, timer presentation |
| Headless/state-driven components | Interaction state and callbacks are separate from rendering | Complex interactions with significantly different visuals |

These patterns can coexist; do not create an abstraction layer merely to hide all Compose APIs.

## How complex assembled components work

Use **slot-based composition**. Define meaningful content and action slots (for example header, expanded editor content, trailing action) rather than exposing every border, icon, animation, or layout detail.

- **Accordion:** The feature owns expanded state, step identity, editing data, ordering, validation, and callbacks. The surface renders header and expanded content and invokes `onExpandedChange`. Material 3 may assemble `Card`, `Row`, `IconButton`, and `AnimatedVisibility`; CyberpunkAndroid may use its own accordion or compose equivalent primitives.
- **Timer:** The runner supplies current step/substep, durations, elapsed/remaining time, progress, completion state, and action callbacks. Cyberpunk may use glowing dials; Material 3 may use progress indicators and cards. Timekeeping and progression never move into either visual implementation.
- **Substep editor:** The feature owns creation, text, duration, ordering, and validation. Each surface controls the appearance and layout of fields and buttons without altering those actions.

A provider switch must not destroy feature state. Hoist state that must survive switching above the replaceable surface. Keep transient ripple, hover, and animation state local to its visual component.

Prefer native Compose slot signatures and preserve relevant `RowScope`/`ColumnScope` contracts. Avoid unintentional API narrowing, especially in wrappers around Material 3 components.

## Suggested TaskChain organization

Start with packages; add Gradle modules only when justified by build or ownership boundaries.

```text
ui/
  designsystem/
    TaskChainTheme.kt
    UiProvider.kt
    components/
      TaskCard.kt
      TaskButton.kt
    cyberpunk/
      CyberpunkComponents.kt
    material/
      MaterialComponents.kt
  builder/
    RoutineBuilderRoute.kt
    components/
      StepAccordion.kt
      SubstepEditor.kt
  runner/
    RoutineRunnerRoute.kt
    components/
      TimerPanel.kt
```

**Domain/ViewModel layer:** routines, substeps, persistence, validation, timer state, actions, and navigation intent.

**Semantic component layer:** state-driven, minimal Compose contracts for interactions that must be equivalent across providers.

**Visual surfaces:** complete Material 3, CyberpunkAndroid, or future implementations. Visual effects are optional implementation details, **not mandatory core API members**.

A single app-level provider preference can be exposed via a `CompositionLocal`. Only semantic components requiring alternate implementations need a dispatch boundary; ordinary Material 3 controls can be used directly within the Material surface.

## Historical illustrative contract (superseded)

**Do not implement this feature-named provider API as written.** It is preserved as a record of the original proposal; the corrected generic accordion contract is in [component contracts](ui-reference/component-contracts.md). This is **not existing TaskChain code**:

```kotlin
/**
 * Displays an expandable routine step using the selected visual surface.
 *
 * @param expanded Current feature-owned expansion state.
 * @param onExpandedChange Requests a change to expansion state.
 * @param title Feature-provided header content.
 * @param modifier Caller-provided layout and accessibility modifier.
 * @param content Feature-provided expanded editor content.
 */
@Composable
fun StepAccordion(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when (LocalUiProvider.current) {
        UiProvider.CYBERPUNK -> CyberpunkStepAccordion(
            expanded, onExpandedChange, title, modifier, content,
        )
        UiProvider.MATERIAL -> MaterialStepAccordion(
            expanded, onExpandedChange, title, modifier, content,
        )
    }
}
```

The two implementation composables and provider local in this example would need to be defined in their respective packages. A real implementation should reflect existing TaskChain APIs and existing CyberpunkAndroid component contracts.

## Decision table

| Decision | Direction | Rationale |
| --- | --- | --- |
| Provider selection | One app-level setting, provided to Compose descendants | Runtime switching without prop-drilling |
| Standard controls | Direct Material 3 within Material implementation; thin adapters as needed | Avoid reimplementing library controls |
| Complex controls | Compose existing primitives behind minimal semantic contracts | Flexible visuals and unchanged actions |
| Feature state | ViewModel or hoisted state holder | Consistency across provider changes |
| Transient visual state | Visual component | Avoid unnecessary ViewModel coupling |
| Effects and decoration | Provider-specific surface | No core API pollution |
| Content slots | Standard composable lambdas and appropriate scopes | Compose idioms and flexibility |
| Verification | State/interaction tests, accessibility tests, provider-switch tests, targeted screenshot tests | Visual differences are allowed; functional regressions are not |

## When to add an abstraction

Introduce a shared contract when multiple providers must preserve the same interaction, a composed component is reused, library APIs would otherwise leak into feature logic, or a stable accessibility/testing boundary is necessary. Do **not** introduce `UiIcon`, `UiGlow`, `UiBorder`, `UiSpacer`, etc. simply to represent every possible provider primitive.

## Migration plan

1. Retain the existing provider preference and runtime selection mechanism.
2. Correct existing wrappers to preserve Compose API and scope semantics.
3. Extract the routine step accordion as the first complete composed semantic component.
4. Implement both CyberpunkAndroid and Material presentations without changing expansion, editing, or validation logic.
5. Apply the same approach to runner presentation, keeping all timer mechanics outside visual surfaces.
6. Verify interactions, accessibility, provider switching, and existing behavior before broad migration.

## Reference implementations and documentation

1. **Android Developers — Compose layering:** https://developer.android.com/develop/ui/compose/layering — composable UI layers and custom component composition.
2. **Android Developers — Custom design systems:** https://developer.android.com/develop/ui/compose/designsystems/custom — wrapping Material components and replacing design-system implementations.
3. **AndroidX — Compose component API guidelines:** https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md — slot APIs, composable conventions, and state/structure concerns.
4. **Google — Now in Android:** https://github.com/android/nowinandroid — reference application separating reusable design-system elements and application-specific composed UI.
5. **Now in Android modularization documentation:** https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md — organization of UI and feature modules.
6. **Compose Unstyled:** https://github.com/composablehorizons/compose-unstyled — third-party example of behavior-focused components styled for different design systems.

These sources illustrate complementary patterns. They do **not** establish that any one example implements runtime switching between CyberpunkAndroid and Material 3.

## Scope and implementation status

This document is a research reference, not a completed implementation specification. The existing `docs/swappable-compose-ui.md` records current TaskChain migration status and concrete implementation traps. Preserve domain behavior and verify compilation and tests before treating provider migration as complete.
