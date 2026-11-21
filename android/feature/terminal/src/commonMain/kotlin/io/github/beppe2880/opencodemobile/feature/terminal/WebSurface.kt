package io.github.beppe2880.opencodemobile.feature.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The page of a web view running xterm.js; implemented by [WebTerminalView]. */
interface WebTerminalPage {
    /** Calls `window.oc.<function>` with arguments already encoded as JavaScript literals. */
    fun call(function: String, vararg arguments: String)
}

/**
 * The screen the shell draws on: xterm.js in a web view. Owned by the ViewModel, while the
 * web view lives in the UI and is recreated with it, so the output is kept here (up to
 * [MAX_KEPT_CHARS]) and written again to each new page. Every call happens on the main thread.
 */
class WebSurface(
    private val send: (ByteArray) -> Unit,
    private val onResize: (columns: Int, rows: Int) -> Unit,
    private val onModifiersUsed: () -> Unit,
) : AutoCloseable {
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val output = StringBuilder()
    private data class Size(val at: Int, val columns: Int, val rows: Int)
    private val sizes = mutableListOf<Size>()

    // Output trimmed from the start of [output], so positions in the shell's output are `dropped + index`.
    private var dropped = 0L

    // Sizes from an earlier visit, applied as the replay reaches their positions.
    private val replaySizes = ArrayDeque<TerminalSize>()
    private var page: WebTerminalPage? = null
    private var live = false
    private var modifiers = 0
    private var theme: String? = null
    private var fitBeforeReplay = false
    private var selectionLabels: List<String>? = null
    private val mutableSelectionAvailable = MutableStateFlow(false)
    val selectionAvailable = mutableSelectionAvailable.asStateFlow()
    private val mutableSelectionPicking = MutableStateFlow(false)
    val selectionPicking = mutableSelectionPicking.asStateFlow()
    private var imeVisible = false

    /** Called by the page once xterm.js is ready. */
    fun attach(page: WebTerminalPage) {
        this.page = page
        theme?.let { page.call("theme", it) }
        selectionLabels?.let { labels -> page.call("selectionLabels", *labels.map(::js).toTypedArray()) }
        page.call("keyboardVisible", imeVisible.toString())
        if (fitBeforeReplay && sizes.isEmpty()) page.call("fitBeforeReplay")
        if (output.isNotEmpty() || sizes.isNotEmpty()) {
            val geometry = sizes.joinToString(prefix = "[", postfix = "]") { "[${it.at},${it.columns},${it.rows}]" }
            page.call("restore", js(output.toString()), geometry)
        }
        page.call("setModifiers", modifiers.toString())
        page.call("setLive", live.toString())
        page.call("focus")
    }

    fun detach(page: WebTerminalPage) {
        if (this.page === page) {
            this.page = null
            mutableSelectionAvailable.value = false
            mutableSelectionPicking.value = false
        }
    }

    /** [theme] is an xterm.js `ITheme` as a JavaScript object literal. */
    fun setTheme(theme: String) {
        this.theme = theme
        page?.call("theme", theme)
    }

    // Bridge callbacks may arrive on a web view thread (Android).
    fun onInput(data: String) = send(data.encodeToByteArray())

    fun onPageResize(columns: Int, rows: Int) {
        main.launch {
            val last = sizes.lastOrNull()
            // Approximate replay position: output can arrive while the bridge callback
            // hops to Main, before this offset is read. It is not a server resize timeline.
            // A replay parsed at the fitted size starts at that size.
            val at = if (last == null && fitBeforeReplay) 0 else output.length
            if (last?.columns != columns || last.rows != rows) sizes += Size(at, columns, rows)
            onResize(columns, rows)
        }
    }

    /**
     * The shell was already running before this screen, so its output so far was drawn at the
     * size the app gave it, not at the server's initial one: the page parses it at its own size.
     */
    fun fitBeforeReplay() {
        fitBeforeReplay = true
        if (sizes.isEmpty()) page?.call("fitBeforeReplay")
    }

    /** The sizes the shell's output was drawn at, by position in that output. */
    fun timeline(): List<TerminalSize> = sizes.map { TerminalSize(dropped + it.at, it.columns, it.rows) }

    /**
     * Replays the shell's output at the sizes of an earlier [timeline], from the beginning of the
     * output, instead of guessing one size for all of it.
     */
    fun replayAt(timeline: List<TerminalSize>) {
        replaySizes.clear()
        replaySizes += timeline.sortedBy { it.at }
    }

    fun onPageModifiersUsed() {
        main.launch { onModifiersUsed() }
    }

    fun write(text: String) {
        val start = dropped + output.length
        val due = buildList {
            while (replaySizes.isNotEmpty() && replaySizes.first().at <= start + text.length) {
                val size = replaySizes.removeFirst()
                add(Size((size.at - start).coerceAtLeast(0).toInt(), size.columns, size.rows))
            }
        }
        due.forEach { sizes += it.copy(at = output.length + it.at) }
        output.append(text)
        if (output.length > MAX_KEPT_CHARS) {
            // Cut at a line start so the kept output does not begin inside an escape sequence.
            val cut = output.indexOf("\n", output.length - MAX_KEPT_CHARS).takeIf { it >= 0 } ?: (output.length - MAX_KEPT_CHARS)
            output.deleteRange(0, cut + 1)
            dropped += cut + 1
            val previous = sizes.lastOrNull { it.at <= cut + 1 }
            val kept = sizes.filter { it.at > cut + 1 }.map { it.copy(at = it.at - cut - 1) }
            sizes.clear()
            previous?.let { sizes += it.copy(at = 0) }
            sizes += kept
        }
        if (due.isEmpty()) {
            page?.call("write", js(text))
        } else {
            page?.call("restore", js(text), due.joinToString(prefix = "[", postfix = "]") { "[${it.at},${it.columns},${it.rows}]" })
        }
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

    fun setSelectionLabels(start: String, end: String, position: String) {
        selectionLabels = listOf(start, end, position)
        page?.call("selectionLabels", js(start), js(end), js(position))
    }

    fun onPageSelectionAvailable(available: Boolean) {
        main.launch { mutableSelectionAvailable.value = available }
    }

    fun onPageSelectionPicking(picking: Boolean) {
        main.launch { mutableSelectionPicking.value = picking }
    }

    fun setKeyboardVisible(visible: Boolean) {
        imeVisible = visible
        page?.call("keyboardVisible", visible.toString())
    }

    /** Toggle tap-to-select without guessing a word or moving shell input focus. */
    fun toggleSelectionMode() { page?.call("toggleSelectionMode") }

    fun selectAll() { page?.call("selectAll") }

    fun copySelection() { page?.call("copySelection") }

    fun clearSelection() { page?.call("clearSelection") }

    /** Clears the screen and modes for a new shell. */
    fun reset() {
        output.clear()
        sizes.clear()
        dropped = 0
        replaySizes.clear()
        fitBeforeReplay = false
        page?.call("reset")
    }

    /** Key bar modifiers, applied by the page to the next key typed on the keyboard. */
    fun setModifiers(mask: Int) {
        modifiers = mask
        page?.call("setModifiers", mask.toString())
    }

    override fun close() {
        main.cancel()
        page = null
    }

    private companion object {
        const val MAX_KEPT_CHARS = 1_000_000
    }
}

/** The terminal was [columns]×[rows] from position [at] of the shell's output on. */
data class TerminalSize(val at: Long, val columns: Int, val rows: Int)

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
            else -> if (c < ' ' || c == '\u007f') append("\\u").append(c.code.toString(16).padStart(4, '0')) else append(c)
        }
    }
    append('"')
}
