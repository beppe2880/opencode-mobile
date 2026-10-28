package io.github.beppe2880.opencodemobile.feature.connect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcLogo
import io.github.beppe2880.opencodemobile.core.ui.components.OcPasswordField
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextField
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_error_pairing
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.connect.resources.Res
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_active
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_add_title
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_code_label
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_discover_again
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_discovered_hint
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_discovered_title
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_discovering
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_hint_body
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_hint_command
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_hint_title
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_manual
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_method_code
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_method_password
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_password_label
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_paste
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_probe_needs_pairing
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_probe_ok
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_saved_servers
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_scan
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_submit
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_subtitle
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_test
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_title
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_try_demo
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_url_label
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_url_placeholder
import io.github.beppe2880.opencodemobile.feature.connect.resources.connect_warning_public_http
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
fun ConnectScreen(
    state: ConnectState,
    onEvent: (ConnectEvent) -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onTryDemo: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    secretFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    val c = OcTheme.colors
    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).imePadding()) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = OcSpacing.xl, vertical = OcSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.lg),
            ) {
                if (showBack) {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiRes.string.oc_back),
                        onClick = onBack,
                        modifier = Modifier.padding(start = 0.dp).testTag("connect_back_button"),
                    )
                }
                Hero(addMode = showBack)
                if (onTryDemo != null) {
                    OcButton(
                        text = stringResource(Res.string.connect_try_demo),
                        onClick = onTryDemo,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().testTag("connect_try_demo"),
                    )
                }
                if (state.discovering || state.newDiscovered.isNotEmpty()) {
                    DiscoveredServers(
                        urls = state.newDiscovered,
                        discovering = state.discovering,
                        enabled = !state.busy,
                        onSelect = { onEvent(ConnectEvent.UseDiscovered(it)) },
                        onRescan = { onEvent(ConnectEvent.Rediscover) },
                    )
                }
                QuickActions(onScan = { onEvent(ConnectEvent.OpenScanner) }, onPaste = onPaste, busy = state.busy)
                PairHint()
                ManualSection(state = state, onEvent = onEvent, secretFocusRequester = secretFocusRequester)
                if (state.servers.isNotEmpty()) {
                    SavedServers(state.servers, state.activeServerId, onSelect = { onEvent(ConnectEvent.SelectServer(it)) })
                }
            }
        }
        if (state.scannerOpen) {
            QrScannerOverlay(
                onResult = { onEvent(ConnectEvent.QrScanned(it)) },
                onClose = { onEvent(ConnectEvent.CloseScanner) },
            )
        }
    }
}

@Composable
private fun Hero(addMode: Boolean) {
    val c = OcTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = if (addMode) 0.dp else OcSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        OcLogo(size = 48.dp)
        Text(
            text = stringResource(if (addMode) Res.string.connect_add_title else Res.string.connect_title),
            style = MaterialTheme.typography.headlineSmall,
            color = c.textBase,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.connect_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textMuted,
        )
    }
}

@Composable
private fun DiscoveredServers(
    urls: List<String>,
    discovering: Boolean,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    onRescan: () -> Unit,
) {
    val c = OcTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("connect_discovered"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.connect_discovered_title),
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted,
                modifier = Modifier.weight(1f),
            )
            if (discovering) {
                OcSpinner()
            } else {
                OcIconButton(
                    icon = Icons.Outlined.Refresh,
                    contentDescription = stringResource(Res.string.connect_discover_again),
                    onClick = onRescan,
                    modifier = Modifier.testTag("connect_discover_again"),
                )
            }
        }
        OcCard {
            if (urls.isEmpty()) {
                Text(
                    text = stringResource(Res.string.connect_discovering),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.padding(OcSpacing.md),
                )
            }
            urls.forEachIndexed { index, url ->
                if (index > 0) OcDivider()
                OcListRow(
                    title = url.substringAfter("://"),
                    subtitle = stringResource(Res.string.connect_discovered_hint),
                    onClick = if (enabled) {
                        { onSelect(url) }
                    } else {
                        null
                    },
                    leading = { Icon(Icons.Outlined.Wifi, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("connect_discovered_${url.substringAfter("://")}"),
                )
            }
        }
    }
}

@Composable
private fun QuickActions(onScan: () -> Unit, onPaste: () -> Unit, busy: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        OcButton(
            text = stringResource(Res.string.connect_scan),
            onClick = onScan,
            enabled = !busy,
            leadingIcon = Icons.Outlined.QrCodeScanner,
            variant = OcButtonVariant.Inverse,
            modifier = Modifier.fillMaxWidth().testTag("connect_scan_button"),
        )
        OcButton(
            text = stringResource(Res.string.connect_paste),
            onClick = onPaste,
            enabled = !busy,
            leadingIcon = Icons.Outlined.ContentPaste,
            variant = OcButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth().testTag("connect_paste_button"),
        )
    }
}

