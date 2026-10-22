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
  // Modifiers armed or locked on the key bar; they apply to the next key typed on the keyboard.
  let modifiers = 0;

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

  window.oc = {
    write(data) { term.write(data); },
    setLive(value) {
      if (!value) { live = false; return; }
      // Runs once everything written so far has been parsed.
      term.write("", () => { live = true; });
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
    reset() { term.reset(); },
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
    term.onData((data) => {
      if (modifiers && data.length === 1) {
        data = withModifiers(data, modifiers);
        bridge.modifiersUsed();
      }
      send(data);
    });
    term.onResize(({ cols, rows }) => bridge.resize(cols, rows));
    // A tap is the user gesture that lets the focus open the soft keyboard.
    document.getElementById("terminal").addEventListener("click", () => term.focus());
    new ResizeObserver(() => fit.fit()).observe(document.getElementById("terminal"));
    fit.fit();
    bridge.resize(term.cols, term.rows);
    bridge.ready();
  }

  start();
})();
