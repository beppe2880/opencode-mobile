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
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Science
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.DEFAULT_LIST_PANE_FRACTION
import io.github.beppe2880.opencodemobile.core.domain.LIST_PANE_FRACTION_RANGE
import io.github.beppe2880.opencodemobile.core.domain.LIVE_INTERVAL_CHOICES
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
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
import io.github.beppe2880.opencodemobile.core.ui.components.OcSliderRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextInputDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.customize.ChatOption
import io.github.beppe2880.opencodemobile.feature.settings.customize.ChatOptionControl
import io.github.beppe2880.opencodemobile.feature.settings.customize.appliesTo
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_about
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_active
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_add_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_api_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_appearance
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_back_shows_list
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_back_shows_list_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_check_updates
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_check_updates_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_confirm_stop
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_confirm_stop_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_conversation
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_customize_chat
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_customize_chat_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_demo_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_demo_server_hint
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_follow_running
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_follow_running_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_keep_live_open
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_keep_live_open_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_handle
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_handle_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_width
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_width_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_list_width_value
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_activity
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_edits
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_edits_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_interval
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_interval_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_other
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_other_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_reads
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_reads_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_shell
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_shell_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_thinking
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_thinking_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model_last
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_new_chat_model_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notifications
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_finished
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_finished_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_permissions
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_permissions_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_questions
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_notify_questions_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_paired
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_password
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_refresh_fast
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_refresh_fastest
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_refresh_normal
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_refresh_slow
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_remove
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_remove_message
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_remove_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_rename
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_rename_label
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_rename_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_reset
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_reset_confirm
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_reset_message
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_reset_section
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_reset_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_running_in_list
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_running_in_list_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_save
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply_next
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply_now
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_selection_apply_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_server_actions
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_server_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_servers
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_session_notifications
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_session_notifications_blocked
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_session_notifications_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_split_view
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_split_view_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_swipe_chats_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_switch
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_fullscreen
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_fullscreen_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_keyboard
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_terminal_keyboard_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_dark
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_light
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_theme_system
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_unknown
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_usage
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_usage_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.usage_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
    showNotifications: Boolean = notificationsSupported,
    /** The offline demo: one demo entry instead of the saved servers. */
    demo: Boolean = false,
) {
    val c = OcTheme.colors
    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = stringResource(Res.string.settings_title),
                leading = {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiRes.string.oc_back),
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
                item(key = "customize") { CustomizeEntry(onEvent) }
                item(key = "servers") { ServersSection(state, onEvent, demo) }
                if (state.activeServerId != null) item(key = "usage") { UsageSection(onEvent) }
                item(key = "appearance") { AppearanceSection(state.settings, onEvent) }
                item(key = "conversation") { ConversationSection(state.settings, onEvent) }
                item(key = "live") { LiveActivitySection(state.settings, onEvent) }
                if (showNotifications) item(key = "notifications") { NotificationsSection(state, onEvent) }
                item(key = "terminal") { TerminalSection(state.settings, onEvent) }
                item(key = "reset") { ResetSection(onEvent) }
                item(key = "about") { AboutSection(state, onEvent) }
            }
        }
    }
    state.renaming?.let { server ->
        OcTextInputDialog(
            title = stringResource(Res.string.settings_rename_title),
            initialValue = server.name,
            confirmLabel = stringResource(Res.string.settings_save),
            label = stringResource(Res.string.settings_rename_label),
            onConfirm = { onEvent(SettingsEvent.ConfirmRename(it)) },
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            testTagPrefix = "settings_rename",
        )
    }
    if (state.resetting) {
        OcConfirmDialog(
            title = stringResource(Res.string.settings_reset_title),
            text = stringResource(Res.string.settings_reset_message),
            confirmLabel = stringResource(Res.string.settings_reset_confirm),
            destructive = true,
            onConfirm = { onEvent(SettingsEvent.ConfirmReset) },
            onDismiss = { onEvent(SettingsEvent.DismissDialog) },
            testTagPrefix = "settings_reset",
        )
    }
    state.removing?.let { server ->
        OcConfirmDialog(
            title = stringResource(Res.string.settings_remove_title),
            text = stringResource(Res.string.settings_remove_message, server.name),
            confirmLabel = stringResource(Res.string.settings_remove),
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
private fun ServersSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit, demo: Boolean) {
    val c = OcTheme.colors
    Section(stringResource(Res.string.settings_servers)) {
        OcCard {
            if (demo) {
                OcListRow(
                    title = stringResource(Res.string.settings_demo_server),
                    subtitle = stringResource(Res.string.settings_demo_server_hint),
                    leading = { Icon(Icons.Outlined.Science, null, tint = c.iconMuted, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("settings_demo_server"),
                )
                OcDivider()
            } else {
                state.servers.forEach { server ->
                    ServerRow(server, active = server.id == state.activeServerId, onEvent = onEvent)
                    OcDivider()
                }
            }
            OcListRow(
                title = stringResource(Res.string.settings_add_server),
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
    val kind = stringResource(if (server.credential == CredentialKind.Password) Res.string.settings_password else Res.string.settings_paired)
    OcListRow(
        title = server.name,
        subtitle = "${server.baseUrl} · $kind",
        monoSubtitle = true,
        onClick = { if (!active) onEvent(SettingsEvent.SelectServer(server.id)) },
        leading = { OcProjectAvatar(server.name, size = 28.dp) },
        trailing = {
            if (active) OcBadge(stringResource(Res.string.settings_active), c.success)
            Box {
                OcIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(Res.string.settings_server_actions),
                    onClick = { menu = true },
                    tint = c.iconMuted,
                    modifier = Modifier.testTag("settings_server_menu_${server.id}"),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.bgBase, modifier = Modifier.exposeTestTags()) {
                    if (!active) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.settings_switch)) },
                            onClick = { menu = false; onEvent(SettingsEvent.SelectServer(server.id)) },
                            modifier = Modifier.testTag("settings_server_switch_${server.id}"),
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.settings_rename)) },
                        onClick = { menu = false; onEvent(SettingsEvent.RequestRename(server)) },
                        modifier = Modifier.testTag("settings_server_rename_${server.id}"),
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.settings_remove), color = c.danger.fg) },
                        onClick = { menu = false; onEvent(SettingsEvent.RequestRemove(server)) },
                        modifier = Modifier.testTag("settings_server_remove_${server.id}"),
                    )
                }
            }
        },
        modifier = Modifier.testTag("settings_server_${server.id}"),
    )
}

