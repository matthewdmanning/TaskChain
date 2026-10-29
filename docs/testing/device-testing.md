# Physical-device testing

Use this file for navigation tests and screenshot tests on a physical device.
Use a Pixel 7 as the reference device.

## Safety rules

- Install only the `deviceTest` build, `com.taskchain.testapp`.
- Check the application ID of every APK before install.
- Never run `pm clear` or uninstall on `com.taskchain`.
- Take a snapshot before any work on valued data: `scripts/backup-device-data.ps1`.
- Unlock the device and keep the screen on. A locked screen hides the Compose hierarchy and fails every UI test.

## Device setup

- Turn on USB debugging.
- Set animation scales to 0 for stable assertions.
- Set a fixed locale, theme, font scale and display size. Use font scale 1.0 unless a reference below records a different value. Record them with each run.
- Use the SDK `adb.exe` by absolute path. See `docs/agents/android-sdk-access.md`.
- Run `adb devices` and note the serial before each run.

## Test files

- Run navigation tests, screenshot tests and the run order from [automated-physical-device-tests.md](automated-physical-device-tests.md).
- Run person-driven checks from [human-driven-tests.md](human-driven-tests.md).
- Run accessibility checks from [accessibility-testing.md](accessibility-testing.md).

## Accepted screenshot references

| Reference | Device | Android | Size / density | Font scale | Appearance |
| --- | --- | --- | --- | --- | --- |
| `routines_tab.png` | Pixel 7 | 17 | 1080 × 2400 / 420 dpi | 1.15 | Cyberpunk provider, dark |

The user approved the current Routines rendering on 2026-10-29. It replaces the historical reference; the previous difference is accepted as a font-scale change. Verify this reference at its recorded scale, without increasing the comparison tolerance.
