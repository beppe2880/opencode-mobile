# OpenCode Mobile design system

The app mirrors the OpenCode V2 web UI: dense but calm, neutral greys, hairline borders,
small radii, accent used sparingly. Everything lives in `android/core/ui`
(`io.github.beppe2880.opencodemobile.core.ui`). Feature code uses only these tokens and `Oc*` components;
no hard-coded colors outside `theme/OcColors.kt`.

## Theme entry points

| API | Purpose |
|---|---|
| `OcTheme(darkTheme) { }` | Provides `LocalOcColors` and a Material 3 `ColorScheme`, typography and shapes mapped from the tokens. |
| `ThemeMode.isDark()` | Resolves `AppSettings.theme` (System / Light / Dark). `MainActivity` also updates system bar icon appearance. |
| `OcTheme.colors` | Full token set (`OcColors`), including state and agent colors. |
| `OcTheme.mono` | IBM Plex Mono styles: `code`, `codeSmall`, `label`. |
| `OcSpacing`, `OcRadius` | Spacing and corner scales. |

Material components keep working because the token set is mapped onto `MaterialTheme.colorScheme`
(`primary = bg-accent`, `surface = bg-base`, `background = bg-deep`, `outlineVariant = border-base`,
`error = danger.fg`, ...). Prefer `OcTheme.colors.*` in app code for anything beyond Material defaults.

## Color tokens

| Token | Light | Dark | Use |
|---|---|---|---|
| `bgDeep` | `#FAFAFA` | `#080808` | Window / page background |
| `bgBase` | `#FFFFFF` | `#161616` | Surfaces, top bars, cards, composer |
| `layer01..04` | `#FAFAFA` `#F2F2F2` `#EEEEEE` `#DBDBDB` | `#242424` `#2E2E2E` `#3A3A3A` `#5C5C5C` | Nested surfaces, code blocks, search field, skeletons |
| `bgInverse` / `textInverse` | `#161616` / `#FAFAFA` | `#FFFFFF` / `#161616` | High-contrast buttons (Scan QR, send, Allow once, FAB) |
| `bgAccent` / `onAccent` | `#3B5CF6` / white | `#3B5CF6` / white | Primary actions, switches, focus caret |
| `textBase` / `textMuted` / `textFaint` | `#161616` `#5C5C5C` `#808080` | `#FAFAFA` `#AEAEAE` `#808080` | Text hierarchy |
| `textAccent` | `#3B5CF6` | `#A2BCFF` | Links, "Add server", show more |
| `textCodePath` / `bgCodePath` | `#2C47C8` / `#E2F7FB` | `#C3D4FD` / `#00353F` | Paths, commands, permission resources |
| `borderBase` / `borderMuted` / `borderStrong` | `#0000001A` `#00000014` `#00000033` | `#FFFFFF1A` `#FFFFFF14` `#FFFFFF33` | Hairlines, dividers, outlined controls |
| `borderFocus` | `#7698FD` | `#7698FD` | Focused text fields |
| `iconBase` / `iconMuted` | `#3A3A3A` / `#808080` | `#DBDBDB` / `#808080` | Icons |
| `userBubbleBg` / `userBubbleText` | `#ECF1FE` / `#2C47C8` | `#021945` / `#C3D4FD` | User prompts (info palette, like the web UI) |
| `success` fg/bg/border | `#198B43` `#E7F9EA` `#B8E9C1` | `#6BD586` `#14361D` `#1D783C` | Connected, completed tools, Active badge |
| `warning` fg/bg/border | `#8F6E1E`* `#FEFAEC` `#F7E5B5` | `#F2CF76` `#4B4025` `#AC8833` | Interrupted, reconnecting, permission card |
| `danger` fg/bg/border | `#B82D35` `#FCECEB` `#F2BBB7` | `#F17471` `#461516` `#97252B` | Errors, failed tools, destructive actions |
| `info` fg/bg/border | `#3250DF` `#ECF1FE` `#C3D4FD` | `#7698FD` `#021945` `#2C47C8` | Running dot, informational states |

