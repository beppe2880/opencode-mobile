package io.github.beppe2880.opencodemobile.feature.terminal

import android.os.Handler
import android.os.Looper

/** The page of a WebView running xterm.js; implemented by [WebTerminalView]. */
interface WebTerminalPage {
    /** Calls `window.oc.<function>` with arguments already encoded as JavaScript literals. */
    fun call(function: String, vararg arguments: String)
}

/**
 * The screen the shell draws on: xterm.js in a WebView. Owned by the ViewModel, while the
 * WebView lives in the UI and is recreated with it, so the output is kept here (up to
 * [MAX_KEPT_CHARS]) and written again to each new page. Every call happens on the main thread.
 */
class WebSurface(
    private val send: (ByteArray) -> Unit,
    private val onResize: (columns: Int, rows: Int) -> Unit,
    private val onModifiersUsed: () -> Unit,
) : AutoCloseable {
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

    fun write(text: String) {
        output.append(text)
        if (output.length > MAX_KEPT_CHARS) {
            // Cut at a line start so the kept output does not begin inside an escape sequence.
            val cut = output.indexOf("\n", output.length - MAX_KEPT_CHARS).takeIf { it >= 0 } ?: (output.length - MAX_KEPT_CHARS)
            output.delete(0, cut + 1)
        }
        page?.call("write", js(text))
    }

    /**
     * Whether input reaches the shell. Off while old output is replayed, or the terminal's
     * answers to queries the shell asked long ago would be typed into it, and while reconnecting.
     */
    fun setLive(live: Boolean) {
        this.live = live
        page?.call("setLive", live.toString())
    }

    fun key(key: SpecialKey, modifiers: Int) {
        page?.call("key", js(key.id), modifiers.toString())
    }

    fun character(codePoint: Int, modifiers: Int) {
        page?.call("character", codePoint.toString(), modifiers.toString())
    }

    fun paste(text: String) {
        page?.call("paste", js(text))
    }

    /** Clears the screen and modes for a new shell. */
    fun reset() {
        output.clear()
        page?.call("reset")
    }

    /** Key bar modifiers, applied by the page to the next key typed on the keyboard. */
    fun setModifiers(mask: Int) {
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
