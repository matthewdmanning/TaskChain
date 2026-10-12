# Testing strategy

This file sets the TaskChain testing strategy. Work in progress (WIP) does not have to conform yet.
Track conformance in [adoption-backlog.md](adoption-backlog.md).

Status words in this folder:

- **Exists**: the repo has it today.
- **Proposed**: the strategy requires it. The repo does not have it yet.

## Principles

- Test behavior that can lose user data or break a run. Skip trivial code and framework code.
- Keep domain rules in `domain/` so JVM tests can reach them. See `architecture.md`.
- Run fast checks on every pull request. Run slow checks on demand or on `main`.
- Never test against the regular app `com.taskchain`. It holds valued user data.
- Use synthetic fixtures only. Instrumentation uses `com.taskchain.testapp`.

## Test layers

| Layer | Location | Runs on | Status | Covers |
| --- | --- | --- | --- | --- |
| Unit (JVM) | `app/src/test` | CI and local | Exists | Run engine, schedule, migration, validation, presentation math |
| Compose and storage instrumentation | `app/src/androidTest` | Device | Exists | Builder, runner Back, bubble launch, startup recovery, legacy migration |
| Navigation on device | `app/src/androidTest` | Physical device | Partly exists | Back, gesture, bubble, recreation. See [device-testing.md](device-testing.md) |
| Screenshot on device | `app/src/androidTest` | Physical device | Proposed | Screen and component appearance. See [device-testing.md](device-testing.md) |
| Upgrade and reboot | Manual script plus checks | Physical device | Proposed | Data survives update and reboot. See [on-device-data.md](../on-device-data.md) |
| Manual device pass | Checklist | Physical device | Exists (informal) | TalkBack, haptics, sound, notifications |

## What to test at each layer

### Unit (JVM)

- Run engine transitions: skip, complete, resume, abort, finish confirmation.
- Timestamp-derived timer state and the 18-hour expiry rule.
- Schedule and reminder calculation.
- Storage envelope, schema version and legacy migration.
- Routine and builder validation.
- Pure presentation math, such as countdown progress and rim geometry.
- UI provider contracts that need no Android runtime (`UiProviderTest`).

Add a focused regression test when a change touches run transitions, persisted timestamps or confirmation rules. This rule comes from `AGENTS.md`.

### Instrumentation

- Real file storage: concurrent access, I/O errors, quarantine, unsupported schema.
- Compose behavior with real semantics: tap, drag, text input, state restoration.
- Activity recreation and process restore.
- Match by semantic matcher first. Use `testTag` only when a match needs more than three matchers.

### Navigation and screenshots

Both run on a physical device. See [device-testing.md](device-testing.md).

## Profiling (optional)

Profiling runs only when the user asks for it. Never run it as part of the default suite. Never add it to CI.

- Target the `com.taskchain.testapp` package on the reference physical device. Never profile `com.taskchain`.
- Profile one scenario at a time. Name the scenario and the metric before you start.
- Keep the device state fixed: charge level, thermal state, animation scales and font scale. Record them.
- Run each scenario at least 3 times. Report every run, not only the best one.
- Report the numbers with the trace file path. Set no pass or fail gate. A number is a finding, not a verdict.

| Scenario | Metric | Tool | Status |
| --- | --- | --- | --- |
| Cold start to Home | Startup time | Macrobenchmark `StartupTimingMetric` | Proposed |
| Runner dial and subtask rims while running | Frame timing | `adb shell dumpsys gfxinfo <package> framestats`, Perfetto trace | Proposed |
| Builder drag reorder with many tasks | Frame timing | `dumpsys gfxinfo framestats`, Perfetto trace | Proposed |
| Long run (1 hour) | Battery use | `adb shell dumpsys batterystats --charged <package>` | Proposed |
| Provider swap (#26) | Frame timing on both providers | `dumpsys gfxinfo framestats`, Perfetto trace | Proposed |

Commands, checked against developer.android.com through Context7 on 2026-10-12:

```text
adb shell dumpsys gfxinfo <package> framestats
adb shell perfetto --out <file> <atrace-category>
adb pull /data/local/traces/ .
adb shell dumpsys batterystats --charged <package>
```

- Macrobenchmark needs its own `com.android.test` module. The app under test must be `profileable`. Use `MacrobenchmarkRule.measureRepeated`.
- Macrobenchmark writes reports and traces to `<module>/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device_id>/`.
- Not found in the docs, so excluded: `am start -W` and `FrameTimingMetric`. Add them only after you verify them.
- The Perfetto atrace categories for TaskChain are not chosen yet. Choose them before the first run.

Put the results in the `profiling` block of the results artifact. Leave the block `null` when the user did not ask.

## Results artifact

Each test run that matters produces one results artifact. It is part of the strategy.

- Start from [results-template.html](results-template.html). Copy it. Never edit the template in place.
- Fill in only the JSON block. Keep every key. Use `not-run` for work you did not do.
- Save the copy as `.scratch/test-results/<yyyy-mm-dd>-<branch>.html`.
- Publish the copy as an artifact when the run is evidence for a pull request or a release.
- Show a visual regression for each screen with a failed screenshot test.
- Also show a visual regression for each screen that a changed file is expected to affect, even when the test did not fail.
- Use a frame strip for a dynamic regression, such as the timer, an animation or a drag. Capture the frames on the device at fixed times.
- In the published copy, embed every image as a `data:` URI. The page cannot load other images.
- Never invent an image. An empty field shows "No image", which is itself a gap to report.
- Report device evidence apart from JVM evidence.

## Do not test

- Getters, setters and data-class `equals`.
- Material 3 and cyberpunkAndroid internals. Test only the TaskChain contract around them.
- One-off scripts such as `scripts/patch_fastup_digits.py`.
- Pixel-exact animation frames. Test the end state.

## CI split

| Trigger | Checks | Status |
| --- | --- | --- |
| Pull request and `main` | Unit tests, lint, debug APK build | Exists (`ci.yml`) |
| Every push | Gitleaks scan | Exists (`ci.yml`) |
| Before merge of UI or reminder changes | Device suite from [device-testing.md](device-testing.md), run by hand | Proposed |
| Release | Device suite, upgrade test, reboot test, manual pass | Proposed |

Do not add an emulator job to the pull-request workflow. Emulator runs are slow and flaky for this app. The device suite needs real hardware for Back gestures, TalkBack and system UI.

## Commands

Use JDK 17. Humans use `./scripts/gradle.ps1`. Sandboxed agents use `scripts\gradle-agent.cmd`.

```text
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
:app:assembleDeviceTest :app:assembleDeviceTestAndroidTest
:app:connectedDeviceTestAndroidTest
```

On Windows, run Python tests with `uv run python -m pytest`. TaskChain has none today.

## Coverage targets

- No numeric coverage threshold. A threshold invites tests that add no value.
- Every behavior invariant in `CONTEXT.md` has at least one test. Track gaps in the backlog.
- Every user-visible navigation event in [navigation-events-review.md](../navigation-events-review.md) has at least one device test.

## Related documents

- [device-testing.md](device-testing.md): physical-device navigation and screenshot tests.
- [results-template.html](results-template.html): template for the results artifact.
- [adoption-backlog.md](adoption-backlog.md): gaps between this strategy and the repo.
- [on-device-data.md](../on-device-data.md): data safety, snapshots, upgrade and reboot rules.
- [navigation-events-review.md](../navigation-events-review.md): event map and open navigation risks.
