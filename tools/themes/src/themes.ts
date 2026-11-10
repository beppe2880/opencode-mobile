// Resolves the OpenCode TUI themes with OpenCode's own theme package and renders them as Kotlin.
//
// The theme files are the TUI's built-in ones (packages/tui/src/theme/assets at the tag of the pinned
// @opencode/theme version), cached in ../assets so that generating needs no network. The TUI loads
// a V1 file with `migrateV1` and the V2 default theme with `parseThemeDocument`, then resolves the
// requested mode with `resolveThemeDocument`, falling back to the only mode a theme has
// (packages/tui/src/context/theme.tsx, `loadTheme`). The same steps run here.
import { migrateV1, parseThemeDocument, resolveThemeDocument, themeModes } from "@opencode/theme/tui"
import { join, resolve } from "node:path"
import pkg from "../package.json" with { type: "json" }

/** The pinned OpenCode version: @opencode/theme and the theme files come from the same release. */
export const VERSION: string = pkg.dependencies["@opencode/theme"]

export const ROOT = resolve(import.meta.dir, "../../..")
export const ASSETS = resolve(import.meta.dir, "../assets")
export const KOTLIN_FILE = join(
  ROOT,
  "android/core/ui/src/commonMain/kotlin/io/github/beppe2880/opencodemobile/core/ui/theme/generated/OpenCodeThemes.kt",
)

/** The default theme, a V2 document; listed first like in the TUI. */
export const DEFAULT_THEME = "opencode"

/**
 * The TUI's V1 themes (`DEFAULT_THEMES` in packages/tui/src/theme/v1.ts). The V1 `opencode.json`
 * is shadowed there by the V2 default theme, and the experimental `opencode2` is not listed.
 */
export const V1_THEMES = [
  "aura", "ayu", "carbonfox", "catppuccin", "catppuccin-frappe", "catppuccin-macchiato", "cobalt2",
  "cursor", "dracula", "everforest", "flexoki", "github", "gruvbox", "kanagawa", "lucent-orng",
  "material", "matrix", "mercury", "monokai", "nightowl", "nord", "one-dark", "orng", "osaka-jade",
  "palenight", "rosepine", "solarized", "synthwave84", "tokyonight", "vercel", "vesper", "zenburn",
] as const

export const THEME_IDS: readonly string[] = [DEFAULT_THEME, ...[...V1_THEMES].sort()]

/** Path of a theme file below packages/tui/src/theme/assets (and below ../assets). */
export function assetPath(id: string): string {
  return id === DEFAULT_THEME ? "v2/opencode.json" : `${id}.json`
}

export function assetUrl(id: string): string {
  return `https://raw.githubusercontent.com/anomalyco/opencode/v${VERSION}/packages/tui/src/theme/assets/${assetPath(id)}`
}

export const LICENSE_URL = `https://raw.githubusercontent.com/anomalyco/opencode/v${VERSION}/LICENSE`

/** ARGB hex, for example `FFF5A742`. */
export type Argb = string

/** The flat token set of one mode; field names are the Kotlin `OpenCodeThemeTokens` properties. */
export type Tokens = Record<string, Argb>

export type ResolvedMode = { tokens: Tokens; isDark: boolean; categorical: Argb[] }
export type ThemeModel = { id: string; displayName: string; light: ResolvedMode; dark: ResolvedMode; modes: string[] }

type Rgba = { r: number; g: number; b: number; a: number }
type Scale = Record<number, Rgba>
// The resolved theme of @opencode/theme/tui (types.ts, `ResolvedTheme`), read structurally.
type Resolved = {
  hue: Record<string, Scale | undefined>
  categorical: Scale[]
  text: {
    base: Rgba
    muted: Rgba
    faint?: Rgba
    action: Record<string, Record<string, Rgba>>
    feedback: Record<"error" | "warning" | "success" | "info", { base: Rgba }>
  }
  background: { base: Rgba; raised: { base: Rgba; high: Rgba; max: Rgba }; action: Record<string, Record<string, Rgba>> }
  border: { base: Rgba }
  scrollbar: { base: Rgba }
  diff: {
    text: { added: Rgba; removed: Rgba; context: Rgba; hunkHeader: Rgba }
    background: { added: Rgba; removed: Rgba; context: Rgba }
    highlight: { added: Rgba; removed: Rgba }
    lineNumber: { text: Rgba; background: { added: Rgba; removed: Rgba } }
  }
  syntax: Record<string, Rgba>
  markdown: Record<string, Rgba>
}

