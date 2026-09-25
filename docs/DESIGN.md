# Design System

## Design Direction

Phase 2 audits the daily run from Home through finishing. Visual direction is deferred to Phase 4. Findings are source-based hypotheses; no current device walkthrough or user observation has validated them. Provisional UX heuristic score: 6/10. There is no severity-4 finding in this pass.

## Typography

Deferred to Phase 4.

## Tokens

Deferred to Phase 4.

## Components

| Component | Decision | Status |
|---|---|---|
| Home and runner | Keep the current system while Phase 2 tests task clarity. | Pending Phase 4 visual review |

## UX Audit Findings

Severity uses 0-4: 3 significantly disrupts a task; 2 causes delay or confusion; 1 is cosmetic. Frequency is an estimate until the task checks below are run. Both severity-3 issues are in the first fix pass; within a severity tier, implementation ease determines order.

| Issue and code evidence | Heuristic | Severity | Estimated frequency | Fix | Owner / priority | Status |
|---|---|---:|---|---|---|---|
| Tapping routine B while A is active opens A without explaining the substitution. `FeatureViewModels.kt:543-546` takes any active run, while `TaskChainApp.kt:150,175-179` passes B's ID. | Consistency; visibility of system status | 3 | After interrupted runs | Show a named Resume action on Home; explain the active run before opening another routine. | Maintainer / first fix pass, first | Proposed; validate on device |
| There is no direct Pause/Resume action during a step. The runner exposes Complete, Back, and Skip (`TaskChainApp.kt:1070-1183`); the engine pauses only when switching steps or during confirmation (`RoutineRunEngine.kt:45-61,157-160`). | User control and freedom | 3 | Potentially every interrupted run | Add persisted pause/resume and a visible runner action; keep timer and actual duration honest. | Maintainer / first fix pass, second | Proposed; validate on device |
| Complete or Skip on the final step always opens a finish dialog, even when every step is complete (`RoutineRunEngine.kt:219-226`; `TaskChainApp.kt:1189-1209`). | Flexibility and efficiency | 2 | Every run | Consider direct finish when all steps are complete; retain unfinished-step review. This requires changing the current `CONTEXT.md` invariant. | Maintainer / backlog | Awaiting domain decision |
| The runner has step circles but no visible position text (`TaskChainApp.kt:1050-1144`); `runner_step_of` exists in strings but is unused. | Visibility of system status; recognition | 2 | Each multi-step run | Show “Step N of M” beside the current step title. | Maintainer / backlog | Proposed; validate on device |
| Home renders Scheduled, Manual, and Completed headings even when the lists are empty (`TaskChainApp.kt:253-262`). | Aesthetic and minimalist design | 1 | Every Home visit | Render only nonempty sections. | Maintainer / backlog | Proposed |

### Trunk Test

| Screen | What is clear from the source | What needs a task walkthrough |
|---|---|---|
| Home | The app bar says Home; bottom tabs name the major sections. | The Start action is an icon on each row; confirm that users see which routine to run and notice an active run. |
| Runner | The app bar names the routine; the current step and Complete, Back, Skip actions are visible. | Confirm that users can identify their step position and find a way to handle an interruption. |
| Finish dialog | The title asks whether to finish; unfinished steps are listed and selectable. | Confirm whether a dialog with no unfinished steps adds useful information. |

Search is not part of this narrow, local routine flow. The Trunk Test above is a code review, not a rendered-screen pass.

### Copy to check during tasks

| Surface | Current | Candidate | Owner / priority |
|---|---|---|---|
| Active run on Home | No visible Resume label | “Resume [routine name]” | Maintainer / first fix pass |
| Runner progress | No visible position label | “Step N of M” | Maintainer / backlog |
| Finish dialog action | “Complete” repeats the step action label | “Finish run” | Maintainer / backlog |
| Finish dialog dismissal | “Cancel” | “Keep running” | Maintainer / backlog |

## Microinteraction Inventory

Deferred to Phase 5.
