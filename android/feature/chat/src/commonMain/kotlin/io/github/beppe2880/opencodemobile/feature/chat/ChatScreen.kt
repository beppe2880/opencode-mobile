package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcModelPicker
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.markdown.MarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_running
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.text.runningSummary
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_changes_open
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_empty_message
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_empty_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_error_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_mcps
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_more
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_terminal_open
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_untitled
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
fun ChatScreen(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAttachPhoto: () -> Unit = {},
    onAttachFile: () -> Unit = {},
    voice: VoiceInput? = null,
    onDictate: () -> Unit = {},
    showBack: Boolean = true,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    listState: LazyListState = rememberLazyListState(),
    markdownCache: MarkdownCache = rememberMarkdownCache(),
) {
    val c = OcTheme.colors
    var contextOpen by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        containerColor = c.bgBase,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                ChatTopBar(
                    state,
                    showBack,
                    onBack = { onEvent(ChatEvent.Back) },
                    onContext = { contextOpen = true },
                    onChanges = { onEvent(ChatEvent.OpenChanges) },
                    onTerminal = { onEvent(ChatEvent.OpenTerminal) },
                    onMcps = { onEvent(ChatEvent.OpenMcps) },
                    onRunning = { onEvent(ChatEvent.RunningSheet(true)) },
                )
                if (!state.work.isEmpty || state.running) {
                    TextButton(
                        onClick = { onEvent(ChatEvent.RunningSheet(true)) },
                        modifier = Modifier.fillMaxWidth().testTag("chat_running_button"),
                    ) {
                        Text(runningSummary(state.work, state.running), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Keyed so a chat replaced in place (list + detail) starts without a leftover drag.
            key(state.sessionId) {
                SessionSwipe(
                    previous = state.previousSession,
                    next = state.nextSession,
                    enabled = state.swipeEnabled,
                    onSwitch = { onEvent(ChatEvent.SwitchSession(it)) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                    when {
                        state.loading -> TranscriptSkeleton()
                        state.error != null -> OcErrorState(
                            title = stringResource(Res.string.chat_error_title),
                            message = state.error.resolve(),
                            onRetry = { onEvent(ChatEvent.Retry) },
                            retryTestTag = "chat_retry_button",
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.items.isEmpty() -> OcEmptyState(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = stringResource(Res.string.chat_empty_title),
                            message = stringResource(Res.string.chat_empty_message, state.directory.ifEmpty { "~" }),
                            modifier = Modifier.align(Alignment.Center),
                        )
                        else -> Transcript(state, onEvent, onCopy, listState, markdownCache)
                    }
                }
            }
            BottomArea(state, onEvent, onAttachPhoto, onAttachFile, voice, onDictate)
        }
    }
    if (state.modelSheetOpen) {
        OcBottomSheet(onDismiss = { onEvent(ChatEvent.ModelSheet(false)) }, testTag = "chat_model_sheet") {
            OcModelPicker(
                models = state.models,
                selected = state.model,
                recent = state.recentModels,
                onSelect = { onEvent(ChatEvent.SelectModel(it)) },
                onSelectVariant = { onEvent(ChatEvent.SelectVariant(it)) },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        }
    }
    state.preview?.let { preview ->
        ImagePreviewDialog(preview, onDismiss = { onEvent(ChatEvent.ClosePreview) })
    }
    state.changes?.let { ChangesSheet(state, it, onEvent) }
    state.mcps?.let { McpSheet(state.directory, it, onEvent) }
    if (state.workOpen) RunningSheet(state, onEvent)
    if (contextOpen) {
        ContextSheet(
            context = state.context,
            compacting = state.compacting,
            onCompact = { onEvent(ChatEvent.Compact) },
            onDismiss = { contextOpen = false },
        )
    }
}

@Composable
private fun ChatTopBar(
    state: ChatState,
    showBack: Boolean,
    onBack: () -> Unit,
    onContext: () -> Unit,
    onChanges: () -> Unit,
    onTerminal: () -> Unit,
    onMcps: () -> Unit,
    onRunning: () -> Unit,
) {
    val c = OcTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    OcTopBar(
        // Blank until the session is known, then a placeholder until the server names it.
        title = state.title.ifBlank { if (state.directory.isEmpty()) " " else stringResource(Res.string.chat_untitled) },
        subtitle = state.directory.takeIf { it.isNotEmpty() },
        leading = if (showBack) {
            {
                OcIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(UiRes.string.oc_back),
                    onClick = onBack,
                    modifier = Modifier.testTag("chat_back_button"),
                )
            }
        } else {
            null
        },
        titleAccessory = {
            if (state.running) OcPulsingDot(c.agents.forAgent(state.agent), Modifier.testTag("chat_running_indicator"))
        },
        actions = {
            state.context?.let { ContextIndicator(it, state.compacting, onClick = onContext) }
            OcIconButton(
                icon = Icons.Outlined.Difference,
                contentDescription = stringResource(Res.string.chat_changes_open),
                onClick = onChanges,
                modifier = Modifier.testTag("chat_changes_button"),
            )
            OcIconButton(
                icon = Icons.Outlined.Terminal,
                contentDescription = stringResource(Res.string.chat_terminal_open),
                onClick = onTerminal,
                enabled = state.directory.isNotEmpty(),
                modifier = Modifier.testTag("chat_terminal_button"),
            )
            Box {
                OcIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(Res.string.chat_more),
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("chat_more_button"),
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = c.bgBase,
                    modifier = Modifier.exposeTestTags(),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(UiRes.string.oc_running)) },
                        onClick = {
                            menuOpen = false
                            onRunning()
                        },
                        modifier = Modifier.testTag("chat_running_menu_item"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.chat_mcps)) },
                        onClick = {
                            menuOpen = false
                            onMcps()
                        },
                        enabled = state.directory.isNotEmpty(),
                        modifier = Modifier.testTag("chat_mcps_menu_item"),
                    )
                }
            }
        },
    )
}

private fun previewState(running: Boolean = false) = ChatState(
    sessionId = "ses_1",
    title = "Native Android client for OpenCode",
    directory = "/home/user/projects/opencode-mobile",
    agent = "build",
    model = SampleData.haiku,
    models = SampleData.models,
    agents = SampleData.agents.filter { !it.hidden },
    items = ChatItemsBuilder.build(SampleData.transcript, running, showReasoning = true, SampleData.models::labelFor),
    loading = false,
    running = running,
    draft = if (running) "Also add tests" else "",
    permissions = if (running) listOf(SampleData.permission) else emptyList(),
)

@PreviewLightDark
@Composable
private fun ChatScreenPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        ChatScreen(state = previewState(), onEvent = {}, onCopy = {})
    }
}

@PreviewLightDark
@Composable
private fun ChatScreenRunningPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        ChatScreen(state = previewState(running = true), onEvent = {}, onCopy = {})
    }
}
