# Contributing

Thanks for helping! This guide covers how to build the app, the conventions the code follows
and what a pull request needs. Automated checks enforce the title and template rules:
issues and pull requests that still miss them after 72 hours are closed.

## Before you start

- **Bugs and features**: open an issue from a template first, so we can agree on the approach
  before you write code. Small fixes and typos can go straight to a pull request.
- **AI tools are welcome**, but you must understand and test what you submit. Long
  AI-generated descriptions get closed.
- Never put server addresses, passwords, pairing codes or private sessions in issues,
  screenshots, logs or test fixtures.

## Build and run

Requirements: JDK 21, the Android SDK (compile SDK 37), and an OpenCode 2.x server.

```bash
cd android
./gradlew :app:installDebug          # debug build: io.github.beppe2880.opencodemobile.debug, "OpenCode Mobile Dev"
./gradlew :app:installDebug -PfakeData=true   # sample data, no server needed
```

On an emulator the host's server is at `http://10.0.2.2:4096` and shows up under "Found on
this network"; alternatively `adb reverse tcp:4096 tcp:4096` makes it reachable at
`http://localhost:4096`. Pair with `opencode pair`.

Before opening a pull request, run what CI runs:

```bash
(cd android && ./gradlew testDebugUnitTest :app:lintDebug :app:assembleDebug)
(cd tools/api-gen && bun install && bun run check)   # only if you touched the API or the generator
```

## Code conventions

The architecture and rules are documented in [`AGENTS.md`](AGENTS.md) and
[`android/AGENTS.md`](android/AGENTS.md) (they also guide AI coding agents). In short:

- **Modules**: `app -> feature -> core:{ui,mvi,domain}`; `app -> core:data -> core:network ->
  core:api`. Features never depend on `core:data`, `core:network`, `core:api` or each other.
- **MVI**: each screen has immutable `State`, `Event` and one-shot `Effect` types, a
  `BaseViewModel`, a `Route` that wires Koin and a stateless, previewable `Screen`.
- **Design system**: use `Oc*` components and `OcTheme` tokens, no hard-coded colors
  ([`docs/design-system.md`](docs/design-system.md)). Every interactive control has a
  `testTag`.
- **Strings**: in resources, English in `values`. Keep all supported translations in sync
  (`es`, `fr`, `de`, `it`, `pt-BR`, `ru`, `zh-Hans`, `ja`, `ko`, `ar`), preserving format
  arguments and locale-specific plurals. Run `bun test tools/locales.test.ts` from the root.
- **Server contract**: `android/core/api` is generated from `api/openapi.json`; never edit it
  and never hand-write DTOs. Run `bun run generate` in `tools/api-gen` after changing
  the spec (Bun and Java are needed; `bun run refresh` pulls the spec from a local server).
- **Tests**: ViewModels and repositories get unit tests next to their module; fixtures are
  shared in `core:testing` and contain only neutral data.

## Commits and pull request titles

Pull requests are squash-merged, so the **PR title becomes the commit on `main`** and the
release notes. Titles follow [Conventional Commits](https://www.conventionalcommits.org) with
a capitalized subject:

```
type(scope): Subject starting with a capital letter
```

| Types | Use for |
|---|---|
| `feat` | A new feature |
| `fix` | A bug fix |
| `perf` | A performance improvement |
| `refactor` | Code changes that neither fix a bug nor add a feature |
| `docs` | Documentation only |
| `test` | Tests only |
| `build`, `ci` | Gradle, dependencies, scripts, workflows |
| `style`, `chore`, `revert` | Formatting, maintenance, reverts |

Optional scopes: `chat`, `sessions`, `connect`, `settings`, `ui`, `data`, `network`, `api`,
`deps`, `release`. Examples: `feat(chat): Group thinking and tool rows`,
`fix(connect): Validate the server URL`. Add `!` after the type for breaking changes.

## Pull requests

- Fill in the template: what changed, how you verified it, screenshots for UI changes
  (light and dark theme).
- Keep each pull request to one change; no unrelated edits.
- CI must pass: unit tests, lint, debug build and the generated API check.
- [CodeRabbit](https://coderabbit.ai) reviews every pull request automatically. Its comments
  are suggestions: reply or resolve them, a maintainer makes the final call.

## Releases

Releases are automated with [release-please](https://github.com/googleapis/release-please):

1. After every merge to `main`, a pull request titled `chore(release): Release x.y.z` is
   opened or updated. It bumps `version.txt` and adds the new entries to `CHANGELOG.md`.
   `feat` bumps the minor version, `fix` and `perf` the patch version, and `!` the major
   version (minor while the version is below 1.0).
2. When a maintainer merges it, the release is tagged, published on GitHub Releases, and the
   signed APK, its R8 mapping file and SHA-256 checksums are attached.

Only `feat`, `fix`, `perf`, `revert` and `docs` appear in the changelog. The app's version
name and code come from `version.txt` (1.2.3 becomes version code 1002003).

## Repository automation

| Workflow | What it does |
|---|---|
| `ci.yml` | Unit tests, lint, debug build, generated API check |
| `pr-title.yml` | Conventional Commit PR titles |
| `pr-standards.yml`, `issue-standards.yml` | Label and comment on PRs and issues that skip the templates |
| `compliance-close.yml` | Closes them after 72 hours |
| `stale.yml` | Marks and closes inactive issues and PRs |
| `release.yml` | Release pull request, tag, GitHub Release and signed APK |
| `labels.yml` | Syncs labels from `.github/labels.yml` |

Dependabot (`.github/dependabot.yml`) updates Gradle dependencies and actions.
