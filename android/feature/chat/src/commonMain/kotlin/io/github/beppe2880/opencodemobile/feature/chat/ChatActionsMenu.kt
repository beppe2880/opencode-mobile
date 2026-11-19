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
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_running
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_compact
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_compacting
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_fork
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_mcps
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_menu_history
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_menu_tools
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_new
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_pin
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_redo
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_undo
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_unpin
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

internal val ChatState.menuReady: Boolean
    get() = directory.isNotBlank() && !loading && error == null && connection == ConnectionState.Connected && !creatingSession && !forking && !exporting

internal val ChatState.historyReady: Boolean
    get() = menuReady && !running && !sending && !reverting && !compacting

/** Session actions first, history next, tools last. No nested menus on phones. */
@Composable
internal fun ChatActionsMenu(state: ChatState, expanded: Boolean, onDismiss: () -> Unit, onEvent: (ChatEvent) -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = OcTheme.colors.bgBase,
        modifier = Modifier.widthIn(min = 280.dp, max = 340.dp).heightIn(max = 600.dp).exposeTestTags().testTag("chat_actions_menu"),
    ) {
        val ready = state.menuReady
        val history = state.historyReady
        val act: (ChatEvent) -> Unit = { onDismiss(); onEvent(it) }
        MenuAction(stringResource(Res.string.chat_new), Icons.Outlined.Add, "chat_menu_new", ready, state.creatingSession) { act(ChatEvent.NewSession) }
        MenuAction(stringResource(Res.string.chat_fork), Icons.Outlined.AccountTree, "chat_menu_fork", ready, state.forking) { act(ChatEvent.Fork()) }
        MenuAction(
            stringResource(if (state.pinned) Res.string.chat_unpin else Res.string.chat_pin),
            Icons.Outlined.PushPin, "chat_menu_pin", state.directory.isNotBlank(),
        ) { act(ChatEvent.SetPinned(!state.pinned)) }
        MenuAction(stringResource(Res.string.chat_export), Icons.Outlined.Share, "chat_menu_export", ready, state.exporting) {
            act(ChatEvent.ExportSheet(true))
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
    }
}

@Composable
private fun MenuGroup(label: String) {
    Row(
        modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.xs),
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
private fun MenuAction(label: String, icon: ImageVector, tag: String, enabled: Boolean, loading: Boolean = false, onClick: () -> Unit) {
    val color = if (enabled) OcTheme.colors.textBase else OcTheme.colors.textFaint
    DropdownMenuItem(
        text = { Text(label, color = color) },
        leadingIcon = { if (loading) OcSpinner(size = 20.dp) else Icon(icon, contentDescription = null, tint = color) },
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
    )
}
