package io.github.beppe2880.opencodemobile.feature.terminal

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.SoftwareKeyboardControllerCompat

/** A WebView that only loads the bundled page and has no network access. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun PlatformTerminalView(
    surface: WebSurface,
    page: TerminalPage,
    openKeyboard: Boolean,
    onSelectionChanged: (active: Boolean, toolbarAtTop: Boolean) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier,
) {
    val selectionChanged by rememberUpdatedState(onSelectionChanged)
    val copy by rememberUpdatedState(onCopy)
    val keyboardRequest = rememberSaveable(
        saver = Saver<TerminalKeyboardRequest, Boolean>(
            save = { it.handled },
            restore = { TerminalKeyboardRequest(it) },
        ),
    ) { TerminalKeyboardRequest() }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TerminalWebView(context, keyboardRequest).apply {
                // Wrap-content height would give the page a zero-height viewport.
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.blockNetworkLoads = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.domStorageEnabled = false
                isVerticalScrollBarEnabled = false
                // xterm's hidden textarea must not open Android's native Paste popup.
                setOnLongClickListener { true }
                val bridgePage = object : WebTerminalPage {
                    override fun call(function: String, vararg arguments: String) {
                        val callback = if (function == "focus") ValueCallback<String> { onPageFocused() } else null
                        evaluateJavascript("window.oc.$function(${arguments.joinToString(",")})", callback)
                    }
                }
                addJavascriptInterface(
                    Bridge(surface, bridgePage, this, page.fontBase64, { active, top -> selectionChanged(active, top) }, { copy(it) }),
                    "OcTerminal",
                )
                webViewClient = object : WebViewClient() {
                    // Nothing but the bundled page is ever shown.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                }
                tag = bridgePage
                loadUrl(page.uri)
            }
        },
        update = { it.openKeyboard = openKeyboard },
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
private class TerminalWebView(context: Context, private val keyboardRequest: TerminalKeyboardRequest) : WebView(context) {
    private var pageFocused = false

    /** The setting to show the on-screen keyboard when the terminal opens. */
    var openKeyboard = true

    fun onPageFocused() {
        pageFocused = true
        showInitialKeyboard()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) showInitialKeyboard()
    }

    private fun showInitialKeyboard() {
        val configuration = resources.configuration
        val hardwareKeyboardActive = configuration.keyboard != Configuration.KEYBOARD_NOKEYS &&
            configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
        // Respect Android's "Show on-screen keyboard" preference when a hardware keyboard is connected.
        val allowSoftKeyboard = openKeyboard &&
            (!hardwareKeyboardActive || Settings.Secure.getInt(context.contentResolver, "show_ime_with_hard_keyboard", 0) != 0)
        if (keyboardRequest.consumeIfReady(pageFocused, isAttachedToWindow && hasWindowFocus(), allowSoftKeyboard)) {
            SoftwareKeyboardControllerCompat(this).show()
        }
    }

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
    private val fontBase64: String,
    private val onSelectionChanged: (Boolean, Boolean) -> Unit,
    private val onCopy: (String) -> Unit,
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

    @JavascriptInterface
    fun selectionAvailable(available: Boolean) = surface.onPageSelectionAvailable(available)

    @JavascriptInterface
    fun selectionPicking(picking: Boolean) = surface.onPageSelectionPicking(picking)

    @JavascriptInterface
    fun selectionChanged(active: Boolean, toolbarAtTop: Boolean) {
        view.post { onSelectionChanged(active, toolbarAtTop) }
    }

    @JavascriptInterface
    fun copy(text: String) {
        view.post { onCopy(text) }
    }

    /** IBM Plex Mono, the app's monospace font, for the page to load. */
    @JavascriptInterface
    fun font(): String = fontBase64
}
