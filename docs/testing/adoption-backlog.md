# Testing adoption backlog

The strategy in [README.md](README.md) leads the repo. The WIP does not conform yet.
Do not block current work on this list. Open one issue per item when you schedule it.

Rank is by risk to user data and to the run flow.

| # | Gap | Why it matters | Source |
| --- | --- | --- | --- |
| 1 | Upgrade (old APK to new) and reboot durability are unverified on a device | A failed upgrade loses user data | `docs/on-device-data.md` |
| 2 | Bubble launch can replay after recreation. A test exists, so confirm it passes on a device | Duplicate runner entry | `docs/navigation-events-review.md` |
| 3 | Runner and Home system Back tests are written but never run on a device. Predictive Back has no test | Custom handlers own these events | `docs/navigation-events-review.md` |
| 4 | UI checks never ran on an unlocked device | Compose tests are unproven on hardware | `docs/on-device-data.md` |
| 5 | One screenshot test with a verified reference. Other screens and the record task are not covered | UI provider swap in #26 can change appearance unseen | #26 |
| 6 | Device suite has no run record or trigger | Slow checks run by memory only | CI split in README |
| 7 | `CONTEXT.md` invariants have no mapping to tests | Gaps stay hidden | README coverage targets |
| 8 | Untested UI packages: settings, progress, home presentation | No regression guard | test file list |

## Decisions to make

- Decide which physical device is the reference for screenshots, and who owns it.
- Decide where `docs/testing/` links from: `AGENTS.md` or `architecture.md`. This session changed neither.
- Decide whether to add Jacoco for local coverage reports. The strategy sets no numeric target.

## Not planned

- Emulator job in CI.
- Numeric coverage gate.
- Robolectric or JVM screenshot tools.
