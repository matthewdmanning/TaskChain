# Routine Home Organization and Builder Editing

Status: ready-for-agent

## Problem Statement

The Home screen presents routines as one flat list, so a user cannot distinguish routines that are currently scheduled, routines that are always started manually, and scheduled routines already completed for their current occurrence.

The Routine Builder also exposes routine concerns on individual steps, requires explicit update and add buttons, and retains the Task stack interaction. This makes the editing model harder to understand than the domain model: a Reminder prompts for a routine at its scheduled time, while steps are the ordered tasks performed during that routine.

## Solution

Organize every displayed routine into exactly one Home section: Scheduled, Manual, or Completed. A scheduled routine moves to Completed when its current occurrence is completed and returns to Scheduled at its next schedule trigger. A manual routine remains Manual.

Make schedule, deadline, and Reminder settings properties of the routine template and place their controls near the routine description. Keep configured duration on the routine step, but replace numeric text entry with a stopwatch-style duration control. Simplify step editing by removing Task stack controls, autosaving edits into the builder draft, allowing direct drag-and-drop reordering, and keeping a dedicated add-step tile at the bottom of the list. Saving persists the draft and returns to the Routines tab.

## User Stories

1. As a user, I want Scheduled, Manual, and Completed sections on Home, so that I can understand why each routine is being shown.
2. As a user, I want every displayed routine to appear in exactly one section, so that the Home screen never presents conflicting routine states.
3. As a user, I want a routine with an actionable scheduled occurrence to appear under Scheduled, so that I know what is due.
4. As a user, I want a routine without a schedule to appear under Manual, so that I know I must start it myself.
5. As a user, I want a completed scheduled occurrence to move to Completed, so that I can see it is finished without losing it from Home.
6. As a user, I want a completed scheduled routine to return to Scheduled at its next trigger, so that the next occurrence becomes actionable automatically.
7. As a user, I want a manual routine to remain under Manual after I run it, so that completing it does not turn it into a scheduled workflow.
8. As a user, I want Home classification to survive app restart and screen recreation, so that the sections continue to reflect durable local state.
9. As a user, I want Home classification to use my local date and time, so that schedule transitions happen when I expect them.
10. As a user, I want section changes to appear without recreating the screen, so that Home stays current across a schedule trigger.
11. As a user, I want empty sections handled without placeholder routine cards, so that the screen remains clear.
12. As a user, I want to set a routine's schedule in the Routine Builder, so that the whole routine has one understandable cadence.
13. As a user, I want the Schedule controls under a Schedule heading, so that recurrence settings are easy to find.
14. As a user, I do not want Schedule controls on individual steps, so that a routine cannot contain competing step schedules.
15. As a user, I want to set the routine deadline below its description, so that the deadline clearly belongs to the routine.
16. As a user, I want to set the routine Reminder below its description, so that the prompt clearly applies to the routine.
17. As a user, I do not want deadline or Reminder controls on individual steps, so that those settings cannot be mistaken for step behavior.
18. As a user, I want mutually exclusive schedule, deadline, and one-time Reminder choices enforced by the builder, so that I cannot save a contradictory routine.
19. As a user, I want a Steps heading before the ordered step list, so that routine settings and routine contents are visually distinct.
20. As a user, I want to set a step's configured duration with a stopwatch-style control, so that I enter elapsed duration rather than clock time.
21. As a user, I do not want to type timer seconds into a text field, so that duration entry is easier and less error-prone.
22. As a user, I want existing configured durations to populate the stopwatch control when editing, so that an edit does not erase saved values.
23. As a user, I want to drag a step to a new list position, so that I can reorder a routine directly.
24. As a keyboard or assistive-technology user, I want an accessible way to reorder steps, so that drag-and-drop is not the only operable interaction.
25. As a user, I want the reordered list saved as the routine's canonical step order, so that later runs follow what I arranged.
26. As a user, I do not want Task stack or Stack after first step controls, so that the builder has one ordering model.
27. As a user, I want changes to an expanded step to update the builder draft immediately, so that I do not need an Update step button.
28. As a user, I want Cancel and Back protections to continue recognizing autosaved draft changes, so that I do not lose edits accidentally.
29. As a user, I want a dedicated add-step tile at the bottom of the step list, so that the place to add the next step is predictable.
30. As a user, I want the add-step tile to show a centered plus in a circle and no unrelated controls, so that its purpose is clear.
31. As a user, I want pressing the add-step tile to insert a new editable step and place a replacement add-step tile below it, so that I can continue building the routine.
32. As a user, I do not want the add-step tile to participate in reordering, so that it always remains at the bottom.
33. As a user, I want Save to persist the routine and return me to the Routines tab, so that I can see the routine I just edited.
34. As a user, I do not want Save to select Home or another tab, so that navigation after editing is predictable.
35. As a user, I want missing visible fields highlighted with a concise validation message, so that I can correct them.
36. As a user, I do not want a "Fix the highlighted fields." message when the actual problem is an empty step list, so that the message does not refer to nonexistent highlights.
37. As a user, I want an empty step list error presented next to the Steps area, so that I know to add a step.
38. As a user, I want invalid input preserved after validation, so that correcting one field does not erase the rest of my draft.

