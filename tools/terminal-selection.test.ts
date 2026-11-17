import { expect, test } from "bun:test"
import { readFileSync } from "node:fs"
import { runInNewContext } from "node:vm"

const script = readFileSync(new URL("../android/feature/terminal/src/commonMain/composeResources/files/terminal/selection.js", import.meta.url), "utf8")

function fixture() {
  class Element {
    listeners = new Map<string, ((e: any) => void)[]>()
    children: Element[] = []
    style: any = {}
    dataset: any = {}
    attrs: any = {}
    hidden = false
    className = ""
    id = ""
    tabIndex = -1
    appendChild(child: Element) { this.children.push(child) }
    setAttribute(key: string, value: string) { this.attrs[key] = value }
    closest() { return this.className === "oc-selection-handle" ? this : null }
    getBoundingClientRect() { return { left: 0, top: 0, width: 200, height: 200, bottom: 200 } }
    addEventListener(type: string, fn: (e: any) => void) {
      const fns = this.listeners.get(type) ?? []
      fns.push(fn)
      this.listeners.set(type, fns)
    }
    dispatch(type: string, extra: any = {}) {
      const event = {
        type, target: this, touches: [], defaultPrevented: false, stopped: false,
        preventDefault() { this.defaultPrevented = true },
        stopImmediatePropagation() { this.stopped = true }, ...extra,
      }
      for (const fn of this.listeners.get(type) ?? []) { fn(event); if (event.stopped) break }
      return event
    }
  }
  const root = new Element()
  const screen = new Element()
  Object.assign(root, { querySelector: () => screen })
  const rows = Array.from({ length: 50 }, (_, i) => ({
    text: `row${i} alpha beta`, isWrapped: false,
    cells: null as null | { chars: string, width: number }[],
    getCell(x: number) {
      const cell = this.cells?.[x]
      return { getChars: () => cell?.chars ?? this.text[x] ?? "", getWidth: () => cell?.width ?? 1 }
    },
  }))
  const callbacks: Record<string, () => void> = {}
  const buffer = { type: "normal", viewportY: 20, baseY: 40, cursorY: 9, length: 50, getLine: (y: number) => rows[y] }
  const markers = new Set<any>()
  let selection: { x: number, y: number, length: number } | null = null
  let focused = 0
  let blurred = 0
  const term = {
    element: root, cols: 20, rows: 10, options: { theme: { foreground: "#000" }, wordSeparator: " ()[]{}'\"" },
    modes: { mouseTrackingMode: "none" },
    buffer: { active: buffer, onBufferChange: (fn: () => void) => { callbacks.buffer = fn } },
    select(x: number, y: number, length: number) { selection = { x, y, length }; callbacks.selection?.() },
    hasSelection: () => selection !== null,
    focus() { focused++ },
    blur() { blurred++ },
    clearSelection() { selection = null; callbacks.selection?.() },
    getSelection() {
      if (!selection) return ""
      let result = "", { x, y, length } = selection
      while (length > 0 && rows[y]) {
        const take = Math.min(length, this.cols - x)
        const row = rows[y]
        const text = row.cells ? row.cells.slice(x, x + take).map(c => c.chars).join("") : row.text.slice(x, x + take)
        result += text.trimEnd()
        length -= take
        x = 0
        y++
        if (length > 0 && !rows[y]?.isWrapped) result += "\n"
      }
      return result
    },
    scrollLines(amount: number) { buffer.viewportY = Math.max(0, Math.min(buffer.baseY, buffer.viewportY + amount)); callbacks.scroll?.() },
    registerMarker(offset: number) {
      const marker = { line: buffer.baseY + buffer.cursorY + offset, isDisposed: false, dispose() { this.isDisposed = true; markers.delete(this) } }
      markers.add(marker)
      return marker
    },
    onSelectionChange(fn: () => void) { callbacks.selection = fn },
    onScroll(fn: () => void) { callbacks.scroll = fn },
    onResize(fn: () => void) { callbacks.resize = fn },
    onWriteParsed(fn: () => void) { callbacks.write = fn },
  }
  const timers = new Map<number, () => void>(), frames = new Map<number, (time: number) => void>()
  let id = 0
  let clock = 0
  const copied: string[] = [], changes: boolean[] = [], availability: boolean[] = [], picking: boolean[] = []
  const window: any = {}
  runInNewContext(script, {
    window, document: { createElement: () => new Element() },
    Date: { now: () => clock },
    setTimeout(fn: () => void) { timers.set(++id, fn); return id },
    clearTimeout(key: number) { timers.delete(key) },
    requestAnimationFrame(fn: (time: number) => void) { frames.set(++id, fn); return id },
    cancelAnimationFrame(key: number) { frames.delete(key) },
  })
  const controller = window.OcTouchSelection(term, {
    selectionChanged: (active: boolean) => changes.push(active),
    selectionAvailable: (value: boolean) => availability.push(value),
    selectionPicking: (value: boolean) => picking.push(value),
    copy: (text: string) => copied.push(text),
  })
  const touch = (x: number, y: number) => ({ identifier: 1, clientX: x, clientY: y })
  return {
    root, term, rows, buffer, markers, callbacks, controller, copied, changes, availability, picking, frames,
    get selection() { return selection },
    get focused() { return focused },
    get blurred() { return blurred },
    handles: root.children[0].children,
    start(x = 75, y = 10, target: any = screen) { return root.dispatch("touchstart", { target, touches: [touch(x, y)] }) },
    move(x: number, y: number) { return root.dispatch("touchmove", { touches: [touch(x, y)] }) },
    end() { return root.dispatch("touchend") },
    hold() { for (const [key, fn] of [...timers]) { timers.delete(key); fn() } },
    elapsed(ms: number) { clock += ms },
    tick(time = 100) { for (const [key, fn] of [...frames]) { frames.delete(key); fn(time) } },
  }
}

