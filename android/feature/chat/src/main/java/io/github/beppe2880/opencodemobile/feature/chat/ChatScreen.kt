package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.beppe2880.opencodemobile.core.ui.R as UiR
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcModelPicker
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.markdown.MarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

@Composable
fun ChatScreen(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAttachPhoto: () -> Unit = {},
    onAttachFile: () -> Unit = {},
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
            ChatTopBar(
                state,
                showBack,
                onBack = { onEvent(ChatEvent.Back) },
                onContext = { contextOpen = true },
                onChanges = { onEvent(ChatEvent.OpenChanges) },
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
                            title = stringResource(R.string.chat_error_title),
                            message = state.error.resolve(),
                            onRetry = { onEvent(ChatEvent.Retry) },
                            retryTestTag = "chat_retry_button",
                            modifier = Modifier.align(Alignment.Center),
                        )
                        state.items.isEmpty() -> OcEmptyState(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            title = stringResource(R.string.chat_empty_title),
                            message = stringResource(R.string.chat_empty_message, state.directory.ifEmpty { "~" }),
                            modifier = Modifier.align(Alignment.Center),
                        )
                        else -> Transcript(state, onEvent, onCopy, listState, markdownCache)
                    }
                }
            }
            BottomArea(state, onEvent, onAttachPhoto, onAttachFile)
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
private fun ChatTopBar(state: ChatState, showBack: Boolean, onBack: () -> Unit, onContext: () -> Unit, onChanges: () -> Unit) {
    val c = OcTheme.colors
    OcTopBar(
        title = state.title.ifEmpty { " " },
        subtitle = state.directory.takeIf { it.isNotEmpty() },
        leading = if (showBack) {
            {
                OcIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(UiR.string.oc_back),
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
                contentDescription = stringResource(R.string.chat_changes_open),
                onClick = onChanges,
                modifier = Modifier.testTag("chat_changes_button"),
            )
            state.agent?.let { agent ->
                val color = c.agents.forAgent(agent)
                Text(
                    text = agent.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(end = OcSpacing.md)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(OcRadius.sm))
                        .padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xxs)
                        .testTag("chat_agent_chip"),
                )
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
