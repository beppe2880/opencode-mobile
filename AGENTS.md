# AGENTS.md - opencode-mobile

## Scope

Native Android and iOS client for an OpenCode V2 server (`opencode serve`, default port 4096).
The app pairs with a server, lists sessions, streams conversations live, sends prompts,
switches agent/model and answers permission requests. Code is shared with Kotlin
Multiplatform and Compose Multiplatform; the Gradle build lives in `android/`.

Read the nearest nested `AGENTS.md` before changing a subsystem. Nested instructions take
precedence over this file.

## Repository Map

| Path | Responsibility | Instructions |
|---|---|---|
| `android/` | Gradle build: Kotlin Multiplatform modules, the Android app, Compose Multiplatform, MVI, Koin, Ktor | `android/AGENTS.md` |
| `android/core/api/` | **Generated** OpenCode client (models, events, Ktor APIs) | Do not edit; see API Contract |
| `iosApp/` | Xcode project of the iOS app; hosts the `Shared` framework built from `android/shared` | `android/AGENTS.md` |
| `api/` | Pinned server contract: `openapi.json`, `OPENCODE_VERSION` | This file |
| `tools/api-gen/` | Spec preprocessing for the generator (Bun) | This file |
| `tools/themes/` | OpenCode TUI themes (pinned `@opencode/theme` + assets) → generated `core/ui/.../theme/generated/OpenCodeThemes.kt`; `bun run refresh` / `generate` / `check` | `docs/design-system.md` |
| `.maestro/` | Android end-to-end test flows (testing only) | `.maestro/AGENTS.md` |
| `docs/` | Design system, font licenses, README screenshots (planned work: GitHub issues) | - |
| `.github/` | CI, release, PR title and template checks, issue templates | `CONTRIBUTING.md` |
| `.opencode/skills/` | Project skills (Android, UX, Maestro) | `.opencode/skills/AGENTS.md` |

## API Contract

The OpenCode server is the contract source. Never hand-write DTOs for server payloads.

```bash
cd tools/api-gen
bun run generate  # regenerate android/core/api from api/openapi.json
bun run refresh   # download openapi.json from the local service, pin its version, regenerate
bun run check     # fail when committed generated code is stale
```

Pipeline: `api/openapi.json` + event schemas from npm `@opencode/schema` (same version as
the server, pinned in `tools/api-gen/package.json`) -> `tools/api-gen` preprocessing ->
openapi-generator 7.22 (`kotlin`, `kotlinx_serialization`; models from `jvm-retrofit2`, APIs
from the Ktor-based `multiplatform` library) -> `android/core/api/src/commonMain`.

- Preprocessing only works around generator limitations (union discriminators, open structs,
  mixed primitive unions, deepObject params, unused API areas). Keep each step minimal and
  documented in `tools/api-gen/src/prepare-spec.ts`.
- To use a new API area, add its tag to `KEEP_TAGS` and regenerate.
- Generated code is committed so Gradle builds need neither Bun nor Nix.
- The event union is `io.github.beppe2880.opencodemobile.api.models.OpenCodeEvent`; REST unions such as
  `SessionMessageInfo` are sealed interfaces of `*Value`/`*Wrapper` wrappers.

## Server Access

- Authentication is HTTP Basic with username `opencode`. The password is the server password
  or a token obtained by pairing: `POST /api/pair` (authenticated) returns a short-lived code;
  `GET /auth/connect/{code}` with `Accept: application/json` returns `{ "token" }`.
- `opencode pair` prints pairing links and a QR for the phone; `--url` sets the address
  the phone should use (`opencode pair --url http://localhost:4096` for the emulator).
- On the emulator, `adb reverse tcp:4096 tcp:4096` maps the host server to `http://localhost:4096`.
  The iOS simulator shares the Mac's network: use the address of the OpenCode host.
- Phones reach the server over the LAN or a VPN. Never publish port 4096 to the internet:
  traffic is plain HTTP with Basic auth.
- Never print, log, commit or screenshot passwords, tokens or pairing codes. Scripts read
  credentials from environment variables (for example `OPENCODE_LIVE_PASSWORD`) and never
  echo them.
- Machine-specific notes (network, devices, credentials locations) stay outside the
  repository.

## Workflows

- Use `kmp-master` as the entry skill for Android and iOS work and `product-ux-orchestrator` for
  user-flow planning or audits.
- Commits and pull request titles follow Conventional Commits:
  `type(scope): Description`, see `CONTRIBUTING.md`.
- Prompts sent during verification go only to a disposable session in a scratch directory,
  with a cheap model; never prompt in real sessions.
- After UI or behavior changes, install the debug build on the emulator and inspect the
  changed screen before finishing (see `android/AGENTS.md`); for iOS-specific code also run
  the app on the iOS simulator (macOS with Xcode).
- Before declaring Android work complete, run from `android/`
  `./gradlew spotlessCheck testDebugUnitTest :app:lintDebug :app:assembleDebug`, and `bun run check` in
  `tools/api-gen` when the API or the generator changed. CI runs both.