test("long press selects the word without focusing input and blocks compatibility mouse events", () => {
  const f = fixture()
  f.start()
  f.hold()
  expect(f.term.getSelection()).toBe("alpha")
  expect(f.handles.every(h => !h.hidden)).toBe(true)
  expect(f.end().defaultPrevented).toBe(true)
  expect(f.root.dispatch("mousedown").stopped).toBe(true)
  expect(f.root.dispatch("click").stopped).toBe(true)
  f.controller.copy()
  expect(f.copied).toEqual(["alpha"])
})

test("a short tap and movement before the timeout are not selections", () => {
  const f = fixture()
  f.start(); f.end(); f.hold()
  expect(f.selection).toBeNull()
  expect(f.focused).toBe(1)
  expect(f.root.dispatch("click").stopped).toBe(true)
  f.start(); f.move(75, 40); f.hold()
  expect(f.selection).toBeNull()
})

test("selecting needs no scrollback but does not steal mouse-reporting or alternate-buffer long presses", () => {
  const f = fixture()
  f.buffer.baseY = 0
  f.start(); f.hold()
  expect(f.selection).not.toBeNull()
  f.controller.clear()
  f.term.modes.mouseTrackingMode = "vt200"
  f.start(); f.hold()
  expect(f.selection).toBeNull()
  f.term.modes.mouseTrackingMode = "none"
  f.buffer.type = "alternate"
  f.start(); f.hold()
  expect(f.selection).toBeNull()
})

test("dragging a handle extends across real lines and copy comes from the xterm selection", () => {
  const f = fixture()
  f.start(); f.hold(); f.end()
  f.start(110, 25, f.handles[1])
  expect(f.move(150, 55).defaultPrevented).toBe(true)
  f.end()
  f.controller.copy()
  expect(f.copied[0]).toBe("alpha beta\nrow21 alpha beta\nrow22 alpha bet")
})

test("dragged handles can cross without a negative selection length", () => {
  const f = fixture()
  f.start(); f.hold(); f.end()
  f.start(50, 25, f.handles[0]); f.move(180, 55); f.end()
  expect(f.selection!.length).toBeGreaterThan(0)
  expect(f.selection!.x).toBe(11)
})

test("dragging near an edge scrolls and touchend stops its animation", () => {
  const f = fixture()
  f.start(); f.hold(); f.end()
  f.start(110, 25, f.handles[1]); f.move(100, 210)
  f.tick()
  expect(f.buffer.viewportY).toBeGreaterThan(20)
  f.end()
  expect(f.frames.size).toBe(0)
})

