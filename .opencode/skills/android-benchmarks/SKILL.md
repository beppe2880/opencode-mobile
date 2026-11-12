---
name: android-benchmarks
description: Use when measuring opencode-mobile Android performance or adding a performance test - the `android/benchmark` Macrobenchmark module, the app's `benchmark` build type, idle frames and CPU, cold start, scroll frame timings, before/after comparisons of a change, or deciding what a benchmark may gate.
---

# Android Benchmarks

`android/benchmark` holds the automated performance tests. They drive the real app
through UiAutomator and read the system trace, so a number comes from the same code a
user runs. Use them to get the "before" of a performance change and the "after" from the
same command. `android-performance` owns diagnosis and fixes; this skill owns measuring.

## What Is Measured

- **Build:** the app's `benchmark` build type: release code and R8, not debuggable,
  debug-signed, application id suffix `.benchmark`, so it installs beside the real app.
  Never measure a debug build: it is several times slower and misleads.
- **Data:** the offline demo (`USE_FAKE_DATA` is true in this build type). Runs need no
  server, always show the same sessions, and the demo has sessions at work. Do not point
  a benchmark at a real server.
- **Tests** (`benchmark/src/main/kotlin/.../benchmarks`):
  - `IdleWorkBenchmark`: screens left alone for 10 s; `idleFrameCount` and `idleCpuMs`
    from `IdleWorkMetric` (frames and CPU of the app process in the trace).
  - `StartupBenchmark`: cold start to the session list, not compiled and with the
    shipped profile.
  - `SessionListScrollBenchmark`: frame timings while flinging the list.

## Which Numbers To Trust

- **Counts and sums compare everywhere:** `idleFrameCount`, `idleCpuMs` relative to the
  idle reference, `frameCount`. A still screen produces about no frames on any machine;
  one infinite animation produces the display rate (about 600 in 10 s at 60 Hz).
- **Timings are relative on an emulator:** startup and frame durations depend on the
  host and its load. Compare a change with its baseline on the same emulator in the same
  session; never quote them as what users get. Absolute numbers need a physical device.
- A benchmark may **gate** (fail a build) only on counts. Timings are reported and
  compared over several runs, not asserted against a fixed threshold.
- The emulator error is suppressed in `benchmark/build.gradle.kts` on purpose; this is
  what makes local and CI runs possible, not a statement that emulator timings are valid.

## Rules

- One emulator, nothing else heavy running, the same device for before and after.
- Stop Mobile MCP's UiAutomator client first (`adb shell pkill -f mobilecli`): two
  UiAutomator clients cannot hold the device.
- Every test starts from a fresh process (`restartDemo()`), so one test never inherits
  the screen of another.
- Find views by `testTag` (exposed as resource ids); add a tag in the app rather than
  matching visible text.
- A new scenario is a new `@Test` in the class that owns the metric; keep the measured
  block to the thing measured and put navigation in `setupBlock`.
- Report the scenario, device, build, metric medians with min and max, and what was not
  measured. A single run is not a result.
- Keep versions in `gradle/libs.versions.toml`. CI compiles the benchmark sources
  (`:benchmark:compileBenchmarkKotlin`) and never runs them; no Baseline Profile plugin is
  configured. Running in CI or adding the plugin is its own task.

Commands, reading the results, and running on a phone are in
[references/running.md](references/running.md).
