# Human-driven tests

Use this file for checks that a person performs on a physical device.
Follow the safety rules and device setup in [device-testing.md](device-testing.md).

## Manual device checks

Automation cannot cover these. Record each result as device evidence, apart from automated results.

- Haptics and sound for task nudges, completion and subtask advance.
- Reminder notification after reboot and after a clock change.
- Run notification: time display, Skip and Complete buttons, and reminder Snooze and Dismiss.
- Countdown haptic pulse at 3, 2, and 1.
- Edge-to-edge rendering and system-bar legibility.

## Upgrade and reboot

Do not use the connected test task as an upgrade test. It may replace or remove the testing app.
Run these steps apart from the suite, as `docs/on-device-data.md` describes:

1. Install an older `testapp` APK and seed data.
2. Update in place with `adb install -r`.
3. Check routines, run, history and settings.
4. Reboot and unlock the device.
5. Check the same data again.
