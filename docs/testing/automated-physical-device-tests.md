# Automated physical-device tests

Use this file for tests that run on a physical device without a person.
Follow the safety rules and device setup in [device-testing.md](device-testing.md).

## Navigation tests

### Required coverage

Write one test for each event in [navigation-events-review.md](../navigation-events-review.md).

| Event | Test type | Test |
| --- | --- | --- |
| Home tab selection survives recreation | Compose | |
| Create or edit pushes builder, Save pops to Home | Compose | `MainActivityTest` |
| Builder Back with unsaved change shows discard dialog | Compose | `MainActivityTest` |
| Builder predictive Back: slow start, complete, cancel | Device gesture | `NavigationBackHandlerTest` |
| Start or resume pushes runner | Compose | |
| Runner Back on first task asks to abort | Compose | |
| Runner system Back (not the visible button) | Device | `SystemBackNavigationTest` |
| Runner predictive Back with confirmation open | Device gesture | |
| Completion hold consumes Back | Compose | |
| Run notification opens the active run | Device | `OpenActiveRunNavigationTest` |
| Back from Home leaves the app | Device | `SystemBackNavigationTest` |
| Process death during a run restores the run | Device | |

Cover error and recovery routes with `BuilderErrorFlowTest` and `StartupRecoveryTest`.

### Method

- Use `ComposeTestRule` with semantic matchers.
- Match by semantic matcher first. Use `testTag` only when a match needs more than three matchers.
- Always verify state restoration after `recreate()`.
- Use UI Automator for system Back, edge swipes and the notification shade. Compose rules cannot reach system UI.
- Assert the back-stack depth after each event, not only the visible screen.
- Use synthetic routines seeded through the test helpers. Clear state in `@Before` and `@After`, as `MainActivityTest` does.
- Inject system Back with `Instrumentation.sendKeyDownUpSync(KEYCODE_BACK)` when a dialog is open.
- Do not use `Espresso.pressBack()` with an open dialog. The dialog window holds focus, and the call fails.

## Screenshot tests

### Tool choice

- Use Dropshots for device screenshots.
- Apply the plugin as `id("com.dropbox.dropshots")` through the version catalog.
- Keep Maven Central in the plugin repositories.
- Add a `Dropshots()` JUnit rule to each screenshot test.
- Do not add a Dropshots test dependency by hand. The plugin adds it.
- Verify the rule with a Compose screen on the device. The Dropshots README documents no Compose usage.
- Set the pixel tolerance with `CountValidator` or `ThresholdValidator`.
- Do not add Roborazzi or Paparazzi. They render on the JVM and do not show real device rendering.

### Gradle tasks

TaskChain tests run against the `deviceTest` build type.

| Task | Action |
| --- | --- |
| `recordDeviceTestAndroidTestScreenshots` | Write new references |
| `pullDeviceTestAndroidTestScreenshots` | Copy screenshots from the device |
| `clearDeviceTestAndroidTestScreenshots` | Remove screenshots from the device |
| `connectedDeviceTestAndroidTest` | Verify against references |

### Record a reference

1. Unlock the device.
2. Run `recordDeviceTestAndroidTestScreenshots` on the reference device.
3. Review the new image in `app/src/androidTest/screenshots`.
4. Commit the image.

Record and verify on the same device with the same configuration.

If the task leaves no `.isRecordingScreenshots` marker on the device, record by hand:

1. Create the marker with `adb shell` in `/storage/emulated/0/Download/screenshots/com.taskchain.testapp/`.
2. Run `connectedDeviceTestAndroidTest`.
3. Run `adb pull` on `reference/<name>.png` into `app/src/androidTest/screenshots`.
4. Delete the device directory before the verify run.

### What to capture

| Scope | Variants |
| --- | --- |
| Home | Empty, populated, run active |
| Builder | Collapsed tasks, expanded task, subtasks, validation error |
| Runner | Running, paused, overtime, subtask behind schedule, confirmation |
| Progress | Empty, populated |
| Settings | Default |
| Components | Each shared component in `core/designsystem` |

Capture each screen with these settings:

- Use the reference size on the physical device. Record the device model and display size.
- Run the accessibility captures in [accessibility-testing.md](accessibility-testing.md).
- Capture each theme provider: CyberpunkAndroid and Material 3 (issue #26).

Do not build a window-size matrix. A phone is the only target form factor.

### Rules

- Check appearance only. Test behavior in Compose tests.
- Freeze time, animation and random values. Seed fixed state through the test helpers.
- Keep screenshots out of the pull-request workflow. They depend on one device and its renderer.
- Review each changed reference image in the pull request. Attach it to the PR text.
- Re-record references only for an intended visual change. Say why in the commit message.
- Store references in the repo at `app/src/androidTest/screenshots`.

## Run order

1. Unlock the device. Confirm `adb devices` shows the serial.
2. Build: `:app:assembleDeviceTest :app:assembleDeviceTestAndroidTest`.
3. Check both APK application IDs end in `.testapp`.
4. Run: `:app:connectedDeviceTestAndroidTest`.
5. Record the result in the pull request: device model, Android version, pass count and failures.
6. Run the checks in [human-driven-tests.md](human-driven-tests.md) before a release.