test("a swipe outside handles exits custom selection and scrolls in the same gesture", () => {
  const f = fixture()
  f.start(); f.hold(); f.end()
  f.start(100, 100)
  expect(f.move(100, 160).defaultPrevented).toBe(true)
  expect(f.selection).toBeNull()
  expect(f.buffer.viewportY).toBe(17)
  f.move(100, 180)
  expect(f.buffer.viewportY).toBe(16)
  expect(f.end().defaultPrevented).toBe(true)
})

test("tap outside, resize, buffer change and trimmed anchors cancel safely", () => {
  for (const action of ["tap", "resize", "buffer", "trim"]) {
    const f = fixture()
    f.start(); f.hold(); f.end()
    if (action === "tap") { f.start(180, 100); expect(f.end().defaultPrevented).toBe(true) }
    if (action === "resize") f.callbacks.resize()
    if (action === "buffer") f.callbacks.buffer()
    if (action === "trim") { [...f.markers][0].isDisposed = true; f.callbacks.write() }
    expect(f.selection).toBeNull()
    expect(f.handles.every(h => h.hidden)).toBe(true)
    expect(f.markers.size).toBe(0)
  }
})

test("wide and combining characters are selected as whole cells", () => {
  const f = fixture()
  f.rows[20].cells = [{ chars: "界", width: 2 }, { chars: "", width: 0 }, { chars: "é", width: 1 }, { chars: " ", width: 1 }]
  f.start(15, 10); f.hold()
  expect(f.selection).toEqual({ x: 0, y: 20, length: 3 })
  expect(f.term.getSelection()).toBe("界é")
})

test("word selection joins soft wraps but stops at real newlines", () => {
  const f = fixture()
  f.rows[20].text = "long_word_1234567890"
  f.rows[21].text = "continuation end"
  f.rows[21].isWrapped = true
  f.start(100, 30); f.hold()
  expect(f.selection!.y).toBe(20)
  expect(f.term.getSelection()).toBe("long_word_1234567890continuation")
})

test("toolbar select all, explicit entry and localized keyboard-accessible handles", () => {
  const f = fixture()
  f.controller.labels("Start", "End", "Row %1$d, column %2$d")
  f.controller.arm()
  expect(f.selection).toBeNull()
  f.start(25, 10); f.end()
  expect(f.term.getSelection()).toBe("row20")
  expect(f.handles[0].attrs["aria-label"]).toBe("Start")
  expect(f.handles[0].attrs["aria-valuetext"]).toBe("Row 21, column 1")
  f.handles[1].dispatch("keydown", { key: "ArrowRight" })
  expect(f.selection!.length).toBe(6)
  f.controller.all()
  expect(f.selection!.y).toBe(0)
  expect(f.selection!.length).toBe(1000)
})

test("handle touch targets remain inside the viewport at its bottom and side edges", () => {
  const f = fixture()
  f.buffer.viewportY = 40
  f.controller.all()
  const end = f.handles[1]
  expect(end.hidden).toBe(false)
  expect(parseFloat(end.style.left)).toBeLessThanOrEqual(152)
  expect(parseFloat(end.style.top)).toBeLessThanOrEqual(152)
})

test("a native contextmenu is intercepted before it can focus xterm's hidden input", () => {
  const f = fixture()
  f.start()
  expect(f.root.dispatch("contextmenu").stopped).toBe(true)
  expect(f.term.getSelection()).toBe("alpha")
  expect(f.root.dispatch("mousedown", { sourceCapabilities: { firesTouchEvents: false } }).stopped).toBe(false)
})

test("keyboard animation does not cancel a pending long press", () => {
  const f = fixture()
  f.start()
  f.callbacks.resize()
  f.hold()
  expect(f.term.getSelection()).toBe("alpha")
  expect(f.root.dispatch("contextmenu").stopped).toBe(true)
  expect(f.end().defaultPrevented).toBe(true)
})

test("touch-down prevents native focus changes and double tap selects without a second input focus", () => {
  const f = fixture()
  expect(f.start().defaultPrevented).toBe(true)
  f.end()
  f.start(); f.end()
  expect(f.term.getSelection()).toBe("alpha")
  expect(f.focused).toBe(1)
})

test("a deferred WebView timeout cannot turn a held press into a keyboard-opening tap", () => {
  const f = fixture()
  f.start()
  f.elapsed(700) // touchend arrives before the throttled timer callback.
  expect(f.end().defaultPrevented).toBe(true)
  expect(f.term.getSelection()).toBe("alpha")
  expect(f.focused).toBe(0)
})

