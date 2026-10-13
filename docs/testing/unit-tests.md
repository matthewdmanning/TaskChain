# Unit tests (JVM)

Use this file to run JVM tests and static checks.
Run device tests from [automated-physical-device-tests.md](automated-physical-device-tests.md).

- Use JDK 17.
- Humans: run `./scripts/gradle.ps1`.
- Sandboxed agents: run `scripts\gradle-agent.cmd`.

```text
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
```

On Windows, run Python tests with `uv run python -m pytest`.
