# Perfetto Workflow

Use Perfetto/System Trace for startup, frame jank, CPU scheduling, blocking IO,
binder, memory pressure, and service interactions that source inspection cannot
quantify.

1. Use a release-like/profileable build on a representative device or emulator.
2. Define one scenario and start/end marker before capture.
3. Enable only data sources needed for the hypothesis to keep the trace useful.
4. Reproduce once without unrelated interaction.
5. Inspect the main thread, RenderThread, binder, coroutine/worker threads,
   frame timeline, and IO around the marker.
6. Use Trace Processor SQL for repeated slices, long main-thread work, frame
   misses, or startup phases when visual inspection is insufficient.
7. Record baseline and after measurements from the same scenario/build/device.

Macrobenchmark lives in `android/benchmark` (see the `android-benchmarks` skill); its runs
save a Perfetto trace per iteration next to the results. No Baseline Profile module is
configured; introducing one is a dedicated performance task.
