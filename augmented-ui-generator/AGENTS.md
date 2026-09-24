# Augmented UI Generator Agent Guide

This directory is a standalone library. Do not couple it to TaskChain application modules or place experiment output in `src/main/`.

Before changing the library, read [[README|Augmented UI Generator introduction]]. Before changing fixtures or tests, also read [[docs/testing|Testing and fixture conventions]]. The short rules are:

- Production library code belongs in `src/main/`.
- JUnit tests belong in `src/test/` and should protect library behavior.
- Hand-authored shape inputs belong in `fixtures/manual/`.
- AI-authored shape inputs belong in `fixtures/agent/`.
- Generated galleries, screenshots, logs, and rendered files belong in `build/fixture-output/<fixture-id>/`; never place them at the module root or in a source set.
- A fixture filename must match its JSON `id`. Use `<purpose>.shape.json` for manual fixtures and `<yyyy-mm-dd>-<producer>-<purpose>.shape.json` for agent fixtures.

Run the fixture contract before reporting a fixture complete:

```cmd
node --test tools\fixture-contract.test.mjs
```

Run library tests from this directory with:

```cmd
gradle-agent.cmd testDebugUnitTest
```

Compose geometry is authoritative. Browser galleries are approximate visual output and do not replace Kotlin tests or native visual verification.
