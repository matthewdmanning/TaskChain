# 03 — Redesign the Routine Play screen

**What to build:** Refocus Routine Play on the current step's description and timer. Increase the routine-title size; remove the “New …” and “Pending” labels; place the step description between the top safe area and the timer; center a timer whose text width is one third of the screen's smaller dimension; place Skip and Back just above the bottom safe area; and show one semantic status icon per run step below the timer.

**Blocked by:** None — can start immediately.

**Status:** resolved

- [x] The routine title is visually larger, and the screen no longer shows the “New …” header or “Pending” label.
- [x] The current step description appears above the timer, approximately halfway between the timer and the top safe-area boundary, using the former “New …” header size.
- [x] The timer is centered and its text width is one third of the screen's smaller dimension in both portrait and landscape.
- [x] Skip and Back sit above the bottom safe-area boundary with a visible buffer and use the same text size as the description.
- [x] One accessible status icon appears below the timer for every run step: outlined when unfinished, the completed semantic color when completed, and the warning semantic color when skipped.
- [x] Long descriptions, larger font settings, and system-bar insets do not hide the timer or actions.

