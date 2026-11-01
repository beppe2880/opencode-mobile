package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.components.LocalEditMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcModelPicker
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.editable
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.markdown.MarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_running
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.text.formatInteger
import io.github.beppe2880.opencodemobile.core.ui.text.runningSummary
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
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
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_branch_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_detached
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_detached_short
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_edits_short
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_files_edited
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_files_short
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_vcs_folder_description
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
            Column {
                ChatTopBar(
                    state,
                    showBack,
                    onBack = { onEvent(ChatEvent.Back) },
                    onContext = { contextOpen = true },
                    onChanges = { onEvent(ChatEvent.OpenChanges) },
                    onHeaderChanges = { onEvent(ChatEvent.OpenHeaderChanges) },
                    onTerminal = { onEvent(ChatEvent.OpenTerminal) },
                    onMcps = { onEvent(ChatEvent.OpenMcps) },
                    onRunning = { onEvent(ChatEvent.RunningSheet(true)) },
                )
                if (!state.work.isEmpty) RunningLine(state.work, state.headerStyle, onClick = { onEvent(ChatEvent.RunningSheet(true)) })
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
internal fun ChatTopBar(
    state: ChatState,
    showBack: Boolean,
    onBack: () -> Unit,
    onContext: () -> Unit,
    onChanges: () -> Unit,
    onHeaderChanges: () -> Unit,
    onTerminal: () -> Unit,
    onMcps: () -> Unit,
    onRunning: () -> Unit,
    applyStatusBarInset: Boolean = true,
) {
    val c = OcTheme.colors
    val editMode = LocalEditMode.current
    var menuOpen by remember { mutableStateOf(false) }
    OcTopBar(
        // Blank until the session is known, then a placeholder until the server names it.
        title = state.title.ifBlank { if (state.directory.isEmpty()) " " else stringResource(Res.string.chat_untitled) },
        subtitle = state.directory.takeIf { it.isNotEmpty() },
        subtitleContent = state.vcs?.let { line -> { VcsSubtitle(line, state.headerStyle, onHeaderChanges) } }
            ?: editMode?.takeIf { it.active && state.directory.isNotEmpty() }?.let { mode ->
                // The folder path alone, marked so its setting can bring the branch line back.
                {
                    Text(
                        state.directory,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.editable(SettingsPart.Header, mode),
                    )
                }
            },
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
        applyStatusBarInset = applyStatusBarInset,
    )
}

/**
 * "folder · branch · 3 files changed" under the title, in the [style] of the setting; opens the
 * changes sheet. On narrow screens the folder name shrinks first so the branch stays readable.
 */
@Composable
private fun VcsSubtitle(line: VcsLine, style: ChatHeaderStyle, onClick: () -> Unit) {
    val n = line.files
    val detached = stringResource(Res.string.chat_vcs_detached)
    val strings = VcsStrings(
        detached = detached,
        detachedShort = stringResource(Res.string.chat_vcs_detached_short),
        count = formatInteger(n.toLong()),
        filesShort = pluralStringResource(Res.plurals.chat_vcs_files_short, n, n),
        filesFull = pluralStringResource(Res.plurals.chat_changes_files, n, n),
        editsShort = pluralStringResource(Res.plurals.chat_vcs_edits_short, n, n),
        editsFull = pluralStringResource(Res.plurals.chat_vcs_files_edited, n, n),
        folderDescription = stringResource(Res.string.chat_vcs_folder_description, line.folder.orEmpty()),
        branchDescription = line.branch?.let { stringResource(Res.string.chat_vcs_branch_description, it) } ?: detached,
    )
    val text = line.text(style, strings)
    val description = line.description(strings)
    val textStyle = MaterialTheme.typography.bodySmall
    val color = OcTheme.colors.textMuted
    // Icons follow the text size, font scale included.
    val iconSize = with(LocalDensity.current) { textStyle.fontSize.toDp() }
    val icons = style == ChatHeaderStyle.Icons
    // The 56dp top bar has no room for a 48dp target: the changes button is the full-size alternative.
    val modifier = Modifier
        .editable(SettingsPart.Header, LocalEditMode.current)
        .testTag("chat_vcs_line")
        .clickable(onClickLabel = stringResource(Res.string.chat_changes_open), role = Role.Button, onClick = onClick)
        // One sentence for screen readers instead of the icons and fragments.
        .clearAndSetSemantics { contentDescription = description }
    val part: @Composable (VcsPart) -> Unit = { VcsPartView(it, textStyle, color, iconSize) }
    val rest: @Composable () -> Unit = {
        if (icons) {
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) { text.rest.forEach { part(it) } }
        } else {
            part(VcsPart(null, text.rest.joinToString(SEPARATOR) { it.text }))
        }
    }
    val folder = text.folder
    if (folder == null) {
        Box(modifier) { rest() }
        return
    }
    ShrinkFirstRow(
        first = { part(folder) },
        separator = { if (icons) Spacer(Modifier.width(OcSpacing.sm)) else part(VcsPart(null, SEPARATOR)) },
        second = rest,
        firstMinWidth = FOLDER_MIN_WIDTH,
        modifier = modifier,
    )
}

private const val SEPARATOR = " · "

/**
 * "2 subagents · 1 command" under the header, written like the header line ([style]: counts after
 * icons, or words); opens the Running sheet. Like the header line it is a compact target: the
 * "Running" menu item is the full-size alternative.
 */
@Composable
internal fun RunningLine(work: RunningWork, style: ChatHeaderStyle, onClick: () -> Unit) {
    val c = OcTheme.colors
    val textStyle = MaterialTheme.typography.bodySmall
    val iconSize = with(LocalDensity.current) { textStyle.fontSize.toDp() }
    val summary = runningSummary(work)
    Column(Modifier.editable(SettingsPart.RunningLine, LocalEditMode.current)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_running_button")
                .clickable(onClickLabel = stringResource(UiRes.string.oc_running), role = Role.Button, onClick = onClick)
                .clearAndSetSemantics { contentDescription = summary }
                .heightIn(min = 32.dp)
                .padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            OcPulsingDot(c.info.fg)
            if (style == ChatHeaderStyle.Icons) {
                for ((icon, count) in listOf(Icons.Outlined.SmartToy to work.agents.size, Icons.Outlined.Terminal to work.commands.size)) {
                    if (count == 0) continue
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
                        Icon(icon, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(iconSize))
                        Text(formatInteger(count.toLong()), style = textStyle, color = c.textMuted, maxLines = 1)
                    }
                }
            } else {
                Text(summary, style = textStyle, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        OcDivider()
    }
}

@Composable
private fun VcsPartView(part: VcsPart, style: TextStyle, color: Color, iconSize: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
        part.icon?.let { Icon(it.vector, contentDescription = null, tint = color, modifier = Modifier.size(iconSize)) }
        // Measured after the icon, so a narrow part ellipsizes its text and keeps the icon; no
        // wrapping, so "vcs-header" is cut at its end rather than after the hyphen.
        Text(part.text, style = style, color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

private val VcsIcon.vector: ImageVector
    get() = when (this) {
        VcsIcon.Folder -> Icons.Outlined.Folder
        VcsIcon.Branch -> Icons.Outlined.AccountTree
        VcsIcon.Changes -> Icons.Outlined.Difference
        VcsIcon.Edits -> Icons.Outlined.Edit
    }

/** Width the folder (icon and "openc…") keeps, when it is that long; below it the folder is left out. */
private val FOLDER_MIN_WIDTH = 40.dp

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
