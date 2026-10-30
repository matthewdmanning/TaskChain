# Notifications feature integration

Base: `feature/keep-screen-on-feature-structure` at `bf37430`.
Branch: `refactor-notifications`.

The notification behavior from the notification PR/branch is already present in
this base through the feature-design refactor. This change isolates that code
rather than merging older package layouts or unrelated main-branch changes.

## Ownership and dependencies

- Notifications owns run content/button policy, background run alert policy and
  delivery, and routine-reminder channel/shade presentation.
- Running retains the run engine, foreground feedback and nudge bucket rules.
  Root binds those rules to a Notifications-owned capability contract.
- Reminders retains recurrence, snooze/dismiss and alarm restoration. Root maps
  its delivery protocol to Notifications presentation and cancellation callbacks.
- Existing Core code remains in place and unchanged. There are no direct imports
  between Notifications and any other feature, in either direction.
- The existing shared mutex, revision, foreground visibility and persisted
  acknowledgement ordering remain in use. Notification actions still ignore stale
  targets and route final-task/confirmation actions through the app.
- Android manifest receiver names, intent extras/actions, channel IDs, request
  codes, notification IDs and resource identifiers remain unchanged. No storage
  migration or UI redesign is introduced.

## Validation

- `python scripts/check-feature-boundaries.py`: passed; 76 Kotlin files,
  18 owners, no forbidden feature imports or cycles. Self-tests explicitly reject
  Notifications dependencies in either direction with Running and Reminders.
- All 158 JVM tests passed, with zero failures, errors or skipped tests. The moved
  notification/alert policy suites exercise the root-bound existing run engine.
- `:app:lintDebug`: passed, zero errors (111 warnings and 7 hints across the app).
- `:app:assembleDebug`: passed.
- Whitespace check passed with `core.whitespace=cr-at-eol` to respect the existing
  composition root's CRLF format. No Core sources were changed.
- Review checked engine bindings, persistence-before-revision, shared mutex and
  visibility wiring, stable Android identifiers and notification retry behavior.
- JDK 17.0.16 (`java`, `javac`, `jlink`) verified; pinned Gradle 9.8.0 was cached.
  Initial offline configuration could not resolve uncached Dropshots 0.6.0;
  the online build resolved it and completed all checks successfully.

The Linux executor uses the checked-in Unix Gradle wrapper and the installed
complete JDK 17, equivalent to the Windows-only agent helper. The published
cyberpunkAndroid dependency remains unchanged. No Android device is connected,
so notification shade/button and background/foreground behavior needs device
verification.
