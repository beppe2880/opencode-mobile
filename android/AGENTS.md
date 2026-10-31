# Android and iOS App Guidance

## Scope

`android/` is the Gradle build of the Android and iOS apps: Gradle 9.8.1, AGP 9.4.1, Kotlin
2.4.21 Multiplatform, Java 11, compile SDK 37, target SDK 36, min SDK 26, iOS 15+ (`iosArm64`,
`iosSimulatorArm64`). Every module except `:app` and `:core:testing` is a Kotlin Multiplatform
library (`com.android.kotlin.multiplatform.library`); the UI uses Compose Multiplatform and
Material 3 themed with OpenCode V2 tokens; state uses MVI; Koin provides dependency
injection; the OpenCode HTTP client is generated (Ktor + kotlinx.serialization).

Use `kmp-master` as the entry skill. It routes to the smallest relevant specialist
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
| `:app` | Android only: `OpenCodeApplication` (starts Koin), `MainActivity`, `appModule`, optional session notifications (`notifications/`: `SessionMonitor`, foreground service, permission reply receiver), home screen widget (`widget/`, Glance) |
| `:shared` | Shared by both apps: `OpenCodeApp` shell, root navigation (`AppNavHost`), `sharedModule` (ViewModels), widget model and loader (`widget/`); iOS entry point `MainViewController()`, `opencodemobile://` links and the widget snapshot for WidgetKit, in the `Shared` framework |
| `:feature:connect` | QR scan, paste link, manual URL + password/code, saved servers |
| `:feature:sessions` | Session list, search, project filter, create/rename/delete |
| `:feature:chat` | Live transcript, composer, agent/model pickers, permissions, interrupt |
| `:feature:settings` | Servers, theme, display preferences, about |
| `:feature:terminal` | Server shell over the PTY WebSocket (xterm.js from `composeResources/files/terminal` in a WebView on Android, a WKWebView on iOS), configurable key bar |
| `:feature:files` | Read-only file viewer (`/api/fs/read`): highlighted text with line numbers, images |
| `:core:ui` | `OcTheme` tokens, typography (Inter, IBM Plex Mono), `Oc*` components, Markdown |
| `:core:mvi` | `BaseViewModel`, `CollectEffect`, `OptimisticState` |
| `:core:domain` | UI-facing models, repository interfaces, `TranscriptReducer` |
| `:core:data` | Repository implementations, server store (EncryptedSharedPreferences / iOS Keychain), Room cache, shared event stream, `dataModule` + `platformDataModule` |
| `:core:network` | Ktor client (OkHttp engine on Android, Darwin on iOS), Basic auth, SSE reader, error mapping, generated-to-domain mappers |
| `:core:api` | Generated OpenCode client. Never edit; run `bun run generate` in `../tools/api-gen` |
| `:core:testing` | `MainDispatcherRule` and shared test support |

Dependency direction: `app -> shared -> feature -> core:{ui,mvi,domain}`; `app -> core:data
-> core:network -> core:api` (on iOS `shared` links `core:data`). Features never depend on
`:app`, `:shared`, other features, `core:network` or `core:api`.

## Multiplatform Layout

- Sources live in `src/commonMain/kotlin`; platform code in `src/androidMain/kotlin` and
  `src/iosMain/kotlin` (interfaces bound in Koin, or `expect`/`actual` for small helpers;
  see the `kmp-platform-boundaries` skill). Pure-logic tests go in `src/commonTest/kotlin`
  with `kotlin.test` and run on the JVM and the iOS simulator; tests needing JVM tooling
  (MockWebServer, Robolectric, Room) live in `src/androidHostTest/kotlin`. Both run with
  `testAndroidHostTest`, also reachable as `testDebugUnitTest`.
- Keep `java.*`, `android.*`, OkHttp and Activity APIs out of `commonMain`. Use
  kotlinx-datetime, `kotlin.time.Clock`, `kotlin.uuid.Uuid`, atomicfu locks and Compose
  Multiplatform APIs instead.
- Android-only features stay in `androidMain` or `:app`; on iOS they are hidden
  (`notificationsSupported`) rather than faked. The home screen widget exists on both: Glance
  in `:app`, a WidgetKit extension in `iosApp/SessionsWidget` fed by the app (see the
  `kmp-ios` skill).

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
- Strings live in Compose Multiplatform resources (`src/commonMain/composeResources/values*`,
  accessed through each module's generated `Res`) with English defaults and translations for
  Italian, Spanish, French, German, Brazilian Portuguese, Russian, Simplified Chinese,
  Japanese, Korean and Arabic. Placeholders are positional only (`%1$s`, `%2$d`); write `'`,
  `"` and `%` unescaped. `:app` keeps Android resources for notifications. From the
  repository root, `bun test tools/locales.test.ts` checks translation coverage.

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
./gradlew spotlessApply   # ktlint formatting, rules in .editorconfig; core/api is excluded
```

Before finishing: `./gradlew spotlessCheck testDebugUnitTest :app:lintDebug :app:assembleDebug`
and `./gradlew compileKotlinIosArm64 compileKotlinIosSimulatorArm64` (what CI runs; iOS klibs
compile on Linux too), plus `bun run check` in `../tools/api-gen` when the API changed.

### iOS

Linking the `Shared` framework and building `../iosApp` need macOS with Xcode. From
`../iosApp`:

```bash
xcodebuild -project iosApp.xcodeproj -target iosApp -configuration Debug -sdk iphonesimulator \
  -arch arm64 CODE_SIGN_IDENTITY=- CODE_SIGNING_ALLOWED=YES CODE_SIGNING_REQUIRED=NO build
xcrun simctl install booted build/Debug-iphonesimulator/OpenCodeMobile.app
xcrun simctl launch booted io.github.beppe2880.opencodemobile
```

Ad-hoc signing embeds the entitlements the app needs at runtime: a build with
`CODE_SIGNING_ALLOWED=NO` (enough for CI, which only compiles) cannot store the server
password in the Keychain and fails to connect with error -34018.

The Xcode build phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` (needs
`JAVA_HOME`). Set `TEAM_ID` in `../iosApp/Configuration/Config.xcconfig` to sign for a device.

After every UI or behavior change, install the debug build (`io.github.beppe2880.opencodemobile.debug`)
on a running emulator and inspect the changed screen and Logcat. `adb reverse tcp:4096
tcp:4096` lets the app reach the host OpenCode server at `http://localhost:4096` (it is also
at `http://10.0.2.2:4096`). Bring the existing task back without force-stopping:

```bash
adb -s <serial> shell am start -n io.github.beppe2880.opencodemobile.debug/io.github.beppe2880.opencodemobile.MainActivity
adb -s <serial> logcat -d -v threadtime AndroidRuntime:E '*:S'
```

Do not hard-code an emulator serial; use the one reported by `adb devices` or the script.
