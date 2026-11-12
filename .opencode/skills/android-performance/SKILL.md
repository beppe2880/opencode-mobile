---
name: android-performance
description: Use only for evidenced opencode-mobile startup, streaming-chat jank, recomposition, memory, APK size, R8, baseline profile, or Perfetto trace work; requires measurement before optimization.
---

# Android Performance

No performance optimization without evidence. Measure one user-visible
transition, change one axis, and measure the same transition again.

## Route The Problem

- Compose recomposition/skipping: compiler reports and recomposition evidence.
- Frame jank, CPU, scheduling, startup, IO, binder, or system interaction:
  Perfetto/System Trace.
- Release shrinking, missing classes, or APK size: R8 mapping/usage/configuration
  analysis.
- Critical startup/journey regression, or a screen that keeps drawing while idle:
  the Macrobenchmark module (`android-benchmarks` skill) for the before and after;
  Baseline Profiles when justified.
- Memory/resource lifetime: profiler/heap evidence plus explicit lifecycle
  ownership review.

Do not infer a Compose problem from a slow screen without a trace.

Use `references/compose-diagnostics.md` for Compose-specific diagnosis and
`references/perfetto.md` for trace capture/analysis.

## Compose

- Kotlin 2.2 strong skipping changes old stability assumptions.
- Do not add `@Stable`, `@Immutable`, `remember`, `derivedStateOf`, immutable
  collections, or callback wrappers by default.
- Distinguish actual data changes, unstable instance churn, State reads in the
  wrong phase, and cross-phase back-writing.
- False stability annotations can produce stale UI.

## Compose Multiplatform Resources

`stringResource`/`Font` from Compose resources resolve synchronously on first
composition; many distinct strings in a lazy list item can cost frame time. Measure
(trace or macrobenchmark against the previous build) before restructuring; hoist
repeated lookups out of list items only with evidence.

## Startup

Inspect synchronous work in `Application`, Activity creation, Koin graph startup,
network discovery, disk reads, class loading, and main-thread blocking. Do not
move work to a background scope merely to hide it; define when consumers may use
the initialized dependency and how failures/retries behave.

## R8 And Release

- Inspect AGP version and current release configuration first.
- Release builds use R8 (`isMinifyEnabled`, `isShrinkResources`); libraries ship
  their own keep rules and `app/proguard-rules.pro` only fills gaps.
- AGP 9.4.1 includes the standalone R8 analyzer task (added in AGP 9.3); verify
  its output against the release build before acting on it.
- Remove or narrow keep rules only with release-build and runtime evidence.
- Validate kotlinx-serialization (generated API models and union serializers),
  Ktor and kotlinx-serialization, Koin, CameraX and the markdown renderer after R8 changes.

## Report

Record the scenario, build/device, measurement tool, baseline, change,
after result, and residual uncertainty. Do not claim an improvement from source
inspection alone.