/** Opens the chat customizer: the first thing most people look for in Settings. */
@Composable
private fun CustomizeEntry(onEvent: (SettingsEvent) -> Unit) {
    val c = OcTheme.colors
    OcCard(Modifier.padding(bottom = OcSpacing.lg)) {
        OcListRow(
            title = stringResource(Res.string.settings_customize_chat),
            subtitle = stringResource(Res.string.settings_customize_chat_sub),
            onClick = { onEvent(SettingsEvent.OpenCustomizer) },
            leading = { Icon(Icons.Outlined.Palette, contentDescription = null, tint = c.textAccent) },
            trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.iconMuted) },
            modifier = Modifier.testTag("settings_customize_row"),
        )
    }
}

@Composable
private fun UsageSection(onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(Res.string.usage_title)) {
        OcCard {
            OcListRow(
                title = stringResource(Res.string.settings_usage),
                subtitle = stringResource(Res.string.settings_usage_sub),
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
    // Only wide windows (unfolded foldables, tablets) can show the chat beside the list.
    val wide = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() } >= SplitViewMinWidth
    Section(stringResource(Res.string.settings_appearance)) {
        OcSegmentedControl(
            segments = listOf(
                OcSegment(ThemeMode.System, stringResource(Res.string.settings_theme_system), "settings_theme_system"),
                OcSegment(ThemeMode.Light, stringResource(Res.string.settings_theme_light), "settings_theme_light"),
                OcSegment(ThemeMode.Dark, stringResource(Res.string.settings_theme_dark), "settings_theme_dark"),
            ),
            selected = settings.theme,
            onSelect = { onEvent(SettingsEvent.ThemeChanged(it)) },
        )
        OcCard {
            ChatOptionControl(ChatOption.TextSize, settings, onEvent)
            if (wide) {
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_split_view),
                    subtitle = stringResource(Res.string.settings_split_view_sub),
                    checked = settings.splitView,
                    onCheckedChange = { onEvent(SettingsEvent.SplitViewChanged(it)) },
                    modifier = Modifier.testTag("settings_split_view_switch"),
                )
                if (settings.splitView) {
                    OcDivider()
                    val fraction = settings.listPaneFraction ?: DEFAULT_LIST_PANE_FRACTION
                    OcSliderRow(
                        title = stringResource(Res.string.settings_list_width),
                        subtitle = stringResource(Res.string.settings_list_width_sub),
                        value = fraction.coerceIn(LIST_PANE_FRACTION_RANGE),
                        onValueChange = { onEvent(SettingsEvent.ListPaneFractionChanged(it)) },
                        valueRange = LIST_PANE_FRACTION_RANGE,
                        // 5% steps.
                        steps = ((LIST_PANE_FRACTION_RANGE.endInclusive - LIST_PANE_FRACTION_RANGE.start) * 20).roundToInt() - 1,
                        valueLabel = stringResource(Res.string.settings_list_width_value, (fraction * 100).roundToInt()),
                        modifier = Modifier.testTag("settings_list_width_slider"),
                    )
                    OcDivider()
                    OcSwitchRow(
                        title = stringResource(Res.string.settings_list_handle),
                        subtitle = stringResource(Res.string.settings_list_handle_sub),
                        checked = settings.listPaneHandle,
                        onCheckedChange = { onEvent(SettingsEvent.ListPaneHandleChanged(it)) },
                        modifier = Modifier.testTag("settings_list_handle_switch"),
                    )
                    if (settings.listPaneHandle) {
                        OcDivider()
                        OcSwitchRow(
                            title = stringResource(Res.string.settings_back_shows_list),
                            subtitle = stringResource(Res.string.settings_back_shows_list_sub),
                            checked = settings.backShowsList,
                            onCheckedChange = { onEvent(SettingsEvent.BackShowsListChanged(it)) },
                            modifier = Modifier.testTag("settings_back_shows_list_switch"),
                        )
                    }
                }
            }
        }
    }
}