\* The web token for light warning text is `#CB9F34`; it fails WCAG AA on `#FEFAEC`
(about 2.3:1), so the app darkens it to `#8F6E1E` (about 4.6:1). Background and border are unchanged.

### Agent colors

`OcTheme.colors.agents.forAgent(name)` maps known agents to the web palette and any other agent to
a stable palette slot.

| Agent | Light | Dark |
|---|---|---|
| build | `#3250DF` | `#C3D4FD` |
| plan | `#C83D8B` | `#F799C6` |
| explore | `#AC8833` | `#F7E5B5` |
| review | `#198B43` | `#B8E9C1` |
| writer | `#623BE2` | `#9E99F7` |

## Typography

- UI: **Inter** (regular 400, medium 500, semibold 600), bundled in `core/ui/res/font`.
- Code, tool names, paths, URLs: **IBM Plex Mono** via `OcTheme.mono`.
- Body is 14sp (`bodyMedium`) to match the web density; `bodySmall` 12sp for metadata,
  `labelSmall` 11sp for footers and badges, `titleSmall` 14sp/medium for top bars and section titles.
- All sizes are `sp`; layouts were checked at font scale 1.3.

## Spacing and shape

| `OcSpacing` | `xxs` 2 | `xs` 4 | `sm` 8 | `md` 12 | `lg` 16 | `xl` 24 | `xxl` 32 | `xxxl` 48 | `gutter` 16 | `contentMaxWidth` 760 |
|---|---|---|---|---|---|---|---|---|---|---|

| `OcRadius` | `xs` 4 | `sm` 6 | `md` 8 | `lg` 10 | `xl` 14 | `sheet` 16 |
|---|---|---|---|---|---|---|

Cards use `lg` radius with a 1dp `borderBase` hairline and no elevation; only the composer and FAB
have a subtle 2dp shadow. Touch targets are at least 48dp (icon buttons, rows) even when the visual
control is smaller (chips 32dp, round send button 36dp inside a 48dp target).

## Components (`core/ui/components`)

| Component | Notes |
|---|---|
| `OcButton(variant = Primary/Secondary/Ghost/Danger/Inverse, loading, leadingIcon, compact)` | 44dp (36dp compact), `md` radius, inline spinner while loading. |
| `OcIconButton`, `OcRoundIconButton` | 48dp targets; round variant for send/stop/jump-to-latest. |
| `OcTextField`, `OcPasswordField`, `OcSearchField` | Outlined, token colors, `mono` option for URLs/codes/paths; error text replaces supporting text. |
| `OcCard`, `OcDivider`, `OcSectionHeader`, `OcListRow`, `OcSwitchRow` | Hairline surfaces and settings-style rows. |
| `OcChip`, `OcSegmentedControl`, `OcAgentChips` | Filters, selectors (agent dot color), theme / auth method switchers (`Role.Tab`). |
| `OcBadge` | Small state pill (Failed, Interrupted, Active). |
| `OcProjectAvatar` | Rounded-square initial, like the web session list. |
| `OcPulsingDot`, `OcStatusDot`, `OcSpinner`, `OcSkeleton` | Running / connection indicators and loading placeholders. |
| `OcEmptyState`, `OcErrorState` | Centered icon + title + message + action (retry). |
| `OcConfirmDialog`, `OcTextInputDialog` | Destructive confirmations and rename flows. |
| `OcBottomSheet` | Modal sheet with token colors, starts below the status bar. |
| `OcModelPicker` | Searchable model list grouped by provider with a "Recently used" section. |
| `OcTopBar` | 56dp bar with optional back button, title accessory (status dot), subtitle or custom subtitle content, and actions; owns the status bar inset. |
| `OcLogo` | Canvas-drawn OpenCode-style mark. |
| `OcMarkdown` + `MarkdownCache` | mikepenz renderer with highlighted code fences (language header + copy button); parse cache keyed by item so only the streaming part re-parses. |
| `Modifier.exposeTestTags()` | Applies `testTagsAsResourceId` inside popups, dialogs and sheets (separate composition roots). |

