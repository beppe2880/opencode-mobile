// Glue between xterm.js and the app. The app calls window.oc.*; the page calls the
// OcTerminal Java bridge: ready(), input(data), resize(cols, rows), modifiersUsed(), font().
(function () {
  "use strict";

  const bridge = window.OcTerminal;
  const SHIFT = 1, ALT = 2, CTRL = 4;
  let term = null;
  let fit = null;
  // Until the replayed output has been parsed, the terminal's answers to old queries are dropped.
  let live = false;
  let liveEpoch = 0;
  // Modifiers armed or locked on the key bar; they apply to the next key typed on the keyboard.
  let modifiers = 0;
  // Writes are parsed asynchronously. Replaying a resize must wait for all text
  // before it, and later text must wait for that resize.
  const pending = [];
  let parsing = false;

  function enqueue(action) {
    pending.push(action);
    if (!parsing) drain();
  }

  function drain() {
    const action = pending.shift();
    parsing = !!action;
    if (action) action(drain);
  }

  function write(data) {
    enqueue((done) => term.write(data, done));
  }

  function send(data) {
    if (live && data.length > 0) bridge.input(data);
  }

  function withModifiers(text, mask) {
    let s = text;
    if (mask & SHIFT) s = s.toUpperCase();
    if (mask & CTRL && s.length === 1) {
      const code = s.toUpperCase().charCodeAt(0);
      if (code >= 64 && code <= 95) s = String.fromCharCode(code - 64);
      else if (s === " ") s = "\x00";
      else if (s === "?") s = "\x7f";
    }
    if (mask & ALT) s = "\x1b" + s;
    return s;
  }

  // xterm encodes modifiers as 1 + shift(1) + alt(2) + ctrl(4), the same bits as the app's mask.
  function cursorKey(final, mask) {
    if (mask) return "\x1b[1;" + (1 + mask) + final;
    return (term.modes.applicationCursorKeysMode ? "\x1bO" : "\x1b[") + final;
  }

  function tildeKey(code, mask) {
    return "\x1b[" + code + (mask ? ";" + (1 + mask) : "") + "~";
  }

  const FUNCTION_TILDE = { f5: 15, f6: 17, f7: 18, f8: 19, f9: 20, f10: 21, f11: 23, f12: 24 };
  const FUNCTION_SS3 = { f1: "P", f2: "Q", f3: "R", f4: "S" };

  function keySequence(id, mask) {
    const alt = mask & ALT ? "\x1b" : "";
    switch (id) {
      case "esc": return alt + "\x1b";
      case "tab": return mask & SHIFT ? "\x1b[Z" : alt + "\t";
      case "enter": return alt + "\r";
      case "bksp": return alt + (mask & CTRL ? "\b" : "\x7f");
      case "up": return cursorKey("A", mask);
      case "down": return cursorKey("B", mask);
      case "right": return cursorKey("C", mask);
      case "left": return cursorKey("D", mask);
      case "home": return cursorKey("H", mask);
      case "end": return cursorKey("F", mask);
      case "ins": return tildeKey(2, mask);
      case "del": return tildeKey(3, mask);
      case "pgup": return tildeKey(5, mask);
      case "pgdn": return tildeKey(6, mask);
    }
    if (id in FUNCTION_SS3) return mask ? "\x1b[1;" + (1 + mask) + FUNCTION_SS3[id] : "\x1bO" + FUNCTION_SS3[id];
    if (id in FUNCTION_TILDE) return tildeKey(FUNCTION_TILDE[id], mask);
    return "";
  }

  // xterm 6 scrolls a virtual viewport, so browser panning cannot move its history.
  // Upstream fixes touch scrolling in 6.1 (https://github.com/xtermjs/xterm.js/issues/5489);
  // remove this when upgrading, or the two would handle the same swipe.
  function bindTouchScroll() {
    const screen = term.element.querySelector(".xterm-screen");
    let gesture = null;
    let suppressClick = false;
    const canScroll = () => term.buffer.active.type === "normal" &&
      term.buffer.active.baseY > 0 && term.modes.mouseTrackingMode === "none" &&
      !term.hasSelection() && !window.getSelection()?.toString();

    screen.addEventListener("touchstart", (event) => {
      gesture = null;
      suppressClick = false;
      if (event.touches.length !== 1 || !canScroll()) return;
      const touch = event.touches[0];
      gesture = { id: touch.identifier, x: touch.clientX, y: touch.clientY, lastY: touch.clientY, remainder: 0, scrolling: false };
    }, { passive: true });
    screen.addEventListener("touchmove", (event) => {
      if (!gesture) return;
      if (event.touches.length !== 1 || !canScroll()) { gesture = null; return; }
      const touch = event.touches[0];
      if (touch.identifier !== gesture.id) { gesture = null; return; }
      if (!gesture.scrolling) {
        const dx = Math.abs(touch.clientX - gesture.x);
        const dy = Math.abs(touch.clientY - gesture.y);
        if (Math.max(dx, dy) < 8) return;
        if (dx >= dy) { gesture = null; return; }
        gesture.scrolling = true;
      }
      const rowHeight = screen.getBoundingClientRect().height / term.rows;
      if (!(rowHeight > 0)) { gesture = null; return; }
      event.preventDefault();
      suppressClick = true;
      gesture.remainder += (gesture.lastY - touch.clientY) / rowHeight;
      gesture.lastY = touch.clientY;
      const rows = Math.trunc(gesture.remainder);
      if (rows) {
        term.scrollLines(rows);
        gesture.remainder -= rows;
      }
    }, { passive: false });
    for (const type of ["touchend", "touchcancel", "contextmenu"]) {
      screen.addEventListener(type, () => { gesture = null; }, { passive: true });
    }
    // Some WebViews synthesize a click after a drag; do not focus the input or open the IME.
    screen.addEventListener("click", (event) => {
      if (!suppressClick || event.pointerType === "mouse" || event.detail === 0 ||
          event.sourceCapabilities?.firesTouchEvents === false) return;
      suppressClick = false;
      event.preventDefault();
      event.stopImmediatePropagation();
    }, true);
  }

  // Applies the key bar modifiers to one typed character, then sends it.
  function typed(data) {
    if (modifiers && data.length === 1) {
      data = withModifiers(data, modifiers);
      bridge.modifiersUsed();
    }
    send(data);
  }

  // Android delivers keys through the input method: keydown has keyCode 229 and the text
  // arrives in the next input event. xterm 6 instead diffs its textarea after a timeout,
  // which repeats text when keys come faster than the timeout or when Enter clears the
  // textarea, so that input is sent straight from the input event. Compositions (and every
  // other platform, where keydown carries the real key) are left to xterm.
  function bindImeInput() {
    let imeKey = false;
    term.attachCustomKeyEventHandler((event) => {
      if (event.type !== "keydown") return true;
      imeKey = event.keyCode === 229 && !event.isComposing;
      return !imeKey;
    });
    // Capture on an ancestor runs before xterm's own listener on the textarea.
    term.element.addEventListener("input", (event) => {
      if (!imeKey || event.isComposing) return;
      imeKey = false;
      let data = null;
      switch (event.inputType) {
        case "insertText": data = event.data; break;
        case "insertLineBreak": case "insertParagraph": data = "\r"; break;
        case "deleteContentBackward": data = "\x7f"; break;
      }
      if (!data) return;
      event.stopImmediatePropagation();
      typed(data);
    }, true);
  }

  window.oc = {
    write,
    restore(data, sizes) {
      let offset = 0;
      for (const [at, cols, rows] of sizes) {
        write(data.slice(offset, at));
        enqueue((done) => { term.resize(cols, rows); done(); });
        offset = at;
      }
      write(data.slice(offset));
    },
    setLive(value) {
      const epoch = ++liveEpoch;
      if (!value) { live = false; return; }
      // Runs once everything written so far has been parsed.
      enqueue((done) => term.write("", () => {
        if (epoch !== liveEpoch) { done(); return; }
        live = true;
        fit.fit();
        bridge.resize(term.cols, term.rows);
        done();
      }));
    },
    setModifiers(mask) { modifiers = mask; },
    key(id, mask) {
      term.scrollToBottom();
      send(keySequence(id, mask));
    },
    character(codePoint, mask) {
      term.scrollToBottom();
      send(withModifiers(String.fromCodePoint(codePoint), mask));
    },
    paste(text) { term.paste(text); },
    reset() {
      live = false;
      ++liveEpoch;
      enqueue((done) => { term.reset(); term.resize(80, 24); done(); });
    },
    focus() { term.focus(); },
    theme(theme) {
      term.options.theme = theme;
      document.body.style.background = theme.background;
    },
  };

  async function start() {
    let fontFamily = "monospace";
    try {
      const bytes = Uint8Array.from(atob(bridge.font()), (c) => c.charCodeAt(0));
      const face = new FontFace("Plex Mono", bytes.buffer);
      await face.load();
      document.fonts.add(face);
      fontFamily = "'Plex Mono', monospace";
    } catch (e) {
      // The system monospace font is a fine fallback.
    }
    term = new Terminal({
      fontFamily,
      fontSize: 13,
      scrollback: 5000,
      // A blinking cursor repaints the WebView twice a second even when nothing happens.
      cursorBlink: false,
      allowProposedApi: false,
    });
    fit = new FitAddon.FitAddon();
    term.loadAddon(fit);
    term.open(document.getElementById("terminal"));
    bindTouchScroll();
    bindImeInput();
    term.onData(typed);
    term.onResize(({ cols, rows }) => { if (live) bridge.resize(cols, rows); });
    // A tap is the user gesture that lets the focus open the soft keyboard.
    document.getElementById("terminal").addEventListener("click", () => term.focus());
    // The server starts PTYs at 80x24. Parse the initial replay at that geometry,
    // then fit and notify the shell together in setLive. Fitting before replay
    // wraps the prompt differently from readline's redraw and leaves ghost rows.
    new ResizeObserver(() => { if (live) fit.fit(); }).observe(document.getElementById("terminal"));
    bridge.ready();
  }

  start();
})();
