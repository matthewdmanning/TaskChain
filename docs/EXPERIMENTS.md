# Experiments

## Experiment Cards

No experiment has run. The checks below are pre-committed scripted tasks with the maintainer, not evidence about other users. They require a current device build before any verdict.

## Experiment Backlog

ICE scores are expert estimates on a 1-10 scale (impact / confidence / ease). Confidence is low where no user behavior has been observed. Both severity-3 issues are in the first fix pass; ease orders work within a severity tier.

| Idea | ICE | Pre-committed task check and threshold | Owner / priority | Status |
|---|---|---|---|---|
| Make the active run explicit before opening a different routine. | 8 / 7 / 5 | In five scripted attempts to start B while A is active, all five identify A before a choice; zero silently open A as B. | Maintainer / first fix pass, first | Proposed |
| Let the user pause and resume a step. | 8 / 5 / 3 | In five scripted mid-step interruptions, all five resume the same step without Skip or Abort; the paused interval is excluded from the recorded step duration. | Maintainer / first fix pass, second | Proposed |
| Hide empty Home sections. | 3 / 8 / 9 | With only one nonempty section, Home shows exactly one section heading. | Maintainer / backlog | Proposed |
| Show current step position in the runner. | 5 / 7 / 8 | In five scripted multi-step runs, the maintainer can identify the current step number without counting circles. | Maintainer / backlog | Proposed |
| Remove redundant clean-finish confirmation, while retaining review for unfinished steps. | 6 / 7 / 6 | Five fully completed runs finish in one final action; five runs with unfinished steps still list those steps before finishing. | Maintainer / backlog | Awaiting `CONTEXT.md` decision |
