# Generic component contract guidance

## Shape of an interface

Use native UI toolkit primitives where possible. A shared contract is appropriate when provider switching must preserve interaction semantics. Avoid a universal registry of every visual element.

A generic, **illustrative** Compose disclosure contract:

```kotlin
/**
 * Displays expandable content without owning the meaning of that content.
 *
 * @param expanded Whether the content is currently visible.
 * @param onExpandedChange Requests a new expansion state; the caller decides whether to apply it.
 * @param modifier Caller-owned layout and semantics modifier.
 * @param header Content rendered as the disclosure trigger.
 * @param content Content rendered when expanded.
 */
@Composable
fun AppAccordion(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
)
```

This is **not** a claim that `AppAccordion` exists or should be implemented before a concrete need is established. No step IDs, validation, duration parsing, or timer actions belong in its signature.

## Contract checklist

- **Controlled state:** values are inputs, changes are requests/callbacks. The caller owns durable state.
- **Slots:** allow arbitrary content without introducing feature-domain types; preserve relevant `RowScope` or `ColumnScope` when wrapping native Compose APIs.
- **Semantics:** expose role, selected/expanded state, labels, focus, keyboard support, and adequate touch targets.
- **Provider behavior:** keep visual effects, motion, and layout local; do not duplicate haptic handling or business callbacks.
- **Interop:** Android View components can be hosted in Compose with `AndroidView`, but require explicit lifecycle, focus, state, accessibility, and theme mapping checks. Android compatibility does not imply automatic Material 3 token parity.
- **Styling:** use provider semantic tokens first, existing library APIs next, and configurable settings rather than hardcoded constants.
- **Tests:** component callback/semantics tests plus feature tests for validation, ordering, timer transitions and provider-switch continuity.

## TaskChain application examples

| UI primitive | Feature composition | Behavior outside provider |
| --- | --- | --- |
| Accordion/disclosure | Builder's expandable row | Which step is expanded |
| Text field | Title or duration editor | Parsing, validation, persistence |
| Icon button | Add substep action | Creating and inserting a substep |
| Drag interaction | Reorderable row | Permitted moves and resulting order |
| Progress ring/text | Runner countdown display | Clock, pause/resume, completion and sounds |

The existing `UiComponents` button signature narrows Material 3's `RowScope` content API; evaluate scope preservation independently of the separation-of-concerns issue.
