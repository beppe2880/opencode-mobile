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
- Critical startup/journey regression: Macrobenchmark and, when justified,
  Baseline Profiles.
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

## Startup

Inspect synchronous work in `Application`, Activity creation, Koin graph startup,
network discovery, disk reads, class loading, and main-thread blocking. Do not
move work to a background scope merely to hide it; define when consumers may use
the initialized dependency and how failures/retries behave.

## R8 And Release

- Inspect AGP version and current release configuration first.
- This project currently has minification disabled; enabling it is a dedicated
  release task, not cleanup.
- AGP 9.4.1 includes the standalone R8 analyzer task (added in AGP 9.3); verify
  its output against the release build before acting on it.
- Remove or narrow keep rules only with release-build and runtime evidence.
- Validate kotlinx-serialization (generated API models and union serializers),
  Retrofit interfaces, Koin, CameraX and the markdown renderer after R8 changes.

## Report

Record the scenario, build/device, measurement tool, baseline, change,
after result, and residual uncertainty. Do not claim an improvement from source
inspection alone.
