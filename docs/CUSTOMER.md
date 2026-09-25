# Customer

## Job Statement

When my attention is low but the routine still has to happen, I want to stop choosing
what to do next, so I can act without spending willpower on order.

Confirmed 2026-09-25. The statement names no product and no solution. The circumstance
is low attention under obligation, not a demographic.

## Job Dimensions

### Functional - worst underdelivery
What the user needs to do: hand the ordered steps over to something else, take them one
at a time, and record what was done, skipped, and how long it really took.

Where the app underdelivers, in daily use:
- No pause. `RoutineRunEngine` exposes start, complete, skip, select, back, continue,
  confirm, and abort, but no pause. `pausedAtEpochMillis` is set only as a side effect of
  moving to another step or of opening the finish confirmation. A real interruption mid-step
  therefore has no honest answer: the user skips, leaves the step, or abandons the run.
- The last step of every run costs an extra confirmation. Complete or Skip on the final step
  always requests confirmation, so the most repeated routine carries a dialog every time.
- Where the run resumes after completing a previously skipped step is undecided. `CONTEXT.md`
  records it as open, so the behavior the user meets each day is unspecified.
- Starting the routine still costs a decision. Home splits routines into Scheduled, Manual,
  and Completed. In the low-attention circumstance the user must pick from a categorized
  list, which is the choice the job statement asks the app to remove.

Where it underdelivers on repeated editing, a lesser cost than the daily run:
- Changing a routine reopens the full builder: title, description, per-step timers, drag
  reordering, schedule, deadline, one-time reminder, sound, and vibrate.
- Schedule, deadline, and reminder are mutually exclusive. The builder surfaces that as a
  validation error after the fact rather than as a choice up front.

### Emotional
How the user wants to feel: relieved of the decision, and confident an interrupted run is
not lost.

Where the app underdelivers:
- Progress renders as seven plain text lines: completed runs, aborted runs, actual duration,
  step adherence percent, skipped step percent, then a seven-day list. It reads as an audit
  of the user, not as evidence they are making progress.
- One metric is labelled "Aborted runs". The app names the user's quitting in its own UI.
- The run survives process death, since timer state is derived from persisted timestamps.
  That reliability is real but invisible, so it cannot reduce anxiety.

### Social
How the user wants to be perceived: as someone who simply gets on with it, not as someone
who needs an app to remember basic steps.

Where the app underdelivers:
- Thin by design, and mostly already served. The app is local-only and account-free, so
  nothing is exposed and nothing is shared. Recorded as a deliberate non-target: adding
  sharing or accountability would contradict the local-only constraint.

## Competing Alternatives

| Alternative | Why hired | Weakness |
|---|---|---|
| Non-consumption: remember it and muddle through | Zero setup, zero cost, always available | Low attention is exactly when memory fails; the routine half-finishes |
| Paper or whiteboard list | Written once, visible, no device needed | No timing, no history, no reminder, and it does not hand over the next step |
| A generic to-do app | Already installed, already habitual | Flat checklists; no ordered step-at-a-time execution and no per-step timer |
| Phone timer and alarm stack | Timers are genuinely good, already trusted, and pause freely | The user still holds the order and the next decision in their head |
| A habit tracker with streaks | Strong emotional payoff from the streak | Tracks whether it happened, not how to get through it |

The alternative that wins a given day is non-consumption. A user in a low-attention
circumstance abandons the run and muddles through from memory. The phone timer is the sharper
comparison for the pause gap: it does the one thing the runner cannot.

## Big Hire vs Little Hire

- The leak is a Little Hire failure: friction in the daily run, not in first setup. The user
  decided this on 2026-09-25, overriding the first reading that named first-run authoring cost.
- Noted tension: the user also named the routine builder as the highest-friction flow, which
  usually reads as a Big Hire cost. Resolution recorded here: the builder is treated as
  repeated-edit cost, and the daily run is the primary target.
- Consequence for this journey: Phases 2-5 aim at the path from opening the app to one
  completed run - starting without choosing, running a step, pausing, and finishing - with the
  runner as the primary surface and the builder as the secondary one.

## Evidence Status

Unvalidated. As of 2026-09-25 the app has no real users other than the maintainer, and no
analytics, recordings, or support tickets exist. Every statement above is an expert reading of
the code and the strings, not observed behavior. Treat it as a hypothesis to test, not a
finding. The optional continuous-discovery phase remains open.
