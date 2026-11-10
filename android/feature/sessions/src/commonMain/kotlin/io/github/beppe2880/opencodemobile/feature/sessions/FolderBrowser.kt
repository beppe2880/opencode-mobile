package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ServerPaths
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.sessions.resources.Res
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_back
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_empty
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_hide_hidden
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_pick
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_recent
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_retry
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_show_hidden
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_title
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_browse_use
import org.jetbrains.compose.resources.stringResource

/** Browses the server's folders for a new session: the path on top goes up, a row goes in. */
@Composable
internal fun FolderBrowser(
    state: FolderBrowserState,
    onEvent: (SessionsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    // Recent folders open on a page of their own; back returns to the browser.
    var showRecent by rememberSaveable { mutableStateOf(false) }
    if (showRecent && state.recent.isNotEmpty()) {
        RecentFolders(
            recent = state.recent,
            onBack = { showRecent = false },
            onOpen = {
                showRecent = false
                onEvent(SessionsEvent.BrowseTo(it))
            },
            onPick = { onEvent(SessionsEvent.PickFolder(it)) },
            modifier = modifier,
        )
        return
    }
    // Back climbs one folder until the start folder (or the root), then returns to the form.
    val goBack = {
        onEvent(state.backTo?.let { SessionsEvent.BrowseTo(it) } ?: SessionsEvent.CloseFolderBrowser)
    }
    OcBackHandler(onBack = goBack)
    // Dot-folders stay hidden until the eye shows them.
    var showDotFolders by rememberSaveable { mutableStateOf(false) }
    val folders = remember(state.folders, showDotFolders) { if (showDotFolders) state.folders else state.folders.filterNot { it.startsWith(".") } }
    val recent = if (state.atStart) state.recent else emptyList()
    Column(modifier = modifier.fillMaxWidth().testTag("sessions_browse")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OcIconButton(
                Icons.AutoMirrored.Outlined.ArrowBack,
                stringResource(Res.string.sessions_browse_back),
                goBack,
                modifier = Modifier.testTag("sessions_browse_back"),
            )
            Text(
                stringResource(Res.string.sessions_browse_title),
                style = MaterialTheme.typography.titleSmall,
                color = c.textBase,
                modifier = Modifier.weight(1f),
            )
            if (state.loading) OcSpinner(Modifier.padding(end = OcSpacing.sm).testTag("sessions_browse_loading"))
            OcIconButton(
                if (showDotFolders) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                stringResource(if (showDotFolders) Res.string.sessions_browse_hide_hidden else Res.string.sessions_browse_show_hidden),
                { showDotFolders = !showDotFolders },
                tint = if (showDotFolders) c.textAccent else c.iconBase,
                modifier = Modifier.testTag("sessions_browse_dot_folders"),
            )
        }
        PathCrumbs(state.path, onOpen = { onEvent(SessionsEvent.BrowseTo(it)) })
        OcDivider()
        val listState = rememberLazyListState()
        LaunchedEffect(state.path) { listState.scrollToItem(0) }
        // Shrinks (down from its usual minimum) when the keyboard leaves less room.
        LazyColumn(state = listState, modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(min = 280.dp, max = 440.dp)) {
            if (state.error != null) {
                item("error") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(OcSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        Text(
                            state.error.resolve(),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.danger.fg,
                            modifier = Modifier.testTag("sessions_browse_error"),
                        )
                        OcButton(
                            text = stringResource(Res.string.sessions_browse_retry),
                            onClick = { onEvent(SessionsEvent.BrowseTo(state.path.ifBlank { null })) },
                            variant = OcButtonVariant.Secondary,
                            compact = true,
                            modifier = Modifier.testTag("sessions_browse_retry"),
                        )
                    }
                }
            } else if (folders.isEmpty() && !state.loading) {
                item("empty") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = OcSpacing.xxl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        Icon(Icons.Outlined.FolderOff, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(22.dp))
                        Text(
                            stringResource(Res.string.sessions_browse_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                            modifier = Modifier.testTag("sessions_browse_empty"),
                        )
                    }
                }
            }
            if (recent.isNotEmpty()) {
                item("recent") {
                    RecentEntry(count = recent.size, onClick = { showRecent = true })
                    OcDivider()
                }
            }
            itemsIndexed(folders, key = { _, name -> name }) { index, name ->
                FolderRow(
                    name = name,
                    onClick = { onEvent(SessionsEvent.BrowseTo(ServerPaths.child(state.path, name))) },
                    modifier = Modifier.testTag("sessions_browse_folder_$index"),
                )
                OcDivider()
            }
        }
        OcDivider()
        OcButton(
            text = stringResource(Res.string.sessions_browse_use),
            onClick = { onEvent(SessionsEvent.PickBrowsedFolder) },
            enabled = state.path.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md)
                .testTag("sessions_browse_use"),
        )
    }
}

