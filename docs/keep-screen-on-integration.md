# Keep-screen-on feature integration

Branch: `feature/keep-screen-on-feature-structure`.
Base: `refactor-feature-based-design` at `8f663d2`.

Cherry-picked `0289b58` (active-run display behavior) and `c3d8d40` (persisted,
default-on Settings switch). Resolved the old package paths against the feature
refactor and retained newer sound preferences and background run-alert behavior.
No other changes from `main` were merged.

## Ownership

- `features/keepscreenon/domain/RunKeepScreenOnPolicy.kt`: active-run/preference rule.
- `features/keepscreenon/ui`: Settings row, its ViewModel and run-effect composition.
- `core/ui/KeepScreenOnEffect.kt`: generic host-view flag acquisition/restoration.
- `core/model` and `core/data`: shared preferences and existing DataStore ownership.
- `TaskChainApp`: constructs the feature ViewModel and injects Settings content and
  the run effect. Settings and Running do not depend on the new feature.
- `values/keep_screen_on_strings.xml`: feature-owned switch label.

The persisted key remains `keep_screen_on_during_run`, default true. Pausing or
opening a confirmation leaves an active run awake. Turning the switch off, ending
the run, or leaving the Run screen releases the effect and restores the previous
view flag. Android controls screen sleep when the app is in the background.

## Verification

- `python scripts/check-feature-boundaries.py`: passed, 72 Kotlin files, 17 owners,
  no forbidden imports or cycles.
- `git diff --check`: passed.
- Feature resource XML and conflict-marker checks: passed.
- `:app:testDebugUnitTest`: passed, 158 tests, zero failures or skipped tests.
  Feature cases cover active, disabled, loading, completed, aborted, paused and
  confirming runs; Core covers new and legacy preference defaults.
- `:app:assembleDebug`: passed.
- `:app:lintDebug`: passed.
- No Android device was connected, so screen sleep/Settings behavior has not been
  verified on a device in this environment.
- JDK 17.0.16, including `javac` and `jlink`, was verified. Gradle 9.8.0 was already
  cached. On this Linux executor, checks use the checked-in Unix wrapper with the
  installed toolchain environment (the repository's agent helper is Windows-only).
- Offline `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` stopped during
  configuration because `com.dropbox.dropshots` 0.6.0 was not cached. The subsequent
  online run resolved dependencies and passed compilation, all JVM tests, lint and
  APK assembly (`BUILD SUCCESSFUL`).

Windows follow-up, with a complete JDK 17 selected:

```powershell
.\scripts\gradle.ps1 :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
