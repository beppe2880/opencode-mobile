package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberTextClipboard
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_close
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_copied
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_copy
import io.github.beppe2880.opencodemobile.core.ui.theme.OcColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.terminalAnsi
import io.github.beppe2880.opencodemobile.feature.terminal.resources.Res
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_copy_failed
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_select_all
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_selection_end
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_selection_position
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_selection_start
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_selection_tap_word
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.io.encoding.Base64
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/**
 * The bundled page and what it needs: [uri] of `index.html` among its scripts, and IBM Plex
 * Mono, the app's monospace font, as base64 for the page to load.
 */
internal class TerminalPage(val uri: String, val fontBase64: String)

/** xterm.js in a web view that only loads the bundled page. */
@Composable
internal fun WebTerminalView(surface: WebSurface, openKeyboard: Boolean, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = OcTheme.colors
    val clipboard = rememberTextClipboard()
    val scope = rememberCoroutineScope()
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    var selecting by remember(surface) { mutableStateOf(false) }
    var toolbarAtTop by remember(surface) { mutableStateOf(true) }
    val picking by surface.selectionPicking.collectAsState()
    val copiedMessage = stringResource(UiRes.string.oc_copied)
    val copyFailedMessage = stringResource(Res.string.terminal_copy_failed)
    val startLabel = stringResource(Res.string.terminal_selection_start)
    val endLabel = stringResource(Res.string.terminal_selection_end)
    val positionLabel = stringResource(Res.string.terminal_selection_position)
    LaunchedEffect(surface, colors) { surface.setTheme(colors.xtermTheme()) }
    LaunchedEffect(surface, ime, density) {
        snapshotFlow { ime.getBottom(density) > 0 }.distinctUntilChanged().collect(surface::setKeyboardVisible)
    }
    LaunchedEffect(surface, startLabel, endLabel, positionLabel) { surface.setSelectionLabels(startLabel, endLabel, positionLabel) }
    OcBackHandler(enabled = selecting || picking) { surface.clearSelection() }
    val font by produceState(TerminalFont.cached) { value = TerminalFont.load() }
    Box(modifier) {
        font?.let {
            PlatformTerminalView(
                surface, TerminalPage(Res.getUri(PAGE), it), openKeyboard,
                onSelectionChanged = { active, atTop ->
                    selecting = active
                    toolbarAtTop = atTop
                },
                onCopy = { text ->
                    scope.launch {
                        try {
                            clipboard.setText(text)
                            surface.clearSelection()
                            snackbarHostState.showSnackbar(copiedMessage)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar(copyFailedMessage)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (picking) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(OcSpacing.sm).widthIn(max = 380.dp)
                    .testTag("terminal_selection_hint"),
                color = colors.layer02,
                contentColor = colors.textBase,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, colors.borderBase),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.terminal_selection_tap_word),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f, fill = false).padding(OcSpacing.md)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    OcIconButton(
                        icon = Icons.Outlined.Close, contentDescription = stringResource(UiRes.string.oc_close),
                        onClick = surface::clearSelection, modifier = Modifier.testTag("terminal_selection_hint_close"),
                    )
                }
            }
        } else if (selecting) {
            Surface(
                modifier = Modifier.align(if (toolbarAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(OcSpacing.sm).widthIn(max = 380.dp).testTag("terminal_selection_toolbar"),
                color = colors.layer02,
                contentColor = colors.textBase,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, colors.borderBase),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OcButton(
                        text = stringResource(UiRes.string.oc_copy), onClick = surface::copySelection,
                        modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp).testTag("terminal_selection_copy"),
                        variant = OcButtonVariant.Ghost,
                    )
                    OcButton(
                        text = stringResource(Res.string.terminal_select_all), onClick = surface::selectAll,
                        modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp).testTag("terminal_selection_all"),
                        variant = OcButtonVariant.Ghost,
                    )
                    OcIconButton(
                        icon = Icons.Outlined.Close, contentDescription = stringResource(UiRes.string.oc_close),
                        onClick = surface::clearSelection, modifier = Modifier.testTag("terminal_selection_close"),
                    )
                }
            }
        }
    }
}

/** The platform web view showing [page]; [openKeyboard] is the setting to show the keyboard on entry. */
@Composable
internal expect fun PlatformTerminalView(
    surface: WebSurface,
    page: TerminalPage,
    openKeyboard: Boolean,
    onSelectionChanged: (active: Boolean, toolbarAtTop: Boolean) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier,
)

private const val PAGE = "files/terminal/index.html"

private object TerminalFont {
    var cached: String? = null
        private set

    suspend fun load(): String = cached ?: try {
        Base64.encode(UiRes.readBytes("font/plex_mono_regular.ttf"))
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // The page falls back to the system monospace font.
        ""
    }.also { cached = it }
}

internal fun OcColors.xtermTheme(): String {
    fun css(color: Color, alpha: String = "") =
        js("#" + (color.toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0') + alpha)
    val names = listOf(
        "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white",
        "brightBlack", "brightRed", "brightGreen", "brightYellow", "brightBlue", "brightMagenta", "brightCyan", "brightWhite",
    )
    val ansi = names.zip(terminalAnsi).joinToString(",") { (name, color) -> "$name:${css(color)}" }
    return "{background:${css(bgDeep)},foreground:${css(textBase)},cursor:${css(textBase)}," +
        "selectionBackground:${css(bgAccent, alpha = "66")},$ansi}"
}