/** The path as tappable segments; the server icon opens the path's root. Scrolls to keep the current folder in view. */
@Composable
private fun PathCrumbs(path: String, onOpen: (String) -> Unit) {
    val c = OcTheme.colors
    val segments = remember(path) { ServerPaths.segments(path) }
    // Without a path yet, "/" still reaches the root of the server's own drive.
    val root = remember(path) { ServerPaths.root(path) ?: "/" }
    val separator = remember(path) { ServerPaths.separator(path).toString() }
    val listState = rememberLazyListState()
    LaunchedEffect(segments) { listState.scrollToItem(segments.size) }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth().testTag("sessions_browse_path"),
        contentPadding = PaddingValues(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xxs),
    ) {
        item("root") {
            Crumb(current = segments.isEmpty() && path.isNotBlank(), onClick = { onOpen(root) }, modifier = Modifier.testTag("sessions_browse_root")) {
                Icon(Icons.Outlined.Dns, contentDescription = root, tint = c.iconMuted, modifier = Modifier.size(14.dp))
            }
        }
        itemsIndexed(segments) { index, (name, full) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
                Text(separator, style = OcTheme.mono.codeSmall, color = c.textFaint)
                val current = index == segments.lastIndex
                Crumb(current = current, onClick = { onOpen(full) }, modifier = Modifier.testTag("sessions_browse_crumb_$index")) {
                    Text(
                        name,
                        style = OcTheme.mono.codeSmall,
                        color = if (current) c.textBase else c.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun Crumb(current: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.sm)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (current) Modifier.background(c.layer02).border(1.dp, c.borderStrong, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 28.dp)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun FolderRow(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val hidden = name.startsWith(".")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        Icon(Icons.Outlined.Folder, contentDescription = null, tint = if (hidden) c.textFaint else c.iconBase, modifier = Modifier.size(18.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (hidden) c.textMuted else c.textBase,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
    }
}

/** The one row that stands for the recent folders at the start folder. */
@Composable
private fun RecentEntry(count: Int, onClick: () -> Unit) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .background(c.layer01)
            .heightIn(min = 48.dp)
            .padding(horizontal = OcSpacing.lg)
            .testTag("sessions_browse_recent"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        Icon(Icons.Outlined.History, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
        Text(
            stringResource(Res.string.sessions_browse_recent),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.clip(RoundedCornerShape(50)).background(c.layer02).padding(horizontal = OcSpacing.sm, vertical = 1.dp),
        ) {
            Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
    }
}

/** The recent folders on a page of their own: a row opens the folder in the browser, Use picks it. */
@Composable
private fun RecentFolders(
    recent: List<String>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    OcBackHandler(onBack = onBack)
    Column(modifier = modifier.fillMaxWidth().testTag("sessions_browse_recent_page")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OcIconButton(
                Icons.AutoMirrored.Outlined.ArrowBack,
                stringResource(Res.string.sessions_browse_back),
                onBack,
                modifier = Modifier.testTag("sessions_browse_recent_back"),
            )
            Text(
                stringResource(Res.string.sessions_browse_recent),
                style = MaterialTheme.typography.titleSmall,
                color = c.textBase,
                modifier = Modifier.weight(1f),
            )
        }
        OcDivider()
        LazyColumn(modifier = Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 440.dp).padding(bottom = OcSpacing.md)) {
            itemsIndexed(recent, key = { _, path -> path }) { index, path ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { onOpen(path) }
                        .heightIn(min = 60.dp)
                        .padding(start = OcSpacing.lg, end = OcSpacing.md, top = OcSpacing.sm, bottom = OcSpacing.sm)
                        .testTag("sessions_browse_recent_$index"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            ServerPaths.name(path) ?: path,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textBase,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            path,
                            style = OcTheme.mono.codeSmall,
                            color = c.textFaint,
                            maxLines = 1,
                            overflow = TextOverflow.StartEllipsis,
                        )
                    }
                    OcButton(
                        text = stringResource(Res.string.sessions_browse_pick),
                        onClick = { onPick(path) },
                        variant = OcButtonVariant.Secondary,
                        compact = true,
                        modifier = Modifier.testTag("sessions_browse_recent_use_$index"),
                    )
                }
                OcDivider()
            }
        }
    }
}

/** Opens the folder browser from inside the path field. */
@Composable
internal fun BrowseButton(onClick: () -> Unit) {
    val c = OcTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(OcRadius.sm),
        color = c.bgBase,
        contentColor = c.textAccent,
        border = BorderStroke(1.dp, c.borderFocus),
        modifier = Modifier.padding(end = OcSpacing.sm).heightIn(min = 32.dp).testTag("sessions_new_browse"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(stringResource(Res.string.sessions_browse), style = MaterialTheme.typography.labelMedium)
        }
    }
}
