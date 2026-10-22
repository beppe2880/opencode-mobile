import { afterAll, expect, test } from "bun:test"
import { resolve } from "node:path"

const root = resolve(import.meta.dir, "..")
const locales = ["it", "es", "fr", "de", "pt-rBR", "ru", "b+zh+Hans", "ja", "ko", "ar"]
const modules = ["app", "core/ui", "feature/connect", "feature/chat", "feature/sessions", "feature/settings", "feature/terminal", "feature/files"]

type Resource = { type: string; body: string; translatable: boolean }

// These resource files contain only strings and plurals, not styled markup.
function resources(xml: string): Map<string, Resource> {
  const entries = [...xml.matchAll(/<(string|plurals)\s+name="([^"]+)"([^>]*)>([\s\S]*?)<\/\1>/g)]
  const map = new Map<string, Resource>()
  for (const [, type, name, attrs, body] of entries) {
    if (map.has(name)) throw new Error(`Duplicate resource: ${name}`)
    map.set(name, { type, body, translatable: !attrs.includes('translatable="false"') })
  }
  return map
}

function placeholders(text: string): string[] {
  return [...text.matchAll(/%(?:\d+\$)?[ds%]/g)].map(match => match[0]).sort()
}

// English and Italian are written with every change; the other languages are translated in
// one batch before a release. STRICT_LOCALES=1 (set on release pull requests) requires all.
const alwaysTranslated = ["it"]
const strict = process.env.STRICT_LOCALES === "1"
const pending: string[] = []

afterAll(() => {
  if (pending.length === 0) return
  console.warn(`${pending.length} strings to translate before the next release:\n  ${pending.join("\n  ")}`)
})

for (const module of modules) {
  const res = `${root}/android/${module}/src/main/res`
  const source = resources(await Bun.file(`${res}/values/strings.xml`).text())
  for (const locale of locales) {
    test(`${module}: ${locale} coverage, format arguments and plural forms`, async () => {
      const translated = resources(await Bun.file(`${res}/values-${locale}/strings.xml`).text())
      for (const [name, original] of source) {
        if (!original.translatable) continue
        const value = translated.get(name)
        if (value === undefined && !strict && !alwaysTranslated.includes(locale)) {
          pending.push(`${module} ${locale} ${name}`)
          continue
        }
        expect(value, `Missing ${name}`).toBeDefined()
        expect(value!.type).toBe(original.type)
        if (original.type === "string") {
          expect(value!.body.trim().length, name).toBeGreaterThan(0)
          expect(placeholders(value!.body), name).toEqual(placeholders(original.body))
        } else {
          const items = [...value!.body.matchAll(/<item quantity="([^"]+)">([^<]*)<\/item>/g)]
          const sourceOther = original.body.match(/<item quantity="other">([^<]*)<\/item>/)![1]
          const required = locale === "ar" ? ["zero", "one", "two", "few", "many", "other"]
            : locale === "ru" ? ["one", "few", "many", "other"]
            : ["b+zh+Hans", "ja", "ko"].includes(locale) ? ["other"] : ["one", "other"]
          for (const quantity of required) expect(items.some(item => item[1] === quantity), `${name}.${quantity}`).toBe(true)
          for (const [, quantity, text] of items) {
            expect(text.trim().length, `${name}.${quantity}`).toBeGreaterThan(0)
            expect(placeholders(text), `${name}.${quantity}`).toEqual(placeholders(sourceOther))
          }
        }
      }
      for (const name of translated.keys()) expect(source.has(name), `Unknown resource ${name}`).toBe(true)
    })
  }
}
