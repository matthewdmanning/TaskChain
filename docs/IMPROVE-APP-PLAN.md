# Improve App Plan

## Context
Started 2026-09-25. TaskChain: a local-only Android routine builder and step-by-step runner.
Single Android module, no network service, no accounts, no monetization.

Intake answers:
- Job (user's words): "When I have a multi-step routine I keep half-finishing, I want the next
  step handed to me, so I can finish without holding the list in my head."
- Roughest today: amateur visual design; dead interactions.
- Evidence: none. The user is the main user. Every finding is unvalidated opinion until tested.
- Platform: native Android only. No browser surface.
- Upsell surfaces: none. Local-only and account-free by design.
- Highest-friction flow: building a routine.
- Existing docs: no CUSTOMER.md, DESIGN.md, POSITIONING.md, or PRODUCT.md. A severity-rated
  Android UX audit exists at docs/agents/ux-audit-implementation-notes.md (commit 06b0611,
  recorded 2026-09-18). Domain terms live in CONTEXT.md and architecture.md.

## Phase Status
| Phase | Skill | Status | Artifact | Date |
|---|---|---|---|---|
| 1 | jobs-to-be-done | done | CUSTOMER.md | 2026-09-25 |
| 2 | ux-heuristics | done | DESIGN.md, EXPERIMENTS.md | 2026-09-25 |
| 3 | design-everyday-things | pending | DESIGN.md, EXPERIMENTS.md | |
| 4 | refactoring-ui | pending | DESIGN.md, EXPERIMENTS.md | |
| 5 | microinteractions | pending | DESIGN.md, EXPERIMENTS.md | |
| 6 | made-to-stick | pending | POSITIONING.md, EXPERIMENTS.md | |
| 7 | influence-psychology | skipped: local-only app, no paywall or upsell surface | POSITIONING.md, EXPERIMENTS.md | 2026-09-25 |
| 8 | high-perf-browser | skipped: native Android, no browser surface | DESIGN.md, EXPERIMENTS.md | 2026-09-25 |
| 9 | steve-jobs-design-review | pending | PRODUCT.md, DESIGN.md, EXPERIMENTS.md | |
Statuses: pending - in-progress - awaiting-evidence - done - deferred: <reason> - skipped: <reason>

## Key Decisions
| Date | Phase | Decision | Rationale |
|---|---|---|---|
| 2026-09-25 | Intake | Skip Phase 7 | The app has no paywall, upgrade prompt, or trial. Nothing to persuade at. |
| 2026-09-25 | Intake | Skip Phase 8 | Native Android only. Perceived speed is a Phase 5 concern here, not a web-vitals one. |
| 2026-09-25 | Intake | Run Phases 4 and 5 after 2 and 3 | The user feels visuals and feedback, but no visual change ships without a Phase 1-3 finding behind it. |
| 2026-09-25 | Intake | Hold Phase 6 at pending | Decide after the Phase 2 audit shows whether copy is a real friction source. |
| 2026-09-25 | Intake | Target Phase 2 at the routine builder | The user named it as the highest-friction flow. |
| 2026-09-25 | 1 | Job statement: "When my attention is low but the routine still has to happen, I want to stop choosing what to do next, so I can act without spending willpower on order." | Names the low-attention circumstance and the decision load, not the app. |
| 2026-09-25 | 1 | Worst underdelivered dimension is functional | The runner cannot pause, the final step always confirms, the resume point after a skipped step is undecided, and starting still demands a choice. |
| 2026-09-25 | 1 | The leak is a Little Hire failure | The user chose daily-run friction over first-run authoring cost. |
| 2026-09-25 | 1 | Retarget Phases 2-5 from the builder to the runner | Follows the Little Hire decision. The builder drops to repeated-edit cost, a secondary surface. |
| 2026-09-25 | 1 | The social dimension is a deliberate non-target | Sharing or accountability would contradict the local-only, account-free constraint. |
| 2026-09-25 | 2 | Put active-run clarity and Pause/Resume in the first fix pass | Both severity-3 daily-run issues were selected by the user. |
| 2026-09-25 | 2 | Order implementation by ease within each severity tier | The user chose option 2 for backlog ordering; task observations will validate frequency. |
| 2026-09-25 | 2 | Treat the audit as source-based until a device walkthrough | No current rendered walkthrough or non-maintainer user evidence exists. |

## Next Actions
- [ ] Run the Phase 2 scripted task checks on a current device build before treating the severity and frequency estimates as observed (agent, maintainer)
- [ ] Enter Phase 3, design-everyday-things, on the daily run path (user, agent)
- [ ] Decide whether to add the optional continuous-discovery phase, since no real-user evidence exists (user)
- [ ] Close the open CONTEXT.md decision: where the run resumes after completing a previously skipped step (user)