test("selection drops hidden-input focus only when the keyboard was already closed", () => {
  const closed = fixture()
  closed.start(); closed.hold()
  expect(closed.blurred).toBe(1)
  const open = fixture()
  open.controller.keyboardVisible(true)
  open.start(); open.hold()
  expect(open.blurred).toBe(0)
})

test("cancel and multitouch cancel a pending long press", () => {
  const f = fixture()
  f.start(); f.root.dispatch("touchcancel"); f.hold()
  expect(f.selection).toBeNull()
  f.start(); f.root.dispatch("touchmove", { touches: [{}, {}] }); f.hold()
  expect(f.selection).toBeNull()
})

test("incoming output follows surviving markers and never jumps to unrelated rows", () => {
  const f = fixture()
  f.start(); f.hold(); f.end()
  f.rows.shift()
  for (const marker of f.markers) marker.line--
  f.callbacks.write()
  expect(f.selection!.y).toBe(19)
  expect(f.markers.size).toBe(2)
  f.controller.clear()
  f.controller.copy()
  expect(f.copied).toEqual([])
})

test("rewriting selected output cancels rather than copying replacement text", () => {
  const f = fixture()
  f.start(); f.hold(); f.end()
  f.rows[20].text = "row20 wrong text"
  f.callbacks.write()
  f.controller.copy()
  expect(f.selection).toBeNull()
  expect(f.copied).toEqual([])
})

test("pressing empty output cannot clear the clipboard", () => {
  const f = fixture()
  f.rows[20].text = ""
  f.start(); f.hold(); f.end(); f.controller.copy()
  expect(f.selection).toBeNull()
  expect(f.copied).toEqual([])
})

for (const mode of ["alternate", "mouse"]) {
  test(`${mode} disables all entry points, cancels pending gestures and restores availability`, () => {
    const f = fixture()
    expect(f.availability).toEqual([true])
    f.start()
    if (mode === "alternate") f.buffer.type = "alternate"
    else f.term.modes.mouseTrackingMode = "vt200"
    f.callbacks.write()
    expect(f.availability).toEqual([true, false])
    f.hold(); f.end()
    f.controller.arm(); f.controller.all(); f.controller.copy()
    expect(f.selection).toBeNull()
    expect(f.copied).toEqual([])
    expect(f.start().defaultPrevented).toBe(false) // Do not steal TUI mouse input.
    expect(f.root.dispatch("contextmenu").defaultPrevented).toBe(true)
    expect(f.selection).toBeNull()
    f.buffer.type = "normal"
    f.term.modes.mouseTrackingMode = "none"
    f.callbacks.write()
    expect(f.availability).toEqual([true, false, true])
    f.controller.arm()
    f.start(); f.end()
    expect(f.selection).not.toBeNull()
  })

  test(`${mode} cancels an active handle drag and never copies replacement output`, () => {
    const f = fixture()
    f.start(); f.hold(); f.end()
    f.start(110, 25, f.handles[1])
    if (mode === "alternate") { f.buffer.type = "alternate"; f.callbacks.buffer() }
    else { f.term.modes.mouseTrackingMode = "vt200"; f.callbacks.write() }
    expect(f.selection).toBeNull()
    expect(f.frames.size).toBe(0)
    expect(f.markers.size).toBe(0)
    f.controller.copy()
    expect(f.copied).toEqual([])
  })
}

test("entry points reject a mode change even before the parsed-output notification", () => {
  const f = fixture()
  f.buffer.type = "alternate"
  f.controller.arm(); f.controller.all()
  expect(f.selection).toBeNull()
  expect(f.availability).toEqual([true, false])
})

test("unrelated output preserves markers and does not notify Compose again", () => {
  const f = fixture()
  f.start(); f.hold(); f.end()
  const markers = [...f.markers]
  const changes = [...f.changes]
  for (let i = 0; i < 10; i++) f.callbacks.write()
  expect([...f.markers]).toEqual(markers)
  expect(f.changes).toEqual(changes)
  expect(f.availability).toEqual([true])
})

test("keyboard and TalkBack click activation survives a suppressed touch click", () => {
  const f = fixture()
  f.start(); f.end()
  expect(f.root.dispatch("click", { detail: 0 }).stopped).toBe(false)
  expect(f.root.dispatch("click", { detail: 1 }).stopped).toBe(true)
})

