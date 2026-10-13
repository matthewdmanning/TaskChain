# Separation of concerns: application, feature, component, provider

Related: [issue #35](https://github.com/matthewdmanning/TaskChain/issues/35).

## Ownership matrix

| Concern | Owner | Examples |
| --- | --- | --- |
| Domain rules and persistence | Domain/data/application services | Step identity, valid duration, persisted routine ordering, timer progression, sound scheduling |
| Feature state and actions | ViewModel/state holder and feature route | Editing, validation, selection, expanded step, binding UI events to commands |
| Feature composition | Builder, runner, home composables | Arranging generic fields, buttons, cards, disclosures and progress displays |
| Generic component contract | UI design-system boundary | Values, interaction callbacks, content slots, accessibility semantics |
| Provider implementation | Material 3, CyberpunkAndroid, or third-party adapter | Visuals, layout, focus/ripple/animation, component-specific transient state |

## Boundary rules

- Generic provider interfaces **must not** import `RoutineId`, step/substep entities, ViewModels, repositories, or timer engines.
- An accordion accepts `expanded`, `onExpandedChange`, trigger/header and content; it does not own the step being edited.
- A text field emits edited text; it does not persist, validate, or interpret a routine title or duration.
- A drag control reports gestures or proposed movement; application logic validates and commits ordering.
- A progress display consumes supplied progress; it does not calculate time, advance tasks, or play audio.
- A feature-specific `RoutineStepEditor` may compose generic controls, but it is **not** a provider contract or a library primitive.
- The current `UiComponents` registry (`button`, `switch`, `card`) is generic; the issue concerns architectural guidance and potential future coupling, not an established domain-logic defect in that registry.

## Review questions

1. Can the generic component be used in an unrelated screen without domain terminology?
2. Does a provider change preserve the same state transitions and emitted application commands?
3. Is persistent state owned above the replaceable surface, with only transient visual state local?
4. Does each component preserve accessibility and native API semantics?
5. Is the component abstraction justified by more than one actual use or provider need?

Do not relocate working timer or routine logic merely to simplify a visual implementation. Record concrete code violations separately from speculative risks.
