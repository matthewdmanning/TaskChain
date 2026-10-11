# Reorganize agent docs by the scope of the AI's task

Status: needs-triage

## Problem

- `AGENTS.md` should be strictly a router. It now also holds build commands, style rules, and subagent procedures.
- Agent docs are not grouped by the scope of the task an AI performs. Some facts have no natural home, so they stay unwritten or land in the wrong file.
- `docs/agents/android-sdk-access.md` is a diagnosis record. It is the wrong place for device-install rules.

## Goal

Group agent docs by task scope, for example ui/ux, git operations, state machine, accessibility, build and device testing. `AGENTS.md` only routes to them.

## Facts waiting for a home

- **Device install:**
  - `installDebug` updates in place (`applicationId` `com.taskchain`, `allowBackup=false`).
  - Never run `adb uninstall` or `pm clear` on it. Both delete on-device data.
  - A second package, `com.taskchain.dragcheck`, may sit on the same device. Both share a launcher label.
- **Gradle from Git Bash (agents):**
  - `export JAVA_HOME="C:\\Program Files\\Java\\jdk-17"; cmd //c "scripts\\gradle-agent.cmd :app:compileDebugKotlin :app:testDebugUnitTest"`.
  - Calling `gradle-agent.cmd` with no task only starts a daemon.
- **Line endings:**
  - `gradlew.bat` has `eol=crlf`. Several sources use CRLF.
  - Edit scripts must read and write with `newline=''` and keep each file's ending.
  - A wrong ending makes the file show as modified and blocks `git merge --ff-only`.
- **cyberpunkAndroid:** `CyberPrimitives.Spacing` stops at `dp32`. `dp48` exists only in `IconSizes`. Check an API at the pinned tag with `gh api repos/matthewdmanning/cyberpunkAndroid/contents/<path>?ref=v1.0.8`.
- **Gesture tokens:** the first draft of `RunnerGestureTokens` uses fractions and ranges that are untuned. Document how to tune them once the values settle.

## Acceptance

- `AGENTS.md` contains only routing.
- Each fact above lives in a doc named for its task scope.
- `docs/agents/domain.md` and the other routed docs still resolve.

## Comments
