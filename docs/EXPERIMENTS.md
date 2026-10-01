# Experiments

## Experiment Cards

No experiment has run. The checks below are pre-committed scripted tasks with the maintainer, not evidence about other users. They require a current device build before any verdict.

## Experiment Backlog

ICE scores are expert estimates on a 1-10 scale (impact / confidence / ease). Confidence is low where no user behavior has been observed. Both severity-3 issues are in the first fix pass; ease orders work within a severity tier.

| Idea | ICE | Pre-committed task check and threshold | Owner / priority | Status |
|---|---|---|---|---|
| Make the active run explicit before opening a different routine. | 8 / 7 / 5 | In five scripted attempts to start B while A is active, all five identify A before a choice; zero silently open A as B. | Maintainer / first fix pass, first | Implemented; device check pending |
| Let the user pause and resume a step. | 8 / 5 / 3 | In five scripted mid-step interruptions, all five resume the same step without Skip or Abort; the paused interval is excluded from the recorded step duration. | Maintainer / first fix pass, second | Implemented; device check pending |
| Keep empty Home sections with room for a routine. | 3 / 8 / 9 | With only Manual populated, Home still shows Scheduled, Manual, and Completed; each empty section keeps about one row of open space. | Maintainer / first-screen design | Implemented; Pixel 7 visual check |
| Show current step position in the runner. | 5 / 7 / 8 | In five scripted multi-step runs, the maintainer can identify the current step number without counting circles. | Maintainer / backlog | Dropped: maintainer reports the current circles require no effort to interpret. |
| Finish clean runs directly; retain review when steps remain unfinished. | 6 / 7 / 6 | Five clean runs finish on the last Complete action without a dialog; five runs with pending or skipped steps show the unfinished-step review. | Maintainer / first fix pass; issue 05 | Specified; implementation pending |
| Discard an aborted run without changing completion or schedule state. | — | In five scripted aborts, each returns Home with no completed or aborted history, no scheduled-completion change, and Settings preserved; another run can start. | Maintainer / first fix pass; issue 05 | Specified; implementation pending |
| Show a graphic reaction after clean completion. | — | In five clean completions, show a brief graphic reaction without delaying Home; show none after an abort or an unfinished-run confirmation. | Maintainer / polish; issue 05 | Specified; implementation pending |
| Refine runner hierarchy, spacing, and state markers with cyberpunkAndroid. | — | In a grayscale device walkthrough, the current-step text enters large and centered, then shrinks and moves slightly up; the countdown becomes the strongest element for most of the step; completed and skipped states are distinguishable by their library icons; the layout no longer feels cramped. Changing the app config values changes transition size, offset, duration, and easing. Review palette and effects only after this passes. | Maintainer / visual polish | Proposed; implementation pending |
