# TaskChain

TaskChain is a local-only Android routine builder and step-by-step runner. It combines reusable tasks, habits, goals, optional timers, scheduling, reminders, and progress history without a database or network service.

The app is intentionally a single Android module. Source and resources live under `app/src/`; architecture and domain terminology are documented in `architecture.md` and `CONTEXT.md`.

## Build

From PowerShell, run `./scripts/gradle.ps1 :app:assembleDebug`. Set JAVA_HOME to a complete JDK 17. The script rejects other runtimes and forwards additional arguments to the checked-in Gradle wrapper.
