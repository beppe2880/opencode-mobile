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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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
    val reversed = remember(state.items) { state.items.asReversed() }
    val currentRows by rememberUpdatedState(reversed)
    val scope = rememberCoroutineScope()
    // Where the user left this chat; until it is back on screen nothing follows new messages.
    val saved = remember(state.sessionId) { state.savedScroll }
    var restored by remember(state.sessionId) { mutableStateOf(saved == null) }
    var stickToBottom by remember(state.sessionId) { mutableStateOf(saved == null) }
    // Set when content arrives while the user reads older messages; cleared at the bottom.
    var hasNew by remember { mutableStateOf(false) }
    var actionsFor by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(reversed, state.synced) {
        if (restored || saved == null) return@LaunchedEffect
        val index = reversed.indexOfFirst { it.key == saved.key }
        when {
            index >= 0 -> {
                listState.scrollToItem(index, saved.offset)
                restored = true
            }
            // The row is gone (reverted, compacted, beyond the cache): start from the bottom.
            state.synced -> {
                stickToBottom = true
                restored = true
                listState.scrollToItem(0)
            }
        }
    }
    LaunchedEffect(listState, state.sessionId) {
        snapshotFlow { listState.isScrollInProgress to (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 48) }
            .collect { (scrolling, atBottom) ->
                if (!restored) return@collect
                if (scrolling || atBottom) stickToBottom = atBottom
                if (atBottom) hasNew = false
            }
    }
    LaunchedEffect(listState, state.sessionId) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (!restored) return@collect
                val anchor = if (index == 0 && offset < 48) null else currentRows.getOrNull(index)?.let { ScrollMemory.Anchor(it.key, offset) }
                onEvent(ChatEvent.ScrollMoved(anchor))
            }
    }
    // Older history is fetched only when the user gets near the oldest loaded row.
    LaunchedEffect(listState, state.synced) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - LOAD_OLDER_WITHIN
        }
            .distinctUntilChanged()
            .collect { nearTop -> if (nearTop) onEvent(ChatEvent.LoadOlder) }
    }
    val newestKey = state.items.lastOrNull()?.key
    // Newest row once the server transcript is in; swapping cached rows for it is not news.
    var seenNewest by remember(state.sessionId) { mutableStateOf<String?>(null) }
    LaunchedEffect(newestKey, state.synced) {
        if (!restored) return@LaunchedEffect
        if (stickToBottom) {
            listState.scrollToItem(0)
        } else if (state.synced && newestKey != null) {
            if (seenNewest != null && newestKey != seenNewest) hasNew = true
        }
        if (state.synced) seenNewest = newestKey
    }
    val defaultToolsExpanded = state.expandToolsByDefault
    val isExpanded: (String) -> Boolean = { key -> state.isExpanded(key) }
    val onToggle: (String) -> Unit = { key -> onEvent(ChatEvent.Toggle(key)) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize().testTag("chat_transcript"),
            contentPadding = PaddingValues(vertical = OcSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.md, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(reversed, key = { it.key }, contentType = { it::class }) { item ->
                val rowModifier = Modifier.widthIn(max = OcSpacing.contentMaxWidth).fillMaxWidth().padding(horizontal = OcSpacing.gutter)
                when (item) {
                    is ChatItem.User -> UserBubble(
                        item = item,
                        modifier = rowModifier,
                        onLongPress = if (item.pending) null else { { actionsFor = item.key } },
                        onOpenImage = { onEvent(ChatEvent.OpenImage(item.key, it)) },
                        directory = state.directory,
                        onOpenFile = { onEvent(ChatEvent.OpenFile(it)) },
                    )
                    is ChatItem.Text -> AssistantText(item, cache, rowModifier)
                    is ChatItem.Reasoning -> ReasoningBlock(
                        item = item,
                        expanded = state.isExpanded("r:${item.key}"),
                        onToggle = { onToggle("r:${item.key}") },
                        modifier = rowModifier,
                    )
                    is ChatItem.Tools -> ToolsGroup(
                        item = item,
                        directory = state.directory,
                        expanded = state.isExpanded("g:${item.key}", defaultToolsExpanded),
                        isExpanded = isExpanded,
                        onToggle = onToggle,
                        onOpenSession = { onEvent(ChatEvent.OpenSession(it)) },
                        onOpenFile = { onEvent(ChatEvent.OpenFile(it)) },
                        modifier = rowModifier,
                    )
                    is ChatItem.Activity -> ActivityGroup(
                        item = item,
                        directory = state.directory,
                        expanded = state.isExpanded("a:${item.key}", defaultToolsExpanded),
                        defaultToolsExpanded = defaultToolsExpanded,
                        isExpanded = isExpanded,
                        onToggle = onToggle,
                        onOpenSession = { onEvent(ChatEvent.OpenSession(it)) },
                        onOpenFile = { onEvent(ChatEvent.OpenFile(it)) },
                        modifier = rowModifier,
                    )
                    is ChatItem.Error -> ErrorRow(item, rowModifier)
                    is ChatItem.Notice -> NoticeRow(
                        item = item,
                        cache = cache,
                        expanded = state.isExpanded("n:${item.key}"),
                        onToggle = { onToggle("n:${item.key}") },
                        modifier = rowModifier,
                    )
                    is ChatItem.Shell -> ShellBlock(item, rowModifier)
                    is ChatItem.Footer -> TurnFooter(item, onCopy, rowModifier.padding(bottom = OcSpacing.md), showCost = state.showCost)
                    is ChatItem.Working -> WorkingRow(rowModifier)
                }
            }
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
                        Text(
                            stringResource(R.string.chat_older_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = OcTheme.colors.textMuted,
                        )
                        OcChip(
                            text = stringResource(R.string.chat_older_retry),
                            onClick = { onEvent(ChatEvent.LoadOlder) },
                            modifier = Modifier.testTag("chat_older_retry_button"),
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = !stickToBottom,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = OcSpacing.sm),
        ) {
            val jump = {
                stickToBottom = true
                hasNew = false
                scope.launch { listState.animateScrollToItem(0) }
                Unit
            }
            if (hasNew) {
                OcChip(
                    text = stringResource(R.string.chat_new_messages),
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
                    contentDescription = stringResource(R.string.chat_jump_latest),
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

@Composable
private fun MessageActionsSheet(onRevert: () -> Unit, onFork: () -> Unit, onCopy: () -> Unit, onDismiss: () -> Unit) {
    val c = OcTheme.colors
    OcBottomSheet(onDismiss = onDismiss, testTag = "chat_message_actions") {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = OcSpacing.md)) {
            Text(
                stringResource(R.string.chat_message_actions),
                style = MaterialTheme.typography.titleSmall,
                color = c.textBase,
                modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
            )
            OcListRow(
                title = stringResource(R.string.chat_action_revert),
                subtitle = stringResource(R.string.chat_action_revert_hint),
                onClick = onRevert,
                leading = { Icon(Icons.AutoMirrored.Outlined.Undo, null, tint = c.iconBase) },
                modifier = Modifier.testTag("chat_action_revert"),
            )
            OcListRow(
                title = stringResource(R.string.chat_action_fork),
                subtitle = stringResource(R.string.chat_action_fork_hint),
                onClick = onFork,
                leading = { Icon(Icons.AutoMirrored.Outlined.CallSplit, null, tint = c.iconBase) },
                modifier = Modifier.testTag("chat_action_fork"),
            )
            OcListRow(
                title = stringResource(R.string.chat_action_copy),
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
