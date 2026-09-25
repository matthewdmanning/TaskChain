# TaskChain tool recovery handoff

Current worktree: `C:\GitHub\TaskChain`, branch `docs-improve-app-journey`. Read `AGENTS.md` before using tools and preserve the existing dirty worktree. The app workflow is tracked in `docs/IMPROVE-APP-PLAN.md`; Phase 2 is complete.

- Earlier failures came mainly from Windows shell quoting, guessed paths, and repeated retries. For parent-side commands, PowerShell with `login: false`, single-quoted paths, and `-LiteralPath` worked. Subagents must use `cmd.exe` and native executables, as `AGENTS.md` specifies.
- From the repo root, these searches worked: `rg.exe --files app\src\main\java\com\taskchain\ui` and `rg.exe -n TaskChainApp app\src\main\java\com\taskchain\ui\TaskChainApp.kt`. Check paths before searching; `rg` exit 1 without a diagnostic can simply mean no match. Never repeat an unchanged failed command.
- The global hook was changed from `PostToolUse` to `PostToolUseFailure`. Its script now writes the command, exit code, and error to `~/.codex/tool-failures.log`. The old false positive on successful text containing “Access is denied” was removed. The installed regression test passes with `node.exe (Join-Path ([Environment]::GetFolderPath('UserProfile')) '.codex\hooks\tool-failure.test.js')` in PowerShell.
- **Live logging remains unverified.** A failed nested command in the already-running session did not auto-log. Its observed `PostToolUse` payload contained raw output but no exit code. In a fresh session, make one controlled failing call and inspect the global log. Until that works, manually log missed failures. Pre-execution rejections also require manual logging. Stop and alert the user after two consecutive tool failures.
- The session that wrote this handoff had approval policy `never`; do not pass `sandbox_permissions` under that profile. Check the next session’s actual permissions before acting.

## Suggested skills

- `diagnosing-bugs` for the remaining live hook check.
- `improve-app` to resume the app workflow.
