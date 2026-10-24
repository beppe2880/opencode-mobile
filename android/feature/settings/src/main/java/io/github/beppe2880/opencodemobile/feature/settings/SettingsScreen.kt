package io.github.beppe2880.opencodemobile.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.QueryStats
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
import io.github.beppe2880.opencodemobile.core.domain.LIVE_INTERVAL_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.OUTPUT_LINE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.TEXT_SCALE_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
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
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.R as UiR

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
                if (state.activeServerId != null) item(key = "usage") { UsageSection(onEvent) }
                item(key = "appearance") { AppearanceSection(state.settings, onEvent) }
                item(key = "conversation") { ConversationSection(state.settings, onEvent) }
                item(key = "live") { LiveActivitySection(state.settings, onEvent) }
                item(key = "notifications") { NotificationsSection(state, onEvent) }
                item(key = "reset") { ResetSection(onEvent) }
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
    if (state.resetting) {
        OcConfirmDialog(
            title = stringResource(R.string.settings_reset_title),
            text = stringResource(R.string.settings_reset_message),
            confirmLabel = stringResource(R.string.settings_reset_confirm),
            destructive = true,
            onConfirm = { onEvent(SettingsEvent.ConfirmReset) },
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            testTagPrefix = "settings_reset",
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
private fun UsageSection(onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(R.string.usage_title)) {
        OcCard {
            OcListRow(
                title = stringResource(R.string.settings_usage),
                subtitle = stringResource(R.string.settings_usage_sub),
                onClick = { onEvent(SettingsEvent.OpenUsage) },
                leading = { Icon(Icons.Outlined.QueryStats, contentDescription = null, tint = OcTheme.colors.iconMuted) },
                trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = OcTheme.colors.iconMuted) },
                modifier = Modifier.testTag("settings_usage_row"),
            )
        }
    }
}

@Composable
private fun AppearanceSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(R.string.settings_appearance)) {
        OcSegmentedControl(
            segments = listOf(
                OcSegment(ThemeMode.System, stringResource(R.string.settings_theme_system), "settings_theme_system"),
                OcSegment(ThemeMode.Light, stringResource(R.string.settings_theme_light), "settings_theme_light"),
                OcSegment(ThemeMode.Dark, stringResource(R.string.settings_theme_dark), "settings_theme_dark"),
            ),
            selected = settings.theme,
            onSelect = { onEvent(SettingsEvent.ThemeChanged(it)) },
        )
        OcCard {
            val sizes = listOf(R.string.settings_text_small, R.string.settings_text_default, R.string.settings_text_large, R.string.settings_text_largest)
            ChoiceRow(
                title = stringResource(R.string.settings_text_size),
                subtitle = stringResource(R.string.settings_text_size_sub),
                choices = TEXT_SCALE_CHOICES.mapIndexed { i, scale -> OcSegment(scale, stringResource(sizes[i]), "settings_text_size_$i") },
                selected = settings.chatTextScale,
                onSelect = { onEvent(SettingsEvent.ChatTextScaleChanged(it)) },
            )
        }
    }
}

/** A title and subtitle with a segmented choice under them, inside a card. */
@Composable
private fun <T> ChoiceRow(title: String, subtitle: String, choices: List<OcSegment<T>>, selected: T, onSelect: (T) -> Unit) {
    val c = OcTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        OcSegmentedControl(segments = choices, selected = selected, onSelect = onSelect)
    }
}

@Composable
private fun ResetSection(onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(R.string.settings_reset_section)) {
        OcButton(
            text = stringResource(R.string.settings_reset),
            onClick = { onEvent(SettingsEvent.RequestReset) },
            variant = OcButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth().testTag("settings_reset_button"),
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
            OcDivider()
            OcSwitchRow(
                title = stringResource(R.string.settings_dictation),
                subtitle = stringResource(R.string.settings_dictation_sub),
                checked = settings.dictation,
                onCheckedChange = { onEvent(SettingsEvent.DictationChanged(it)) },
                modifier = Modifier.testTag("settings_dictation_switch"),
            )
        }
    }
}

