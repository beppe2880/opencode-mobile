package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.ui.components.LocalEditMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcConfirmDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.editable
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberTextClipboard
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.terminal.resources.Res
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_close
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_close_confirm
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_close_message
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_close_title
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_error_title
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_exited
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_fullscreen
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_new
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_reconnecting
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
fun TerminalScreen(
    state: TerminalState,
    surface: WebSurface,
    onEvent: (TerminalEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = OcTheme.colors
    val clipboard = rememberTextClipboard()
    val scope = rememberCoroutineScope()

    SystemBarsHidden(state.fullscreen)
    OcBackHandler(enabled = state.fullscreen) { onEvent(TerminalEvent.Fullscreen(false)) }

    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (!state.fullscreen) {
                TopBar(state, onEvent)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                // Without the title bar: keep clear of cutouts and of a status bar still shown (iOS).
                .then(if (state.fullscreen) Modifier.windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.statusBars)) else Modifier)
                .imePadding(),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.error != null) {
                    OcErrorState(
                        title = stringResource(Res.string.terminal_error_title),
                        message = state.error.resolve(),
                        onRetry = { onEvent(TerminalEvent.Retry) },
                        retryTestTag = "terminal_retry_button",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    WebTerminalView(surface, state.openKeyboard, Modifier.fillMaxSize().testTag("terminal_view"))
                    LinkBanner(state.link, onRestart = { onEvent(TerminalEvent.Restart) }, Modifier.align(Alignment.BottomCenter))
                }
            }
            KeyBar(
                rows = state.keyRows,
                modifierState = state::modifier,
                onPress = { key ->
                    if (key == BarKey.Paste) {
                        scope.launch {
                            val text = clipboard.getText()
                            if (!text.isNullOrEmpty()) onEvent(TerminalEvent.Paste(text))
                        }
                    } else {
                        onEvent(TerminalEvent.Press(key))
                    }
                },
                onEdit = { onEvent(TerminalEvent.EditKeys(true)) },
                fullscreen = state.fullscreen,
                onExitFullscreen = { onEvent(TerminalEvent.Fullscreen(false)) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).editable(SettingsPart.KeyBar, LocalEditMode.current),
            )
        }
    }

    if (state.editingKeys) {
        KeyBarEditor(
            rows = state.keyRows,
            onSave = { onEvent(TerminalEvent.SaveKeys(it)) },
            onReset = { onEvent(TerminalEvent.ResetKeys) },
            onDismiss = { onEvent(TerminalEvent.EditKeys(false)) },
        )
    }

    if (state.confirmClose) {
        OcConfirmDialog(
            title = stringResource(Res.string.terminal_close_title),
            text = stringResource(Res.string.terminal_close_message),
            confirmLabel = stringResource(Res.string.terminal_close_confirm),
            onConfirm = { onEvent(TerminalEvent.ConfirmClose) },
            onDismiss = { onEvent(TerminalEvent.DismissClose) },
            destructive = true,
            testTagPrefix = "terminal_close",
        )
    }
}

@Composable
private fun TopBar(state: TerminalState, onEvent: (TerminalEvent) -> Unit) {
    OcTopBar(
        title = state.title.ifEmpty { stringResource(Res.string.terminal_title) },
        subtitle = state.directory,
        leading = {
            OcIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(UiRes.string.oc_back),
                onClick = { onEvent(TerminalEvent.Back) },
                modifier = Modifier.testTag("terminal_back_button"),
            )
        },
        titleAccessory = {
            if (state.error == null && state.link in Waiting) OcSpinner(Modifier.testTag("terminal_connecting"))
        },
        actions = {
            OcIconButton(
                icon = Icons.Outlined.Fullscreen,
                contentDescription = stringResource(Res.string.terminal_fullscreen),
                onClick = { onEvent(TerminalEvent.Fullscreen(true)) },
                modifier = Modifier.editable(SettingsPart.TerminalFullscreen, LocalEditMode.current).testTag("terminal_fullscreen_button"),
            )
            OcIconButton(
                icon = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(Res.string.terminal_close),
                onClick = { onEvent(TerminalEvent.AskClose) },
                modifier = Modifier.testTag("terminal_close_button"),
            )
        },
    )
}

private val Waiting = setOf(TerminalLink.Connecting, TerminalLink.Reconnecting)

/** Shown over the bottom of the terminal while it is not live. */
@Composable
private fun LinkBanner(link: TerminalLink, onRestart: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val text = when (link) {
        TerminalLink.Reconnecting -> stringResource(Res.string.terminal_reconnecting)
        TerminalLink.Exited -> stringResource(Res.string.terminal_exited)
        TerminalLink.Connecting, TerminalLink.Connected -> return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.layer02)
            .padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.sm)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("terminal_link_banner"),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (link == TerminalLink.Reconnecting) OcSpinner()
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
        if (link == TerminalLink.Exited) {
            OcButton(
                text = stringResource(Res.string.terminal_new),
                onClick = onRestart,
                compact = true,
                modifier = Modifier.testTag("terminal_restart_button"),
            )
        }
    }
}
