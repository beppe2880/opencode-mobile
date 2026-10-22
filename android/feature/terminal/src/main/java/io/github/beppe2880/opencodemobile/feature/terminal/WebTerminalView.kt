package io.github.beppe2880.opencodemobile.feature.terminal

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.text.InputType
import android.util.Base64
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.github.beppe2880.opencodemobile.core.ui.theme.OcColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.terminalAnsi
import io.github.beppe2880.opencodemobile.core.ui.R as UiR

private const val PAGE = "file:///android_asset/terminal/index.html"

/** xterm.js in a WebView that only loads the bundled page and has no network access. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebTerminalView(surface: WebSurface, modifier: Modifier = Modifier) {
    val colors = OcTheme.colors
    LaunchedEffect(surface, colors) { surface.setTheme(colors.xtermTheme()) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TerminalWebView(context).apply {
                // Wrap-content height would give the page a zero-height viewport.
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.blockNetworkLoads = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                isVerticalScrollBarEnabled = false
                val page = object : WebTerminalPage {
                    override fun call(function: String, vararg arguments: String) {
                        evaluateJavascript("window.oc.$function(${arguments.joinToString(",")})", null)
                    }
                }
                addJavascriptInterface(Bridge(surface, page, this, resources), "OcTerminal")
                webViewClient = object : WebViewClient() {
                    // Nothing but the bundled page is ever shown.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                }
                tag = page
                loadUrl(PAGE)
            }
        },
        onRelease = { webView ->
            (webView.tag as? WebTerminalPage)?.let(surface::detach)
            webView.removeJavascriptInterface("OcTerminal")
            webView.destroy()
        },
    )
}

/**
 * Asks the keyboard for plain keystrokes: with word suggestions on, Android keyboards compose
 * whole words and xterm.js sends every intermediate version of them to the shell.
 */
private class TerminalWebView(context: Context) : WebView(context) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val connection = super.onCreateInputConnection(outAttrs)
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        outAttrs.imeOptions = outAttrs.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        return connection
    }
}

/** Called by the page on a WebView thread. */
private class Bridge(
    private val surface: WebSurface,
    private val page: WebTerminalPage,
    private val view: WebView,
    private val resources: Resources,
) {
    @JavascriptInterface
    fun ready() {
        view.post {
            view.requestFocus()
            surface.attach(page)
        }
    }

    @JavascriptInterface
    fun input(data: String) = surface.onInput(data)

    @JavascriptInterface
    fun resize(columns: Int, rows: Int) {
        if (columns > 0 && rows > 0) surface.onPageResize(columns, rows)
    }

    @JavascriptInterface
    fun modifiersUsed() = surface.onPageModifiersUsed()

    /** IBM Plex Mono, the app's monospace font, for the page to load. */
    @JavascriptInterface
    fun font(): String = resources.openRawResource(UiR.font.plex_mono_regular).use { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }
}

private fun OcColors.xtermTheme(): String {
    fun css(color: Color, alpha: String = "") = js("#%06X%s".format(color.toArgb() and 0xFFFFFF, alpha))
    val names = listOf(
        "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white",
        "brightBlack", "brightRed", "brightGreen", "brightYellow", "brightBlue", "brightMagenta", "brightCyan", "brightWhite",
    )
    val ansi = names.zip(terminalAnsi).joinToString(",") { (name, color) -> "$name:${css(color)}" }
    return "{background:${css(bgDeep)},foreground:${css(textBase)},cursor:${css(textBase)}," +
        "selectionBackground:${css(bgAccent, alpha = "66")},$ansi}"
}
