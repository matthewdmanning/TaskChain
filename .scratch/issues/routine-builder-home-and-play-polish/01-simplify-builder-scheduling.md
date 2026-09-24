# 01 — Simplify Routine Builder scheduling

**What to build:** Make routine scheduling compact and direct: remove the large Schedule heading and the Set deadline and Set reminder actions, rename the schedule toggle to Repeating, and present Custom as the rightmost recurrence choice. Custom means repeating on one or more selected days of the week and uses the platform's established selectable-day pattern rather than a bespoke control. Show the routine time in its own centered, unlabeled row at all times; activating it opens the scrolling time selector.

**Blocked by:** None — can start immediately.

**Status:** resolved

- [x] The builder has no large Schedule heading and no Set deadline or Set reminder actions.
- [x] The schedule toggle is labeled Repeating.
- [x] Custom is the rightmost recurrence choice and replaces the separate visible Selected days choice.
- [x] Choosing Custom exposes an accessible multi-select day-of-week control and requires at least one selected day before saving.
- [x] The centered, unlabeled time row remains visible for every recurrence choice and opens the scrolling time selector.
- [x] Existing recurring routines load with the equivalent visible recurrence choice, selected weekdays, and time intact.
- [x] The change adds no new UI dependency and preserves backward readability of existing local routine data.

