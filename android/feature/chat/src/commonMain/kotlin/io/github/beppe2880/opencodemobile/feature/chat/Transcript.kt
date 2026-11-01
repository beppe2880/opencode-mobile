package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcRoundIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSkeleton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.markdown.MarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_action_copy
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_action_fork
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_action_fork_hint
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_action_revert
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_action_revert_hint
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_jump_latest
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_message_actions
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_new_messages
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_older_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_older_retry
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Rows from the oldest loaded one at which the next older page is requested. */
private const val LOAD_OLDER_WITHIN = 8

/**
 * The scrolling transcript: follows new content while at the bottom, restores the saved
 * reading position, loads older pages near the top and offers the jump-to-latest button.
 */
@Composable
internal fun Transcript(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onCopy: (String) -> Unit,
    listState: LazyListState,
    cache: MarkdownCache,
) {
    val currentRows by rememberUpdatedState(state.items)
    val historyHeader = if (state.loadingOlder || state.olderFailed) 1 else 0
    val currentHeader by rememberUpdatedState(historyHeader)
    val scope = rememberCoroutineScope()
    // Where the user left this chat; until it is back on screen nothing follows new messages.
    val saved = remember(state.sessionId) { state.savedScroll }
    var restored by remember(state.sessionId) { mutableStateOf(false) }
    var stickToBottom by remember(state.sessionId) { mutableStateOf(saved == null) }
    // Set when content arrives while the user reads older messages; cleared at the bottom.
    var hasNew by remember(state.sessionId) { mutableStateOf(false) }
    var actionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(listState, state.sessionId, state.items, state.synced, historyHeader) {
        if (restored || (state.items.isEmpty() && !state.synced)) return@LaunchedEffect
        val index = state.items.indexOfFirst { it.key == saved?.key }
        when {
            saved != null && index >= 0 -> {
                listState.scrollToItem(index + historyHeader, saved.offset)
                restored = true
            }
            // The row is gone (reverted, compacted, beyond the cache): start from the bottom.
            saved == null || state.synced -> {
                stickToBottom = true
                listState.scrollToLatest()
                restored = true
            }
        }
    }
    LaunchedEffect(listState, state.sessionId) {
        snapshotFlow { listState.isScrollInProgress to !listState.canScrollForward }
            .collect { (scrolling, atBottom) ->
                if (!restored) return@collect
                if (scrolling && !atBottom) stickToBottom = false
                if (!scrolling && atBottom) {
                    stickToBottom = true
                    hasNew = false
                }
            }
    }
    // Detach on the gesture itself, before another streamed update can scroll the list.
    val readingScroll = remember(listState, state.sessionId) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) stickToBottom = false
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(listState, state.sessionId) {
        snapshotFlow {
            if (!restored || !stickToBottom || listState.isScrollInProgress) null
            else {
                val info = listState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()
                // Observe measured growth too: tokens, tool output, Markdown reflow and IME.
                Triple(info.totalItemsCount, last?.let { it.offset + it.size }, info.viewportEndOffset)
            }
        }.collect { end -> if (end != null && end.first > 0) listState.scrollToLatest() }
    }
    LaunchedEffect(listState, state.sessionId) {
        snapshotFlow {
            if (!restored || !listState.canScrollForward) null
            else currentRows.getOrNull(listState.firstVisibleItemIndex - currentHeader)?.let {
                ScrollMemory.Anchor(it.key, listState.firstVisibleItemScrollOffset)
            }
        }
            .collect { anchor ->
                if (!restored) return@collect
                onEvent(ChatEvent.ScrollMoved(anchor))
            }
    }
    // Older history is fetched only when the user gets near the oldest loaded row.
    LaunchedEffect(listState, state.synced) {
        snapshotFlow {
            val info = listState.layoutInfo
            restored && info.totalItemsCount > 0 && listState.firstVisibleItemIndex <= LOAD_OLDER_WITHIN
        }
            .distinctUntilChanged()
            .collect { nearTop -> if (nearTop) onEvent(ChatEvent.LoadOlder) }
    }
    val newest = state.items.lastOrNull { it !is ChatItem.Working }
    // Newest row once the server transcript is in; swapping cached rows for it is not news.
    var seenNewest by remember(state.sessionId) { mutableStateOf<ChatItem?>(null) }
    LaunchedEffect(state.sessionId, newest, state.synced, restored) {
        if (!restored) return@LaunchedEffect
        if (!stickToBottom && state.synced && newest != null) {
            if (seenNewest != null && newest != seenNewest) hasNew = true
        }
        if (state.synced) seenNewest = newest
    }
    // While the agent works, its newest tool group stays open between one tool and the next.
    val liveGroupKey = remember(state.items, state.running, state.live) {
        if (!state.running || state.live == null) null
        else state.items.lastOrNull { it !is ChatItem.Working }?.takeIf { it is ChatItem.Tools || it is ChatItem.Activity }?.key
    }
    // Groups followed live in this visit; with the setting on they stay open once the answer ends.
    val followed = remember(state.sessionId) { mutableStateSetOf<String>() }
    LaunchedEffect(liveGroupKey) { liveGroupKey?.let(followed::add) }
    val keptOpen: (String) -> Boolean = { key -> state.keepLiveStepsOpen && key != liveGroupKey && key in followed }
    TranscriptLocals(state) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                // Top anchoring keeps the paragraph being read still when this same row grows.
                modifier = Modifier.fillMaxSize().nestedScroll(readingScroll).testTag("chat_transcript"),
                contentPadding = PaddingValues(vertical = OcSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.md, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                olderHistory(state, onEvent)
                items(state.items, key = { it.key }, contentType = { it::class }) { item ->
                    TranscriptItem(
                        item = item,
                        state = state,
                        onEvent = onEvent,
                        onCopy = onCopy,
                        cache = cache,
                        modifier = Modifier.widthIn(max = OcSpacing.contentMaxWidth).fillMaxWidth().padding(horizontal = OcSpacing.gutter),
                        liveGroupKey = liveGroupKey,
                        keptOpen = keptOpen,
                        onUserActions = { actionsFor = it },
                    )
                }
            }
            AnimatedVisibility(
                visible = !stickToBottom,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = OcSpacing.sm),
            ) {
                val jump = {
                    stickToBottom = false
                    hasNew = false
                    scope.launch {
                        val last = listState.layoutInfo.totalItemsCount - 1
                        if (last >= 0) listState.animateScrollToItem(last, Int.MAX_VALUE)
                        stickToBottom = true
                        hasNew = false
                    }
                    Unit
                }
                if (hasNew) {
                    OcChip(
                        text = stringResource(Res.string.chat_new_messages),
                        onClick = jump,
                        selected = true,
                        leadingIcon = Icons.Outlined.ArrowDownward,
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(OcRadius.md))
                            .testTag("chat_new_messages_button"),
                    )
                } else {
                    OcRoundIconButton(
                        icon = Icons.Outlined.ArrowDownward,
                        contentDescription = stringResource(Res.string.chat_jump_latest),
                        onClick = jump,
                        container = OcTheme.colors.bgBase,
                        content = OcTheme.colors.iconBase,
                        modifier = Modifier
                            .background(OcTheme.colors.bgBase, RoundedCornerShape(50))
                            .testTag("chat_jump_latest_button"),
                    )
                }
            }
        }
    }
    val selected = actionsFor?.let { key -> state.items.firstOrNull { it.key == key } as? ChatItem.User }
    if (selected != null) {
        MessageActionsSheet(
            onRevert = {
                actionsFor = null
                onEvent(ChatEvent.Revert(selected.key))
            },
            onFork = {
                actionsFor = null
                onEvent(ChatEvent.Fork(selected.key))
            },
            onCopy = {
                actionsFor = null
                onCopy(selected.text)
            },
            onDismiss = { actionsFor = null },
        )
    }
}

