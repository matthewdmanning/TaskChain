# Physical-device testing

This file covers navigation tests and screenshot tests on a physical device.
The reference device is a Pixel 7 (used for the 2026-10-11 storage run).

## Safety rules

- Install only the `deviceTest` build, `com.taskchain.testapp`.
- Check the application ID of every APK before install.
- Never run `pm clear` or uninstall on `com.taskchain`.
- Take a snapshot before any work on valued data: `scripts/backup-device-data.ps1`.
- Unlock the device and keep the screen on. A locked screen hides the Compose hierarchy and fails every UI test.

## Device setup

- Turn on USB debugging.
- Set animation scales to 0 for stable assertions.
- Set the device to a fixed locale, theme, font scale 1.0 and display size. Record them with each run.
- Use the SDK `adb.exe` by absolute path. See `docs/agents/android-sdk-access.md`.
- Run `adb devices` and note the serial before each run.

## Navigation tests

### Existing

| Test | Covers |
| --- | --- |
| `MainActivityTest` | Builder Back and Cancel discard prompt, routine card actions |
| `BubbleNavigationTest` | Bubble launch after recreation leaves one runner entry |
| `NavigationBackHandlerTest` | Cancelled gesture resets preview, completed Back commits once |
| `BuilderErrorFlowTest`, `StartupRecoveryTest` | Error and recovery routes |

### Required coverage

Each row maps to an event in [navigation-events-review.md](../navigation-events-review.md).

| Event | Test type | Status |
| --- | --- | --- |
| Home tab selection survives recreation | Compose | Proposed |
| Create or edit pushes builder, Save pops to Home | Compose | Exists |
| Builder Back with unsaved change shows discard dialog | Compose | Exists |
| Builder predictive Back: slow start, complete, cancel | Device gesture | Proposed |
| Start or resume pushes runner | Compose | Proposed |
| Runner Back on first task asks to abort | Compose | Proposed |
| Runner system Back (not the visible button) | Device | Written (`SystemBackNavigationTest`), compiles, not yet run on a device |
| Runner predictive Back with confirmation open | Device gesture | Proposed |
| Completion hold consumes Back | Compose | Proposed |
| Bubble opens the active run | Device | Exists (recreation case only) |
| Back from Home leaves the app | Device | Written (`SystemBackNavigationTest`), compiles, not yet run on a device |
| Process death during a run restores the run | Device | Proposed |

### Method

- Use `ComposeTestRule` with semantic matchers.
- Always verify state restoration after `recreate()`.
- Use UI Automator for system Back, edge swipes and the bubble. Compose rules cannot reach system UI.
- Assert the back-stack depth after each event, not only the visible screen.
- Use synthetic routines seeded through the test helpers. Clear state in `@Before` and `@After`, as `MainActivityTest` does.

## Screenshot tests

Status: tooling installed, one test written, no reference recorded.

### Tool choice

- Use Dropshots for device screenshots. The `testing-setup` skill recommends it for system-UI cases.
- The plugin is `com.dropbox.dropshots`. Tests use a `Dropshots()` JUnit rule.
- Checked against the Dropshots README on 2026-10-12 (plugin version `0.6.0` shown there):
  - Apply the plugin as `id("com.dropbox.dropshots")`. Maven Central must be in the plugin repositories.
  - References live in `src/androidTest/screenshots` of the module.
  - Record with the `record<Variant>Screenshots` task, for example `recordDebugAndroidTestScreenshots`. Verification runs during `connectedAndroidTest`.
  - Record and verify on a device configured the same way. Use one reference device.
  - Pixel tolerance uses `CountValidator` or `ThresholdValidator`.
- The README documents no Compose usage. Test the rule with a Compose screen on the device before you rely on it.
- Installed on 2026-10-12: plugin `0.6.0` through the version catalog. The build passes with AGP 9.4.1.
- The plugin adds its own test dependency. Do not add one by hand.
- TaskChain's tests run against the `deviceTest` build type. Gradle lists these tasks for it:
  - `recordDeviceTestAndroidTestScreenshots` writes new references.
  - `pullDeviceTestAndroidTestScreenshots` copies screenshots from the device.
  - `clearDeviceTestAndroidTestScreenshots` removes screenshots from the device.
- Existing test: `ScreenshotTest.routinesTab_matchesReference`. No reference image exists yet. Record on the reference device with the device unlocked, then review and commit the image.
- Do not add Roborazzi or Paparazzi in the first pass. They render on the JVM and do not show real device rendering.

### What to capture

| Scope | Variants | Status |
| --- | --- | --- |
| Home | Empty, populated, bubble active | Proposed |
| Builder | Collapsed tasks, expanded task, subtasks, validation error | Proposed |
| Runner | Running, paused, overtime, subtask behind schedule, confirmation | Proposed |
| Progress | Empty, populated | Proposed |
| Settings | Default | Proposed |
| Components | Each shared component in `ui/designsystem` | Proposed |

Capture each screen with these settings:

- Reference size on the physical device. Record the device model and display size.
- Font scale 1.5 for the main screens.
- Each theme provider, once the provider work in #26 lands: CyberpunkAndroid and Material 3.

Skip the 9-size window matrix in the first pass. A phone is the only target form factor today.

### Rules

- Screenshots check appearance only. Behavior stays in Compose tests.
- Freeze time, animation and random values. Seed fixed state through the test helpers.
- Keep screenshots out of the pull-request workflow. They depend on one device and its renderer.
- Review each changed reference image in the pull request. Attach it to the PR text.
- Re-record references only when the visual change is intended. Say why in the commit message.
- Store references in the repo at `app/src/androidTest/screenshots`.

## Run order

1. Unlock the device. Confirm `adb devices` shows the serial.
2. Build: `:app:assembleDeviceTest :app:assembleDeviceTestAndroidTest`.
3. Check both APK application IDs end in `.testapp`.
4. Run: `:app:connectedDeviceTestAndroidTest`.
5. Record the result in the pull request: device model, Android version, pass count and failures.
6. Run the manual checks below before a release.

## Manual device checks

Automation cannot cover these. Record each result as device evidence, apart from automated results.

- Drag reorder with TalkBack on.
- Haptics and sound for task nudges, completion and subtask advance.
- Reminder notification after reboot and after a clock change.
- Bubble placement and tap.
- Edge-to-edge rendering and system-bar legibility.

## Upgrade and reboot

The connected test task may replace or remove the testing app. Do not use it as an upgrade test.
Run these apart from the suite, as `docs/on-device-data.md` describes:

1. Install an older `testapp` APK and seed data.
2. Update in place with `adb install -r`.
3. Check routines, run, history and settings.
4. Reboot, unlock, then check the same data again.

Status: not yet verified on a real device.
