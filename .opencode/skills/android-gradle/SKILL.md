---
name: android-gradle
description: Use for opencode-mobile AGP 9, Gradle Kotlin DSL, Kotlin Multiplatform targets and source sets (com.android.kotlin.multiplatform.library), Compose Multiplatform resources configuration, KSP per target, the version catalog, module graph, SDK/toolchains, dependencies, or build types.
---

# Android And KMP Gradle

Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21, Compose Multiplatform 1.12.2, compile SDK 37,
target SDK 36, min SDK 26, JVM target 11, Gradle daemon on JDK 21. iOS targets are
`iosArm64` and `iosSimulatorArm64`. There are no product flavors; app build types are
`debug` (application ID suffix `.debug`), `release` and `benchmark` (release code, debug
key, suffix `.benchmark`, offline demo data; measured by the `:benchmark` test module).

## Module Shapes

- `:app` is the only `com.android.application` (built-in Kotlin, Android only).
  `:core:testing` stays a plain `com.android.library` for JVM test helpers.
- Every other module is a KMP library: `kotlin.multiplatform` +
  `android.kotlin.multiplatform.library`, configured in `kotlin { android { ... } }`
  (namespace, `compileSdk`, `minSdk`, `compilerOptions.jvmTarget`), plus
  `iosArm64()` and `iosSimulatorArm64()`. Compose modules add `compose.multiplatform`
  and `kotlin.compose`. Use `android/core/mvi/build.gradle.kts` as the template.
- Source sets: `commonMain`, `androidMain`, `iosMain`, `commonTest`,
  `androidHostTest` (enable with `withHostTest {}`); dependencies go in
  `sourceSets { xxx.dependencies { } }`, never in a top-level `dependencies {}`
  except KSP and `androidRuntimeClasspath` tooling.
- Modules with Compose resources need `androidResources { enable = true }` inside
  `kotlin { android { } }` (otherwise Android fails at runtime with
  `MissingResourceException`) and a `compose.resources { packageOfResClass = "<namespace>.resources" }`
  block; only `core:ui` sets `publicResClass = true`.
- KSP (Room) is added per target: `add("kspAndroid", ...)`, `add("kspIosArm64", ...)`,
  `add("kspIosSimulatorArm64", ...)`.
- KMP libraries have no `BuildConfig` and a single variant: pass build values from
  the app (`AppInfo`) or use `Platform.isDebugBinary` on iOS.
- The iOS framework (`Shared`, static) is declared only in `:shared`.

## Rules

- Kotlin DSL and `gradle/libs.versions.toml` only. Compose Multiplatform artifacts use
  the `compose-*`, `jb-lifecycle-*`, `jb-navigation-*` catalog entries; on Android they
  resolve to the AndroidX artifacts. Material 3 is pinned to the CMP version that maps
  to the AndroidX release the app used (1.9.0 → 1.4.0) and AndroidX navigation is kept
  at its own patch level in `:shared` `androidMain`.
- Do not add `org.jetbrains.kotlin.android`, `androidTarget {}`, legacy
  `com.android.library` for KMP modules, `kotlinOptions` or legacy variant APIs.
- Inspect the catalog, the target build file and one similar module before adding
  dependencies or plugins. Do not bump a major dependency, SDK, Kotlin, AGP, CMP or
  Gradle during unrelated work.
- Module build files are explicit; no convention plugins. The only cross-project
  logic is the root alias that registers `testDebugUnitTest` → `testAndroidHostTest`
  in KMP modules, so one test command covers every module.

## Module Graph

```text
app -> shared -> feature:{connect,sessions,chat,settings,terminal,files} -> core:{ui,mvi,domain}
app -> core:data -> core:network -> core:api (generated)
shared (iosMain) -> core:data
```

Features never depend on `:app`, `:shared`, other features, `core:network` or `core:api`.
`core:api` contains only generated sources; its build file is hand-written.

## Verification

- While iterating: `./gradlew :<module>:compileAndroidMain
  :<module>:compileKotlinIosSimulatorArm64 :<module>:testDebugUnitTest`.
- Before finishing broad changes: `./gradlew spotlessCheck testDebugUnitTest
  :app:lintDebug :app:assembleDebug compileKotlinIosArm64 compileKotlinIosSimulatorArm64`.
- Release/R8 changes: `./gradlew :app:assembleRelease` and a smoke test of the release
  APK (pairing, chat, event stream). Libraries ship their own keep rules.
- iOS framework linking and slow native builds: `kmp-ios`,
  `kotlin-tooling-native-build-performance`.
