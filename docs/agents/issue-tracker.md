# Issue Tracker: Local Markdown

Issues and specs for this repository live under `.scratch/issues/`.

## Conventions

- Use one directory per feature: `.scratch/issues/<feature-slug>/`.
- Put the feature spec in `spec.md` and implementation tickets in separate `<NN>-<slug>.md` files, numbered from `01` within that feature.
- Record triage state as a `Status:` line near the top of each ticket. The allowed role strings are in `triage-labels.md`.
- Append conversation under `## Comments` at the bottom of the ticket.

## Skill Operations

When a skill says to publish a ticket, create its Markdown file in the feature directory. When it says to fetch a ticket, read the referenced path; the user may supply a path or ticket number.

For wayfinding work, put `map.md` and numbered child tickets in `.scratch/issues/<effort-slug>/`. A child ticket may include `Type: research`, `prototype`, `grilling`, or `task`; `Status: claimed` or `resolved`; and `Blocked by: NN, NN`. Claim an open, unblocked ticket before working, then record its answer under `## Answer`, mark it resolved, and add a brief decision pointer to the map.