@Composable
private fun LiveActivitySection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(R.string.settings_live_activity)) {
        OcCard {
            OcSwitchRow(
                title = stringResource(R.string.settings_follow_running),
                subtitle = stringResource(R.string.settings_follow_running_sub),
                checked = settings.followRunningTools,
                onCheckedChange = { onEvent(SettingsEvent.FollowRunningToolsChanged(it)) },
                modifier = Modifier.testTag("settings_follow_running_switch"),
            )
            // Each kind only matters while following is on.
            if (settings.followRunningTools) {
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_live_thinking),
                    subtitle = stringResource(R.string.settings_live_thinking_sub),
                    checked = settings.liveThinking,
                    onCheckedChange = { onEvent(SettingsEvent.LiveThinkingChanged(it)) },
                    modifier = Modifier.testTag("settings_live_thinking_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_live_shell),
                    subtitle = stringResource(R.string.settings_live_shell_sub),
                    checked = settings.liveShell,
                    onCheckedChange = { onEvent(SettingsEvent.LiveShellChanged(it)) },
                    modifier = Modifier.testTag("settings_live_shell_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_live_edits),
                    subtitle = stringResource(R.string.settings_live_edits_sub),
                    checked = settings.liveEdits,
                    onCheckedChange = { onEvent(SettingsEvent.LiveEditsChanged(it)) },
                    modifier = Modifier.testTag("settings_live_edits_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_live_reads),
                    subtitle = stringResource(R.string.settings_live_reads_sub),
                    checked = settings.liveReads,
                    onCheckedChange = { onEvent(SettingsEvent.LiveReadsChanged(it)) },
                    modifier = Modifier.testTag("settings_live_reads_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_live_other),
                    subtitle = stringResource(R.string.settings_live_other_sub),
                    checked = settings.liveOther,
                    onCheckedChange = { onEvent(SettingsEvent.LiveOtherChanged(it)) },
                    modifier = Modifier.testTag("settings_live_other_switch"),
                )
            }
            OcDivider()
            OcSwitchRow(
                title = stringResource(R.string.settings_full_commands),
                subtitle = stringResource(R.string.settings_full_commands_sub),
                checked = settings.showFullCommands,
                onCheckedChange = { onEvent(SettingsEvent.ShowFullCommandsChanged(it)) },
                modifier = Modifier.testTag("settings_full_commands_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(R.string.settings_tool_details),
                subtitle = stringResource(R.string.settings_tool_details_sub),
                checked = settings.showToolDetails,
                onCheckedChange = { onEvent(SettingsEvent.ShowToolDetailsChanged(it)) },
                modifier = Modifier.testTag("settings_tool_details_switch"),
            )
            OcDivider()
            ChoiceRow(
                title = stringResource(R.string.settings_output_lines),
                subtitle = stringResource(R.string.settings_output_lines_sub),
                choices = OUTPUT_LINE_CHOICES.map { OcSegment(it, it.toString(), "settings_output_lines_$it") },
                selected = settings.outputPreviewLines,
                onSelect = { onEvent(SettingsEvent.OutputPreviewLinesChanged(it)) },
            )
            if (settings.followRunningTools) {
                OcDivider()
                OcSwitchRow(
                    title = stringResource(R.string.settings_keep_live_open),
                    subtitle = stringResource(R.string.settings_keep_live_open_sub),
                    checked = settings.keepLiveStepsOpen,
                    onCheckedChange = { onEvent(SettingsEvent.KeepLiveStepsOpenChanged(it)) },
                    modifier = Modifier.testTag("settings_keep_live_open_switch"),
                )
            }
            if (settings.followRunningTools && settings.liveShell) {
                OcDivider()
                val speeds =
                    listOf(R.string.settings_refresh_fastest, R.string.settings_refresh_fast, R.string.settings_refresh_normal, R.string.settings_refresh_slow)
                ChoiceRow(
                    title = stringResource(R.string.settings_live_interval),
                    subtitle = stringResource(R.string.settings_live_interval_sub),
                    choices = LIVE_INTERVAL_CHOICES.mapIndexed { i, ms -> OcSegment(ms, stringResource(speeds[i]), "settings_live_interval_$ms") },
                    selected = settings.liveOutputIntervalMs,
                    onSelect = { onEvent(SettingsEvent.LiveOutputIntervalChanged(it)) },
                )
            }
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
