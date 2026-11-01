package io.github.beppe2880.opencodemobile.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.TouchApp
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
import androidx.compose.ui.text.style.TextOverflow
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
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsStyle
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
import io.github.beppe2880.opencodemobile.core.ui.components.OcSearchField
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.components.OcSliderRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextInputDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcThumbnail
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.PartPreview
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.text.label
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.customize.AreaPreview
import io.github.beppe2880.opencodemobile.feature.settings.customize.AreaPreviewContent
import io.github.beppe2880.opencodemobile.feature.settings.customize.AreaScreen
import io.github.beppe2880.opencodemobile.feature.settings.customize.SettingOption
import io.github.beppe2880.opencodemobile.feature.settings.customize.SettingOptionControl
import io.github.beppe2880.opencodemobile.feature.settings.customize.descriptionRes
import io.github.beppe2880.opencodemobile.feature.settings.customize.searchOptions
import io.github.beppe2880.opencodemobile.feature.settings.customize.titleRes
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_about
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_active
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_add_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_api_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_app_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_appearance_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_chat_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_composer_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_live_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_notifications_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_sessions_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_terminal_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_area_updates_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_areas
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_demo_server
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_demo_server_hint
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_live_demo_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_paired
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_password
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
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_save
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_search
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_search_empty
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_search_result
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_server_actions
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_server_version
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_servers
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_style_list
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_style_preview
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_switch
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
    preview: AreaPreviewContent,
    modifier: Modifier = Modifier,
    showNotifications: Boolean = notificationsSupported,
    /** The offline demo: one demo entry instead of the saved servers. */
    demo: Boolean = false,
    /** Opens the offline demo to change settings on its real screens; null where there is none (iOS, the demo itself). */
    onOpenLiveDemo: (() -> Unit)? = null,
) {
    val areas = SettingsArea.entries.filter { showNotifications || it != SettingsArea.Notifications }
    OcBackHandler(enabled = state.area != null) { onEvent(SettingsEvent.CloseArea) }
    AnimatedContent(targetState = state.area, modifier = modifier, label = "settings_area") { area ->
        if (area != null) {
            AreaScreen(state, onEvent, preview)
        } else {
            SettingsHome(state, onEvent, preview, areas, demo, showNotifications, onOpenLiveDemo)
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

/**
 * The first screen of Settings, in the style the user picked at its top: the classic list, or
 * the groups of settings with a live miniature of their preview, a search for any option, then
 * servers, usage, reset and versions. Both offer the live demo where there is one.
 */
@Composable
private fun SettingsHome(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    preview: AreaPreviewContent,
    areas: List<SettingsArea>,
    demo: Boolean,
    showNotifications: Boolean,
    onOpenLiveDemo: (() -> Unit)?,
) {
    val list = state.settings.settingsStyle == SettingsStyle.List
    val c = OcTheme.colors
    Scaffold(
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
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            val width = minOf(maxWidth, 840.dp)
            val columns = ((width - OcSpacing.gutter * 2) / AreaCardMinWidth).toInt().coerceIn(2, 4)
            val results = searchResults(state.query, areas)
            LazyColumn(
                modifier = Modifier.widthIn(max = if (list) 640.dp else 840.dp).fillMaxSize().testTag("settings_list"),
                contentPadding = PaddingValues(
                    start = OcSpacing.gutter,
                    end = OcSpacing.gutter,
                    top = OcSpacing.lg,
                    bottom = OcSpacing.xxl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                item(key = "style") { StylePicker(state.settings.settingsStyle, onEvent, onOpenLiveDemo) }
                if (list) {
                    settingsList(state, onEvent, showNotifications, demo)
                    return@LazyColumn
                }
                item(key = "search") {
                    OcSearchField(
                        value = state.query,
                        onValueChange = { onEvent(SettingsEvent.QueryChanged(it)) },
                        placeholder = stringResource(Res.string.settings_search),
                        modifier = Modifier.padding(bottom = OcSpacing.sm).testTag("settings_search_field"),
                    )
                }
                if (results != null) {
                    if (results.isEmpty()) {
                        item(key = "no_results") {
                            Text(
                                stringResource(Res.string.settings_search_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textMuted,
                                modifier = Modifier.padding(OcSpacing.lg),
                            )
                        }
                    } else {
                        item(key = "results") { SearchResults(results, onEvent) }
                    }
                    return@LazyColumn
                }
                item(key = "areas_title") { SectionTitle(stringResource(Res.string.settings_areas)) }
                areas.chunked(columns).forEachIndexed { row, chunk ->
                    item(key = "areas_$row") {
                        Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                            chunk.forEach { area -> AreaCard(area, state, preview, onEvent, Modifier.weight(1f)) }
                            repeat(columns - chunk.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                item(key = "servers") { ServersSection(state, onEvent, demo) }
                if (state.activeServerId != null) item(key = "usage") { UsageSection(onEvent) }
                item(key = "reset") { ResetSection(onEvent) }
                item(key = "about") { AboutSection(state, onEvent, showUpdateNotice = false) }
            }
        }
    }
}

private val AreaCardMinWidth = 168.dp

/**
 * How Settings looks: the list or the previews, kept for next time, and the live demo where the
 * real screens can be changed in place.
 */
@Composable
private fun StylePicker(style: SettingsStyle, onEvent: (SettingsEvent) -> Unit, onOpenLiveDemo: (() -> Unit)?) {
    Column(Modifier.padding(bottom = OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        OcSegmentedControl(
            segments = listOf(
                OcSegment(SettingsStyle.List, stringResource(Res.string.settings_style_list), "settings_style_list"),
                OcSegment(SettingsStyle.Preview, stringResource(Res.string.settings_style_preview), "settings_style_preview"),
            ),
            selected = style,
            onSelect = { onEvent(SettingsEvent.SettingsStyleChanged(it)) },
        )
        if (onOpenLiveDemo != null) {
            OcCard {
                OcListRow(
                    title = stringResource(Res.string.settings_live_demo),
                    subtitle = stringResource(Res.string.settings_live_demo_sub),
                    onClick = onOpenLiveDemo,
                    leading = { Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = OcTheme.colors.textAccent) },
                    trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = OcTheme.colors.iconMuted) },
                    modifier = Modifier.testTag("settings_live_demo"),
                )
            }
        }
    }
}

/** Options matching [query] in [areas], or null while the query is blank. */
@Composable
private fun searchResults(query: String, areas: List<SettingsArea>): List<SettingOption>? {
    if (query.isBlank()) return null
    val titles = SettingOption.entries.associateWith { stringResource(it.titleRes) }
    val descriptions = SettingOption.entries.associateWith { option ->
        stringResource(option.descriptionRes) + " " + option.part.label() + " " + option.area.label()
    }
    return searchOptions(query, { titles.getValue(it) }, { descriptions.getValue(it) }).filter { it.area in areas }
}

@Composable
private fun SearchResults(results: List<SettingOption>, onEvent: (SettingsEvent) -> Unit) {
    OcCard {
        results.forEachIndexed { i, option ->
            if (i > 0) OcDivider()
            OcListRow(
                title = stringResource(option.titleRes),
                subtitle = stringResource(Res.string.settings_search_result, option.area.label(), option.part.label()),
                onClick = { onEvent(SettingsEvent.OpenArea(option.area, option.part, option)) },
                trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = OcTheme.colors.iconMuted) },
                modifier = Modifier.testTag("settings_result_${option.name.lowercase()}"),
            )
        }
    }
}

/** A group of settings: a live miniature of its preview, its name and what it covers. */
@Composable
private fun AreaCard(area: SettingsArea, state: SettingsState, preview: AreaPreviewContent, onEvent: (SettingsEvent) -> Unit, modifier: Modifier) {
    val c = OcTheme.colors
    val name = area.label()
    OcCard(
        onClick = { onEvent(SettingsEvent.OpenArea(area)) },
        modifier = modifier.testTag("settings_area_${area.name.lowercase()}"),
    ) {
        OcThumbnail(
            scale = THUMBNAIL_SCALE,
            modifier = Modifier.fillMaxWidth().height(112.dp).background(c.bgBase),
        ) {
            Column {
                AreaPreview(area, PartPreview(state.settings, marks = false, still = true), preview, updateTarget = state.updateTarget)
            }
        }
        OcDivider()
        Column(Modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm)) {
            Text(name, style = MaterialTheme.typography.titleSmall, color = c.textBase, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(area.summary), style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Miniatures are drawn at this scale: a phone-wide preview fits a card of half a phone. */
private const val THUMBNAIL_SCALE = 0.42f

private val SettingsArea.summary
    get() = when (this) {
        SettingsArea.Appearance -> Res.string.settings_area_appearance_sub
        SettingsArea.Chat -> Res.string.settings_area_chat_sub
        SettingsArea.LiveActivity -> Res.string.settings_area_live_sub
        SettingsArea.Composer -> Res.string.settings_area_composer_sub
        SettingsArea.Sessions -> Res.string.settings_area_sessions_sub
        SettingsArea.Terminal -> Res.string.settings_area_terminal_sub
        SettingsArea.Notifications -> Res.string.settings_area_notifications_sub
        SettingsArea.Updates -> Res.string.settings_area_updates_sub
    }

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = OcTheme.colors.textMuted,
        modifier = Modifier.padding(start = OcSpacing.xs, top = OcSpacing.xs).semantics { heading() },
    )
}

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
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
internal fun ServersSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit, demo: Boolean) {
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

@Composable
internal fun UsageSection(onEvent: (SettingsEvent) -> Unit) {
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
internal fun ResetSection(onEvent: (SettingsEvent) -> Unit) {
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
internal fun AboutSection(state: SettingsState, onEvent: (SettingsEvent) -> Unit, showUpdateNotice: Boolean) {
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
            if (showUpdateNotice) {
                OcDivider()
                SettingOptionControl(SettingOption.UpdateNotice, state, onEvent)
            }
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
            preview = { _, _, modifier -> Box(modifier) },
        )
    }
}
