# Swappable UI reference index

These scoped references support [issue #35](https://github.com/matthewdmanning/TaskChain/issues/35) and the `feature/swappable-compose-ui` branch. They define architectural intent, **not** a claim that the migration is implemented.

1. [Separation of concerns](separation-of-concerns.md) — ownership, boundaries, and prohibited dependencies.
2. [Generic component contracts](component-contracts.md) — state, callbacks, slots, accessibility, and provider adapters.
3. [Library compatibility and verification](library-compatibility.md) — Android Java/Kotlin candidate evaluation and migration test gates.

See also [existing implementation mapping](../swappable-compose-ui.md) and [historical architecture research](../swappable-ui-research.md). The latter originally used feature-named provider examples; this reference set supersedes that framing.

**Rule:** UI providers render generic controls. Application/feature code composes those controls and owns domain decisions. A provider switch must not change business behavior.
