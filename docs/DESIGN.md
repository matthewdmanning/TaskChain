# Design System

## Design Direction

Phases 2-4 audit the daily run from Home through finishing and aborting. The maintainer has now completed a grayscale device walkthrough for Phase 4. The current runner findings are that Complete draws attention before the task title, completed and skipped states are indistinguishable, and the content feels cramped in the center; the action text is readable. Refactoring UI quick-diagnostic score: 4/10 (3 of 8 checks pass, rounded). Spacing, state distinction, and hierarchy need revision before palette or effects.

## Typography

TaskChain currently uses Material 3 typography. In the runner, the task title and several secondary actions share `headlineLarge`, while the timer uses `displayLarge` at 100sp (`TaskChainApp.kt`). The selected [`cyberpunkAndroid` library](https://github.com/matthewdmanning/cyberpunkAndroid) provides `CyberTypography` roles for display (32sp bold), terminal (14sp monospace), and body (16sp). Map those existing roles to the runner hierarchy in grayscale; do not add a task-position label because the maintainer finds the current circles effortless to read.

## Tokens

Use the selected library's existing primitives and theme: `CyberPrimitives.Spacing` (4/8/12/16/24/32dp), `CyberColors` and `CyberSemanticTokens`, `CyberTypography`, and `CyberShapes`. Avoid a parallel token set. TaskChain's current 8/16/24dp resources and unused 720dp content maximum (`dimens.xml`) are audit evidence; constrain wide layouts as needed while using the library spacing scale. Review grayscale hierarchy and state shapes before applying the library's saturated palette or visual effects. The library's `CyberTheme` exposes these roles through Compose composition locals. Keep the runner's task-entry transition values in an app config file: initial and final size, vertical offset, duration, and easing.

## Components

| Component       | Decision                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Status                                     |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------ |
| Home and runner | Use the library's existing theme tokens and Compose effect modifiers. On task entry, show the current-step text large and centered, then shrink and move it slightly up to its final position; let the countdown hold strongest emphasis for most of the task. Keep transition size, offset, duration, and easing in app config. Use distinct `cyberpunkAndroid` icons for completed and skipped states so they remain distinguishable in grayscale. Apply color and effects after the grayscale hierarchy reads clearly. | Proposed; implement and validate on device |

### Phase 4 Device Observations

| Screen check            | Maintainer observation                                        | Design response                                                                    |
| ----------------------- | ------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| First visual emphasis   | Complete button draws attention before the current-step text. | Let the task text lead on entry, then transition visual emphasis to the countdown. |
| Action text readability | The text is readable.                                         | No text-size change proposed from this check.                                      |
| Completed vs. skipped   | The states are not distinguishable in grayscale.              | Use distinct icons from `cyberpunkAndroid`, not color alone.                       |
| Spacing and composition | Content feels squished in the center.                         | Rework the layout with the library spacing scale and clearer group separation.     |

## UX Audit Findings

Severity uses 0-4: 3 significantly disrupts a task; 2 causes delay or confusion; 1 is cosmetic. Frequency is an estimate until the task checks below are run. Both severity-3 issues are in the first fix pass; within a severity tier, implementation ease determines order.

| Issue and code evidence                                                                                                                                                                                                                                           | Heuristic                                             | Severity | Estimated frequency               | Fix                                                                                                                                                     | Owner / priority                      | Status                                    |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------- | -------: | --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- | ----------------------------------------- |
| Tapping routine B while A is active opens A without explaining the substitution. `FeatureViewModels.kt:543-546` takes any active run, while `TaskChainApp.kt:150,175-179` passes B's ID.                                                                          | Consistency; visibility of system status              |        3 | After interrupted runs            | Show a named Resume action on Home; explain the active run before opening another routine.                                                              | Maintainer / first fix pass, first    | Implemented; device check pending         |
| There is no direct Pause/Resume action during a task. The runner exposes Complete, Back, and Skip (`TaskChainApp.kt:1070-1183`); the engine pauses only when switching tasks or during confirmation (`RoutineRunEngine.kt:45-61,157-160`).                        | User control and freedom                              |        3 | Potentially every interrupted run | Add persisted pause/resume and a visible runner action; keep timer and actual duration honest.                                                          | Maintainer / first fix pass, second   | Implemented; device check pending         |
| Complete or Skip on the final task always opens a finish dialog, even when every task is complete (`RoutineRunEngine.kt:219-226`; `TaskChainApp.kt:1189-1209`).                                                                                                   | Flexibility and efficiency; Norman gulf of evaluation |        2 | Clean completion                  | Finish directly when all tasks are completed; retain the unfinished-step review otherwise.                                                              | Maintainer / first fix pass; issue 05 | Specified; implementation pending         |
| The runner has task circles but no visible position text (`TaskChainApp.kt:1050-1144`); `runner_step_of` exists in strings but is unused.                                                                                                                         | Visibility of system status; recognition              |        0 | None reported                     | No added position label; maintainer reports the current circles require no effort to interpret.                                                         | Maintainer / closed                   | Closed by maintainer decision             |
| Home renders Scheduled, Manual, and Completed headings even when the lists are empty.                                                                                                                                                                             | Aesthetic and minimalist design                       |        1 | Every Home visit                  | Keep all three headings and leave about one routine row of open space in each empty section.                                                            | Maintainer / first-screen design      | Implemented; Pixel 7 visual check         |
| Back on the first task requests abort confirmation, then `RoutineRunnerViewModel.finish` persists an `ABORTED` event before clearing active storage (`RoutineRunEngine.kt:108-122,161-173`; `FeatureViewModels.kt:632-651`). This makes Abort change run history. | Norman gulf of execution and recovery                 |        3 | Whenever a run is aborted         | Discard the active run, return Home, create no completed or aborted history, and leave scheduled-completion state unchanged. Preserve Settings changes. | Maintainer / first fix pass; issue 05 | Specified; implementation pending         |
| The run-history failure message says history could not be saved, reassures that the run remains stored locally, and offers Retry (`run_strings.xml`).                                                                                                             | Norman gulf of evaluation; error-message recovery     |        0 | On history persistence failure    | No copy change; preserve the local run and retry action.                                                                                                | Maintainer / closed                   | Source audit passes; device check pending |

### Trunk Test

| Screen        | What is clear from the source                                                                                   | What needs a task walkthrough                                                                                  |
| ------------- | --------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| Home          | The app bar says Home; bottom tabs name the major sections.                                                     | The Start action is an icon on each row; confirm that users see which routine to run and notice an active run. |
| Runner        | The app bar names the routine; the current task and Complete, Back, Pause/Resume, and Skip actions are visible. | Confirm that Abort returns Home without leaving run history and that unfinished tasks remain reviewable.       |
| Finish dialog | The title asks whether to finish; unfinished tasks are listed and selectable.                                   | Confirm that clean completion skips the dialog and unfinished runs retain the review.                          |

Search is not part of this narrow, local routine flow. The Trunk Test above is a code review, not a rendered-screen pass.

### Copy to check during tasks

| Surface                 | Current                                  | Candidate           | Owner / priority                  |
| ----------------------- | ---------------------------------------- | ------------------- | --------------------------------- |
| Active run on Home      | “Resume [routine name]”                  | No rewrite proposed | Maintainer / device check pending |
| Finish dialog action    | “Complete” repeats the task action label | “Finish run”        | Maintainer / backlog              |
| Finish dialog dismissal | “Cancel”                                 | “Keep running”      | Maintainer / backlog              |

## Microinteraction Inventory

Deferred to Phase 5.
