package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.platform.sessionNotificationsSupported
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_running
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_archive
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_compact
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_compacting
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_delete
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_fork
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_mcps
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_menu_history
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_menu_tools
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_mute
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_new
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_pin
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_redo
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_rename
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_unarchive
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_undo
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_unmute
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_unpin
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

internal val ChatState.menuReady: Boolean
    get() = directory.isNotBlank() && !loading && error == null && connection == ConnectionState.Connected && !creatingSession && !forking

internal val ChatState.historyReady: Boolean
    get() = menuReady && !running && !sending && !reverting && !compacting

/** Session actions first, history next, tools last. No nested menus on phones. */
@Composable
internal fun ChatActionsMenu(state: ChatState, expanded: Boolean, onDismiss: () -> Unit, onEvent: (ChatEvent) -> Unit) {
    // Keep the menu above the message area's footer and composer; the rest scrolls.
    val maxMenuHeight = (with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } - 370.dp)
        .coerceAtLeast(320.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = OcTheme.colors.bgBase,
        modifier = Modifier.widthIn(min = 280.dp, max = 340.dp).heightIn(max = maxMenuHeight).exposeTestTags().testTag("chat_actions_menu"),
    ) {
        val ready = state.menuReady
        val history = state.historyReady
        val act: (ChatEvent) -> Unit = { onDismiss(); onEvent(it) }
        MenuAction(stringResource(Res.string.chat_new), Icons.Outlined.Add, "chat_menu_new", ready, state.creatingSession) { act(ChatEvent.NewSession) }
        MenuAction(stringResource(Res.string.chat_fork), Icons.Outlined.AccountTree, "chat_menu_fork", ready, state.forking) { act(ChatEvent.Fork()) }
        // Shared with the session selection menu: Rename, Mute, Pin, Archive, Delete.
        MenuAction(
            stringResource(Res.string.chat_rename), Icons.Outlined.DriveFileRenameOutline,
            "chat_menu_rename", state.directory.isNotBlank(),
        ) { act(ChatEvent.RequestRename) }
        if (sessionNotificationsSupported) {
            MenuAction(
                stringResource(if (state.muted) Res.string.chat_unmute else Res.string.chat_mute),
                if (state.muted) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                "chat_menu_mute", state.directory.isNotEmpty(),
            ) { act(ChatEvent.SetMuted(!state.muted)) }
        }
        MenuAction(
            stringResource(if (state.pinned) Res.string.chat_unpin else Res.string.chat_pin),
            Icons.Outlined.PushPin, "chat_menu_pin", state.directory.isNotBlank(),
        ) { act(ChatEvent.SetPinned(!state.pinned)) }
        if (state.archiveSupported) {
            MenuAction(
                stringResource(if (state.archived) Res.string.chat_unarchive else Res.string.chat_archive),
                if (state.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                "chat_menu_archive", state.directory.isNotBlank(),
            ) { act(ChatEvent.SetArchived(!state.archived)) }
        }
        MenuGroup(stringResource(Res.string.chat_menu_history))
        MenuAction(
            stringResource(if (state.compacting) Res.string.chat_compacting else Res.string.chat_compact),
            Icons.Outlined.Compress, "chat_menu_compact", history && state.context != null, state.compacting,
        ) { act(ChatEvent.Compact) }
        MenuAction(
            stringResource(Res.string.chat_undo), Icons.AutoMirrored.Outlined.Undo,
            "chat_menu_undo", history && state.canUndo, state.reverting,
        ) { act(ChatEvent.Undo) }
        if (state.canRedo) {
            MenuAction(stringResource(Res.string.chat_redo), Icons.AutoMirrored.Outlined.Redo, "chat_menu_redo", history) {
                act(ChatEvent.RestoreRevert)
            }
        }
        MenuGroup(stringResource(Res.string.chat_menu_tools))
        MenuAction(stringResource(UiRes.string.oc_running), Icons.Outlined.PlayCircleOutline, "chat_running_menu_item", ready) {
            act(ChatEvent.RunningSheet(true))
        }
        MenuAction(stringResource(Res.string.chat_providers), Icons.Outlined.VpnKey, "chat_providers_menu_item", ready) { act(ChatEvent.OpenProviders) }
        MenuAction(stringResource(Res.string.chat_mcps), Icons.Outlined.Hub, "chat_mcps_menu_item", ready) { act(ChatEvent.OpenMcps) }
        OcDivider()
        MenuAction(
            stringResource(Res.string.chat_delete), Icons.Outlined.Delete, "chat_menu_delete",
            state.directory.isNotBlank(), tint = OcTheme.colors.danger.fg,
        ) { act(ChatEvent.RequestDelete) }
    }
}

@Composable
private fun MenuGroup(label: String) {
    Row(
        modifier = Modifier.padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        OcDivider(Modifier.weight(1f))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
            color = OcTheme.colors.textFaint,
            modifier = Modifier.semantics { heading() },
        )
        OcDivider(Modifier.weight(1f))
    }
}

@Composable
private fun MenuAction(
    label: String,
    icon: ImageVector,
    tag: String,
    enabled: Boolean,
    loading: Boolean = false,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    val color = if (enabled) tint ?: OcTheme.colors.textBase else OcTheme.colors.textFaint
    DropdownMenuItem(
        text = { Text(label, color = color) },
        leadingIcon = { if (loading) OcSpinner(size = 20.dp) else Icon(icon, contentDescription = null, tint = color) },
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
    )
}
