package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.UnreadSessions
import io.github.beppe2880.opencodemobile.core.ui.components.LocalEditMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcConfirmDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcInfoDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcSearchField
import io.github.beppe2880.opencodemobile.core.ui.components.OcSectionHeader
import io.github.beppe2880.opencodemobile.core.ui.components.OcSkeleton
import io.github.beppe2880.opencodemobile.core.ui.components.OcStatusDot
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextInputDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.SessionMuteSheet
import io.github.beppe2880.opencodemobile.core.ui.components.editable
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.platform.sessionNotificationsSupported
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_cancel
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_close
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.text.formatMediumDate
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.sessions.resources.Res
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_all_projects
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_archive
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_archived
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_bulk_delete_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_bulk_delete_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_clear_filters
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_connected
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_connecting
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_disconnected
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_reconnecting
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_unauthorized
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete_failed_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete_failed_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_demo_server
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_demo_status
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_archived_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_archived_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_filter_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_filter_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_search_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_search_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_unread_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_unread_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_error_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_failed
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_active
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_any_time
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_apply
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_custom_range
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_last_30_days
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_last_7_days
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_last_updated
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_outcome
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_range_hint
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_read
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_remove
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_reset
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_show
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filter_status
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filters
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_filters_active
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_interrupted
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_mark_all_read
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_mark_all_read_count
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_mark_read
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_mark_unread
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_mute
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_older
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_outcome_succeeded
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_pin
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_pinned
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_project_hide
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_read
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_rename
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_rename_label
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_rename_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_save
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_search
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_selected_count
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_selection_actions
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_selection_close
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_settings
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_this_week
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_title_fallback
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_today
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_unarchive
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_unmute
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_unpin
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_unread
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_unread_count
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_untitled
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_yesterday
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    state: SessionsState,
    onEvent: (SessionsEvent) -> Unit,
    modifier: Modifier = Modifier,
    selectedSessionId: String? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    /** Drawn under the list header (project filter and search), above the sessions, such as the app update notice; may draw nothing. */
    banner: @Composable () -> Unit = {},
    /** Drawn at the top of the list header, above the project filter, such as the What's New button; may draw nothing. */
    headerTop: @Composable () -> Unit = {},
    /** The offline demo: the top bar names it instead of showing a connection. */
    demo: Boolean = false,
    /**
     * Shown in a drawer over a chat: the list first scrolls to [selectedSessionId], and rows do
     * not swipe to delete since that swipe closes the drawer (delete stays in the row actions).
     */
    inDrawer: Boolean = false,
) {
    val c = OcTheme.colors
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.selectionMode) {
        if (state.selectionMode) focusManager.clearFocus()
    }
    OcBackHandler(enabled = state.selectionMode) { onEvent(SessionsEvent.ExitSelection) }
    val now by produceState(nowProvider()) {
        while (true) {
            delay(30_000)
            value = nowProvider()
        }
    }
    Scaffold(
        modifier = modifier,
        containerColor = c.bgBase,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (state.selectionMode) SelectionTopBar(state, onEvent) else SessionsTopBar(state, onEvent, demo)
        },
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
        floatingActionButton = {
            if (!state.selectionMode && (!state.isEmpty || state.query.isNotEmpty() || state.directoryFilter != null)) {
                val newLabel = stringResource(Res.string.sessions_new)
                ExtendedFloatingActionButton(
                    onClick = { onEvent(SessionsEvent.StartNewSession) },
                    // Material clears the semantics of the (animated) text: the icon names the button.
                    icon = { Icon(Icons.Outlined.Add, contentDescription = newLabel) },
                    text = { Text(newLabel) },
                    containerColor = c.bgInverse,
                    contentColor = c.textInverse,
                    shape = RoundedCornerShape(OcRadius.lg),
                    elevation = FloatingActionButtonDefaults.elevation(2.dp, 2.dp, 2.dp, 2.dp),
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("sessions_new_button"),
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { onEvent(SessionsEvent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            var listWidthPx by remember { mutableIntStateOf(Int.MAX_VALUE) }
            val listState = rememberLazyListState()
            if (inDrawer && selectedSessionId != null) {
                var revealed by remember(selectedSessionId) { mutableStateOf(false) }
                LaunchedEffect(selectedSessionId, state.groups, state.loading) {
                    if (revealed || state.loading || state.error != null) return@LaunchedEffect
                    val index = sessionRowIndex(state.groups, selectedSessionId) ?: return@LaunchedEffect
                    revealed = true
                    // One item above, so the row is not stuck under the top edge.
                    if (listState.layoutInfo.visibleItemsInfo.none { it.key == selectedSessionId }) listState.scrollToItem((index - 1).coerceAtLeast(0))
                }
            }
            // Narrower than any phone: the list sits beside the chat.
            val compact = with(LocalDensity.current) { listWidthPx.toDp() } < CompactListWidth
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().onSizeChanged { listWidthPx = it.width }.testTag("sessions_list"),
                contentPadding = PaddingValues(bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                item(key = "header", contentType = "header") {
                    // In the first item: a banner appearing later pushes the list down instead
                    // of opening above the scroll position. Under the search, right above the chats.
                    // Kept mounted while selecting, so removing it cannot move the rows under
                    // the finger; only its controls are disabled.
                    Column {
                        state.sharing?.let { ShareChoice(it, onEvent) }
                        ListHeader(state, onEvent, headerTop, enabled = !state.selectionMode)
                        banner()
                    }
                }
                when {
                    state.loading -> skeletonItems()
                    state.error != null -> item(key = "error") {
                        OcErrorState(
                            title = stringResource(Res.string.sessions_error_title),
                            message = state.error.resolve(),
                            onRetry = { onEvent(SessionsEvent.Refresh) },
                            retryTestTag = "sessions_retry_button",
                        )
                    }
                    state.isEmpty -> item(key = "empty") { EmptyContent(state, onEvent) }
                    else -> state.groups.forEach { group ->
                        item(key = "group-${group.bucket}", contentType = "group") {
                            if (group.bucket == DayBucket.Unread) {
                                UnreadGroupHeader(group.sessions.size, state.markingAllRead, state.selectionMode, onEvent)
                            } else {
                                OcSectionHeader(
                                    stringResource(group.bucket.label()),
                                    Modifier.padding(top = OcSpacing.sm).testTag("sessions_group_${group.bucket.name.lowercase()}"),
                                )
                            }
                        }
                        items(group.sessions, key = { it.id }, contentType = { "session" }) { session ->
                            val pinned = session.id in state.pinned
                            SessionRow(
                                session = session,
                                running = session.id in state.running,
                                selected = session.id == selectedSessionId,
                                selectionMode = state.selectionMode,
                                checked = session.id in state.selection,
                                now = now,
                                onClick = {
                                    if (state.selectionMode) {
                                        onEvent(SessionsEvent.ToggleSelection(session.id))
                                    } else {
                                        onEvent(SessionsEvent.Open(session.id))
                                    }
                                },
                                onLongClick = {
                                    if (state.selectionMode) {
                                        onEvent(SessionsEvent.ToggleSelection(session.id))
                                    } else {
                                        onEvent(SessionsEvent.StartSelection(session.id))
                                    }
                                },
                                onSwipeDelete = { onEvent(SessionsEvent.RequestDelete(session)) },
                                modifier = Modifier.animateItem().editable(SettingsPart.SessionRows, LocalEditMode.current),
                                compact = compact,
                                work = if (state.showRunningWork) state.work[session.id] ?: RunningWork() else null,
                                swipeToDelete = !inDrawer && !state.selectionMode,
                                pinned = pinned,
                                muted = session.id in state.muted,
                                onTogglePin = { onEvent(SessionsEvent.SetPinned(session.id, !pinned)) },
                                waiting = state.waiting[session.id],
                                showUnread = state.unreadSessions != UnreadSessions.Off,
                                markedUnread = session.id in state.manualUnread,
                            )
                        }
                    }
                }
            }
        }
    }
    SessionsOverlays(state, onEvent)
}

/** Position of [sessionId]'s row in the list: after the header item, each group has its header and rows; null when absent. */
internal fun sessionRowIndex(groups: List<SessionGroup>, sessionId: String): Int? {
    var index = 1
    for (group in groups) {
        index++
        val row = group.sessions.indexOfFirst { it.id == sessionId }
        if (row >= 0) return index + row
        index += group.sessions.size
    }
    return null
}

@Composable
private fun SessionsTopBar(state: SessionsState, onEvent: (SessionsEvent) -> Unit, demo: Boolean) {
    val c = OcTheme.colors
    val (label, color) = when (state.connection) {
        ConnectionState.Connected -> Res.string.sessions_connection_connected to c.success.fg
        ConnectionState.Connecting -> Res.string.sessions_connection_connecting to c.warning.fg
        ConnectionState.Reconnecting -> Res.string.sessions_connection_reconnecting to c.warning.fg
        ConnectionState.Disconnected -> Res.string.sessions_connection_disconnected to c.danger.fg
        ConnectionState.Unauthorized -> Res.string.sessions_connection_unauthorized to c.danger.fg
    }
    OcTopBar(
        title = if (demo) stringResource(Res.string.sessions_demo_server) else state.serverName.ifEmpty { stringResource(Res.string.sessions_title_fallback) },
        subtitle = stringResource(if (demo) Res.string.sessions_demo_status else label),
        titleAccessory = { if (!demo) OcStatusDot(color, Modifier.testTag("sessions_connection_indicator")) },
        actions = {
            OcIconButton(
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(Res.string.sessions_settings),
                onClick = { onEvent(SessionsEvent.OpenSettings) },
                modifier = Modifier.testTag("sessions_settings_button"),
            )
        },
    )
}

/** The top bar while rows are selected: close, the count and the actions for the selection. */
@Composable
private fun SelectionTopBar(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    val c = OcTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    var choosingMute by remember { mutableStateOf(false) }
    val selected = state.selectedSessions
    val count = selected.size
    val single = selected.singleOrNull()
    val muteActions = bulkMuteActions(selected, state.muted)
    val pinActions = bulkPinActions(selected, state.pinned)
    val archiveActions = if (state.archiveSupported) bulkArchiveActions(selected) else emptyList()
    val anyUnread = selected.any(state::isUnread)
    val anyRead = selected.any { !state.isUnread(it) }
    val showReadState = state.unreadSessions != UnreadSessions.Off
    OcTopBar(
        title = pluralStringResource(Res.plurals.sessions_selected_count, count, count),
        leading = {
            OcIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.sessions_selection_close),
                onClick = { onEvent(SessionsEvent.ExitSelection) },
                modifier = Modifier.testTag("sessions_selection_close"),
            )
        },
        actions = {
            Box(Modifier.padding(end = OcSpacing.sm)) {
                OcIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(Res.string.sessions_selection_actions),
                    onClick = { menuOpen = true },
                    modifier = Modifier.testTag("sessions_selection_menu_button"),
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = c.bgBase,
                    modifier = Modifier.exposeTestTags(),
                ) {
                    if (count == 1) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.sessions_rename)) },
                            onClick = { menuOpen = false; onEvent(SessionsEvent.RenameSelected) },
                            leadingIcon = {
                                Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null, tint = c.iconBase)
                            },
                            modifier = Modifier.testTag("sessions_bulk_rename"),
                        )
                    }
                    if (sessionNotificationsSupported) {
                        muteActions.forEach { action ->
                            val mute = action == BulkMuteAction.Mute
                            DropdownMenuItem(
                                text = { Text(stringResource(if (mute) Res.string.sessions_mute else Res.string.sessions_unmute)) },
                                onClick = {
                                    menuOpen = false
                                    if (mute && count == 1) {
                                        choosingMute = true
                                    } else {
                                        onEvent(SessionsEvent.SetSelectedMuted(mute))
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        if (mute) Icons.Outlined.NotificationsOff else Icons.Outlined.NotificationsActive,
                                        contentDescription = null,
                                        tint = c.iconBase,
                                    )
                                },
                                modifier = Modifier.testTag(if (mute) "sessions_bulk_mute" else "sessions_bulk_unmute"),
                            )
                        }
                    }
                    if (showReadState && anyUnread) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.sessions_mark_read)) },
                            onClick = { menuOpen = false; onEvent(SessionsEvent.MarkSelectedRead) },
                            leadingIcon = { Icon(Icons.Outlined.MarkEmailRead, contentDescription = null, tint = c.iconBase) },
                            modifier = Modifier.testTag("sessions_bulk_mark_read"),
                        )
                    }
                    if (showReadState && anyRead) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.sessions_mark_unread)) },
                            onClick = { menuOpen = false; onEvent(SessionsEvent.MarkSelectedUnread) },
                            leadingIcon = { Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null, tint = c.iconBase) },
                            modifier = Modifier.testTag("sessions_bulk_mark_unread"),
                        )
                    }
                    pinActions.forEach { action ->
                        val pin = action == BulkPinAction.Pin
                        DropdownMenuItem(
                            text = { Text(stringResource(if (pin) Res.string.sessions_pin else Res.string.sessions_unpin)) },
                            onClick = { menuOpen = false; onEvent(SessionsEvent.SetSelectedPinned(pin)) },
                            leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null, tint = c.iconBase) },
                            modifier = Modifier.testTag(if (pin) "sessions_bulk_pin" else "sessions_bulk_unpin"),
                        )
                    }
                    archiveActions.forEach { action ->
                        val archive = action == BulkArchiveAction.Archive
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(if (archive) Res.string.sessions_archive else Res.string.sessions_unarchive))
                            },
                            onClick = { menuOpen = false; onEvent(SessionsEvent.SetSelectedArchived(archive)) },
                            leadingIcon = {
                                Icon(
                                    if (archive) Icons.Outlined.Archive else Icons.Outlined.Unarchive,
                                    contentDescription = null,
                                    tint = c.iconBase,
                                )
                            },
                            modifier = Modifier.testTag(if (archive) "sessions_bulk_archive" else "sessions_bulk_unarchive"),
                        )
                    }
                    OcDivider(Modifier.padding(vertical = OcSpacing.xs))
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.sessions_delete), color = c.danger.fg) },
                        onClick = { menuOpen = false; onEvent(SessionsEvent.RequestDeleteSelected) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = c.danger.fg) },
                        modifier = Modifier.testTag("sessions_bulk_delete"),
                    )
                }
            }
        },
    )
    if (choosingMute && single != null) {
        SessionMuteSheet(
            running = single.id in state.running && state.connection == ConnectionState.Connected,
            onDismiss = { choosingMute = false },
        ) { untilFinished ->
            choosingMute = false
            onEvent(SessionsEvent.SetMuted(single.id, true, untilFinished = untilFinished))
        }
    }
}

