// Touch selection stays in terminal cell coordinates. Nothing here sends PTY input.
(function () {
  "use strict";
  window.OcTouchSelection = function (term, bridge) {
    const root = term.element;
    const screen = root.querySelector(".xterm-screen");
    const layer = document.createElement("div");
    layer.className = "oc-selection-layer";
    root.appendChild(layer);
    const handles = ["start", "end"].map((name) => {
      const handle = document.createElement("div");
      handle.id = "terminal_selection_" + name;
      handle.className = "oc-selection-handle";
      handle.dataset.selectionHandle = name;
      handle.setAttribute("role", "slider");
      handle.tabIndex = 0;
      handle.appendChild(document.createElement("span"));
      layer.appendChild(handle);
      return handle;
    });
    let range = null, markers = [], pending = null, drag = null, scroll = null;
    let selectedText = "";
    let keyboardVisible = false;
    let lastTap = null;
    let suppressMouse = false, applying = false, frame = null;
    let available = null, notified = "", positionLabel = "";
    let picking = false;
    const compare = (a, b) => a.y - b.y || a.x - b.x;
    const normalTouch = () => term.buffer.active.type === "normal" && term.modes.mouseTrackingMode === "none";
    function notify(active, top) {
      const state = active + ":" + top;
      if (state === notified) return;
      notified = state;
      bridge.selectionChanged(active, top);
    }
    function updateAvailability() {
      const next = normalTouch();
      if (available !== next) {
        available = next;
        if (!next) { clear(); scroll = null; }
        bridge.selectionAvailable(next);
      }
      return next;
    }
    const metrics = () => {
      const rect = screen.getBoundingClientRect();
      return { rect, w: rect.width / term.cols, h: rect.height / term.rows };
    };
    function point(x, y, boundary = false) {
      const { rect, w, h } = metrics();
      return {
        x: Math.max(0, Math.min(term.cols - (boundary ? 0 : 1),
          boundary ? Math.round((x - rect.left) / w) : Math.floor((x - rect.left) / w))),
        y: term.buffer.active.viewportY + Math.max(0, Math.min(term.rows - 1, Math.floor((y - rect.top) / h))),
      };
    }
    function releaseMarkers() {
      markers.forEach((marker) => marker?.dispose());
      markers = [];
    }
    function cancelPending() {
      if (pending) clearTimeout(pending.timer);
      pending = null;
    }
    function stopDrag() {
      drag = null;
      if (frame !== null) cancelAnimationFrame(frame);
      frame = null;
    }
    function clear() {
      setPicking(false);
      lastTap = null;
      cancelPending();
      stopDrag();
      range = null;
      selectedText = "";
      releaseMarkers();
      applying = true;
      term.clearSelection();
      applying = false;
      render();
      notify(false, false);
    }
    function setPicking(value) {
      if (picking === value) return;
      picking = value;
      bridge.selectionPicking(value);
    }
    function render() {
      handles.forEach((handle, i) => {
        if (!range) { handle.hidden = true; return; }
        const p = i ? range.end : range.start;
        const row = p.y - term.buffer.active.viewportY;
        handle.hidden = row < 0 || row >= term.rows;
        const { rect, w, h } = metrics();
        const parent = root.getBoundingClientRect();
        // Keep the full 48px touch target reachable at the screen edges.
        const anchorX = rect.left - parent.left + p.x * w;
        const left = Math.max(0, Math.min(parent.width - 48, anchorX - 24));
        handle.style.left = left + "px";
        handle.children[0].style.left = Math.max(0, Math.min(32, anchorX - left - 8)) + "px";
        const anchorY = rect.top - parent.top + (row + 1) * h;
        const top = Math.max(0, Math.min(parent.height - 48, anchorY - 8));
        handle.style.top = top + "px";
        handle.children[0].style.top = Math.max(0, Math.min(28, anchorY - top)) + "px";
        handle.style.color = term.options.theme?.cursor || term.options.theme?.foreground;
        handle.setAttribute("aria-valuemin", "0");
        handle.setAttribute("aria-valuemax", String(term.buffer.active.length * term.cols));
        handle.setAttribute("aria-valuenow", String(p.y * term.cols + p.x));
        handle.setAttribute("aria-valuetext", positionLabel.replace("%1$d", String(p.y + 1)).replace("%2$d", String(p.x + 1)));
      });
    }
    function select(a, b) {
      if (!updateAvailability()) return;
      if (compare(a, b) === 0) return;
      setPicking(false);
      const start = compare(a, b) < 0 ? a : b;
      const end = start === a ? b : a;
      range = { start: { ...start }, end: { ...end } };
      releaseMarkers();
      if (term.buffer.active.type === "normal") {
        const cursor = term.buffer.active.baseY + term.buffer.active.cursorY;
        markers = [start, end].map((p) => term.registerMarker(p.y - cursor));
      }
      applying = true;
      // Back can hide the IME while leaving xterm's hidden input focused.
      // Android would reopen it after the touch, even with DOM defaults
      // prevented. Release that stale focus only when the IME was closed.
      if (!keyboardVisible) term.blur();
      term.select(start.x, start.y, (end.y - start.y) * term.cols + end.x - start.x);
      selectedText = term.getSelection();
      applying = false;
      suppressMouse = true;
      render();
      // Put the app toolbar on the opposite half, without resizing the terminal.
      notify(true, (start.y + end.y) / 2 - term.buffer.active.viewportY >= term.rows / 2);
    }
    function wordAt(p) {
      const buffer = term.buffer.active;
      const line = buffer.getLine(p.y);
      if (!line) return;
      // Columns are cells, not UTF-16 offsets: wide and combining characters
      // must never be split by a handle or by word-boundary detection.
      while (p.x > 0 && line.getCell(p.x)?.getWidth() === 0) p.x--;
      if (/^\s*$/u.test(line.getCell(p.x)?.getChars() || "")) return;
      const separator = (cell) => {
        const chars = cell?.getChars() || " ";
        return /^\s+$/u.test(chars) || (term.options.wordSeparator || " ()[]{}'\"").includes(chars);
      };
      const isSeparator = separator(line.getCell(p.x));
      let start = { ...p }, end = { x: p.x + Math.max(1, line.getCell(p.x)?.getWidth() || 1), y: p.y };
      // Follow wrapped rows, but never cross a real line break.
      while (!isSeparator) {
        let previous = { x: start.x - 1, y: start.y };
        if (previous.x < 0) {
          if (!buffer.getLine(start.y)?.isWrapped) break;
          previous = { x: term.cols - 1, y: start.y - 1 };
        }
        const row = buffer.getLine(previous.y);
        while (previous.x > 0 && row?.getCell(previous.x)?.getWidth() === 0) previous.x--;
        if (separator(row?.getCell(previous.x))) break;
        start = previous;
      }
      while (!isSeparator) {
        let next = { ...end };
        if (next.x >= term.cols) {
          if (!buffer.getLine(next.y + 1)?.isWrapped) break;
          next = { x: 0, y: next.y + 1 };
        }
        const cell = buffer.getLine(next.y)?.getCell(next.x);
        if (separator(cell)) break;
        end = { x: next.x + Math.max(1, cell.getWidth()), y: next.y };
      }
      select(start, end);
    }
    function begin() {
      if (!updateAvailability()) return;
      if (!pending) return;
      const p = pending;
      cancelPending();
      suppressMouse = true;
      lastTap = null;
      wordAt(point(p.x, p.y));
    }
    function updateDrag() {
      if (!drag || !range) return;
      const p = point(drag.x - drag.offsetX, drag.y - drag.offsetY, true);
      const cell = term.buffer.active.getLine(p.y)?.getCell(p.x);
      if (cell?.getWidth() === 0) p.x--;
      select(drag.anchor, p);
    }
    function autoScroll(time) {
      frame = null;
      if (!drag || !range) return;
      const { rect } = metrics();
      const distance = drag.y < rect.top + 32 ? drag.y - rect.top - 32 :
        drag.y > rect.bottom - 32 ? drag.y - rect.bottom + 32 : 0;
      if (distance && time - drag.lastScroll >= 60 && term.buffer.active.type === "normal") {
        drag.lastScroll = time;
        term.scrollLines(Math.sign(distance) * Math.min(3, Math.ceil(Math.abs(distance) / 32)));
        updateDrag();
      }
      frame = requestAnimationFrame(autoScroll);
    }
    root.addEventListener("touchstart", (event) => {
      cancelPending();
      stopDrag();
      scroll = null;
      suppressMouse = false;
      if (event.touches.length !== 1) return;
      const touch = event.touches[0];
      const handle = event.target.closest?.(".oc-selection-handle");
      if (range && handle) {
        const start = handle.dataset.selectionHandle === "start";
        const anchor = start ? range.end : range.start;
        const endpoint = start ? range.start : range.end;
        const { rect, w, h } = metrics();
        drag = {
          id: touch.identifier, anchor: { ...anchor }, x: touch.clientX, y: touch.clientY, lastScroll: 0,
          offsetX: touch.clientX - rect.left - endpoint.x * w,
          offsetY: touch.clientY - rect.top - (endpoint.y - term.buffer.active.viewportY + 0.5) * h,
        };
        suppressMouse = true;
        event.preventDefault();
        event.stopImmediatePropagation();
        frame = requestAnimationFrame(autoScroll);
        return;
      }
      if (!updateAvailability()) return;
      // The browser would focus the non-editable output on touch-down and
      // dismiss an already-open IME before contextmenu can be cancelled.
      // Own touch taps too; focus only for a completed short tap below.
      event.preventDefault();
      pending = { id: touch.identifier, x: touch.clientX, y: touch.clientY, wasSelecting: !!range, startedAt: Date.now() };
      pending.timer = setTimeout(begin, 500);
      // A swipe leaving our selection is owned here, not by the independent
      // normal-scrolling handler. This feature does not change its behavior.
      if (range) event.stopImmediatePropagation();
    }, { passive: false, capture: true });
    root.addEventListener("touchmove", (event) => {
      if (event.touches.length !== 1) { cancelPending(); stopDrag(); scroll = null; return; }
      const touch = event.touches[0];
      if (!drag && pending?.wasSelecting && touch.identifier === pending.id) {
        const dx = Math.abs(touch.clientX - pending.x), dy = Math.abs(touch.clientY - pending.y);
        if (Math.max(dx, dy) >= 8 && dy > dx) {
          scroll = { id: touch.identifier, lastY: pending.y, remainder: 0 };
          clear();
          suppressMouse = true;
        }
      }
      if (scroll) {
        if (scroll.id !== touch.identifier) { scroll = null; return; }
        event.preventDefault();
        event.stopImmediatePropagation();
        scroll.remainder += (scroll.lastY - touch.clientY) / metrics().h;
        scroll.lastY = touch.clientY;
        const rows = Math.trunc(scroll.remainder);
        if (rows) { term.scrollLines(rows); scroll.remainder -= rows; }
        return;
      }
      if (drag) {
        if (drag.id !== touch.identifier) { stopDrag(); return; }
        drag.x = touch.clientX; drag.y = touch.clientY;
        event.preventDefault();
        event.stopImmediatePropagation();
        updateDrag();
      } else if (pending && (touch.identifier !== pending.id ||
        Math.max(Math.abs(touch.clientX - pending.x), Math.abs(touch.clientY - pending.y)) >= 8)) {
        lastTap = null;
        cancelPending();
      }
    }, { passive: false, capture: true });
    root.addEventListener("touchend", (event) => {
      // WebView can defer timers while rendering/handling a touch. A held
      // press must not become a short tap (and open the IME) just because
      // its timeout callback has not run yet.
      if (pending && Date.now() - pending.startedAt >= 500) begin();
      const tap = pending;
      const dismiss = tap?.wasSelecting;
      cancelPending();
      if (dismiss) { clear(); suppressMouse = true; }
      else if (tap && picking) {
        // A completed tap chooses the word, not the hidden shell input.
        wordAt(point(tap.x, tap.y));
        suppressMouse = true;
      } else if (tap) {
        const now = Date.now();
        if (lastTap && now - lastTap.time < 350 && Math.hypot(tap.x - lastTap.x, tap.y - lastTap.y) < 24) {
          wordAt(point(tap.x, tap.y));
          lastTap = null;
        } else {
          term.focus();
          lastTap = { x: tap.x, y: tap.y, time: now };
        }
        suppressMouse = true;
      }
      if (picking) {
        suppressMouse = true;
        // Empty taps and swipes must not reopen a dismissed IME either.
        if (!keyboardVisible) term.blur();
      }
      if (drag || suppressMouse) {
        event.preventDefault();
        event.stopImmediatePropagation();
      }
      stopDrag();
      scroll = null;
    }, { passive: false, capture: true });
    root.addEventListener("touchcancel", () => { cancelPending(); stopDrag(); scroll = null; }, { passive: true, capture: true });
    root.addEventListener("contextmenu", (event) => {
      // Never expose the hidden textarea's native Paste menu, including in TUIs.
      if (normalTouch()) begin();
      event.preventDefault();
      event.stopImmediatePropagation();
    }, true);
    for (const type of ["mousedown", "click"]) {
      root.addEventListener(type, (event) => {
        if (picking) {
          event.preventDefault();
          event.stopImmediatePropagation();
          if (type === "click" && (!suppressMouse || event.sourceCapabilities?.firesTouchEvents === false || event.pointerType === "mouse")) {
            const { rect } = metrics();
            if (event.clientX >= rect.left && event.clientX < rect.left + rect.width &&
                event.clientY >= rect.top && event.clientY < rect.bottom) {
              wordAt(point(event.clientX, event.clientY));
            }
          }
          return;
        }
        if (!suppressMouse || (type === "click" && event.detail === 0) ||
            event.sourceCapabilities?.firesTouchEvents === false || event.pointerType === "mouse") return;
        event.preventDefault();
        event.stopImmediatePropagation();
      }, true);
    }
    handles.forEach((handle, i) => handle.addEventListener("keydown", (event) => {
      if (!range || !["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(event.key)) return;
      event.preventDefault();
      event.stopImmediatePropagation();
      const p = { ...(i ? range.end : range.start) };
      const offset = event.key === "ArrowLeft" ? -1 : event.key === "ArrowRight" ? 1 : event.key === "ArrowUp" ? -term.cols : term.cols;
      const index = Math.max(0, Math.min(term.buffer.active.length * term.cols, p.y * term.cols + p.x + offset));
      p.x = index === term.buffer.active.length * term.cols ? term.cols : index % term.cols;
      p.y = Math.min(term.buffer.active.length - 1, Math.floor(index / term.cols));
      if (term.buffer.active.getLine(p.y)?.getCell(p.x)?.getWidth() === 0) p.x += offset > 0 ? 1 : -1;
      select(i ? range.start : p, i ? p : range.end);
    }));
    term.onSelectionChange(() => { if (!applying && range && !term.hasSelection()) clear(); });
    term.onScroll(render);
    term.onResize(() => {
      // Cancel an existing range on reflow, but not a pending long press while
      // the IME is still animating: select against the settled cell geometry.
      if (range || drag) clear();
      else render();
    });
    term.buffer.onBufferChange(() => { clear(); updateAvailability(); });
    term.onWriteParsed(() => {
      if (!updateAvailability()) return;
      if (!range) return;
      if (markers.length !== 2 || markers.some((marker) => !marker || marker.isDisposed)) { clear(); return; }
      const before = selectedText;
      if (markers[0].line === range.start.y && markers[1].line === range.end.y) {
        // Still validate in-place rewrites even when the anchors did not move.
        if (!term.hasSelection() || term.getSelection() !== before) clear();
        return;
      }
      if (drag) drag.anchor.y = markers[compare(drag.anchor, range.start) === 0 ? 0 : 1].line;
      select({ x: range.start.x, y: markers[0].line }, { x: range.end.x, y: markers[1].line });
      // Programs can rewrite an existing row without trimming it. Do not let
      // Copy silently copy new text instead of what the user selected.
      if (selectedText !== before) clear();
    });
    render();
    updateAvailability();
    return {
      clear,
      keyboardVisible(value) { keyboardVisible = value; },
      labels(start, end, position) {
        handles[0].setAttribute("aria-label", start); handles[1].setAttribute("aria-label", end);
        positionLabel = position; render();
      },
      copy() {
        if (!updateAvailability()) return;
        const text = range && term.hasSelection() ? term.getSelection() : "";
        if (text) bridge.copy(text);
      },
      all() {
        if (!updateAvailability()) return;
        const last = term.buffer.active.length - 1;
        select({ x: 0, y: 0 }, { x: term.cols, y: last });
      },
      arm() {
        if (!updateAvailability()) return;
        const wasPicking = picking;
        clear();
        if (!wasPicking) setPicking(true);
      },
      dispose() { cancelPending(); stopDrag(); releaseMarkers(); },
    };
  };
})();
