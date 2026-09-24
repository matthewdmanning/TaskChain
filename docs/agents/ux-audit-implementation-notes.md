# Android UX Audit Implementation Notes

Historical verification snapshot for commit `06b0611` (`ux-audit`), recorded
2026-09-18. This is evidence for that change boundary, not the current tree.

## Scope

This change addresses the published severity 2 and 3 Android UX findings. It does not change persistence formats, domain transitions, remote services, or unrelated visual-design work.

## Implemented

- Added typed routine-builder validation, preserved draft input after errors, dirty-state tracking, and duplicate-save prevention.
- Added consistent Cancel and Android Back handling with a discard confirmation for changed drafts.
- Replaced weekday buttons with selectable chips and exposed selected semantics.
- Localized scheduled date/time presentation and honored the device 12/24-hour setting.
- Added the Today empty-state action, explicit routine and step edit actions, and non-clickable routine-card containers.
- Added labeled switch rows, runner position text, and scrolling or wrapping for constrained-height, landscape, and enlarged-font layouts.
- Added focused JVM validation tests and Compose instrumentation coverage for the remediated controls and layouts.

## Verification

Passed locally:

- `scripts\gradle-agent.cmd :app:testDebugUnitTest`
- `scripts\gradle-agent.cmd :app:assembleDebug`
- `scripts\gradle-agent.cmd :app:lintDebug`
- `scripts\gradle-agent.cmd :app:compileDebugAndroidTestKotlin`
- `git diff --check`

Physical-device and connected instrumentation verification was not completed because the ADB device inventory could not be read after the permitted retry. No device data was changed. TalkBack, 200% font scale, rotation, and state-preserving device checks remain manual verification items.