test("selection initializes before ordinary scrolling listeners", () => {
  const glue = readFileSync(new URL("../android/feature/terminal/src/commonMain/composeResources/files/terminal/terminal.js", import.meta.url), "utf8")
  expect(glue.indexOf("selection = window.OcTouchSelection(term, bridge)")).toBeLessThan(glue.indexOf("    bindTouchScroll();"))
})

test("explicit selection waits for a tapped word instead of guessing from the prompt", () => {
  const f = fixture()
  f.controller.arm()
  expect(f.picking).toEqual([true])
  expect(f.selection).toBeNull()
  expect(f.handles.every(h => h.hidden)).toBe(true)
  f.controller.copy()
  expect(f.copied).toEqual([])
  expect(f.focused).toBe(0)
  f.start(125, 50); f.end()
  expect(f.term.getSelection()).toBe("beta")
  expect(f.selection!.y).toBe(22)
  expect(f.picking).toEqual([true, false])
  expect(f.focused).toBe(0)
  expect(f.root.dispatch("click", { clientX: 125, clientY: 50, detail: 1 }).stopped).toBe(true)
  f.controller.copy()
  expect(f.copied).toEqual(["beta"])
})

test("tap-to-select preserves keyboard focus only when the keyboard is already open", () => {
  for (const open of [false, true]) {
    const f = fixture()
    f.controller.keyboardVisible(open)
    f.controller.arm()
    f.start(); f.end()
    expect(f.focused).toBe(0)
    expect(f.blurred).toBe(open ? 0 : 1)
    expect(f.term.getSelection()).toBe("alpha")
  }
})

test("empty taps keep the word picker active without opening the keyboard", () => {
  const f = fixture()
  f.controller.arm()
  f.start(190, 10); f.end()
  expect(f.selection).toBeNull()
  expect(f.picking).toEqual([true])
  expect(f.focused).toBe(0)
  f.start(); f.end()
  expect(f.term.getSelection()).toBe("alpha")
})

test("swipes and multitouch do not accidentally pick a word or open the keyboard", () => {
  const f = fixture()
  f.controller.arm()
  f.start(); f.move(75, 100); f.end()
  f.root.dispatch("click", { clientX: 75, clientY: 100, detail: 1 })
  expect(f.selection).toBeNull()
  f.start(); f.root.dispatch("touchmove", { touches: [{}, {}] }); f.end()
  expect(f.selection).toBeNull()
  expect(f.focused).toBe(0)
  expect(f.picking).toEqual([true])
})

test("the picker can be cancelled or toggled off, and ordinary taps resume", () => {
  for (const cancel of ["clear", "toggle"]) {
    const f = fixture()
    f.controller.arm()
    if (cancel === "clear") f.controller.clear()
    else f.controller.arm()
    expect(f.picking).toEqual([true, false])
    f.start(); f.end()
    expect(f.selection).toBeNull()
    expect(f.focused).toBe(1)
  }
})

test("buffer and mouse mode transitions cancel a word picker", () => {
  for (const mode of ["buffer", "mouse"]) {
    const f = fixture()
    f.controller.arm()
    if (mode === "buffer") { f.buffer.type = "alternate"; f.callbacks.buffer() }
    else { f.term.modes.mouseTrackingMode = "vt200"; f.callbacks.write() }
    expect(f.picking).toEqual([true, false])
    expect(f.selection).toBeNull()
  }
})

test("long press remains a direct selection while the picker is armed", () => {
  const f = fixture()
  f.controller.arm()
  f.start(); f.hold(); f.end()
  expect(f.term.getSelection()).toBe("alpha")
  expect(f.picking).toEqual([true, false])
})

test("a mouse click in the picker selects without forwarding mouse input", () => {
  const f = fixture()
  f.controller.arm()
  const mouse = { clientX: 75, clientY: 10, pointerType: "mouse", sourceCapabilities: { firesTouchEvents: false } }
  expect(f.root.dispatch("mousedown", mouse).stopped).toBe(true)
  expect(f.root.dispatch("click", mouse).stopped).toBe(true)
  expect(f.term.getSelection()).toBe("alpha")
  expect(f.focused).toBe(0)
})