const SYNTAX = ["comment", "keyword", "function", "variable", "string", "number", "type", "operator", "punctuation"] as const
const MARKDOWN = [
  "text", "heading", "link", "linkText", "code", "blockQuote", "emphasis", "strong", "horizontalRule",
  "listItem", "listEnumeration", "image", "imageText", "codeBlock",
] as const
const STEPS = [100, 200, 300, 400, 500, 600, 700, 800, 900] as const

function channel(value: number): number {
  return Math.max(0, Math.min(255, Math.round(value * 255)))
}

export function argb(color: Rgba): Argb {
  return [color.a, color.r, color.g, color.b].map((value) => channel(value).toString(16).toUpperCase().padStart(2, "0")).join("")
}

/** `detectMode` of OpenCode's V1 migration: dark when the text is brighter than the background. */
function luminance(color: Rgba): number {
  return 0.299 * color.r + 0.587 * color.g + 0.114 * color.b
}

function capitalize(name: string): string {
  return name.charAt(0).toUpperCase() + name.slice(1)
}

/**
 * One mode of a resolved theme as flat tokens. Chromatic hues feed the terminal colors; a theme
 * without one falls back to the matching feedback, interactive or accent color (documented in
 * docs/design-system.md).
 */
export function flatten(theme: Resolved): ResolvedMode {
  const t: Record<string, Argb> = {}
  const set = (name: string, color: Rgba | undefined) => {
    if (!color) throw new Error(`Missing color for ${name}`)
    t[name] = argb(color)
  }
  set("background", theme.background.base)
  set("raised", theme.background.raised.base)
  set("raisedHigh", theme.background.raised.high)
  set("raisedMax", theme.background.raised.max)
  set("text", theme.text.base)
  set("textMuted", theme.text.muted)
  set("textFaint", theme.text.faint ?? theme.text.muted)
  set("primary", theme.hue.interactive?.[200])
  set("accent", theme.hue.accent?.[200])
  set("actionPrimaryFocusedBackground", theme.background.action.primary?.focused)
  set("actionPrimaryFocusedText", theme.text.action.primary?.focused)
  set("border", theme.border.base)
  set("scrollbar", theme.scrollbar.base)
  set("error", theme.text.feedback.error.base)
  set("warning", theme.text.feedback.warning.base)
  set("success", theme.text.feedback.success.base)
  set("info", theme.text.feedback.info.base)
  for (const step of STEPS) set(`neutral${step}`, theme.hue.neutral?.[step])
  const hue = (names: string[], step: number, fallback: Rgba) => {
    for (const name of names) {
      const color = theme.hue[name]?.[step]
      if (color) return color
    }
    return fallback
  }
  const chromatic: Record<string, [string[], Rgba]> = {
    red: [["red"], theme.text.feedback.error.base],
    yellow: [["yellow", "orange"], theme.text.feedback.warning.base],
    green: [["green"], theme.text.feedback.success.base],
    cyan: [["cyan"], theme.text.feedback.info.base],
    blue: [["blue"], theme.hue.interactive![200]!],
    purple: [["purple"], theme.hue.accent![200]!],
  }
  for (const [name, [names, fallback]] of Object.entries(chromatic)) {
    set(name, hue(names, 200, fallback))
    set(`${name}Bright`, hue(names, 100, hue(names, 200, fallback)))
  }
  set("diffAdded", theme.diff.text.added)
  set("diffRemoved", theme.diff.text.removed)
  set("diffContext", theme.diff.text.context)
  set("diffHunkHeader", theme.diff.text.hunkHeader)
  set("diffAddedBackground", theme.diff.background.added)
  set("diffRemovedBackground", theme.diff.background.removed)
  set("diffContextBackground", theme.diff.background.context)
  set("diffHighlightAdded", theme.diff.highlight.added)
  set("diffHighlightRemoved", theme.diff.highlight.removed)
  set("diffLineNumber", theme.diff.lineNumber.text)
  set("diffLineNumberAddedBackground", theme.diff.lineNumber.background.added)
  set("diffLineNumberRemovedBackground", theme.diff.lineNumber.background.removed)
  for (const token of SYNTAX) set(`syntax${capitalize(token)}`, theme.syntax[token])
  for (const token of MARKDOWN) set(`markdown${capitalize(token)}`, theme.markdown[token])
  // Agent colors: like the TUI footer, step 200 of each categorical hue, without repeats.
  const categorical = theme.categorical.map((scale) => argb(scale[200]!)).filter((color, i, all) => all.indexOf(color) === i)
  return { tokens: t, isDark: luminance(theme.text.base) > luminance(theme.background.base), categorical }
}

