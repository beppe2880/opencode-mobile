package io.github.beppe2880.opencodemobile.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.ui.R as UiR
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcConfirmDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextInputDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = stringResource(R.string.settings_title),
                leading = {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiR.string.oc_back),
                        onClick = { onEvent(SettingsEvent.Back) },
                        modifier = Modifier.testTag("settings_back_button"),
                    )
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("settings_list"),
                contentPadding = PaddingValues(
                    start = OcSpacing.gutter,
                    end = OcSpacing.gutter,
                    top = OcSpacing.lg,
                    bottom = OcSpacing.xxl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                item(key = "servers") { ServersSection(state, onEvent) }
                item(key = "appearance") { AppearanceSection(state.settings.theme, onEvent) }
                item(key = "conversation") { ConversationSection(state.settings, onEvent) }
                item(key = "notifications") { NotificationsSection(state, onEvent) }
                item(key = "about") { AboutSection(state) }
            }
        }
    }
    state.renaming?.let { server ->
        OcTextInputDialog(
            title = stringResource(R.string.settings_rename_title),
            initialValue = server.name,
            confirmLabel = stringResource(R.string.settings_save),
            label = stringResource(R.string.settings_rename_label),
            onConfirm = { onEvent(SettingsEvent.ConfirmRename(it)) },
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            testTagPrefix = "settings_rename",
        )
    }
    state.removing?.let { server ->
        OcConfirmDialog(
            title = stringResource(R.string.settings_remove_title),
            text = stringResource(R.string.settings_remove_message, server.name),
            confirmLabel = stringResource(R.string.settings_remove),
            destructive = true,
            onConfirm = { onEvent(SettingsEvent.ConfirmRemove) },
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            testTagPrefix = "settings_remove",
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = OcTheme.colors.textMuted,
            modifier = Modifier.padding(start = OcSpacing.xs).semantics { heading() },
        )
        content()
    }
}

@Composable
private fun ServersSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val c = OcTheme.colors
    Section(stringResource(R.string.settings_servers)) {
        OcCard {
            state.servers.forEach { server ->
                ServerRow(server, active = server.id == state.activeServerId, onEvent = onEvent)
                OcDivider()
            }
            OcListRow(
                title = stringResource(R.string.settings_add_server),
                titleColor = c.textAccent,
                onClick = { onEvent(SettingsEvent.AddServer) },
                leading = { Icon(Icons.Outlined.Add, null, tint = c.textAccent, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.testTag("settings_add_server_button"),
            )
        }
    }
}

@Composable
private fun ServerRow(server: ServerProfile, active: Boolean, onEvent: (SettingsEvent) -> Unit) {
    val c = OcTheme.colors
    var menu by rememberSaveable(server.id) { mutableStateOf(false) }
    val kind = stringResource(if (server.credential == CredentialKind.Password) R.string.settings_password else R.string.settings_paired)
    OcListRow(
        title = server.name,
        subtitle = "${server.baseUrl} · $kind",
        monoSubtitle = true,
        onClick = { if (!active) onEvent(SettingsEvent.SelectServer(server.id)) },
        leading = { OcProjectAvatar(server.name, size = 28.dp) },
        trailing = {
            if (active) OcBadge(stringResource(R.string.settings_active), c.success)
            Box {
                OcIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.settings_server_actions),
                    onClick = { menu = true },
                    tint = c.iconMuted,
                    modifier = Modifier.testTag("settings_server_menu_${server.id}"),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.bgBase, modifier = Modifier.exposeTestTags()) {
                    if (!active) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_switch)) },
                            onClick = { menu = false; onEvent(SettingsEvent.SelectServer(server.id)) },
                            modifier = Modifier.testTag("settings_server_switch_${server.id}"),
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_rename)) },
                        onClick = { menu = false; onEvent(SettingsEvent.RequestRename(server)) },
                        modifier = Modifier.testTag("settings_server_rename_${server.id}"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_remove), color = c.danger.fg) },
                        onClick = { menu = false; onEvent(SettingsEvent.RequestRemove(server)) },
                        modifier = Modifier.testTag("settings_server_remove_${server.id}"),
                    )
                }
            }
        },
        modifier = Modifier.testTag("settings_server_${server.id}"),
    )
}

@Composable
private fun AppearanceSection(theme: ThemeMode, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(R.string.settings_appearance)) {
        OcSegmentedControl(
            segments = listOf(
                OcSegment(ThemeMode.System, stringResource(R.string.settings_theme_system), "settings_theme_system"),
                OcSegment(ThemeMode.Light, stringResource(R.string.settings_theme_light), "settings_theme_light"),
                OcSegment(ThemeMode.Dark, stringResource(R.string.settings_theme_dark), "settings_theme_dark"),
            ),
            selected = theme,
            onSelect = { onEvent(SettingsEvent.ThemeChanged(it)) },
        )
    }
}

