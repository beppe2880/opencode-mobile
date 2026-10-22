package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcConfirmDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import kotlinx.coroutines.launch
import io.github.beppe2880.opencodemobile.core.ui.R as UiR

@Composable
fun TerminalScreen(
    state: TerminalState,
    surface: WebSurface,
    onEvent: (TerminalEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = OcTheme.colors
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = state.title.ifEmpty { stringResource(R.string.terminal_title) },
                subtitle = state.directory,
                leading = {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiR.string.oc_back),
                        onClick = { onEvent(TerminalEvent.Back) },
                        modifier = Modifier.testTag("terminal_back_button"),
                    )
                },
                titleAccessory = {
                    if (state.error == null && state.link in Waiting) OcSpinner(Modifier.testTag("terminal_connecting"))
                },
                actions = {
                    OcIconButton(
                        icon = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.terminal_close),
                        onClick = { onEvent(TerminalEvent.AskClose) },
                        modifier = Modifier.testTag("terminal_close_button"),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.error != null) {
                    OcErrorState(
                        title = stringResource(R.string.terminal_error_title),
                        message = state.error.resolve(),
                        onRetry = { onEvent(TerminalEvent.Retry) },
                        retryTestTag = "terminal_retry_button",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    WebTerminalView(surface, Modifier.fillMaxSize().testTag("terminal_view"))
                    LinkBanner(state.link, onRestart = { onEvent(TerminalEvent.Restart) }, Modifier.align(Alignment.BottomCenter))
                }
            }
            KeyBar(
                rows = state.keyRows,
                modifierState = state::modifier,
                onPress = { key ->
                    if (key == BarKey.Paste) {
                        scope.launch {
                            val text = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
                            if (!text.isNullOrEmpty()) onEvent(TerminalEvent.Paste(text.toString()))
                        }
                    } else {
                        onEvent(TerminalEvent.Press(key))
                    }
                },
                onEdit = { onEvent(TerminalEvent.EditKeys(true)) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
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
            title = stringResource(R.string.terminal_close_title),
            text = stringResource(R.string.terminal_close_message),
            confirmLabel = stringResource(R.string.terminal_close_confirm),
            onConfirm = { onEvent(TerminalEvent.ConfirmClose) },
            onDismiss = { onEvent(TerminalEvent.DismissClose) },
            destructive = true,
            testTagPrefix = "terminal_close",
        )
    }
}

private val Waiting = setOf(TerminalLink.Connecting, TerminalLink.Reconnecting)

/** Shown over the bottom of the terminal while it is not live. */
@Composable
private fun LinkBanner(link: TerminalLink, onRestart: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val text = when (link) {
        TerminalLink.Reconnecting -> stringResource(R.string.terminal_reconnecting)
        TerminalLink.Exited -> stringResource(R.string.terminal_exited)
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
                text = stringResource(R.string.terminal_new),
                onClick = onRestart,
                compact = true,
                modifier = Modifier.testTag("terminal_restart_button"),
            )
        }
    }
}
