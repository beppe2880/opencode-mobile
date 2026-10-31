package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_changes_files
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_changes_open
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_empty_message
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_empty_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_error_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_mcps
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_more
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_terminal_open
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_untitled
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_detached
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_files_edited
import org.jetbrains.compose.resources.pluralStringResource
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
            ChatTopBar(
                state,
                showBack,
                onBack = { onEvent(ChatEvent.Back) },
                onContext = { contextOpen = true },
                onChanges = { onEvent(ChatEvent.OpenChanges) },
                onHeaderChanges = { onEvent(ChatEvent.OpenHeaderChanges) },
                onTerminal = { onEvent(ChatEvent.OpenTerminal) },
                onMcps = { onEvent(ChatEvent.OpenMcps) },
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
    onHeaderChanges: () -> Unit,
    onTerminal: () -> Unit,
    onMcps: () -> Unit,
) {
    val c = OcTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    OcTopBar(
        // Blank until the session is known, then a placeholder until the server names it.
        title = state.title.ifBlank { if (state.directory.isEmpty()) " " else stringResource(Res.string.chat_untitled) },
        subtitle = state.directory.takeIf { it.isNotEmpty() },
        subtitleContent = state.vcs?.let { line -> { VcsSubtitle(line, onHeaderChanges) } },
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

/**
 * "folder · branch · 3 files changed" under the title; opens the changes sheet. On narrow
 * screens the folder name shrinks first so the branch stays readable.
 */
@Composable
private fun VcsSubtitle(line: VcsLine, onClick: () -> Unit) {
    val files = when {
        line.files == 0 -> null
        line.agentEdits -> pluralStringResource(Res.plurals.chat_vcs_files_edited, line.files, line.files)
        else -> pluralStringResource(Res.plurals.chat_changes_files, line.files, line.files)
    }
    val text = line.text(stringResource(Res.string.chat_vcs_detached), files)
    val style = MaterialTheme.typography.bodySmall
    val color = OcTheme.colors.textMuted
    // The 56dp top bar has no room for a 48dp target: the changes button is the full-size alternative.
    val modifier = Modifier
        .clickable(onClickLabel = stringResource(Res.string.chat_changes_open), role = Role.Button, onClick = onClick)
        .testTag("chat_vcs_line")
    val part: @Composable (String) -> Unit = { Text(it, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    if (text.folder == null) {
        Box(modifier) { part(text.detail) }
        return
    }
    ShrinkFirstRow(
        first = { part(text.folder) },
        separator = { part(" · ") },
        second = { part(text.detail) },
        firstMinWidth = FOLDER_MIN_WIDTH,
        modifier = modifier,
    )
}

/** Width the folder name keeps, when it is that long; below it the folder is left out. */
private val FOLDER_MIN_WIDTH = 56.dp

/**
 * [first], [separator] and [second] on one line. When they do not fit, [first] shrinks down to
 * [firstMinWidth] (or its own width when shorter); with even less room it is left out with the
 * separator, and only then [second] shrinks.
 */
@Composable
private fun ShrinkFirstRow(
    first: @Composable () -> Unit,
    separator: @Composable () -> Unit,
    second: @Composable () -> Unit,
    firstMinWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(first, separator, second), modifier = modifier) { (firsts, separators, seconds), constraints ->
        val loose = constraints.copy(minWidth = 0)
        val total = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity
        val height = constraints.maxHeight
        val firstWidth = firsts.single().maxIntrinsicWidth(height)
        val separatorWidth = separators.single().maxIntrinsicWidth(height)
        val secondWidth = seconds.single().maxIntrinsicWidth(height)
        val keepFirst = minOf(firstWidth, firstMinWidth.roundToPx()) + separatorWidth + secondWidth <= total
        val placeables = if (keepFirst) {
            val firstMax = (total - separatorWidth - secondWidth).coerceAtLeast(0)
            listOf(
                firsts.single().measure(loose.copy(maxWidth = minOf(firstWidth, firstMax))),
                separators.single().measure(loose),
                seconds.single().measure(loose),
            )
        } else {
            listOf(seconds.single().measure(loose))
        }
        layout(placeables.sumOf { it.width }, placeables.maxOf { it.height }) {
            var x = 0
            placeables.forEach {
                it.placeRelative(x, 0)
                x += it.width
            }
        }
    }
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
    vcs = VcsLine("opencode-mobile", "feat/vcs-header", files = if (running) 3 else 0),
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
