# Physical-device verification, 2026-10-29

Final checks cover the feature-ownership refactor, corrected device tests, Builder headers, and the approved Routines screenshot reference.

| Check | Result |
| --- | --- |
| Debug JVM tests | 153 passed, 0 failed |
| Pixel 7 instrumentation | 21 passed, 0 failed; [JUnit results](device-tests.xml) |
| Independent screenshot repeat | 1 passed, 0 failed; [JUnit results](screenshot-repeat.xml) |
| Android lint | 0 errors, 111 warnings, 7 hints |
| Feature boundaries | 67 Kotlin files, 16 owners, no cycles |
| Diff whitespace | Passed |

Commands: `scripts\gradle-agent.cmd :app:testDebugUnitTest`, `scripts\gradle-agent.cmd :app:connectedDeviceTestAndroidTest`, `scripts\gradle-agent.cmd :app:connectedDeviceTestAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.taskchain.ScreenshotTest`, `scripts\gradle-agent.cmd :app:lintDebug`, `python scripts/check-feature-boundaries.py`, and `git diff --check`.

Gradle ran on JDK 17.0.12 using the checked-in wrapper. Device testing used only `com.taskchain.testapp` on a physical Pixel 7, Android 17, 1080x2400, 420 dpi, font scale 1.15, Cyberpunk provider in dark mode. The production app package was not cleared or uninstalled. Sound/haptic audibility, reboot durability, and process-death recovery were not manually tested in this change.

## Builder appearance

Collapsed task headers omit the down-chevron. Pressing the header still expands the task. Expanded headers have a filled up-chevron with a 48 dp interaction target. Existing dragging, title editing, and validation-reveal behavior remain covered by instrumentation.

| State | Before | After |
| --- | --- | --- |
| Collapsed | [Screenshot](builder-before-collapsed.png) | [Screenshot](builder-collapsed.png) |
| Expanded | [Screenshot](builder-before-expanded.png) | [Screenshot](builder-expanded.png) |

These are actual scrolled device captures, not golden-image assertions for Builder. The test suite verifies collapsed and expanded dragging, the collapse action, and validation errors.

## Accepted Routines reference

The user approved the current Routines rendering. The [updated reference](../../../../app/src/androidTest/screenshots/routines_tab.png) replaces the historical image at the recorded font scale of 1.15. The historical difference is accepted as a font-scale change; the cause was assumed by user instruction rather than independently isolated. Comparison tolerance was not increased. Verification passed in the full device suite and again in a clean screenshot-only run.
