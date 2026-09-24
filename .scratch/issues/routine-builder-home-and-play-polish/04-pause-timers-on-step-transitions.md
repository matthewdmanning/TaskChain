# 04 — Pause timers on step transitions

**What to build:** Stop elapsed time from accumulating for a run step once Skip, Back, or Complete moves it off screen. Keep the currently displayed step's timer based on wall-clock time while the screen is off, the device is locked, or the user temporarily navigates away from TaskChain, so those lifecycle events do not pause an active step.

**Blocked by:** None — can start immediately.

**Status:** resolved

- [x] Skip records the departing step's elapsed duration at the transition time and that duration no longer grows.
- [x] Complete records the departing step's elapsed duration at the transition time and that duration no longer grows.
- [x] Back pauses the departing unfinished step, and revisiting it resumes from its stored remaining time rather than counting time spent on another step.
- [x] Turning the screen off, locking the device, or temporarily leaving the app does not pause the currently displayed step.
- [x] Timer state remains reconstructible after process death from durable run timestamps and does not rely on an in-memory counter.
- [x] A focused domain regression check covers the transition pause/resume behavior without adding cosmetic layout tests.
