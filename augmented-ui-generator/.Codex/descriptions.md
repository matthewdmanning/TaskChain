# Description index

## `requireRecord` — tools/fixture-contract.test.mjs
Use this function to require a non-null JSON object for a named fixture field.
Inputs: `value` — parsed field; `label` — location used in failures.
Dependencies: Node `assert`.

## `requireDp` — tools/fixture-contract.test.mjs
Use this function to require a finite non-negative Dp value in a fixture.
Inputs: `value` — candidate number; `label` — location used in failures.
Dependencies: Node `assert`.

## `validateCorner` — tools/fixture-contract.test.mjs
Use this function to validate one corner primitive against the Kotlin `Cut` vocabulary.
Inputs: `primitive` — parsed corner object; `label` — location used in failures.
Dependencies: `requireRecord`, `requireDp`, `sizedCornerTypes`, Node `assert`.

## `validateEdge` — tools/fixture-contract.test.mjs
Use this function to validate one edge primitive against the Kotlin `EdgeCut` vocabulary.
Inputs: `primitive` — parsed edge object; `label` — location used in failures.
Dependencies: `requireRecord`, `requireDp`, `sizedEdgeTypes`, Node `assert`.

## `validatePaint` — tools/fixture-contract.test.mjs
Use this function to validate an optional border or inlay declaration.
Inputs: `value` — parsed declaration; `label` — location used in failures; `sizeKey` — Dp field name.
Dependencies: `requireRecord`, `requireDp`, `colorPattern`, Node `assert`.

## `validateShape` — tools/fixture-contract.test.mjs
Use this function to validate one shape before a renderer or agent evaluation consumes it.
Inputs: `shape` — parsed definition; `label` — location used in failures.
Dependencies: `cornerNames`, `edgeNames`, `validateCorner`, `validateEdge`, `validatePaint`, Node `assert`.

## `validateFixture` — tools/fixture-contract.test.mjs
Use this function to enforce the shared fixture contract and origin-specific metadata.
Inputs: `fixture` — parsed JSON; `file` — source path; `origin` — `manual` or `agent`.
Dependencies: `validateShape`, `idPattern`, `datePattern`, Node `path`, Node `assert`.

## `loadFixtures` — tools/fixture-contract.test.mjs
Use this function to load every fixture from one origin without a registry file.
Inputs: `origin` — fixture directory and declared source.
Dependencies: Node `readdir`, Node `readFile`, `JSON.parse`, `validateFixture`, `root`, Node `path`.
