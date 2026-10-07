# Feature integration verification

Goal: implement [the agreed plan](feature-integration-plan.md) while retaining Route → ViewModel → Screen.

## Baseline

- GitHub integration verified `main` at `ae2d52ef7ab5382d43c0d1ec1f8c7443cba7dd32` and all seven feature tips on 2026-10-07; local refs match.
- Isolated worktree: `.scratch/feature-integration`; integration branch: `refactor-feature-integration`.
- Original checkout, original feature refs, and `main` are preserved. No branches are published.
- Runtime: complete JDK 17; checked-in Gradle 9.8.0; shared wrapper distribution cache exists.
- Context7 official Compose documentation confirms lifecycle-aware ViewModel state collection and plain-state/callback screens: https://developer.android.com/develop/ui/compose/state-hoisting
- Baseline checks: compilation, JVM tests, lint, assembly (result recorded below when complete).

## Alignment policy

Each feature merges its original history into an alignment branch created from the latest accepted integration tip. Older overlapping implementations are replaced with the current MVVM implementation. The original branch remains unchanged. Full compile, JVM tests, lint, and assembly gate every accepted feature merge.

## Device acceptance

Device validation remains separate from build and JVM evidence. Required flows: cue authoring, main-only icons, Complete/Skip, collapse, cue rims/Warning, gestures, bubble navigation, scaling, animation gates, background/resume, and final-task confirmation.
Baseline outcome: all four checks passed (2026-10-07). Existing warnings remain; no device proof claimed.

Foundation cleanup: removed .gitmodules, the empty third_party/cyberpunkAndroid gitlink, and its stale local Git configuration. No Gradle files reference third_party; the published v1.0.8 library dependency remains. Renormalized the Windows wrapper under existing .gitattributes (line endings only). Compilation, JVM tests, lint, and assembly passed after cleanup.

## Substeps alignment

Original tip: 3b573f9. Content conflicts: ReminderScheduler.kt, FeatureViewModels.kt, TaskChainApp.kt. Replaced flat independent secondary tasks with nested cue definitions and persisted main-clock advancement markers. Current reminder and runner behavior retained. Added MVVM Screen boundary, cue authoring, preference persistence, semantic sound, and backward-compatible JSON tests. Primary review corrected future-cue timing, skipped-task advancement, final-confirmation rollback, readiness/double-action guards, and duration summaries. Gate: compile, 84 JVM tests (zero failures/errors), lint, assembly passed. Device checks remain pending until all visual features are aligned.

## Step accordions alignment

Original tip: 2b705c3. Conflicts: TaskChainApp.kt, TaskChainTheme.kt, deleted third_party gitlink. Current stable-ID accordion/reorder, title editing, native duration pickers, typography, and nested cue editing already implement the feature; retained them and retained submodule deletion. Gate: compile, all 84 JVM tests, lint, assembly passed. An initial premature build encountered unresolved merge markers; corrected resolution and reran the complete gate successfully.

## Frictionless gestures alignment

Original tip: 8e430bc. Conflicts: app/build.gradle.kts, RunnerGestures.kt (add/add), TaskChainApp.kt, build.gradle.kts, settings.gradle.kts. Retained main's consolidated detector, current next-finished/Skip/Pause/Resume intents and completion guards, JDK 17 configuration, and published library dependency. No Gradle third_party wiring restored. Gate: compile, all 84 JVM tests, lint, assembly passed (unchanged code checks reused by Gradle).
