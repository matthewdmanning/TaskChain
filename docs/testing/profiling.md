# Profiling (optional)

Profile only when the user asks. Never run profiling as part of the default suite. Never add it to CI.

- Target the `com.taskchain.testapp` package on the reference physical device. Never profile `com.taskchain`.
- Profile one scenario at a time. Name the scenario and the metric before you start.
- Keep the device state fixed: charge level, thermal state, animation scales and font scale. Record them.
- Run each scenario at least 3 times. Report every run, not only the best one.
- Report the numbers with the trace file path. Set no pass or fail gate. A number is a finding, not a verdict.

## Scenarios

| Scenario | Metric | Tool |
| --- | --- | --- |
| Cold start to Home | Startup time | Macrobenchmark `StartupTimingMetric` |
| Runner dial and subtask rims while running | Frame timing | `adb shell dumpsys gfxinfo <package> framestats`, Perfetto trace |
| Builder drag reorder with many tasks | Frame timing | `dumpsys gfxinfo framestats`, Perfetto trace |
| Long run (1 hour) | Battery use | `adb shell dumpsys batterystats --charged <package>` |
| Provider swap (#26) | Frame timing on both providers | `dumpsys gfxinfo framestats`, Perfetto trace |

## Commands

```text
adb shell dumpsys gfxinfo <package> framestats
adb shell perfetto --out <file> <atrace-category>
adb pull /data/local/traces/ .
adb shell dumpsys batterystats --charged <package>
```

## Macrobenchmark

- Create a separate `com.android.test` module.
- Mark the app under test `profileable`.
- Use `MacrobenchmarkRule.measureRepeated`.
- Read reports and traces in `<module>/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device_id>/`.

## Perfetto

- Choose the atrace categories for TaskChain before the first run.

## Unverified tools

- Do not use `am start -W` or `FrameTimingMetric` until you verify them in the Android documentation.

## Results

- Record each finished test in [past_profiling_test.md](past_profiling_test.md), using its schema.
- Put the results in the `profiling` block of the results artifact.
- Leave the block `null` when the user did not ask for profiling.
