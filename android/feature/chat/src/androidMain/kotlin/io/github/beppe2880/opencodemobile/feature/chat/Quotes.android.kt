package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.ProcessTextKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_selection_ask
import org.jetbrains.compose.resources.stringResource

private object QuoteKey

/**
 * The menu of a transcript selection: Copy, Select all and "Ask". Other apps' text actions
 * (PROCESS_TEXT, e.g. an assistant or read aloud) are left out: Compose appends this item after
 * them, so with them "Ask" would sit in the overflow menu.
 *
 * The selection menu has no way to read the selected text, so "Ask" runs the menu's own Copy
 * with the clipboard of the transcript swapped for one that hands the text to [onQuote] instead
 * of storing it: the system clipboard keeps what it had.
 */
@Composable
internal actual fun QuoteSelection(onQuote: (String) -> Unit, content: @Composable () -> Unit) {
    val system = LocalClipboard.current
    val quote by rememberUpdatedState(onQuote)
    val capture = remember(system) { QuoteClipboard(system) { quote(it) } }
    val label = stringResource(Res.string.chat_selection_ask)
    CompositionLocalProvider(LocalClipboard provides capture) {
        Box(
            Modifier
                .filterTextContextMenuComponents { component ->
                    if (component is TextContextMenuItem && component.key == TextContextMenuKeys.CopyKey) capture.copy = component
                    component.key !is ProcessTextKey
                }
                .appendTextContextMenuComponents {
                    item(key = QuoteKey, label = label) {
                        val copy = capture.copy
                        if (copy == null) {
                            close()
                        } else {
                            capture.armed = true
                            copy.onClick(this)
                        }
                    }
                },
        ) { content() }
    }
}

private class QuoteClipboard(private val system: Clipboard, private val onQuote: (String) -> Unit) : Clipboard {
    /** The Copy item of the menu being shown, run by "Ask". */
    var copy: TextContextMenuItem? = null

    /** The next copy is a quote: it goes to [onQuote], not to the system clipboard. */
    var armed = false

    override val nativeClipboard get() = system.nativeClipboard

    override suspend fun getClipEntry(): ClipEntry? = system.getClipEntry()

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (!armed) return system.setClipEntry(clipEntry)
        armed = false
        val data = clipEntry?.clipData ?: return
        val text = (0 until data.itemCount).joinToString("\n") { data.getItemAt(it).text?.toString().orEmpty() }
        if (text.isNotBlank()) onQuote(text)
    }
}