export async function readAsset(id: string): Promise<unknown> {
  const file = Bun.file(join(ASSETS, assetPath(id)))
  if (!(await file.exists())) throw new Error(`Missing ${assetPath(id)} in tools/themes/assets: run \`bun run refresh\``)
  return file.json()
}

export function resolveTheme(id: string, source: unknown): ThemeModel {
  // V1 files are migrated to a V2 document first; both are then validated like the TUI does.
  const document = parseThemeDocument(
    id === DEFAULT_THEME ? source : migrateV1(source as Parameters<typeof migrateV1>[0]),
    id,
  )
  const modes = [...themeModes(document)] as string[]
  if (modes.length === 0) throw new Error(`Theme ${id} has no mode`)
  const resolveMode = (requested: "light" | "dark") => {
    const mode = modes.includes(requested) ? requested : modes[0]
    return flatten(resolveThemeDocument(document, mode as "light" | "dark") as unknown as Resolved)
  }
  return { id, displayName: id, light: resolveMode("light"), dark: resolveMode("dark"), modes }
}

/** Every theme, the default first then alphabetical, resolved for both modes. */
export async function resolveAll(): Promise<ThemeModel[]> {
  return Promise.all(THEME_IDS.map(async (id) => resolveTheme(id, await readAsset(id))))
}

function functionName(id: string): string {
  return id.replace(/-([a-z0-9])/g, (_, c: string) => c.toUpperCase()) + "Theme"
}

function color(value: Argb): string {
  return `Color(0x${value})`
}

function tokensCall(mode: ResolvedMode, indent: string): string {
  const inner = `${indent}    `
  const lines = Object.entries(mode.tokens).map(([name, value]) => `${inner}${name} = ${color(value)},`)
  lines.push(`${inner}isDark = ${mode.isDark},`)
  lines.push(`${inner}categorical = listOf(`)
  for (const value of mode.categorical) lines.push(`${inner}    ${color(value)},`)
  lines.push(`${inner}),`)
  return `OpenCodeThemeTokens(\n${lines.join("\n")}\n${indent})`
}

/** The Kotlin file with every theme; one function per theme keeps each JVM method small. */
export function renderKotlin(themes: ThemeModel[]): string {
  const out: string[] = []
  out.push("// Generated by tools/themes. Do not edit.")
  out.push(`// The OpenCode TUI themes of OpenCode ${VERSION}, resolved with @opencode/theme ${VERSION}.`)
  out.push("// OpenCode is MIT licensed, Copyright (c) 2025 opencode (docs/licenses/OpenCode-MIT.txt).")
  out.push("package io.github.beppe2880.opencodemobile.core.ui.theme.generated")
  out.push("")
  out.push("import androidx.compose.ui.graphics.Color")
  out.push("import io.github.beppe2880.opencodemobile.core.ui.theme.OpenCodeTheme")
  out.push("import io.github.beppe2880.opencodemobile.core.ui.theme.OpenCodeThemeTokens")
  out.push("")
  out.push("/** The OpenCode version the themes come from. */")
  out.push(`internal const val OPENCODE_THEMES_VERSION = "${VERSION}"`)
  out.push("")
  out.push("/** Every theme: the default one first, then alphabetical by id. */")
  out.push("internal fun generatedOpenCodeThemes(): List<OpenCodeTheme> = listOf(")
  for (const theme of themes) out.push(`    ${functionName(theme.id)}(),`)
  out.push(")")
  for (const theme of themes) {
    out.push("")
    const single = theme.modes.length === 1
    out.push(`private fun ${functionName(theme.id)}(): OpenCodeTheme {`)
    if (single) {
      out.push(`    // Only a ${theme.modes[0]} mode: the TUI uses it for both.`)
      const mode = theme.modes[0] === "light" ? theme.light : theme.dark
      out.push(`    val tokens = ${tokensCall(mode, "    ")}`)
      out.push(`    return OpenCodeTheme(id = "${theme.id}", displayName = "${theme.displayName}", light = tokens, dark = tokens)`)
    } else {
      out.push(`    val light = ${tokensCall(theme.light, "    ")}`)
      out.push(`    val dark = ${tokensCall(theme.dark, "    ")}`)
      out.push(`    return OpenCodeTheme(id = "${theme.id}", displayName = "${theme.displayName}", light = light, dark = dark)`)
    }
    out.push("}")
  }
  return `${out.join("\n")}\n`
}
