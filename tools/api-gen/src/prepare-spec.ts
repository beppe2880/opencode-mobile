// Normalizes the augmented OpenAPI document for openapi-generator's Kotlin client.
//
// Each step works around a specific generator limitation; keep them minimal and
// re-check them when upgrading openapi-generator.

/** API areas used by the mobile app. Other operations and unreachable schemas are dropped. */
const KEEP_TAGS = new Set([
  "server", "location", "agent", "session", "model", "project", "form", "permission",
  "filesystem", "command", "skill", "event", "shell", "worktree", "vcs", "pty", "reference",
  "provider", "mcp", "config",
])

/** Config operations used by the app; the rest of the `config` area stays out. */
const KEEP_CONFIG_PATHS = new Set(["/api/config"])

const refName = (ref: string) => ref.split("/").pop()!

function walk(node: any, visit: (node: any) => void) {
  if (!node || typeof node !== "object") return
  if (Array.isArray(node)) return node.forEach((child) => walk(child, visit))
  for (const child of Object.values(node)) walk(child, visit)
  visit(node)
}

/** Rewrites [spec] in place and returns a one-line summary. */
export function prepareSpec(spec: any): string {
  const components = spec.components.schemas
  const resolve = (schema: any): any => (schema?.$ref ? resolve(components[refName(schema.$ref)]) : schema)

  // 0. The app reads only the global `model` of each config document (new chats start with it,
  //    like OpenCode Desktop). The full config schema would generate dozens of classes and change
  //    shared ones, so `GET /api/config` is narrowed to `{ type, info: { model } }`; `model` is a
  //    "provider/model" string or a `{ providerID, model, variant }` object, kept as raw JSON.
  for (const [path, item] of Object.entries<any>(spec.paths)) {
    if (KEEP_CONFIG_PATHS.has(path)) continue
    for (const method of Object.keys(item)) if (item[method].tags?.includes("config")) delete item[method]
    if (Object.keys(item).length === 0) delete spec.paths[path]
  }
  const configGet = spec.paths["/api/config"]?.get
  if (configGet) {
    components["ConfigModelEntry"] = {
      type: "object",
      properties: {
        type: { type: "string" },
        info: { $ref: "#/components/schemas/ConfigModelInfo" },
      },
      required: ["type"],
    }
    components["ConfigModelInfo"] = { type: "object", properties: { model: {} } }
    configGet.responses["200"].content["application/json"].schema = {
      type: "array",
      items: { $ref: "#/components/schemas/ConfigModelEntry" },
    }
  }

  // 1. anyOf unions whose members share a constant property become oneOf + discriminator,
  //    so the generator emits a sealed interface instead of one flattened class.
  function constValues(schema: any, key: string): string[] | null {
    const resolved = resolve(schema)
    const members = resolved?.anyOf ?? resolved?.oneOf
    if (members) {
      const values: string[] = []
      for (const member of members) {
        if (member.type === "null") continue
        const memberValues = constValues(member, key)
        if (!memberValues) return null
        values.push(...memberValues)
      }
      return values
    }
    const property = resolve(resolved?.properties?.[key])
    return property?.enum?.length ? property.enum : null
  }

  let discriminated = 0
  walk(spec, (node) => {
    if (!node.anyOf) return
    const members = node.anyOf.filter((m: any) => m.type !== "null")
    const nullable = members.length !== node.anyOf.length
    if (members.length < 2 || !members.every((m: any) => m.$ref)) return
    for (const key of ["type", "status", "_tag", "kind"]) {
      const sets = members.map((m: any) => constValues(m, key))
      if (sets.some((s: string[] | null) => !s)) continue
      const flat = sets.flat()
      if (new Set(flat).size !== flat.length) continue
      const mapping: Record<string, string> = {}
      members.forEach((m: any, i: number) => sets[i].forEach((value: string) => (mapping[value] = m.$ref)))
      const union = { oneOf: members, discriminator: { propertyName: key, mapping } }
      delete node.anyOf
      if (nullable) node.anyOf = [union, { type: "null" }]
      else Object.assign(node, union)
      discriminated++
      return
    }
  })

  // 2. Effect marks structs as open (additionalProperties: true); the generator turns that
  //    into HashMap subclasses.
  walk(spec, (node) => {
    if (node.properties && node.additionalProperties === true) delete node.additionalProperties
  })

  // 3. The generator cannot serialize unions of mixed primitives (number | false); keep them
  //    as raw JSON. Nested unions are converted first (`walk` is post-order), so converted
  //    members are marked and count as primitive too: otherwise Form.Value would silently
  //    lose its `number` member.
  const RAW = "x-raw-json"
  const flatten = (m: any): any[] => (m?.anyOf ? m.anyOf.flatMap(flatten) : [m])
  const scalar = (t: string) => ["number", "integer", "boolean", "string"].includes(t)
  walk(spec, (node) => {
    if (!node.anyOf) return
    const all = flatten(node)
    const members = all.filter((m: any) => m.type !== "null")
    const types = members.map((m: any) => resolve(m)?.type)
    const primitive = members.every((m: any) => {
      const r = resolve(m)
      return r?.[RAW] || scalar(r?.type) || (r?.type === "array" && scalar(resolve(r.items)?.type))
    })
    if (members.length > 1 && primitive && !types.every((t: string) => t === "string")) {
      node[RAW] = true
      delete node.anyOf
      if (members.length !== all.length) node.nullable = true
    }
  })

  // 4. The generated client cannot encode deepObject query objects; expose `location[directory]` as a
  //    scalar parameter.
  for (const item of Object.values<any>(spec.paths)) {
    for (const operation of Object.values<any>(item)) {
      if (!Array.isArray(operation.parameters)) continue
      operation.parameters = operation.parameters.flatMap((parameter: any) => {
        if (parameter.style !== "deepObject") return [parameter]
        const object = (parameter.schema?.anyOf ?? [parameter.schema]).find((s: any) => s?.type === "object")
        return Object.entries<any>(object?.properties ?? {}).map(([key, schema]) => ({
          name: `${parameter.name}[${key}]`,
          in: "query",
          required: false,
          schema: (schema.anyOf ?? [schema]).find((s: any) => s.type !== "null") ?? { type: "string" },
        }))
      })
    }
  }

  // 5. Keep only the operations the app uses and the schemas they reach.
  for (const [path, item] of Object.entries<any>(spec.paths)) {
    for (const method of Object.keys(item)) {
      if (!item[method].tags?.some((tag: string) => KEEP_TAGS.has(tag))) delete item[method]
    }
    if (Object.keys(item).length === 0) delete spec.paths[path]
  }
  const reachable = new Set<string>()
  const mark = (node: any) => {
    if (!node || typeof node !== "object") return
    if (Array.isArray(node)) return node.forEach(mark)
    if (typeof node.$ref === "string") {
      const name = refName(node.$ref)
      if (!reachable.has(name)) {
        reachable.add(name)
        mark(components[name])
      }
    }
    for (const child of Object.values(node)) mark(child)
  }
  mark(spec.paths)
  mark({ $ref: "#/components/schemas/OpenCodeEvent" })
  for (const name of Object.keys(components)) if (!reachable.has(name)) delete components[name]

  return `${discriminated} unions, ${Object.keys(spec.paths).length} paths, ${Object.keys(components).length} schemas`
}
