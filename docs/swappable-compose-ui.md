# Swappable Compose UI mapping and implementation notes

> Feature: [#26](https://github.com/matthewdmanning/TaskChain/issues/26). Branch: `feature/swappable-compose-ui`.
> This is an incremental migration record, **not** a declaration that both themes are complete.

## Ownership and boundaries

`ui/designsystem` owns library-specific UI implementation selection. Feature screens consume state and emit callbacks; ViewModels retain business behavior. `UiComponents` supplies **composable functions** via `LocalUiComponents`. It does not precompose UI outside the composition. `TaskChainTheme` currently still installs CyberTheme and its Material mappings. Consequently, choosing `MaterialUiComponents` only replaces the provided components; it does **not** switch the full app to an independent Material color/typography theme yet.

Keep provider contracts small and semantic; add slots only when an actual screen requires them. Prefer the established Compose APIs and existing TaskChain wrappers to bespoke implementations. Preserve press feedback, accessibility, timer behavior, and configured semantic tokens.

## Current component mapping

| TaskChain semantic control | CyberpunkAndroid implementation | Material 3 implementation | Status / caveat |
| --- | --- | --- | --- |
| Basic action button | `CyberButton` (primary) | `Button` | Implemented in `UiComponents.button`; no screen migration yet. Content is a plain composable slot, **not** Material's `RowScope` slot. |
| Binary toggle | Existing Material `Switch` under CyberTheme | `Switch` | Implemented in `UiComponents.switch`; `LabeledSwitchRow` uses provider. Entire row owns toggle semantics; nested switch is cleared. |
| Outlined action | `CyberButton(style = Outline)` | `OutlinedButton` | Pending: requires shared variant contract and press-feedback parity. |
| Ghost/text action | `CyberButton(style = Ghost)` | `TextButton` | Pending. |
| Elevated/content card | `CyberCard` | `Card` | Implemented for non-interactive cards via `TaskCard`; `ColumnScope` preserved. Cards with headers or interactive semantics remain pending. |
| Text entry | `CyberTextField` / `CyberTextArea` | `TextField` / `OutlinedTextField` | Pending: compare value types, labels, placeholder, errors, keyboard actions, and focus behavior. |
| Accordion / expanded step editor | `CyberAccordion` | Custom `Card` + `AnimatedVisibility` or equivalent | Pending: domain-specific composition, no direct Material 3 primitive. |
| Checkbox | `CyberCheckbox` | `Checkbox` | Pending. |
| Slider | `CyberSlider` | `Slider` | Pending. |
| Dropdown | `CyberDropdown` | `ExposedDropdownMenuBox` or `DropdownMenu` | Pending; distinguish editable vs. selection-only. |
| Modal/alert | `CyberModal` / `CyberAlert` | `AlertDialog` | Pending; verify dismiss/back semantics. |
| Progress/loading | `CyberProgress` / `CyberSpinner` | Progress indicators | Pending. |
| Navigation/tabs | `CyberNavigationBar`, `CyberTabs`, `CyberNavLink` | Navigation bar, tabs, navigation item | Pending; TaskChain currently uses Material navigation in places. |
| Badges/status | `CyberBadge` / `CyberNavStatus` | `Badge` / composed status text | Pending. |
| Tables | `CyberTable` | Compose rows/columns/lazy lists | Pending; no exact Material table primitive. |
| Decorative/runner visuals | `CyberDialTicks`, `CyberSectorRim`, `CyberRim`, `CyberHexGrid`, `CyberGlowIcon`, effects and icons | TaskChain-specific Canvas / modifiers / icon adapters | Pending; preserve run-state semantics and dial geometry. |
| Cyberpunk-only decoration | `CyberDecrypter`, `CyberTerminal`, `CyberLaserText`, `CyberPixelTransition`, `CyberSkeleton`, `CyberBiometrics` | Optional composed fallbacks | Pending; do not require meaningless one-to-one Material APIs. |

This matrix is a first-pass catalog. Verify the **published v1.0.8** API before binding any unimplemented signature; repository `main` may differ from the published artifact.

## Migration sequence

1. Audit each screen's direct CyberpunkAndroid and Material imports, including existing TaskChain press-feedback wrappers.
2. Complete semantic provider contracts for controls actually used; retain existing row/scope semantics when necessary.
3. Supply both implementations and verify previews and Compose UI tests.
4. Move library-specific configuration to the design-system boundary.
5. Migrate feature screens in small batches without altering ViewModel or domain run invariants.
6. Introduce persisted provider selection separately from the existing System/Light/Dark appearance setting.
7. Test JDK 17 compilation, JVM tests, Android lint and isolated device UI behavior before closing issues.

## Traps and safeguards

1. **Kotlin Compose scope mismatch:** Material `Button` content is `@Composable RowScope.() -> Unit`, while `CyberButton` uses `@Composable () -> Unit`. A plain content slot can be passed to both, but it cannot expose `RowScope` modifiers. Do not rename parameters and assume signatures match.
2. **Haptics bypass:** TaskChain's `ui/PressFeedback.kt` wrappers add vibration. Directly using library buttons would silently remove it. Providers must call `pressAction` exactly once; avoid double wrapping.
3. **CyberTheme vs component provider:** The initial app boundary always uses CyberTheme. A Material component adapter does not constitute a full Material theme. Color, typography, shapes, and effects need a separately coordinated provider.
4. **Material-backed Cyberpunk controls:** The existing Cyberpunk app already uses Material controls (e.g. switch and navigation) under CyberTheme. Their provider mapping may intentionally use the same primitive until an equivalent Cyberpunk implementation is verified.
5. **Accessible toggle row:** The whole `LabeledSwitchRow` is toggleable; the nested switch must not become a second accessibility target.
6. **Timer/run invariants:** Subtask clocks, main timer position, gesture semantics, advancement sounds, and persisted timestamps must remain in existing domain/run and runner boundaries.
7. **Published library version:** Do not assume CyberpunkAndroid GitHub `main` matches TaskChain's pinned `v1.0.8`.
8. **Testing environment:** The repository requires complete JDK 17 and the pinned Gradle wrapper; GitHub file edits do not prove a build passes. Never run destructive instrumentation against the regular app's user data.
9. **API stability:** Propose upstream CyberpunkAndroid argument-name changes as separate, reviewed changes with compatibility handling; prefer adapter fixes over breaking published consumers.

## KDoc conventions

Use Kotlin **KDoc** (`/** ... */`), not Java-specific Javadoc syntax, for Kotlin declarations. Document intended behavior, arguments, return values, accessibility/interaction semantics, and theme dependencies on public and internal reusable APIs. The provider API and the press-feedback seam are documented in source. Expand KDoc with each migrated component rather than adding misleading boilerplate to untouched files.

## Architectural clarification: preserve TaskChain behavior and compose full UI implementations

TaskChain domain and ViewModel state is authoritative for routine editing, accordion expansion, substep editing, duration handling, timer progression, persistence, and navigation. A UI provider is responsible only for presenting that state and dispatching user intent through callbacks.

Material 3 is a **first-class implementation**, not a fallback or a lowest-common-denominator subset. Prefer direct Material 3 controls when their semantics match the TaskChain contract. For controls without an exact Material 3 counterpart (including accordion lists), compose a Material-themed implementation from existing Jetpack Compose or installed library primitives, keeping it outside feature screens. Do the equivalent with CyberpunkAndroid. A composable built from multiple library primitives is a normal component, not a workaround.

Proposed structure:

- `ui/designsystem/contracts/`: TaskChain semantic composable contracts and state/callback types.
- `ui/designsystem/cyberpunk/`: CyberpunkAndroid-backed implementation and visual effects.
- `ui/designsystem/material/`: Material 3-backed implementation, including composed accordions and timer presentation.
- `ui/designsystem/components/`: library-neutral domain-level wrappers consumed by feature screens.
- `ui/builder`, `ui/runner`, etc.: orchestration, state collection, and navigation only.

Keep existing behavior during migration. Avoid moving expansion state, validation, time calculations, audio hooks, or persistence into presentation adapters. Provider-specific animation and visual feedback may differ, but actions, semantics, accessibility, and state transitions must remain equivalent. A direct Material 3 API may be used inside the Material adapter; dynamic provider selection still requires a Compose dispatch boundary.

**Migration trap:** replacing an entire accordion with a generic card or assuming Material 3 lacks accordion patterns would regress feature semantics. Map full domain-level controls and their interactions, not only button/switch/card primitives.

## Implementation checkpoint — provider persistence and initial screen migration

- Persisted `selectedUiProvider` independently of `selectedTheme` using the existing preference repository and DataStore.
- The application resolves unknown IDs to Cyberpunk and supplies either the Cyberpunk or Material 3 component provider. The Material option installs a Material 3 theme overlay while retaining the outer CyberTheme for unmigrated screens.
- Settings exposes a provider selector. This is a **partial preview**: many controls, screens, typography choices, effects and layout surfaces still use Cyberpunk directly.
- `TaskCard` now preserves `ColumnScope` and is used for simple routine cards in Home and Builder. Cyberpunk cards with named header/footer slots remain unchanged until an equivalent semantic contract is needed.
- Added tests for legacy preference decoding, provider round-trip and unknown-ID fallback (not yet executed).

### Additional traps

- **Named arguments on function-type values:** Kotlin does not allow named arguments when invoking a function-type property. Call `ui.switch(checked, null, modifier)` rather than `ui.switch(checked = checked, ...)`. Prefer a named wrapper composable for public feature-screen use.
- **Card scope:** A `ColumnScope` content slot is necessary to preserve `weight`, `align` and related layout behavior. A generic `@Composable () -> Unit` card slot would silently narrow the API.
- **Incomplete appearance:** Material provider selection is not yet a full-screen material skin. Keep the selection labeled and documented as partial until direct Cyberpunk calls have been migrated.
- **Build verification:** Repository edits made via GitHub API are not compilation evidence. Run the pinned Gradle wrapper and CI before merging.