@Composable
private fun ConversationSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(R.string.settings_conversation)) {
        OcCard {
            OcSwitchRow(
                title = stringResource(R.string.settings_show_reasoning),
                subtitle = stringResource(R.string.settings_show_reasoning_sub),
                checked = settings.showReasoning,
                onCheckedChange = { onEvent(SettingsEvent.ShowReasoningChanged(it)) },
                modifier = Modifier.testTag("settings_show_reasoning_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(R.string.settings_expand_tools),
                subtitle = stringResource(R.string.settings_expand_tools_sub),
                checked = settings.expandToolsByDefault,
                onCheckedChange = { onEvent(SettingsEvent.ExpandToolsChanged(it)) },
                modifier = Modifier.testTag("settings_expand_tools_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(R.string.settings_group_activity),
                subtitle = stringResource(R.string.settings_group_activity_sub),
                checked = settings.groupActivity,
                onCheckedChange = { onEvent(SettingsEvent.GroupActivityChanged(it)) },
                modifier = Modifier.testTag("settings_group_activity_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(R.string.settings_show_cost),
                subtitle = stringResource(R.string.settings_show_cost_sub),
                checked = settings.showCost,
                onCheckedChange = { onEvent(SettingsEvent.ShowCostChanged(it)) },
                modifier = Modifier.testTag("settings_show_cost_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(R.string.settings_swipe_chats),
                subtitle = stringResource(R.string.settings_swipe_chats_sub),
                checked = settings.swipeBetweenChats,
                onCheckedChange = { onEvent(SettingsEvent.SwipeBetweenChatsChanged(it)) },
                modifier = Modifier.testTag("settings_swipe_chats_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(R.string.settings_agent_outline),
                subtitle = stringResource(R.string.settings_agent_outline_sub),
                checked = settings.agentColorOutline,
                onCheckedChange = { onEvent(SettingsEvent.AgentColorOutlineChanged(it)) },
                modifier = Modifier.testTag("settings_agent_outline_switch"),
            )
        }
    }
}

@Composable
private fun NotificationsSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val blocked = state.notificationsBlocked && !state.notificationsAllowed
    val on = state.settings.notifications && state.notificationsAllowed
    Section(stringResource(R.string.settings_notifications)) {
        OcCard {
            OcSwitchRow(
                title = stringResource(R.string.settings_session_notifications),
                subtitle = stringResource(
                    if (blocked) R.string.settings_session_notifications_blocked else R.string.settings_session_notifications_sub,
                ),
                checked = on,
                onCheckedChange = { onEvent(SettingsEvent.NotificationsChanged(it)) },
                modifier = Modifier.testTag("settings_notifications_switch"),
            )
            // Each kind only matters while session notifications are on.
            if (on) {
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_notify_permissions),
                    subtitle = stringResource(R.string.settings_notify_permissions_sub),
                    checked = state.settings.notifyPermissions,
                    onCheckedChange = { onEvent(SettingsEvent.NotifyPermissionsChanged(it)) },
                    modifier = Modifier.testTag("settings_notify_permissions_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_notify_questions),
                    subtitle = stringResource(R.string.settings_notify_questions_sub),
                    checked = state.settings.notifyQuestions,
                    onCheckedChange = { onEvent(SettingsEvent.NotifyQuestionsChanged(it)) },
                    modifier = Modifier.testTag("settings_notify_questions_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_notify_finished),
                    subtitle = stringResource(R.string.settings_notify_finished_sub),
                    checked = state.settings.notifyFinished,
                    onCheckedChange = { onEvent(SettingsEvent.NotifyFinishedChanged(it)) },
                    modifier = Modifier.testTag("settings_notify_finished_switch"),
                )
            }
        }
    }
}

@Composable
private fun AboutSection(state: SettingsState) {
    val c = OcTheme.colors
    Section(stringResource(R.string.settings_about)) {
        OcCard {
            AboutRow(stringResource(R.string.settings_app_version), state.appVersion)
            OcDivider()
            OcListRow(
                title = stringResource(R.string.settings_server_version),
                trailing = {
                    if (state.serverVersionLoading) {
                        OcSpinner()
                    } else {
                        Text(state.serverVersion ?: stringResource(R.string.settings_unknown), style = OcTheme.mono.codeSmall, color = c.textMuted)
                    }
                },
                modifier = Modifier.testTag("settings_server_version"),
            )
            OcDivider()
            AboutRow(stringResource(R.string.settings_api_version), state.apiVersion)
        }
    }
}

@Composable
private fun AboutRow(title: String, value: String) {
    OcListRow(
        title = title,
        trailing = { Text(value, style = OcTheme.mono.codeSmall, color = OcTheme.colors.textMuted) },
    )
}

@PreviewLightDark
@Composable
private fun SettingsScreenPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        SettingsScreen(
            state = SettingsState(
                servers = SampleData.servers,
                activeServerId = SampleData.servers.first().id,
                serverVersion = "2.0.27",
                appVersion = "dev",
                apiVersion = "2.0.27",
            ),
            onEvent = {},
        )
    }
}
