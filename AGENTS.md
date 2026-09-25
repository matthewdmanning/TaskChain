# Repository Guidelines

## Project Structure & Architecture

TaskChain is a single-module, offline Android app. Kotlin source is under `app/src/main/java/com/taskchain/`: `domain/model` defines run data, `domain/run` owns transitions and timers, `data` handles local persistence, `reminder` adapts Android alarms, and `ui` contains Compose screens and ViewModels. Keep Material 3 configuration in `ui/designsystem`. Resources and JSON defaults live in `app/src/main/res/` and `app/src/main/assets/config/`; unit tests live in `app/src/test/java/`. Read `architecture.md` and `CONTEXT.md` before changing behavior.

`augmented-ui-generator` is a separate standalone library. It now lives in its own repository beside this one, at `../augmented-ui-generator`. TaskChain does not depend on it. Read that repository's `README.md` and follow its `AGENTS.md` before working there.

## Build, Test & Development Commands

Humans should use `./scripts/gradle.ps1` on Windows. Sandboxed agents must use `scripts\gradle-agent.cmd`, which avoids PowerShell and runs the same checked-in Gradle wrapper with the shared Gradle cache and a complete JDK 21 containing `java`, `javac`, and `jlink`; the helper prefers explicit `GRADLE_USER_HOME`/`JAVA_HOME`, then the local Gradle-managed Eclipse Temurin JDK 21, then Android Studio JBR. The app's Java source and bytecode compatibility remain version 17.

- `./scripts/gradle.ps1 :app:assembleDebug` builds the debug APK on Windows.
- `./scripts/gradle.ps1 :app:testDebugUnitTest` runs local JVM unit tests.
- `./scripts/gradle.ps1 :app:installDebug` installs the app on a connected emulator or device.
- `./scripts/gradle.ps1 :app:lintDebug` runs Android lint.
- `scripts\gradle-agent.cmd :app:compileDebugKotlin` gives sandboxed coding agents Kotlin and Java compiler diagnostics.
- `scripts\gradle-agent.cmd :app:testDebugUnitTest` runs focused JVM regression tests for sandboxed coding agents.
- `scripts\gradle-agent.cmd :app:lintDebug` gives sandboxed coding agents Android and Kotlin lint diagnostics.

Use the checked-in wrapper's pinned Gradle distribution; do not install or manually download a separate Gradle version. The wrapper caches its distribution under `GRADLE_USER_HOME\wrapper\dists`, reuses it when that exact version is present, and downloads it when the cache is absent or incomplete. If a build appears to download Gradle again, first check `GRADLE_USER_HOME` and that cache; report the pinned version and cache state before retrying network access.

### Android SDK access

The machine-local SDK input is `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` in `local.properties`, which decodes to `C:\Users\<you>\AppData\Local\Android\Sdk`. Sandboxed reads of `%LOCALAPPDATA%` can raise `UnauthorizedAccessException` and then misleadingly report `adb.exe` or `emulator.exe` as missing. Do not change `sdk.dir` from that sandbox-only symptom. Retry the same read-only probe with escalation; use the SDK's absolute `platform-tools\adb.exe` and `emulator\emulator.exe` paths for device work. See `docs/agents/android-sdk-access.md` for the reproduced input, root cause, diagnostic loop, and verified fix.

## Coding Style & Naming

Use four-space indentation in Kotlin and Gradle Kotlin scripts. Follow existing `UpperCamelCase` type names, `lowerCamelCase` functions and properties, and package names under `com.taskchain`. Keep UI screens state-driven, put run rules in `domain/run` rather than ViewModels, and isolate Android/file APIs behind the existing boundaries. No formatter or Kotlin-specific lint tool is configured; match nearby code and run Android lint.

## Testing Guidelines

Tests use JUnit 4 and are named `*Test.kt`, mirroring production packages (for example, `domain/run/RoutineRunEngineTest.kt`). Add a focused regression test when changing run transitions, persisted timestamps, or confirmation rules. No coverage threshold is configured; run the JVM tests before opening a PR and verify UI or reminder changes on a device when applicable.

## Commits & Pull Requests

Use a short imperative subject describing the change. In PRs, explain the behavior and affected requirements, include test results, link a relevant issue when one exists, and attach screenshots for UI changes. Do not claim a requirement is implemented without verification.

## Configuration & Agent Guidance

Keep defaults in `app/src/main/assets/config/` and UI values in resources or design-system configuration. Preserve the local-only, account-free, telemetry-free architecture; do not add secrets or remote services. After a sandboxed command fails, verify the command and its exact scope, then retry once with appropriate escalation. If it still fails, stop and provide the exact PowerShell command for manual execution. During parallel work, the root agent owns full Gradle verification unless a worker is explicitly assigned a focused check.

Delegate to the read-only `codebase_scanner` agent with `fork_turns = "none"` when answering a bounded question requires searching enough code that the raw results would add mostly non-useful material to the primary agent's context. Give it the exact question, search scope, and desired evidence; use its distilled, file-cited findings instead of repeating the scan in the primary context.

## Agent skills

### Issue tracker

Issues live as local Markdown under `.scratch/issues/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the five default triage roles. See `docs/agents/triage-labels.md`.

### Domain docs

Use one root context. See `docs/agents/domain.md`.