/** A positive offset beyond the last row is clamped to the actual bottom, including padding. */
internal suspend fun LazyListState.scrollToLatest() {
    val last = layoutInfo.totalItemsCount - 1
    if (last >= 0) scrollToItem(last, Int.MAX_VALUE)
}

/** The chat text size and the tool output length of the settings, for transcript rows. */
@Composable
internal fun TranscriptLocals(state: ChatState, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * state.chatTextScale),
        LocalOutputLines provides state.outputPreviewLines,
        content = content,
    )
}

/**
 * One transcript row as the settings in [state] show it. [liveGroupKey] is the group followed
 * live, [keptOpen] tells the groups kept open after it; [onUserActions] opens a prompt's actions.
 */
@Composable
internal fun TranscriptItem(
    item: ChatItem,
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onCopy: (String) -> Unit,
    cache: MarkdownCache,
    modifier: Modifier = Modifier,
    liveGroupKey: String? = null,
    keptOpen: (String) -> Boolean = { false },
    onUserActions: ((String) -> Unit)? = null,
) {
    val defaultToolsExpanded = state.expandToolsByDefault
    val isExpanded: (String) -> Boolean = { key -> state.isExpanded(key) }
    val onToggle: (String) -> Unit = { key -> onEvent(ChatEvent.Toggle(key)) }
    when (item) {
        is ChatItem.User -> UserBubble(
            item = item,
            modifier = modifier,
            onLongPress = if (item.pending || onUserActions == null) null else { { onUserActions(item.key) } },
            onOpenImage = { onEvent(ChatEvent.OpenImage(item.key, it)) },
            directory = state.directory,
            onOpenFile = { onEvent(ChatEvent.OpenFile(it)) },
        )
        is ChatItem.Text -> AssistantText(item, cache, modifier)
        is ChatItem.Reasoning -> ReasoningBlock(
            item = item,
            expanded = state.isExpanded("r:${item.key}"),
            onToggle = { onToggle("r:${item.key}") },
            modifier = modifier,
            live = state.live,
        )
        is ChatItem.Thoughts -> ThoughtsGroup(
            item = item,
            expanded = state.isExpanded("t:${item.key}"),
            isExpanded = isExpanded,
            onToggle = onToggle,
            modifier = modifier,
            live = state.live,
        )
        is ChatItem.Tools -> ToolsGroup(
            item = item,
            directory = state.directory,
            expanded = state.isExpanded("g:${item.key}", defaultToolsExpanded || keptOpen(item.key)),
            isExpanded = isExpanded,
            onToggle = onToggle,
            onOpenSession = { onEvent(ChatEvent.OpenSession(it)) },
            onOpenFile = { onEvent(ChatEvent.OpenFile(it)) },
            modifier = modifier,
            live = state.live,
            fullCommands = state.showFullCommands,
            inLiveGroup = item.key == liveGroupKey,
            details = state.showToolDetails || keptOpen(item.key),
        )
        is ChatItem.Activity -> ActivityGroup(
            item = item,
            directory = state.directory,
            expanded = state.isExpanded("a:${item.key}", defaultToolsExpanded || keptOpen(item.key)),
            inLiveGroup = item.key == liveGroupKey,
            details = state.showToolDetails || keptOpen(item.key),
            defaultToolsExpanded = defaultToolsExpanded,
            isExpanded = isExpanded,
            onToggle = onToggle,
            onOpenSession = { onEvent(ChatEvent.OpenSession(it)) },
            onOpenFile = { onEvent(ChatEvent.OpenFile(it)) },
            modifier = modifier,
            live = state.live,
            fullCommands = state.showFullCommands,
        )
        is ChatItem.Error -> ErrorRow(item, modifier)
        is ChatItem.Notice -> NoticeRow(
            item = item,
            cache = cache,
            expanded = state.isExpanded("n:${item.key}"),
            onToggle = { onToggle("n:${item.key}") },
            modifier = modifier,
        )
        is ChatItem.Shell -> ShellBlock(item, modifier)
        is ChatItem.Footer -> TurnFooter(item, onCopy, modifier.padding(bottom = OcSpacing.md), showCost = state.showCost)
        is ChatItem.Working -> WorkingRow(modifier)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.olderHistory(state: ChatState, onEvent: (ChatEvent) -> Unit) {
    if (state.loadingOlder) {
        item(key = "loading-older") {
            Box(Modifier.fillMaxWidth().padding(vertical = OcSpacing.sm).testTag("chat_loading_older"), contentAlignment = Alignment.Center) {
                OcSpinner(size = 16.dp)
            }
        }
    } else if (state.olderFailed) {
        item(key = "older-failed") {
            Column(
                Modifier.fillMaxWidth().padding(vertical = OcSpacing.sm).testTag("chat_older_failed"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                Text(stringResource(Res.string.chat_older_failed), style = MaterialTheme.typography.bodySmall, color = OcTheme.colors.textMuted)
                OcChip(
                    text = stringResource(Res.string.chat_older_retry),
                    onClick = { onEvent(ChatEvent.LoadOlder) },
                    modifier = Modifier.testTag("chat_older_retry_button"),
                )
            }
        }
    }
}

@Composable
private fun MessageActionsSheet(onRevert: () -> Unit, onFork: () -> Unit, onCopy: () -> Unit, onDismiss: () -> Unit) {
    val c = OcTheme.colors
    OcBottomSheet(onDismiss = onDismiss, testTag = "chat_message_actions") {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = OcSpacing.md)) {
            Text(
                stringResource(Res.string.chat_message_actions),
                style = MaterialTheme.typography.titleSmall,
                color = c.textBase,
                modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
            )
            OcListRow(
                title = stringResource(Res.string.chat_action_revert),
                subtitle = stringResource(Res.string.chat_action_revert_hint),
                onClick = onRevert,
                leading = { Icon(Icons.AutoMirrored.Outlined.Undo, null, tint = c.iconBase) },
                modifier = Modifier.testTag("chat_action_revert"),
            )
            OcListRow(
                title = stringResource(Res.string.chat_action_fork),
                subtitle = stringResource(Res.string.chat_action_fork_hint),
                onClick = onFork,
                leading = { Icon(Icons.AutoMirrored.Outlined.CallSplit, null, tint = c.iconBase) },
                modifier = Modifier.testTag("chat_action_fork"),
            )
            OcListRow(
                title = stringResource(Res.string.chat_action_copy),
                onClick = onCopy,
                leading = { Icon(Icons.Outlined.ContentCopy, null, tint = c.iconBase) },
                modifier = Modifier.testTag("chat_action_copy"),
            )
        }
    }
}

@Composable
internal fun TranscriptSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().padding(OcSpacing.gutter).testTag("chat_loading"),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.lg, Alignment.Bottom),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            OcSkeleton(Modifier.widthIn(max = 240.dp).fillMaxWidth(0.7f).height(56.dp), RoundedCornerShape(OcRadius.lg))
        }
        OcSkeleton(Modifier.fillMaxWidth(0.4f).height(12.dp))
        OcSkeleton(Modifier.fillMaxWidth().height(12.dp))
        OcSkeleton(Modifier.fillMaxWidth(0.85f).height(12.dp))
        OcSkeleton(Modifier.fillMaxWidth(0.6f).height(12.dp))
    }
}