## Implementation Decisions

- Use the domain terms Routine template, Routine step, Routine run, Run step, Completion event, and Reminder exactly as defined by the root domain context.
- A Reminder is a platform-neutral request to prompt for a routine at its scheduled time. It is not a step setting.
- Schedule, deadline, and one-time Reminder belong to the routine template. Individual routine steps do not expose or persist those settings as authored behavior.
- Schedule, deadline, and one-time Reminder remain mutually exclusive. Moving them to the routine changes their owner, not the exclusivity rule.
- Configured duration remains a routine-step property and remains distinct from actual duration recorded on a run step.
- The stopwatch control edits elapsed duration; it is not a time-of-day clock. Preserve the existing stored duration meaning rather than introducing a second duration representation.
- Replace the flat Home projection with a deterministic categorized projection in which each displayed routine has exactly one category: Scheduled, Manual, or Completed.
- Manual routines always classify as Manual. Their completion history does not move them to Completed.
- A scheduled routine classifies as Completed after the current occurrence is completed and remains there until the next schedule trigger. At that trigger it classifies as Scheduled again.
- Home classification is derived from routine templates, the current time, active-run state, and durable completion history. Do not introduce a mutable category flag.
- Keep the existing periodic Home refresh so time-based category transitions can appear while the app remains open.
- Remove Task stack authoring and Stack after first step behavior. Ordered steps and explicit reordering are the only builder ordering model.
- Drag-and-drop changes the builder draft order. Provide semantic reorder actions for accessibility without restoring separate visible Up and Down buttons as the primary interaction.
- Autosave means step controls write immediately to the in-memory builder draft. The routine repository is updated only when the user presses Save, preserving existing Cancel and discard-confirmation behavior.
- The add-step tile is a non-draggable terminal list item. Activating it appends one new step and leaves a new add-step tile at the bottom.
- A successful Save explicitly returns to the Routines tab, regardless of which tab originally opened the builder.
- Show the generic highlighted-fields message only when a visible field is missing and highlighted. Present the no-steps error in the Steps area instead.
- Existing locally persisted routines must remain readable during the ownership change. Do not silently discard saved schedule, deadline, Reminder, or configured-duration data.
- Update stale domain documentation and invariant language that still assigns Reminder or scheduling behavior to a task. The new Reminder definition and this routine-level ownership are authoritative.
- Preserve the Android-only, offline-only, local, account-free, telemetry-free, and database-free architecture. Add no remote service or new UI dependency.

## Testing Decisions

- Test externally observable behavior rather than private composable structure, mutable-state plumbing, or repository implementation details.
- Use the categorized Home projection as the primary deterministic seam. Cover manual classification, scheduled classification, completion of the current occurrence, transition at the next trigger, local-time boundaries, active-run completion, durable history, and exactly-one-category behavior.
- Reuse the existing Home projection tests and next-trigger calculator tests as prior art for schedule and completion boundaries.
- Use the existing pure builder-validation seam for routine-level exclusivity, required fields, step-list validation, and preservation of invalid draft input.
- Reuse the current builder-validation tests and routine-template invariant tests as prior art.
- Add one Compose interaction flow for the behavior that cannot be proven at a pure seam: routine-level control placement, absence of step-level controls, stopwatch duration entry, step autosave, drag reordering, terminal add-step tile behavior, validation messaging, and return to the Routines tab after Save.
- Extend existing Compose component and activity tests rather than introducing another UI test framework.
- Verify drag reordering and its accessibility actions on a physical device with TalkBack, along with Save navigation and the stopwatch control. Treat device evidence separately from JVM and instrumentation results.

## Out of Scope

- Changes to Routine run transitions, final-step confirmation, abort confirmation, or timer reconstruction.
- Deciding navigation after completing a previously skipped run step.
- Cloud sync, accounts, telemetry, databases, or remote reminders.
- Broad visual redesign outside the Home sections and Routine Builder controls named here.
- Replacing the existing local Reminder adapter or changing Android alarm policy except where routine-level ownership requires passing the routine request.
- Retaining Task stack as a hidden, alternate, or future authoring mode.

## Further Notes

- The dated punchlist is the acceptance source for this change.
- The root domain context is the vocabulary source. Its current Reminder definition is routine-specific and supersedes older task-specific Reminder wording in plans or implementation notes.
- Older implementation plans describe the behavior they implemented at that time; they do not override this newer product decision.