/** Stands in for the command in connect_hint_body so it can be styled. */
private const val COMMAND_SLOT = "\u0000"

@Composable
private fun PairHint() {
    val c = OcTheme.colors
    OcCard(color = c.layer01) {
        Row(
            modifier = Modifier.padding(OcSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            Icon(Icons.Outlined.Terminal, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                Text(stringResource(Res.string.connect_hint_title), style = MaterialTheme.typography.titleSmall, color = c.textBase)
                // Non-breaking spaces keep the command on one line.
                val command = stringResource(Res.string.connect_hint_command).replace(' ', '\u00A0')
                val (before, after) = stringResource(Res.string.connect_hint_body, COMMAND_SLOT).split(COMMAND_SLOT, limit = 2)
                    .let { it.first() to it.getOrElse(1) { "" } }
                val commandStyle = SpanStyle(color = c.textAccent, fontFamily = OcTheme.mono.code.fontFamily, fontWeight = FontWeight.Medium)
                Text(
                    text = buildAnnotatedString {
                        append(before)
                        withStyle(commandStyle) { append(command) }
                        append(after)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.testTag("connect_hint_body"),
                )
            }
        }
    }
}

@Composable
private fun ManualSection(state: ConnectState, onEvent: (ConnectEvent) -> Unit, secretFocusRequester: FocusRequester) {
    val c = OcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
            OcDivider(Modifier.weight(1f))
            Text(stringResource(Res.string.connect_manual), style = MaterialTheme.typography.labelMedium, color = c.textFaint)
            OcDivider(Modifier.weight(1f))
        }
        OcTextField(
            value = state.url,
            onValueChange = { onEvent(ConnectEvent.UrlChanged(it)) },
            label = stringResource(Res.string.connect_url_label),
            placeholder = stringResource(Res.string.connect_url_placeholder),
            error = state.urlError?.resolve(),
            mono = true,
            enabled = !state.submitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onNext = { secretFocusRequester.requestFocus() }),
            modifier = Modifier.testTag("connect_url_field"),
        )
        val publicCleartext = remember(state.url) { isPublicCleartext(state.url) }
        AnimatedVisibility(visible = publicCleartext) {
            Row(
                modifier = Modifier.fillMaxWidth().testTag("connect_cleartext_warning"),
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = c.warning.fg, modifier = Modifier.size(16.dp))
                Text(stringResource(Res.string.connect_warning_public_http), style = MaterialTheme.typography.bodySmall, color = c.warning.fg)
            }
        }
        OcSegmentedControl(
            segments = listOf(
                OcSegment(AuthMethod.PairingCode, stringResource(Res.string.connect_method_code), "connect_method_code"),
                OcSegment(AuthMethod.Password, stringResource(Res.string.connect_method_password), "connect_method_password"),
            ),
            selected = state.method,
            onSelect = { onEvent(ConnectEvent.MethodChanged(it)) },
        )
        val submit = KeyboardActions(onDone = { onEvent(ConnectEvent.Submit) })
        when (state.method) {
            AuthMethod.PairingCode -> OcTextField(
                value = state.secret,
                onValueChange = { onEvent(ConnectEvent.SecretChanged(it)) },
                label = stringResource(Res.string.connect_code_label),
                error = state.secretError?.resolve(),
                mono = true,
                enabled = !state.submitting,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = submit,
                modifier = Modifier.focusRequester(secretFocusRequester).testTag("connect_code_field"),
            )
            AuthMethod.Password -> OcPasswordField(
                value = state.secret,
                onValueChange = { onEvent(ConnectEvent.SecretChanged(it)) },
                label = stringResource(Res.string.connect_password_label),
                error = state.secretError?.resolve(),
                keyboardActions = submit,
                modifier = Modifier.focusRequester(secretFocusRequester).testTag("connect_password_field"),
            )
        }
        ProbeLine(state)
        Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            OcButton(
                text = stringResource(Res.string.connect_test),
                onClick = { onEvent(ConnectEvent.TestConnection) },
                variant = OcButtonVariant.Secondary,
                loading = state.probing,
                enabled = !state.submitting && state.url.isNotBlank(),
                modifier = Modifier.weight(1f).testTag("connect_test_button"),
            )
            OcButton(
                text = stringResource(Res.string.connect_submit),
                onClick = { onEvent(ConnectEvent.Submit) },
                loading = state.submitting,
                enabled = state.canSubmit,
                modifier = Modifier.weight(1f).testTag("connect_submit_button"),
            )
        }
    }
}

