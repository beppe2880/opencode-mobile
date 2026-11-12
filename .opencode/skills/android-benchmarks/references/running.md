# Running The Benchmarks

All commands run from `android/`. Pick the device with `ANDROID_SERIAL` (discover it with
`adb devices -l`; never hard-code a serial in scripts or docs).

## Run

```bash
# Free the device from other UiAutomator clients.
adb -s <serial> shell pkill -f mobilecli

# Everything (about ten minutes on an emulator).
ANDROID_SERIAL=<serial> ./gradlew :benchmark:connectedBenchmarkAndroidTest

# One class while iterating.
ANDROID_SERIAL=<serial> ./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.beppe2880.opencodemobile.benchmarks.IdleWorkBenchmark
```

The task builds and installs `app-benchmark.apk` (`io.github.beppe2880.opencodemobile.benchmark`)
and the test APK (`io.github.beppe2880.opencodemobile.benchmarks`), and removes them
afterwards. The installed release and debug apps are not touched.

## Read The Results

Results are JSON, one file per run, with the Perfetto traces beside it:

```text
benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/<device>/
```

```bash
f=$(find benchmark/build/outputs -name '*benchmarkData.json' | head -1)
jq -r '.benchmarks[] | "\(.className | split(".") | last).\(.name): " + ((
  [.metrics | to_entries[] | "\(.key) median=\(.value.median) min=\(.value.minimum) max=\(.value.maximum)"] +
  [(.sampledMetrics // {}) | to_entries[] | "\(.key) P50=\(.value.P50) P90=\(.value.P90) P99=\(.value.P99)"]
) | join(" | "))' "$f"
```

`metrics` holds one value per iteration (counts, startup time); `sampledMetrics` holds the
per-frame samples of `FrameTimingMetric` as percentiles (`frameDurationCpuMs`,
`frameOverrunMs`; a positive overrun is a missed frame).

A failed test is in `benchmark/build/outputs/androidTest-results/connected/benchmark/*.xml`
and the HTML report under `benchmark/build/reports/androidTests/`. "Not on screen after
10000ms" means the demo did not reach the expected screen: check the selector in
`Demo.kt` against the app's test tags.

To keep a baseline, copy the JSON out of `build/` before the next run (the directory is
replaced), run again on the changed code, and compare medians.

## What The Idle Numbers Mean

`IdleWorkBenchmark` leaves each screen alone for 10 s after a 3 s settle:

| Test | Screen | Expect |
|---|---|---|
| `chatOfIdleSession` | a chat with nothing at work | about 0 frames, a few ms of CPU: the reference |
| `sessionListWithRunningSessions` | demo list, sessions at work | close to the reference |
| `chatWithRunningAgent` | chat of a working session | close to the reference |

A running screen near 600 frames means something on it animates every frame.

## On A Phone

Connect it, allow USB debugging, and run without the emulator suppression so
Macrobenchmark checks the device state (battery, debuggable target):

```bash
ANDROID_SERIAL=<serial> ./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=
```

Timings from a phone are the ones to quote. Power use is not measured here:
`PowerMetric` needs a Pixel 6 or later; on other phones use `adb shell dumpsys batterystats`
over a fixed, repeated scenario.

## Add A Test

1. Add or reuse a selector in `Demo.kt`; tags come from the app's `testTag`s.
2. Add a `@Test` to the class that owns the metric (`IdleWorkBenchmark` for a still
   screen, a new class for a new journey).
3. Navigate in `setupBlock` after `restartDemo()`; measure only the block.
4. Run that class alone until it passes three times with stable counts.
5. If the demo lacks the data the scenario needs, extend the demo data in
   `app/src/main/java/.../fake`, not the benchmark.
