package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.RunningWork
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcConfirmDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSearchField
import io.github.beppe2880.opencodemobile.core.ui.components.OcSectionHeader
import io.github.beppe2880.opencodemobile.core.ui.components.OcSkeleton
import io.github.beppe2880.opencodemobile.core.ui.components.OcStatusDot
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextInputDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.sessions.resources.Res
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_all_projects
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_connected
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_connecting
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_disconnected
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_reconnecting
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_connection_unauthorized
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_delete_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_search_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_search_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_empty_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_error_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_new
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_older
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_rename
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_rename_label
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_rename_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_save
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_search
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_settings
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_this_week
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_title_fallback
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_today
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_yesterday
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    state: SessionsState,
    onEvent: (SessionsEvent) -> Unit,
    modifier: Modifier = Modifier,
    selectedSessionId: String? = null,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    /** Drawn above the list header, such as the app update notice; may draw nothing. */
    banner: @Composable () -> Unit = {},
) {
    val c = OcTheme.colors
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
        topBar = { SessionsTopBar(state, onEvent) },
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.windowInsetsPadding(WindowInsets.navigationBars)) },
        floatingActionButton = {
            if (!state.isEmpty || state.query.isNotEmpty() || state.directoryFilter != null) {
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
            // Narrower than any phone: the list sits beside the chat.
            val compact = with(LocalDensity.current) { listWidthPx.toDp() } < CompactListWidth
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().onSizeChanged { listWidthPx = it.width }.testTag("sessions_list"),
                contentPadding = PaddingValues(bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                item(key = "header", contentType = "header") {
                    // In the first item: a banner appearing later pushes the list down instead
                    // of opening above the scroll position.
                    Column {
                        banner()
                        ListHeader(state, onEvent)
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
                            OcSectionHeader(stringResource(group.bucket.label()), Modifier.padding(top = OcSpacing.sm))
                        }
                        items(group.sessions, key = { it.id }, contentType = { "session" }) { session ->
                            SessionRow(
                                session = session,
                                running = session.id in state.running,
                                selected = session.id == selectedSessionId,
                                now = now,
                                onClick = { onEvent(SessionsEvent.Open(session.id)) },
                                onLongClick = { onEvent(SessionsEvent.ShowActions(session)) },
                                onSwipeDelete = { onEvent(SessionsEvent.RequestDelete(session)) },
                                modifier = Modifier.animateItem(),
                                compact = compact,
                                work = state.work[session.id] ?: RunningWork(),
                            )
                        }
                    }
                }
            }
        }
    }
    SessionsOverlays(state, onEvent)
}

@Composable
private fun SessionsTopBar(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    val c = OcTheme.colors
    val (label, color) = when (state.connection) {
        ConnectionState.Connected -> Res.string.sessions_connection_connected to c.success.fg
        ConnectionState.Connecting -> Res.string.sessions_connection_connecting to c.warning.fg
        ConnectionState.Reconnecting -> Res.string.sessions_connection_reconnecting to c.warning.fg
        ConnectionState.Disconnected -> Res.string.sessions_connection_disconnected to c.danger.fg
        ConnectionState.Unauthorized -> Res.string.sessions_connection_unauthorized to c.danger.fg
    }
    OcTopBar(
        title = state.serverName.ifEmpty { stringResource(Res.string.sessions_title_fallback) },
        subtitle = stringResource(label),
        titleAccessory = { OcStatusDot(color, Modifier.testTag("sessions_connection_indicator")) },
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

@Composable
private fun ListHeader(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        ProjectFilter(state, onEvent)
        OcSearchField(
            value = state.query,
            onValueChange = { onEvent(SessionsEvent.QueryChanged(it)) },
            placeholder = stringResource(Res.string.sessions_search),
            modifier = Modifier.testTag("sessions_search_field"),
        )
    }
}

@Composable
private fun ProjectFilter(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        OcChip(
            text = state.filterLabel ?: stringResource(Res.string.sessions_all_projects),
            onClick = { expanded = true },
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
            state.projects.forEach { project ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(project.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(project.directory, style = OcTheme.mono.codeSmall, color = OcTheme.colors.textFaint)
                        }
                    },
                    onClick = {
                        expanded = false
                        onEvent(SessionsEvent.FilterChanged(project.directory))
                    },
                    modifier = Modifier.testTag("sessions_project_${project.id}"),
                )
            }
        }
    }
}

@Composable
private fun EmptyContent(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    val filtered = state.query.isNotBlank() || state.directoryFilter != null
    OcEmptyState(
        icon = if (filtered) Icons.Outlined.SearchOff else Icons.Outlined.ChatBubbleOutline,
        title = stringResource(if (filtered) Res.string.sessions_empty_search_title else Res.string.sessions_empty_title),
        message = stringResource(if (filtered) Res.string.sessions_empty_search_message else Res.string.sessions_empty_message),
        action = if (filtered) {
            null
        } else {
            {
                OcButton(
                    text = stringResource(Res.string.sessions_new),
                    onClick = { onEvent(SessionsEvent.StartNewSession) },
                    leadingIcon = Icons.Outlined.Add,
                    modifier = Modifier.testTag("sessions_empty_new_button"),
                )
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

private fun DayBucket.label(): StringResource = when (this) {
    DayBucket.Today -> Res.string.sessions_today
    DayBucket.Yesterday -> Res.string.sessions_yesterday
    DayBucket.ThisWeek -> Res.string.sessions_this_week
    DayBucket.Older -> Res.string.sessions_older
}

@Composable
private fun SessionsOverlays(state: SessionsState, onEvent: (SessionsEvent) -> Unit) {
    state.actionsFor?.let { session ->
        OcBottomSheet(onDismiss = { onEvent(SessionsEvent.DismissActions) }, testTag = "sessions_actions_sheet") {
            Text(
                session.displayTitle(),
                style = MaterialTheme.typography.titleSmall,
                color = OcTheme.colors.textBase,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
            )
            OcListRow(
                title = stringResource(Res.string.sessions_rename),
                onClick = { onEvent(SessionsEvent.RequestRename(session)) },
                leading = { Icon(Icons.Outlined.DriveFileRenameOutline, null, tint = OcTheme.colors.iconBase) },
                modifier = Modifier.testTag("sessions_action_rename"),
            )
            OcListRow(
                title = stringResource(Res.string.sessions_delete),
                titleColor = OcTheme.colors.danger.fg,
                onClick = { onEvent(SessionsEvent.RequestDelete(session)) },
                leading = { Icon(Icons.Outlined.Delete, null, tint = OcTheme.colors.danger.fg) },
                modifier = Modifier.testTag("sessions_action_delete"),
            )
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(OcSpacing.lg))
        }
    }
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
    state.newSession?.let { new ->
        NewSessionSheet(state = new, projects = state.projects, onEvent = onEvent)
    }
}

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
