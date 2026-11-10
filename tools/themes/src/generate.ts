// Generates android/core/ui/.../theme/generated/OpenCodeThemes.kt from the OpenCode TUI themes.
//
// Usage (from tools/themes):
//   bun run generate   resolve the cached theme files in assets/ and write the Kotlin file
//   bun run refresh    download the theme files and OpenCode's license at the pinned tag, then generate
//   bun run check      fail when the committed Kotlin file differs from a fresh generation
//
// The version is pinned once, as the @opencode/theme dependency in package.json; the theme files
// come from the same OpenCode tag. To update: `bun add @opencode/theme@<version>` (write the exact
// version), then `bun run refresh`.
import { join } from "node:path"
import { ASSETS, KOTLIN_FILE, LICENSE_URL, THEME_IDS, VERSION, assetPath, assetUrl, renderKotlin, resolveAll } from "./themes"

const mode = process.argv[2] ?? "generate"
if (!["generate", "refresh", "check"].includes(mode)) {
  console.error(`Unknown mode "${mode}": use generate, refresh or check`)
  process.exit(1)
}

await checkInstalledVersion()
if (mode === "refresh") await refresh()
const kotlin = renderKotlin(await resolveAll())

if (mode === "check") {
  const file = Bun.file(KOTLIN_FILE)
  const committed = (await file.exists()) ? await file.text() : ""
  if (committed !== kotlin) {
    console.error("OpenCodeThemes.kt differs from the themes; run `bun run generate` in tools/themes")
    process.exit(1)
  }
  console.log(`OpenCode themes are up to date (${THEME_IDS.length} themes, OpenCode ${VERSION}).`)
} else {
  await Bun.write(KOTLIN_FILE, kotlin)
  console.log(`Generated ${THEME_IDS.length} themes of OpenCode ${VERSION} in android/core/ui`)
}

/** The installed package must be the pinned one: its resolver must match the theme files. */
async function checkInstalledVersion() {
  const installed = Bun.file(join(import.meta.dir, "../node_modules/@opencode/theme/package.json"))
  if (!(await installed.exists())) throw new Error("@opencode/theme is not installed: run `bun install`")
  const version = (await installed.json()).version
  if (version !== VERSION) throw new Error(`@opencode/theme ${version} is installed, ${VERSION} is pinned: run \`bun install\``)
}

async function refresh() {
  console.log(`Downloading the OpenCode ${VERSION} themes...`)
  for (const id of THEME_IDS) await Bun.write(join(ASSETS, assetPath(id)), await download(assetUrl(id)))
  await Bun.write(join(ASSETS, "LICENSE"), await download(LICENSE_URL))
}

async function download(url: string): Promise<string> {
  const response = await fetch(url)
  if (!response.ok) throw new Error(`Download failed (${response.status}): ${url}`)
  return response.text()
}
