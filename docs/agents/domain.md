# Domain Docs

TaskChain uses a single root context. Engineering skills should consume its domain documentation before exploring or naming project concepts.

## Read Before Work

- Read root `CONTEXT.md` for the glossary, invariants, and open decisions.
- Read `architecture.md` for current package boundaries.
- Read ADRs under `docs/adr/` that affect the area being changed, if that directory exists.

Missing ADRs are not a setup error. Create them only when a decision needs to be recorded, rather than scaffolding speculative documentation.

## Use the Project Vocabulary

Use terms from `CONTEXT.md` in tickets, tests, proposals, and code. If a needed term is absent, identify the gap instead of inventing an interchangeable name. Surface any conflict with an existing ADR explicitly rather than silently overriding it.
