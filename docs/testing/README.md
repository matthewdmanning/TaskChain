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
| Navigation on device | `app/src/androidTest` | Physical device | Partly exists | Back, gesture, bubble, recreation. See [automated-physical-device-tests.md](automated-physical-device-tests.md) |
| Screenshot on device | `app/src/androidTest` | Physical device | Proposed | Screen and component appearance. See [automated-physical-device-tests.md](automated-physical-device-tests.md) |
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

### Navigation and screenshots

Both run on a physical device. See [automated-physical-device-tests.md](automated-physical-device-tests.md).

## Results artifact

Each test run that matters produces one results artifact. Follow [results-artifact.md](results-artifact.md).

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
| Before merge of UI or reminder changes | Device suite from [automated-physical-device-tests.md](automated-physical-device-tests.md), run by hand | Proposed |
| Release | Device suite, upgrade test, reboot test, manual pass | Proposed |

Do not add an emulator job to the pull-request workflow. Emulator runs are slow and flaky for this app. The device suite needs real hardware for Back gestures, TalkBack and system UI.

## Coverage targets

- No numeric coverage threshold. A threshold invites tests that add no value.
- Every behavior invariant in `CONTEXT.md` has at least one test. Track gaps in the backlog.
- Every user-visible navigation event in [navigation-events-review.md](../navigation-events-review.md) has at least one device test.

## Related documents

- [device-testing.md](device-testing.md): physical-device safety rules and setup.
- [automated-physical-device-tests.md](automated-physical-device-tests.md): navigation and screenshot tests.
- [human-driven-tests.md](human-driven-tests.md): manual device checks, upgrade and reboot.
- [accessibility-testing.md](accessibility-testing.md): accessibility checks.
- [profiling.md](profiling.md): optional profiling.
- [unit-tests.md](unit-tests.md): JVM test and lint commands.
- [results-artifact.md](results-artifact.md): how to produce the results artifact.
- [results-template.html](results-template.html): template for the results artifact.
- [adoption-backlog.md](adoption-backlog.md): gaps between this strategy and the repo.
- [on-device-data.md](../on-device-data.md): data safety, snapshots, upgrade and reboot rules.
- [navigation-events-review.md](../navigation-events-review.md): event map and open navigation risks.