/** Matches the width at which the app shows the chat beside the session list. */
private val SplitViewMinWidth = 600.dp

/** A title and subtitle with a segmented choice under them, inside a card. */
@Composable
internal fun <T> ChoiceRow(title: String, subtitle: String, choices: List<OcSegment<T>>, selected: T, onSelect: (T) -> Unit) {
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
private fun TerminalSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(Res.string.settings_terminal)) {
        OcCard {
            OcSwitchRow(
                title = stringResource(Res.string.settings_terminal_keyboard),
                subtitle = stringResource(Res.string.settings_terminal_keyboard_sub),
                checked = settings.terminalOpenKeyboard,
                onCheckedChange = { onEvent(SettingsEvent.TerminalOpenKeyboardChanged(it)) },
                modifier = Modifier.testTag("settings_terminal_keyboard_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(Res.string.settings_terminal_fullscreen),
                subtitle = stringResource(Res.string.settings_terminal_fullscreen_sub),
                checked = settings.terminalFullscreenByDefault,
                onCheckedChange = { onEvent(SettingsEvent.TerminalFullscreenByDefaultChanged(it)) },
                modifier = Modifier.testTag("settings_terminal_fullscreen_switch"),
            )
        }
    }
}

@Composable
private fun ResetSection(onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(Res.string.settings_reset_section)) {
        OcButton(
            text = stringResource(Res.string.settings_reset),
            onClick = { onEvent(SettingsEvent.RequestReset) },
            variant = OcButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth().testTag("settings_reset_button"),
        )
    }
}

