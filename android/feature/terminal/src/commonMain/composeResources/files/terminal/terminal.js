// Glue between xterm.js and the app. The app calls window.oc.*; the page calls the
// OcTerminal Java bridge: ready(), input(data), resize(cols, rows), modifiersUsed(), font().
(function () {
  "use strict";

  const bridge = window.OcTerminal;
  const SHIFT = 1, ALT = 2, CTRL = 4;
  // OpenCode's PTY creation defaults (packages/opencode/src/pty/index.ts).
  const INITIAL_SIZE = { cols: 80, rows: 24 };
  let term = null;
  let fit = null;
  let selection = null;
  // Until the replayed output has been parsed, the terminal's answers to old queries are dropped.
  let live = false;
  let liveEpoch = 0;
  let hasOutput = false;
  let fitPending = false;
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
    enqueue((done) => term.write(data, () => {
      hasOutput ||= data.length > 0;
      fitIfReady();
      done();
    }));
  }

  // A shell draws its first prompt in several chunks (zsh: PROMPT_SP, then the prompt).
  // Fitting between them parses the rest at the new width while the shell still lays
  // it out for the old one, which leaves a ghost row. Fit once output pauses instead.
  const SETTLE_MS = 120, MAX_SETTLE_MS = 1000;
  let settleTimer = null, deadlineTimer = null;

  function cancelFit() {
    clearTimeout(settleTimer);
    clearTimeout(deadlineTimer);
    settleTimer = deadlineTimer = null;
  }

  function fitIfReady() {
    // A newly spawned shell can connect with an empty replay before drawing its
    // first prompt. Keep parsing at the server size until that output arrives too.
    if (!live || !fitPending || !hasOutput) return;
    const epoch = liveEpoch;
    const fitNow = () => enqueue((done) => {
      if (live && fitPending && epoch === liveEpoch) {
        cancelFit();
        fitPending = false;
        fit.fit();
        bridge.resize(term.cols, term.rows);
      }
      done();
    });
    clearTimeout(settleTimer);
    settleTimer = setTimeout(fitNow, SETTLE_MS);
    // Output that never pauses must not keep the phone at the server size.
    deadlineTimer ??= setTimeout(fitNow, MAX_SETTLE_MS);
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
    const root = term.element;
    const screen = term.element.querySelector(".xterm-screen");
    let gesture = null;
    let suppressClick = false;
    // On Android/Linux xterm mirrors its selected word into the hidden input
    // for copying. Opening the IME resizes xterm and can clear its selection
    // without clearing this mirror. Recognize the selected input range itself,
    // independently of term.hasSelection(), including partially selected text.
    const hasNativeSelection = () => {
      const text = window.getSelection()?.toString();
      const input = term.textarea;
      return !!text && !(input && document.activeElement === input &&
        input.value.slice(input.selectionStart, input.selectionEnd) === text);
    };
    const canScroll = () => term.buffer.active.type === "normal" &&
      term.buffer.active.baseY > 0 && term.modes.mouseTrackingMode === "none" &&
      !hasNativeSelection();

    // The selection controller registers first and can stop a touch before it
    // reaches the root listeners below; the document still sees every touch, so
    // a gesture from an earlier touch can never drive this one.
    document.addEventListener("touchstart", () => {
      gesture = null;
      suppressClick = false;
    }, { passive: true, capture: true });
    // Capture on the root: when the IME is open the touch target can be xterm's
    // editable helper or its scrollable wrapper, not just the rendered screen.
    root.addEventListener("touchstart", (event) => {
      gesture = null;
      suppressClick = false;
      if (event.touches.length !== 1 || !canScroll()) return;
      const touch = event.touches[0];
      gesture = { id: touch.identifier, x: touch.clientX, y: touch.clientY, lastY: touch.clientY, remainder: 0, scrolling: false };
    }, { passive: true, capture: true });
    root.addEventListener("touchmove", (event) => {
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
        // A word selected by a previous double tap must not disable history
        // scrolling forever. Native selection handles are still left alone.
        if (term.hasSelection()) term.clearSelection();
        const input = term.textarea;
        if (input && document.activeElement === input && input.selectionStart !== input.selectionEnd) {
          input.setSelectionRange(input.selectionEnd, input.selectionEnd);
        }
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
    }, { passive: false, capture: true });
    for (const type of ["touchend", "touchcancel", "contextmenu"]) {
      root.addEventListener(type, () => { gesture = null; }, { passive: true, capture: true });
    }
    // Some WebViews synthesize a click after a drag; do not focus the input or open the IME.
    const suppressTouchMouse = (event) => {
      if (!suppressClick || event.pointerType === "mouse" || event.detail === 0 ||
          event.sourceCapabilities?.firesTouchEvents === false) return;
      if (event.type === "click") suppressClick = false;
      event.preventDefault();
      event.stopImmediatePropagation();
    };
    // xterm focuses on mousedown, before our click handler gets a chance to run.
    root.addEventListener("mousedown", suppressTouchMouse, true);
    root.addEventListener("click", suppressTouchMouse, true);
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
    // A shell that was already running drew its output for the phone: parse its
    // replay at the current size, not at the server's initial one.
    fitBeforeReplay() {
      enqueue((done) => { fit.fit(); done(); });
    },
    setLive(value) {
      const epoch = ++liveEpoch;
      cancelFit();
      if (!value) { live = false; return; }
      // Runs once everything written so far has been parsed.
      enqueue((done) => term.write("", () => {
        if (epoch !== liveEpoch) { done(); return; }
        live = true;
        fitPending = true;
        fitIfReady();
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
      selection.clear();
      live = false;
      ++liveEpoch;
      cancelFit();
      enqueue((done) => {
        hasOutput = false;
        fitPending = false;
        term.reset();
        term.resize(INITIAL_SIZE.cols, INITIAL_SIZE.rows);
        done();
      });
    },
    focus() { term.focus(); },
    selectionLabels(start, end, position) { selection.labels(start, end, position); },
    keyboardVisible(value) { selection.keyboardVisible(value); },
    toggleSelectionMode() { selection.arm(); },
    selectAll() { selection.all(); },
    copySelection() { selection.copy(); },
    clearSelection() { selection.clear(); },
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
      ...INITIAL_SIZE,
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
    // Selection must register first: its capture listeners own handle drags and
    // selection-dismiss gestures before the ordinary touch-scroll handler runs.
    selection = window.OcTouchSelection(term, bridge);
    bindTouchScroll();
    bindImeInput();
    term.onData(typed);
    term.onResize(({ cols, rows }) => { if (live) bridge.resize(cols, rows); });
    // A tap is the user gesture that lets the focus open the soft keyboard.
    document.getElementById("terminal").addEventListener("click", () => term.focus());
    // Parse the initial replay at INITIAL_SIZE,
    // then fit and notify the shell together in setLive. Fitting before replay
    // wraps the prompt differently from readline's redraw and leaves ghost rows.
    new ResizeObserver(() => {
      if (!live || fitPending) return;
      const epoch = liveEpoch;
      enqueue((done) => {
        if (live && epoch === liveEpoch) fit.fit();
        done();
      });
    }).observe(document.getElementById("terminal"));
    bridge.ready();
  }

  start();
})();
