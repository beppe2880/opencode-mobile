package io.github.beppe2880.opencodemobile.feature.terminal

import android.os.Handler
import android.os.Looper
import org.connectbot.terminal.TerminalEmulator
import org.connectbot.terminal.TerminalEmulatorFactory
import org.connectbot.terminal.VTermKey

/**
 * Draws the shell and turns keys into the bytes it expects. Owned by the ViewModel so it
 * survives configuration changes; every call happens on the main thread.
 */
sealed interface TerminalSurface : AutoCloseable {
    fun write(text: String)

    /**
     * Whether input reaches the shell. Off while old output is replayed, or the emulator's
     * answers to queries the shell asked long ago would be typed into it, and while reconnecting.
     */
    fun setLive(live: Boolean)

    fun key(key: SpecialKey, modifiers: Int)
    fun character(codePoint: Int, modifiers: Int)
    fun paste(text: String)

    /** Clears the screen and modes for a new shell. */
    fun reset()

    /** Key bar modifiers for keys typed on the keyboard; the native surface reads them itself. */
    fun setModifiers(mask: Int) {}
}

/** libvterm through ConnectBot termlib. */
class NativeSurface(send: (ByteArray) -> Unit, onResize: (columns: Int, rows: Int) -> Unit) : TerminalSurface {
    private val main = Handler(Looper.getMainLooper())
    private var live = false

    val emulator: TerminalEmulator = TerminalEmulatorFactory.create(
        onKeyboardInput = { bytes -> if (live) send(bytes) },
        onResize = { onResize(it.columns, it.rows) },
    )

    override fun write(text: String) = emulator.writeInput(text.toByteArray())

    override fun setLive(live: Boolean) {
        // Answers to the replay are posted to the main looper; going live is queued behind them.
        if (live) main.post { this.live = true } else this.live = false
    }

    override fun key(key: SpecialKey, modifiers: Int) = emulator.dispatchKey(modifiers, key.vtermCode)

    override fun character(codePoint: Int, modifiers: Int) = emulator.dispatchCharacter(modifiers, codePoint)

    override fun paste(text: String) = emulator.pasteText(text)

    // Full reset (RIS).
    override fun reset() = emulator.writeInput("\u001bc".toByteArray())

    override fun close() {
        main.removeCallbacksAndMessages(null)
        emulator.close()
    }
}

/** The page of a WebView running xterm.js; implemented by [WebTerminalView]. */
interface WebTerminalPage {
    /** Calls `window.oc.<function>` with arguments already encoded as JavaScript literals. */
    fun call(function: String, vararg arguments: String)
}

/**
 * xterm.js in a WebView. The WebView lives in the UI and is recreated with it, so the output
 * is kept here (up to [MAX_KEPT_CHARS]) and written again to each new page.
 */
class WebSurface(
    private val send: (ByteArray) -> Unit,
    private val onResize: (columns: Int, rows: Int) -> Unit,
    private val onModifiersUsed: () -> Unit,
) : TerminalSurface {
    private val main = Handler(Looper.getMainLooper())
    private val output = StringBuilder()
    private var page: WebTerminalPage? = null
    private var live = false
    private var modifiers = 0
    private var theme: String? = null

    /** Called by the page once xterm.js is ready. */
    fun attach(page: WebTerminalPage) {
        this.page = page
        theme?.let { page.call("theme", it) }
        if (output.isNotEmpty()) page.call("write", js(output.toString()))
        page.call("setModifiers", modifiers.toString())
        page.call("setLive", live.toString())
        page.call("focus")
    }

    fun detach(page: WebTerminalPage) {
        if (this.page === page) this.page = null
    }

    /** [theme] is an xterm.js `ITheme` as a JavaScript object literal. */
    fun setTheme(theme: String) {
        this.theme = theme
        page?.call("theme", theme)
    }

    // Bridge callbacks arrive on a WebView thread.
    fun onInput(data: String) = send(data.toByteArray())

    fun onPageResize(columns: Int, rows: Int) = onResize(columns, rows)

    fun onPageModifiersUsed() {
        main.post(onModifiersUsed)
    }

    override fun write(text: String) {
        output.append(text)
        if (output.length > MAX_KEPT_CHARS) {
            // Cut at a line start so the kept output does not begin inside an escape sequence.
            val cut = output.indexOf("\n", output.length - MAX_KEPT_CHARS).takeIf { it >= 0 } ?: (output.length - MAX_KEPT_CHARS)
            output.delete(0, cut + 1)
        }
        page?.call("write", js(text))
    }

    override fun setLive(live: Boolean) {
        this.live = live
        page?.call("setLive", live.toString())
    }

    override fun key(key: SpecialKey, modifiers: Int) {
        page?.call("key", js(key.id), modifiers.toString())
    }

    override fun character(codePoint: Int, modifiers: Int) {
        page?.call("character", codePoint.toString(), modifiers.toString())
    }

    override fun paste(text: String) {
        page?.call("paste", js(text))
    }

    override fun reset() {
        output.clear()
        page?.call("reset")
    }

    override fun setModifiers(mask: Int) {
        modifiers = mask
        page?.call("setModifiers", mask.toString())
    }

    override fun close() {
        main.removeCallbacksAndMessages(null)
        page = null
    }

    private companion object {
        const val MAX_KEPT_CHARS = 1_000_000
    }
}

/** A JavaScript string literal of [text]. */
internal fun js(text: String): String = buildString(text.length + 2) {
    append('"')
    for (c in text) {
        when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\u2028' -> append("\\u2028")
            '\u2029' -> append("\\u2029")
            else -> if (c < ' ' || c == '\u007f') append("\\u%04x".format(c.code)) else append(c)
        }
    }
    append('"')
}

private val SpecialKey.vtermCode: Int
    get() = when (this) {
        SpecialKey.Escape -> VTermKey.ESCAPE
        SpecialKey.Tab -> VTermKey.TAB
        SpecialKey.Enter -> VTermKey.ENTER
        SpecialKey.Backspace -> VTermKey.BACKSPACE
        SpecialKey.Left -> VTermKey.LEFT
        SpecialKey.Down -> VTermKey.DOWN
        SpecialKey.Up -> VTermKey.UP
        SpecialKey.Right -> VTermKey.RIGHT
        SpecialKey.Home -> VTermKey.HOME
        SpecialKey.End -> VTermKey.END
        SpecialKey.PageUp -> VTermKey.PAGEUP
        SpecialKey.PageDown -> VTermKey.PAGEDOWN
        SpecialKey.Insert -> VTermKey.INS
        SpecialKey.Delete -> VTermKey.DEL
        SpecialKey.F1 -> VTermKey.FUNCTION_1
        SpecialKey.F2 -> VTermKey.FUNCTION_2
        SpecialKey.F3 -> VTermKey.FUNCTION_3
        SpecialKey.F4 -> VTermKey.FUNCTION_4
        SpecialKey.F5 -> VTermKey.FUNCTION_5
        SpecialKey.F6 -> VTermKey.FUNCTION_6
        SpecialKey.F7 -> VTermKey.FUNCTION_7
        SpecialKey.F8 -> VTermKey.FUNCTION_8
        SpecialKey.F9 -> VTermKey.FUNCTION_9
        SpecialKey.F10 -> VTermKey.FUNCTION_10
        SpecialKey.F11 -> VTermKey.FUNCTION_11
        SpecialKey.F12 -> VTermKey.FUNCTION_12
    }