Text helpers live in `core/ui/text`: `UiText` (localizable text for State), `Throwable.toUiText()`
(one message per `OpenCodeError` type), `formatDuration`, `formatCost`, `formatTokens`, `relativeTime`.

## Screen patterns

- **Connect**: hero, "Found on this network" card (LAN scan of port 4096, spinner then a
  refresh button), inverse "Scan QR code" + outlined "Paste link", pairing hint card with
  `opencode pair`, manual form (URL, Pairing code | Password, Test connection, Connect),
  saved servers. Camera permission has a rationale screen and a settings fallback.
- **Sessions**: top bar with server name and connection dot, project filter chip, search field,
  Today / Yesterday / This week / Older groups, avatar + title + directory + relative time,
  pulsing dot while running, Failed / Interrupted badges, swipe-to-delete, long-press actions sheet,
  inverse "New session" FAB, skeleton / empty / error states.
- **Chat**: user prompts as accent-tinted bubbles on the right; assistant markdown full width;
  "Thought for Xs" disclosure; "Used N Read, Shell" tool group expanding to per-tool rows (icon,
  mono name, primary argument, status) with truncated output; notices between hairlines; turn footer
  "Build · Model · 3s · $0.002" with copy; "Working…" row while running; permission card pinned above
  the composer; composer card with agent chip, model chip, send / stop and Steer / Queue choice
  while the agent runs.
- **Settings**: List or Preview (segmented control at the top, remembered), "Customize in the
  demo" (Android) and a search field for every category and option. Both styles share the
  categories of `SettingsArea` (Servers, Appearance, Chat, Live activity, Message box, Voice
  input, Sessions, Sharing on Android, Notifications where supported, Terminal, Updates, About and
  reset); `SettingsCategories.kt` lists each option in exactly one of them. List: one row per
  category with its icon and current values in one line, opening a page of grouped options where
  dependent ones are indented under their parent and hidden until it allows them; a search result
  opens its page scrolled to the option (or the parent that brings it back), briefly highlighted.
  From 600dp the categories stay on the left with the open one on the right. Preview: cards of the
  categories that have a preview, with a live miniature (`OcThumbnail`), then servers, usage,
  reset and versions.
- **Setting group**: a live preview drawn by the real components of each screen from sample data
  (`ChatPreview`, `SessionsPreview`, `TerminalPreview`, ... handed to Settings by `:shared`), every
  part wrapped in `OcCustomizable` (dashed outline, pencil on its corner outside the content, solid
  when selected; marks can be hidden and stay hidden), and the options of each part below. On
  phones in portrait the preview takes about 40% of the height and shrinks to a strip while the
  options scroll; tapping a part scrolls to its options, scrolling the options selects the part
  being read and scrolls the preview to it. Wide or landscape windows put them side by side.
  On narrow windows a divider between preview and options can be dragged or set to Hide, Small,
  Half or Large, and stays where it was left. `SettingsCoverageTest` fails when an `AppSettings`
  property has no option.
- **Live demo edit mode**: the offline demo on the user's real settings. Real composables mark
  their parts with `Modifier.editable(part, LocalEditMode.current)` (no-op outside it); a bottom
  bar switches Edit and Use and has Done; a tapped part's options dock above the bar, pushing
  the screen up so the part scrolls into view above them.
- **Adaptive**: at >= 840dp width Sessions and Chat are shown side by side; single-pane content
  is limited to 760dp and centered.

## Accessibility and testing

- Every interactive control has a stable `testTag` (`connect_scan_button`, `sessions_new_button`,
  `session_row_<id>`, `chat_composer_field`, `chat_send_button`, `chat_stop_button`,
  `chat_agent_selector`, `chat_model_selector`, `permission_allow_once_button`, `settings_theme_dark`, ...);
  the activity root and every popup expose them as resource IDs.
- Headings use `semantics { heading() }`; running rows expose a state description; session rows
  offer rename/delete as custom accessibility actions; probe results and permission cards are
  polite live regions.
- Strings are in `res/values` (English) and `res/values-it` (Italian) per module.

Screenshots of the app (taken with sample data) are in `docs/screenshots/`.