@Composable
private fun ListHeader(
    state: SessionsState,
    onEvent: (SessionsEvent) -> Unit,
    top: @Composable () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier.padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        top()
        ProjectFilter(state, onEvent, enabled)
        OcSearchField(
            value = state.query,
            onValueChange = { onEvent(SessionsEvent.QueryChanged(it)) },
            placeholder = stringResource(Res.string.sessions_search),
            modifier = Modifier.testTag("sessions_search_field"),
            trailingContent = { FilterAction(state, onEvent, enabled) },
            enabled = enabled,
        )
        if (state.filters.isActive) ActiveFilterSummary(state, onEvent, enabled)
    }
}

@Composable
private fun FilterAction(state: SessionsState, onEvent: (SessionsEvent) -> Unit, enabled: Boolean = true) {
    val c = OcTheme.colors
    val count = state.filters.activeCount
    Box {
        OcIconButton(
            icon = Icons.Outlined.FilterAlt,
            contentDescription = if (count > 0) {
                stringResource(Res.string.sessions_filters_active, count)
            } else {
                stringResource(Res.string.sessions_filters)
            },
            onClick = { onEvent(SessionsEvent.OpenFilters) },
            enabled = enabled,
            tint = if (count > 0) c.textAccent else c.iconMuted,
            iconSize = 18.dp,
            modifier = Modifier.testTag("sessions_filters_button"),
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(c.bgAccent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.onAccent,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveFilterSummary(state: SessionsState, onEvent: (SessionsEvent) -> Unit, enabled: Boolean = true) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().testTag("sessions_filter_summary"),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        val statuses = state.filters.effectiveStatuses
        if (SessionStatus.Active in statuses && statuses.size > 1) {
            FilterSummaryChip(
                label = stringResource(Res.string.sessions_filter_active),
                onClick = { onEvent(SessionsEvent.OpenFilters) },
                onRemove = { onEvent(SessionsEvent.ToggleStatus(SessionStatus.Active)) },
                testTag = "sessions_filter_active_chip",
                enabled = enabled,
            )
        }
        if (state.archiveSupported && SessionStatus.Archived in statuses) {
            FilterSummaryChip(
                label = stringResource(Res.string.sessions_archived),
                onClick = { onEvent(SessionsEvent.OpenFilters) },
                onRemove = { onEvent(SessionsEvent.ToggleStatus(SessionStatus.Archived)) },
                testTag = "sessions_filter_archived_chip",
                enabled = enabled,
            )
        }
        state.filters.readState?.let { read ->
            FilterSummaryChip(
                label = readStateLabel(read),
                onClick = { onEvent(SessionsEvent.OpenFilters) },
                onRemove = { onEvent(SessionsEvent.ToggleRead(read)) },
                testTag = "sessions_filter_read_chip",
                enabled = enabled,
            )
        }
        if (state.filters.date != SessionDateFilter.AnyTime) {
            FilterSummaryChip(
                label = dateFilterLabel(state.filters.date),
                onClick = { onEvent(SessionsEvent.OpenFilters) },
                onRemove = { onEvent(SessionsEvent.SetDateFilter(SessionDateFilter.AnyTime)) },
                testTag = "sessions_filter_date_chip",
                enabled = enabled,
            )
        }
        state.filters.outcomes.forEach { outcome ->
            FilterSummaryChip(
                label = outcomeLabel(outcome),
                onClick = { onEvent(SessionsEvent.OpenFilters) },
                onRemove = { onEvent(SessionsEvent.ToggleOutcome(outcome)) },
                testTag = "sessions_filter_outcome_${outcome.name.lowercase()}",
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun FilterSummaryChip(
    label: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    testTag: String,
    enabled: Boolean = true,
) {
    OcChip(
        text = label,
        onClick = onClick,
        enabled = enabled,
        selected = true,
        trailingIcon = Icons.Outlined.Close,
        onTrailingClick = onRemove,
        trailingContentDescription = stringResource(Res.string.sessions_filter_remove, label),
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun dateFilterLabel(filter: SessionDateFilter): String = when (filter) {
    SessionDateFilter.AnyTime -> stringResource(Res.string.sessions_filter_any_time)
    SessionDateFilter.Today -> stringResource(Res.string.sessions_today)
    SessionDateFilter.Yesterday -> stringResource(Res.string.sessions_yesterday)
    SessionDateFilter.Last7Days -> stringResource(Res.string.sessions_filter_last_7_days)
    SessionDateFilter.Last30Days -> stringResource(Res.string.sessions_filter_last_30_days)
    is SessionDateFilter.Custom -> "${formatMediumDate(filter.from)} – ${formatMediumDate(filter.to)}"
}

@Composable
private fun outcomeLabel(outcome: SessionOutcome): String = when (outcome) {
    SessionOutcome.Succeeded -> stringResource(Res.string.sessions_outcome_succeeded)
    SessionOutcome.Failed -> stringResource(Res.string.sessions_failed)
    SessionOutcome.Interrupted -> stringResource(Res.string.sessions_interrupted)
}

@Composable
private fun readStateLabel(state: SessionRead): String = when (state) {
    SessionRead.Read -> stringResource(Res.string.sessions_read)
    SessionRead.Unread -> stringResource(Res.string.sessions_unread)
}

@Composable
private fun ProjectFilter(state: SessionsState, onEvent: (SessionsEvent) -> Unit, enabled: Boolean = true) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OcChip(
            text = state.filterLabel ?: stringResource(Res.string.sessions_all_projects),
            onClick = { expanded = true },
            enabled = enabled,
            selected = state.directoryFilter != null,
            leadingIcon = Icons.Outlined.Folder,
            trailingIcon = Icons.Outlined.ExpandMore,
            modifier = Modifier.testTag("sessions_project_filter"),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = OcTheme.colors.bgBase,
            modifier = Modifier.exposeTestTags(),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.sessions_all_projects)) },
                onClick = {
                    expanded = false
                    onEvent(SessionsEvent.FilterChanged(null))
                },
                modifier = Modifier.testTag("sessions_project_all"),
            )
            state.visibleProjects.forEach { project ->
                FilterProjectItem(
                    project = project,
                    onClick = {
                        expanded = false
                        onEvent(SessionsEvent.FilterChanged(project.directory))
                    },
                    onHide = { onEvent(SessionsEvent.HideProject(project.directory)) },
                )
            }
        }
    }
}

/** A project in the filter menu; long press offers Hide project. DropdownMenuItem has no long click. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilterProjectItem(project: Project, onClick: () -> Unit, onHide: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menu = true },
                    onLongClickLabel = stringResource(Res.string.sessions_project_hide),
                )
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = OcSpacing.xs)
                .testTag("sessions_project_${project.id}"),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(project.displayName, style = MaterialTheme.typography.bodyMedium, color = OcTheme.colors.textBase)
            Text(project.directory, style = OcTheme.mono.codeSmall, color = OcTheme.colors.textFaint)
        }
        ProjectMenu(expanded = menu, onDismiss = { menu = false }, onHide = onHide)
    }
}

@Composable
private fun EmptyContent(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    val filtered = state.query.isNotBlank() || state.directoryFilter != null
    val unreadOnly = state.filters.readState == SessionRead.Unread && state.filters.activeCount == 1 && !filtered
    val archivedOnly = state.filters.effectiveStatuses == setOf(SessionStatus.Archived) &&
        state.filters.date == SessionDateFilter.AnyTime &&
        state.filters.outcomes.isEmpty() &&
        !filtered
    OcEmptyState(
        icon = when {
            unreadOnly -> Icons.Outlined.MarkEmailRead
            archivedOnly -> Icons.Outlined.Archive
            state.filters.isActive -> Icons.Outlined.FilterAlt
            filtered -> Icons.Outlined.SearchOff
            else -> Icons.Outlined.ChatBubbleOutline
        },
        title = stringResource(
            when {
                unreadOnly -> Res.string.sessions_empty_unread_title
                archivedOnly -> Res.string.sessions_empty_archived_title
                state.filters.isActive -> Res.string.sessions_empty_filter_title
                filtered -> Res.string.sessions_empty_search_title
                else -> Res.string.sessions_empty_title
            },
        ),
        message = stringResource(
            when {
                unreadOnly -> Res.string.sessions_empty_unread_message
                archivedOnly -> Res.string.sessions_empty_archived_message
                state.filters.isActive -> Res.string.sessions_empty_filter_message
                filtered -> Res.string.sessions_empty_search_message
                else -> Res.string.sessions_empty_message
            },
        ),
        action = when {
            state.filters.isActive -> {
                {
                    OcButton(
                        text = stringResource(Res.string.sessions_clear_filters),
                        onClick = { onEvent(SessionsEvent.ResetFilters) },
                        variant = OcButtonVariant.Secondary,
                        compact = true,
                        modifier = Modifier.testTag("sessions_empty_clear_filters"),
                    )
                }
            }
            filtered -> null
            else -> {
                {
                    OcButton(
                        text = stringResource(Res.string.sessions_new),
                        onClick = { onEvent(SessionsEvent.StartNewSession) },
                        leadingIcon = Icons.Outlined.Add,
                        modifier = Modifier.testTag("sessions_empty_new_button"),
                    )
                }
            }
        },
    )
}

private fun LazyListScope.skeletonItems() {
    items(8, key = { "skeleton-$it" }, contentType = { "skeleton" }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            OcSkeleton(Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OcSkeleton(Modifier.width((140 + (it * 37) % 120).dp).height(12.dp))
                OcSkeleton(Modifier.width(72.dp).height(10.dp))
            }
        }
    }
}

/** The Unread group's header, with its count and the action that marks them all as read. */
@Composable
private fun UnreadGroupHeader(
    count: Int,
    marking: Boolean,
    selectionMode: Boolean,
    onEvent: (SessionsEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = OcSpacing.sm, end = OcSpacing.gutter - OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OcSectionHeader(
            stringResource(Res.string.sessions_unread_count, count),
            Modifier.weight(1f).testTag("sessions_group_unread"),
        )
        if (selectionMode) {
            // Keep the row height stable while the action is unavailable.
            Spacer(Modifier.height(48.dp))
        } else {
            TextButton(
                onClick = { onEvent(SessionsEvent.MarkUnreadGroupRead) },
                enabled = !marking,
                modifier = Modifier.testTag("sessions_mark_all_read"),
            ) {
                Text(
                    stringResource(Res.string.sessions_mark_all_read),
                    style = MaterialTheme.typography.labelMedium,
                    color = OcTheme.colors.textAccent,
                )
            }
        }
    }
}

private fun DayBucket.label(): StringResource = when (this) {
    DayBucket.Pinned -> Res.string.sessions_pinned
    DayBucket.Unread -> Res.string.sessions_unread
    DayBucket.Today -> Res.string.sessions_today
    DayBucket.Yesterday -> Res.string.sessions_yesterday
    DayBucket.ThisWeek -> Res.string.sessions_this_week
    DayBucket.Older -> Res.string.sessions_older
}

@Composable
private fun SessionsOverlays(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    state.renaming?.let { session ->
        OcTextInputDialog(
            title = stringResource(Res.string.sessions_rename_title),
            initialValue = session.title,
            confirmLabel = stringResource(Res.string.sessions_save),
            label = stringResource(Res.string.sessions_rename_label),
            onConfirm = { onEvent(SessionsEvent.ConfirmRename(it)) },
            onDismiss = { onEvent(SessionsEvent.DismissDialog) },
            testTagPrefix = "sessions_rename",
        )
    }
    state.deleting?.let { session ->
        OcConfirmDialog(
            title = stringResource(Res.string.sessions_delete_title),
            text = stringResource(Res.string.sessions_delete_message, session.displayTitle()),
            confirmLabel = stringResource(Res.string.sessions_delete),
            destructive = true,
            onConfirm = { onEvent(SessionsEvent.ConfirmDelete) },
            onDismiss = { onEvent(SessionsEvent.DismissDialog) },
            testTagPrefix = "sessions_delete",
        )
    }
    state.bulkDeleteCount?.takeIf { it > 1 }?.let { count ->
        OcConfirmDialog(
            title = pluralStringResource(Res.plurals.sessions_bulk_delete_title, count, count),
            text = pluralStringResource(Res.plurals.sessions_bulk_delete_message, count),
            confirmLabel = stringResource(Res.string.sessions_delete),
            destructive = true,
            onConfirm = { onEvent(SessionsEvent.ConfirmDelete) },
            onDismiss = { onEvent(SessionsEvent.DismissDialog) },
            testTagPrefix = "sessions_bulk_delete",
        )
    }
    if (state.deleteFailures.isNotEmpty()) {
        val untitled = stringResource(Res.string.sessions_untitled)
        val names = state.deleteFailures.joinToString("\n") { it.title.ifBlank { untitled } }
        OcInfoDialog(
            title = stringResource(Res.string.sessions_delete_failed_title),
            text = stringResource(Res.string.sessions_delete_failed_message, names),
            confirmLabel = stringResource(UiRes.string.oc_close),
            onDismiss = { onEvent(SessionsEvent.DismissDeleteFailures) },
            testTagPrefix = "sessions_delete_failed",
        )
    }
    state.newSession?.let { new ->
        NewSessionSheet(state = new, projects = state.projects, onEvent = onEvent, hidden = state.hiddenDirectories)
    }
    if (state.filterSheetOpen) SessionsFilterSheet(state, onEvent)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SessionsFilterSheet(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    OcBottomSheet(onDismiss = { onEvent(SessionsEvent.DismissFilters) }, testTag = "sessions_filter_sheet") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OcSpacing.lg)
                .padding(bottom = OcSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.sessions_filters),
                style = MaterialTheme.typography.titleSmall,
                color = OcTheme.colors.textBase,
            )
            // Without archiving, active is the only status: the whole section would be a no-op.
            if (state.archiveSupported) {
                Text(
                    text = stringResource(Res.string.sessions_filter_status),
                    style = MaterialTheme.typography.labelLarge,
                    color = OcTheme.colors.textMuted,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                ) {
                    OcChip(
                        text = stringResource(Res.string.sessions_filter_active),
                        onClick = { onEvent(SessionsEvent.ToggleStatus(SessionStatus.Active)) },
                        selected = SessionStatus.Active in state.filters.effectiveStatuses,
                        modifier = Modifier.testTag("sessions_filter_active"),
                    )
                    OcChip(
                        text = stringResource(Res.string.sessions_archived),
                        onClick = { onEvent(SessionsEvent.ToggleStatus(SessionStatus.Archived)) },
                        selected = SessionStatus.Archived in state.filters.effectiveStatuses,
                        modifier = Modifier.testTag("sessions_filter_archived"),
                    )
                }
            }
            // Hidden when the setting turns unread state off.
            if (state.unreadSessions != UnreadSessions.Off) {
                Text(
                    text = stringResource(Res.string.sessions_filter_read),
                    style = MaterialTheme.typography.labelLarge,
                    color = OcTheme.colors.textMuted,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                ) {
                    SessionRead.entries.forEach { read ->
                        OcChip(
                            text = readStateLabel(read),
                            onClick = { onEvent(SessionsEvent.ToggleRead(read)) },
                            selected = state.filters.readState == read,
                            modifier = Modifier.testTag("sessions_filter_read_${read.name.lowercase()}"),
                        )
                    }
                }
                // Also where there is no Unread group (dot only) or it is out of sight.
                if (state.unreadShown.isNotEmpty()) {
                    OcButton(
                        text = stringResource(Res.string.sessions_mark_all_read_count, state.unreadShown.size),
                        onClick = { onEvent(SessionsEvent.MarkAllRead) },
                        variant = OcButtonVariant.Secondary,
                        compact = true,
                        loading = state.markingAllRead,
                        leadingIcon = Icons.Outlined.MarkEmailRead,
                        modifier = Modifier.testTag("sessions_filter_mark_all_read"),
                    )
                }
            }
            Text(
                text = stringResource(Res.string.sessions_filter_last_updated),
                style = MaterialTheme.typography.labelLarge,
                color = OcTheme.colors.textMuted,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                DateFilterChip(SessionDateFilter.AnyTime, "sessions_filter_date_any", state, onEvent)
                DateFilterChip(SessionDateFilter.Today, "sessions_filter_date_today", state, onEvent)
                DateFilterChip(SessionDateFilter.Yesterday, "sessions_filter_date_yesterday", state, onEvent)
                DateFilterChip(SessionDateFilter.Last7Days, "sessions_filter_date_last_7", state, onEvent)
                DateFilterChip(SessionDateFilter.Last30Days, "sessions_filter_date_last_30", state, onEvent)
                val custom = state.filters.date as? SessionDateFilter.Custom
                OcChip(
                    text = custom?.let { dateFilterLabel(it) } ?: stringResource(Res.string.sessions_filter_custom_range),
                    onClick = { showDatePicker = true },
                    selected = custom != null,
                    modifier = Modifier.testTag("sessions_filter_date_custom"),
                )
            }
            Text(
                text = stringResource(Res.string.sessions_filter_outcome),
                style = MaterialTheme.typography.labelLarge,
                color = OcTheme.colors.textMuted,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                SessionOutcome.entries.forEach { outcome ->
                    OcChip(
                        text = outcomeLabel(outcome),
                        onClick = { onEvent(SessionsEvent.ToggleOutcome(outcome)) },
                        selected = outcome in state.filters.outcomes,
                        modifier = Modifier.testTag("sessions_filter_outcome_${outcome.name.lowercase()}"),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = OcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onEvent(SessionsEvent.ResetFilters) },
                    enabled = state.filters.isActive,
                ) {
                    Text(stringResource(Res.string.sessions_filter_reset))
                }
                Spacer(Modifier.weight(1f))
                OcButton(
                    text = stringResource(Res.string.sessions_filter_show),
                    onClick = { onEvent(SessionsEvent.DismissFilters) },
                    modifier = Modifier.testTag("sessions_filter_show"),
                )
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(OcSpacing.xs))
        }
    }
    if (showDatePicker) {
        val custom = state.filters.date as? SessionDateFilter.Custom
        val picker = rememberDateRangePickerState(
            initialSelectedStartDateMillis = custom?.from?.let { it.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() },
            initialSelectedEndDateMillis = custom?.to?.let { it.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = picker.selectedStartDateMillis
                        val end = picker.selectedEndDateMillis
                        if (start != null && end != null) {
                            onEvent(
                                SessionsEvent.SetDateFilter(
                                    SessionDateFilter.Custom(utcDate(start), utcDate(end)),
                                ),
                            )
                        }
                        showDatePicker = false
                    },
                    enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null,
                ) {
                    Text(stringResource(Res.string.sessions_filter_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(UiRes.string.oc_cancel))
                }
            },
        ) {
            DateRangePicker(
                state = picker,
                modifier = Modifier.heightIn(max = 500.dp),
                title = {},
                headline = {
                    val start = picker.selectedStartDateMillis
                    val end = picker.selectedEndDateMillis
                    Text(
                        text = if (start != null && end != null) {
                            "${formatMediumDate(utcDate(start))} – ${formatMediumDate(utcDate(end))}"
                        } else {
                            stringResource(Res.string.sessions_filter_range_hint)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = OcTheme.colors.textBase,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun DateFilterChip(
    filter: SessionDateFilter,
    testTag: String,
    state: SessionsState,
    onEvent: (SessionsEvent) -> Unit,
) {
    OcChip(
        text = dateFilterLabel(filter),
        onClick = { onEvent(SessionsEvent.SetDateFilter(filter)) },
        selected = state.filters.date == filter,
        modifier = Modifier.testTag(testTag),
    )
}

private fun utcDate(epochMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.UTC).date

@PreviewLightDark
@Composable
private fun SessionsScreenPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        val vm = SessionsState(
            serverName = "Desktop",
            connection = ConnectionState.Connected,
            projects = SampleData.projects,
            groups = listOf(
                SessionGroup(DayBucket.Today, SampleData.sessions.take(4)),
                SessionGroup(DayBucket.Yesterday, SampleData.sessions.subList(4, 6)),
                SessionGroup(DayBucket.Older, SampleData.sessions.drop(6)),
            ),
            running = setOf("ses_1"),
            loading = false,
        )
        SessionsScreen(state = vm, onEvent = {}, nowProvider = { SampleData.NOW })
    }
}

@PreviewLightDark
@Composable
private fun SessionsLoadingPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        SessionsScreen(state = SessionsState(serverName = "Desktop", connection = ConnectionState.Connecting), onEvent = {})
    }
}

@PreviewLightDark
@Composable
private fun SessionsEmptyPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        SessionsScreen(state = SessionsState(serverName = "Desktop", loading = false), onEvent = {})
    }
}

/** Below this width the session rows show their time under the title. */
private val CompactListWidth = 360.dp
