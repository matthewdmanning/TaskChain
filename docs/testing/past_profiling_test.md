# Past profiling tests

This file summarizes finished profiling tests so you can compare builds and configurations.
Android Studio Profiler produced every recorded test.
Follow the directions in [profiling.md](profiling.md).

- Do not paste raw profiler output here.
- Link to the commit.
- Keep each entry short.

## Schema

Each test is one item in the `Tests` list below.
The schema is JSON Schema, written in YAML.

```yaml
type: object
required: [id, date, scenario, task, commit, device, config, metrics]
additionalProperties: false
properties:
  id:
    type: string
    description: Unique slug, for example 2026-10-12-runner-frame-timing-a.
  date:
    type: string
    format: date
  scenario:
    type: string
    description: One scenario from the table in profiling.md.
  task:
    type: string
    description: >
      Android Studio Profiler task. Use one of: system-trace,
      callstack-sample, java-kotlin-methods, heap-dump,
      native-allocations, java-kotlin-allocations.
  commit:
    type: object
    required: [sha, url]
    properties:
      sha: {type: string}
      url: {type: string, format: uri, description: Link to the commit on GitHub.}
  device:
    type: object
    required: [model, android_version]
    properties:
      model: {type: string}
      android_version: {type: string}
      charge_percent: {type: number}
      thermal_state: {type: string}
  config:
    type: object
    description: Settings that vary between compared tests.
    required: [package, build_type]
    properties:
      package: {type: string, description: Must be com.taskchain.testapp.}
      build_type: {type: string, description: For example deviceTest.}
      theme_provider: {type: string}
      font_scale: {type: number}
      animation_scale: {type: number}
      notes: {type: string}
  metrics:
    type: array
    minItems: 1
    items:
      type: object
      required: [name, unit, values]
      properties:
        name:
          type: string
          description: >
            For example frame_count_over_16ms, frame_duration_ms,
            cpu_usage_percent, startup_ms, retained_heap_bytes.
        unit: {type: string}
        values:
          type: array
          minItems: 3
          items: {type: number}
          description: One value per run. At least 3 runs.
        median: {type: number}
  baseline:
    type: string
    description: The id of the test this test compares against.
  findings:
    type: string
    description: One or two sentences on what the numbers show. Set no pass or fail.
```

## Tests

```yaml
[]
```
