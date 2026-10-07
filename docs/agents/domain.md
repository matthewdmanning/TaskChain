# Domain Docs

This file is routing guidance. `CONTEXT.md` is the single source of truth for domain vocabulary, behavior invariants, and open decisions; `architecture.md` is the source of truth for package boundaries and technical architecture. Engineering skills should consult the relevant source before exploring or naming project concepts.

## Read Before Work

- Read root `CONTEXT.md` for domain vocabulary, invariants, and open decisions.
- Read `architecture.md` for package boundaries and technical architecture.
- Read ADRs under `docs/adr/` that affect the area being changed, if that directory exists.

Missing ADRs are not a setup error. Create them only when a decision needs to be recorded, rather than scaffolding speculative documentation.

## Use the Project Vocabulary

Use terms from `CONTEXT.md` in tickets, tests, proposals, and code. If a needed term is absent, identify the gap instead of inventing an interchangeable name. Surface any conflict with an existing ADR explicitly rather than silently overriding it. Keep domain behavior definitions in `CONTEXT.md`, not in this routing guide.
