---
name: android-gradle
description: Use for opencode-mobile AGP 9, Gradle Kotlin DSL, the version catalog, module graph, source sets, SDK/toolchains, dependencies, or build types.
---

# Android Gradle

The project uses Gradle 9.4.1, AGP 9.4.1, Kotlin 2.4.21, compile SDK 37, target SDK 36,
min SDK 26, Java 11 source/target compatibility, and a Gradle daemon toolchain
pinned to JDK 21. There are no product flavors; build types are `debug`
(application ID suffix `.debug`, label "OpenCode Dev") and `release`.

## Modern AGP Rules

- Use Kotlin DSL and `gradle/libs.versions.toml`.
- AGP 9 has built-in Kotlin support; do not add `org.jetbrains.kotlin.android`.
- Do not introduce deprecated `kotlinOptions` or legacy variant APIs.
- Inspect the catalog, the target build file and one similar module before adding
  dependencies or plugins.
- Do not bump a major dependency, SDK, Kotlin, AGP or Gradle during unrelated work.

## Module Graph

```text
app -> feature:{connect,sessions,chat,settings,terminal} -> core:{ui,mvi,domain}
app -> core:data -> core:network -> core:api (generated)
core:data, core:network, core:ui -> core:domain
```

- Features do not depend on `:app`, other features, `core:network` or `core:api`.
- `core:api` contains only generated sources; its build file is hand-written and
  must keep the opt-ins and lint exclusions it needs.
- Add a module to `settings.gradle.kts` and only the configurations consuming it.

## Build Logic

Module build files are intentionally explicit. Create a convention plugin only
when configuration is genuinely repeated and the plugin stays small and
single-purpose. Do not use `allprojects`/`subprojects` hacks.

## Verification

- Module tasks while iterating, e.g. `./gradlew :feature:chat:testDebugUnitTest`.
- `./gradlew :app:assembleDebug` as the standard build check.
- `./gradlew spotlessCheck testDebugUnitTest :app:lintDebug :app:assembleDebug` before finishing broad
  changes.