@Composable
private fun ProbeLine(state: ConnectState) {
    val c = OcTheme.colors
    val (text, color, icon) = when {
        state.submitError != null -> Triple(state.submitError.resolve(), c.danger.fg, Icons.Outlined.ErrorOutline)
        state.probe is ProbeResult.Reachable ->
            Triple(stringResource(Res.string.connect_probe_ok, state.probe.version), c.success.fg, Icons.Outlined.CheckCircle)
        state.probe is ProbeResult.NeedsPairing -> Triple(stringResource(Res.string.connect_probe_needs_pairing), c.success.fg, Icons.Outlined.CheckCircle)
        state.probe is ProbeResult.Failed -> Triple(state.probe.message.resolve(), c.danger.fg, Icons.Outlined.ErrorOutline)
        else -> Triple(null, c.textMuted, null)
    }
    AnimatedVisibility(visible = text != null || state.probing) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag("connect_probe_result"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            if (state.probing) OcSpinner() else if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Text(text.orEmpty(), style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
private fun SavedServers(servers: List<ServerProfile>, activeId: String?, onSelect: (String) -> Unit) {
    val c = OcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        Text(stringResource(Res.string.connect_saved_servers), style = MaterialTheme.typography.labelMedium, color = c.textMuted)
        OcCard {
            servers.forEachIndexed { index, server ->
                if (index > 0) OcDivider()
                OcListRow(
                    title = server.name,
                    subtitle = server.baseUrl,
                    monoSubtitle = true,
                    onClick = { onSelect(server.id) },
                    leading = { OcProjectAvatar(server.name, size = 28.dp) },
                    trailing = if (server.id == activeId) {
                        { OcBadge(stringResource(Res.string.connect_active), c.success) }
                    } else {
                        null
                    },
                    modifier = Modifier.testTag("connect_server_${server.id}"),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ConnectScreenPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        ConnectScreen(
            state = ConnectState(
                url = "http://192.168.1.10:4096",
                probe = ProbeResult.Reachable("2.0.27"),
                servers = SampleData.servers,
                activeServerId = SampleData.servers.first().id,
                discovered = listOf("http://192.168.1.42:4096"),
            ),
            onEvent = {},
            onPaste = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConnectScreenErrorPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        ConnectScreen(
            state = ConnectState(
                url = "localhost:4096",
                secret = "abc",
                secretError = io.github.beppe2880.opencodemobile.core.ui.text.UiText.Resource(UiRes.string.oc_error_pairing),
            ),
            onEvent = {},
            onPaste = {},
            showBack = true,
        )
    }
}