@Composable
private fun ConversationSection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(Res.string.settings_conversation)) {
        OcCard {
            ChatOptionControl(ChatOption.ShowReasoning, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.ExpandTools, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.GroupActivity, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.GroupThoughts, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.ShowCost, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.HeaderLine, settings, onEvent)
            if (ChatOption.HeaderStyle.appliesTo(settings)) {
                OcDivider()
                ChatOptionControl(ChatOption.HeaderStyle, settings, onEvent)
                OcDivider()
                ChatOptionControl(ChatOption.HeaderOpens, settings, onEvent)
            }
            OcDivider()
            OcSwitchRow(
                title = stringResource(Res.string.settings_swipe_chats),
                subtitle = stringResource(Res.string.settings_swipe_chats_sub),
                checked = settings.swipeBetweenChats,
                onCheckedChange = { onEvent(SettingsEvent.SwipeBetweenChatsChanged(it)) },
                modifier = Modifier.testTag("settings_swipe_chats_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(Res.string.settings_running_in_list),
                subtitle = stringResource(Res.string.settings_running_in_list_sub),
                checked = settings.showRunningWorkInList,
                onCheckedChange = { onEvent(SettingsEvent.RunningInListChanged(it)) },
                modifier = Modifier.testTag("settings_running_in_list_switch"),
            )
            OcDivider()
            OcSwitchRow(
                title = stringResource(Res.string.settings_confirm_stop),
                subtitle = stringResource(Res.string.settings_confirm_stop_sub),
                checked = settings.confirmStopCommand,
                onCheckedChange = { onEvent(SettingsEvent.ConfirmStopChanged(it)) },
                modifier = Modifier.testTag("settings_confirm_stop_switch"),
            )
            OcDivider()
            ChatOptionControl(ChatOption.AgentOutline, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.QuestionsCollapsed, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.PermissionsCollapsed, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.Dictation, settings, onEvent)
            OcDivider()
            ChoiceRow(
                title = stringResource(Res.string.settings_selection_apply),
                subtitle = stringResource(Res.string.settings_selection_apply_sub),
                choices = listOf(
                    OcSegment(SelectionApply.NextMessage, stringResource(Res.string.settings_selection_apply_next), "settings_selection_apply_next"),
                    OcSegment(SelectionApply.Immediately, stringResource(Res.string.settings_selection_apply_now), "settings_selection_apply_now"),
                ),
                selected = settings.selectionApply,
                onSelect = { onEvent(SettingsEvent.SelectionApplyChanged(it)) },
            )
            OcDivider()
            ChoiceRow(
                title = stringResource(Res.string.settings_new_chat_model),
                subtitle = stringResource(Res.string.settings_new_chat_model_sub),
                choices = listOf(
                    OcSegment(NewChatModel.LastUsed, stringResource(Res.string.settings_new_chat_model_last), "settings_new_chat_model_last"),
                    OcSegment(NewChatModel.ServerDefault, stringResource(Res.string.settings_new_chat_model_server), "settings_new_chat_model_server"),
                ),
                selected = settings.newChatModel,
                onSelect = { onEvent(SettingsEvent.NewChatModelChanged(it)) },
            )
        }
    }
}

