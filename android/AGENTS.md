# Android App Guidance

## Scope

`android/` is a multi-module Android application built with Gradle 9.4.1, AGP 9.4.1
(built-in Kotlin), Kotlin 2.4.21, Java 11, compile SDK 37, target SDK 36 and min SDK 26. The UI
uses Jetpack Compose and Material 3 themed with OpenCode V2 tokens; state uses MVI; Koin
provides dependency injection; the OpenCode HTTP client is generated (Retrofit +
kotlinx.serialization).

Use `android-master` as the entry skill. It routes to the smallest relevant specialist
skill set. Read the files you are changing and follow a nearby feature before introducing
a new pattern.

## Instruction Precedence

1. Explicit task requirements and behavior confirmed in the running app.
2. This `AGENTS.md` and the repository root `AGENTS.md`.
3. Project skills under `.opencode/skills/`.
4. Official vendor guidance compatible with the pinned versions.
5. External community guidance as advisory material only.

External guidance must never implicitly change the architecture, upgrade a major
dependency, introduce a new framework, bypass the design system, or bypass the
repository boundaries.

## Modules

| Module | Responsibility |
|---|---|
| `:app` | `OpenCodeApplication` (starts Koin), `MainActivity`, root navigation, `appModule` (ViewModels), optional session notifications (`notifications/`: `SessionMonitor`, foreground service, permission reply receiver) |
| `:feature:connect` | QR scan, paste link, manual URL + password/code, saved servers |
| `:feature:sessions` | Session list, search, project filter, create/rename/delete |
| `:feature:chat` | Live transcript, composer, agent/model pickers, permissions, interrupt |
| `:feature:settings` | Servers, theme, display preferences, about |
| `:core:ui` | `OcTheme` tokens, typography (Inter, IBM Plex Mono), `Oc*` components, Markdown |
| `:core:mvi` | `BaseViewModel`, `CollectEffect`, `OptimisticState` |
| `:core:domain` | UI-facing models, repository interfaces, `TranscriptReducer` |
| `:core:data` | Repository implementations, encrypted server store, shared event stream, `dataModule` |
| `:core:network` | OkHttp/Retrofit client, Basic auth, SSE reader, error mapping, generated-to-domain mappers |
| `:core:api` | Generated OpenCode client. Never edit; run `bun run generate` in `../tools/api-gen` |
| `:core:testing` | `MainDispatcherRule` and shared test support |

Dependency direction: `app -> feature -> core:{ui,mvi,domain}`; `app -> core:data ->
core:network -> core:api`. Features never depend on `:app`, other features,
`core:network` or `core:api`.

## Compose And MVI

- Each screen has `Event`, immutable `State` and one-shot `Effect` types; the ViewModel
  extends `BaseViewModel<State, Event, Effect>` and exposes one `onEvent`.
- `XxxRoute` resolves the ViewModel with `koinViewModel()`, collects state with
  `collectAsStateWithLifecycle` and effects with `CollectEffect`; `XxxScreen(state,
  onEvent)` is stateless and previewable.
- Use `Oc*` components and `OcTheme`/`MaterialTheme` tokens; no hard-coded colors in
  features. Tokens and components are documented in `docs/design-system.md`.
- Every interactive control has a stable `testTag`; the root enables
  `testTagsAsResourceId` for Maestro and UI Automator.
- Use stable keys in lazy lists; keep existing content during refresh.
- A nested `Scaffold` sets `contentWindowInsets = WindowInsets(0)` when a parent already
  handles system insets. Keep IME padding at the lowest container that must move.
- Strings live in resources with English defaults and translations for Italian, Spanish,
  French, German, Brazilian Portuguese, Russian, Simplified Chinese, Japanese, Korean and
  Arabic. Keep format arguments and locale-specific plurals intact; from the repository
  root, `bun test tools/locales.test.ts` checks translation coverage.

## Data And API

- The server contract is generated; never hand-write DTOs. See the root `AGENTS.md`.
- Features use repository interfaces from `core:domain`; failures are `OpenCodeError`.
- Live updates come from `EventRepository.events`; the chat applies them with
  `TranscriptReducer` and reloads from the server after reconnects and step ends.
- Transcripts are cached in Room (`core/data/cache`, KSP) and synced incrementally; see the
  `android-data-boundaries` skill before changing message loading.
- Never log or display passwords, pairing tokens/codes, Authorization headers, prompts or
  tool output in logs.

## Build And Verification

From `android/`:

```bash
./gradlew :app:assembleDebug
./gradlew testDebugUnitTest
./gradlew :core:network:testDebugUnitTest
ANDROID_SERIAL=<serial> ./gradlew :app:installDebug
```

Before finishing: `./gradlew testDebugUnitTest :app:lintDebug :app:assembleDebug` (what CI
runs), plus `bun run check` in `../tools/api-gen` when the API changed.

After every UI or behavior change, install the debug build (`io.github.beppe2880.opencodemobile.debug`)
on a running emulator and inspect the changed screen and Logcat. `adb reverse tcp:4096
tcp:4096` lets the app reach the host OpenCode server at `http://localhost:4096` (it is also
at `http://10.0.2.2:4096`). Bring the existing task back without force-stopping:

```bash
adb -s <serial> shell am start -n io.github.beppe2880.opencodemobile.debug/io.github.beppe2880.opencodemobile.MainActivity
adb -s <serial> logcat -d -v threadtime AndroidRuntime:E '*:S'
```

Do not hard-code an emulator serial; use the one reported by `adb devices` or the script.