@Composable
private fun LiveActivitySection(settings: AppSettings, onEvent: (SettingsEvent) -> Unit) {
    Section(stringResource(Res.string.settings_live_activity)) {
        OcCard {
            OcSwitchRow(
                title = stringResource(Res.string.settings_follow_running),
                subtitle = stringResource(Res.string.settings_follow_running_sub),
                checked = settings.followRunningTools,
                onCheckedChange = { onEvent(SettingsEvent.FollowRunningToolsChanged(it)) },
                modifier = Modifier.testTag("settings_follow_running_switch"),
            )
            // Each kind only matters while following is on.
            if (settings.followRunningTools) {
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_live_thinking),
                    subtitle = stringResource(Res.string.settings_live_thinking_sub),
                    checked = settings.liveThinking,
                    onCheckedChange = { onEvent(SettingsEvent.LiveThinkingChanged(it)) },
                    modifier = Modifier.testTag("settings_live_thinking_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_live_shell),
                    subtitle = stringResource(Res.string.settings_live_shell_sub),
                    checked = settings.liveShell,
                    onCheckedChange = { onEvent(SettingsEvent.LiveShellChanged(it)) },
                    modifier = Modifier.testTag("settings_live_shell_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_live_edits),
                    subtitle = stringResource(Res.string.settings_live_edits_sub),
                    checked = settings.liveEdits,
                    onCheckedChange = { onEvent(SettingsEvent.LiveEditsChanged(it)) },
                    modifier = Modifier.testTag("settings_live_edits_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_live_reads),
                    subtitle = stringResource(Res.string.settings_live_reads_sub),
                    checked = settings.liveReads,
                    onCheckedChange = { onEvent(SettingsEvent.LiveReadsChanged(it)) },
                    modifier = Modifier.testTag("settings_live_reads_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_live_other),
                    subtitle = stringResource(Res.string.settings_live_other_sub),
                    checked = settings.liveOther,
                    onCheckedChange = { onEvent(SettingsEvent.LiveOtherChanged(it)) },
                    modifier = Modifier.testTag("settings_live_other_switch"),
                )
            }
            OcDivider()
            ChatOptionControl(ChatOption.FullCommands, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.ToolDetails, settings, onEvent)
            OcDivider()
            ChatOptionControl(ChatOption.OutputLines, settings, onEvent)
            if (settings.followRunningTools) {
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_keep_live_open),
                    subtitle = stringResource(Res.string.settings_keep_live_open_sub),
                    checked = settings.keepLiveStepsOpen,
                    onCheckedChange = { onEvent(SettingsEvent.KeepLiveStepsOpenChanged(it)) },
                    modifier = Modifier.testTag("settings_keep_live_open_switch"),
                )
            }
            if (settings.followRunningTools && settings.liveShell) {
                OcDivider()
                val speeds = listOf(
                    Res.string.settings_refresh_fastest,
                    Res.string.settings_refresh_fast,
                    Res.string.settings_refresh_normal,
                    Res.string.settings_refresh_slow,
                )
                ChoiceRow(
                    title = stringResource(Res.string.settings_live_interval),
                    subtitle = stringResource(Res.string.settings_live_interval_sub),
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
    Section(stringResource(Res.string.settings_notifications)) {
        OcCard {
            OcSwitchRow(
                title = stringResource(Res.string.settings_session_notifications),
                subtitle = stringResource(
                    if (blocked) Res.string.settings_session_notifications_blocked else Res.string.settings_session_notifications_sub,
                ),
                checked = on,
                onCheckedChange = { onEvent(SettingsEvent.NotificationsChanged(it)) },
                modifier = Modifier.testTag("settings_notifications_switch"),
            )
            // Each kind only matters while session notifications are on.
            if (on) {
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_notify_permissions),
                    subtitle = stringResource(Res.string.settings_notify_permissions_sub),
                    checked = state.settings.notifyPermissions,
                    onCheckedChange = { onEvent(SettingsEvent.NotifyPermissionsChanged(it)) },
                    modifier = Modifier.testTag("settings_notify_permissions_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_notify_questions),
                    subtitle = stringResource(Res.string.settings_notify_questions_sub),
                    checked = state.settings.notifyQuestions,
                    onCheckedChange = { onEvent(SettingsEvent.NotifyQuestionsChanged(it)) },
                    modifier = Modifier.testTag("settings_notify_questions_switch"),
                )
                OcDivider()
                OcSwitchRow(
                    title = stringResource(Res.string.settings_notify_finished),
                    subtitle = stringResource(Res.string.settings_notify_finished_sub),
                    checked = state.settings.notifyFinished,
                    onCheckedChange = { onEvent(SettingsEvent.NotifyFinishedChanged(it)) },
                    modifier = Modifier.testTag("settings_notify_finished_switch"),
                )
            }
        }
    }
}

@Composable
private fun AboutSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val c = OcTheme.colors
    Section(stringResource(Res.string.settings_about)) {
        OcCard {
            AboutRow(stringResource(Res.string.settings_app_version), state.appVersion)
            OcDivider()
            OcListRow(
                title = stringResource(Res.string.settings_server_version),
                trailing = {
                    if (state.serverVersionLoading) {
                        OcSpinner()
                    } else {
                        Text(state.serverVersion ?: stringResource(Res.string.settings_unknown), style = OcTheme.mono.codeSmall, color = c.textMuted)
                    }
                },
                modifier = Modifier.testTag("settings_server_version"),
            )
            OcDivider()
            AboutRow(stringResource(Res.string.settings_api_version), state.apiVersion)
            OcDivider()
            OcSwitchRow(
                title = stringResource(Res.string.settings_check_updates),
                subtitle = stringResource(Res.string.settings_check_updates_sub),
                checked = state.settings.checkForUpdates,
                onCheckedChange = { onEvent(SettingsEvent.CheckForUpdatesChanged(it)) },
                modifier = Modifier.testTag("settings_check_updates_switch"),
            )
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
