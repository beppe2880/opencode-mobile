package io.github.beppe2880.opencodemobile.feature.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.text.label
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.customize.SettingOptionControl
import io.github.beppe2880.opencodemobile.feature.settings.customize.anchor
import io.github.beppe2880.opencodemobile.feature.settings.customize.area
import io.github.beppe2880.opencodemobile.feature.settings.customize.depthIn
import io.github.beppe2880.opencodemobile.feature.settings.customize.description
import io.github.beppe2880.opencodemobile.feature.settings.customize.summary
import io.github.beppe2880.opencodemobile.feature.settings.customize.visibleGroups
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/*
 * The List style: a first page with one row per category and its current values, and a page per
 * category with its options in groups, the dependent ones nested under the option they need.
 */

/** The categories as rows of the first page, in three cards: servers, the app, then updates and about. */
internal fun LazyListScope.categoryRows(
    areas: List<SettingsArea>,
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    wide: Boolean,
    demo: Boolean,
    /** The category shown beside the list on wide windows. */
    selected: SettingsArea?,
) {
    val cards = listOf(
        areas.filter { it == SettingsArea.Servers },
        areas.filter { it != SettingsArea.Servers && it != SettingsArea.Updates && it != SettingsArea.About },
        areas.filter { it == SettingsArea.Updates || it == SettingsArea.About },
    ).filter { it.isNotEmpty() }
    cards.forEachIndexed { i, card ->
        item(key = "categories_$i") {
            OcCard(Modifier.padding(bottom = OcSpacing.sm)) {
                card.forEachIndexed { j, area ->
                    if (j > 0) OcDivider()
                    CategoryRow(area, state, onEvent, wide, demo, selected = area == selected)
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(area: SettingsArea, state: SettingsState, onEvent: (SettingsEvent) -> Unit, wide: Boolean, demo: Boolean, selected: Boolean) {
    val c = OcTheme.colors
    val values = summary(area, state, wide, demo).map { it.text() }
    OcListRow(
        title = area.label(),
        subtitle = values.joinToString(" · ").ifEmpty { stringResource(area.description) },
        onClick = { onEvent(SettingsEvent.OpenArea(area)) },
        leading = { Icon(area.icon, contentDescription = null, tint = if (selected) c.textAccent else c.iconMuted) },
        trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.iconMuted) },
        modifier = Modifier
            .background(if (selected) c.layer02 else Color.Transparent)
            .semantics { this.selected = selected }
            .testTag("settings_category_${area.name.lowercase()}"),
    )
}

/** Resolves nested texts too: a value inside a phrase, like the model in "New chats: …". */
@Composable
internal fun UiText.text(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> stringResource(id, *args.map { if (it is UiText) it.text() else it }.toTypedArray())
}

internal val SettingsArea.icon: ImageVector
    get() = when (this) {
        SettingsArea.Servers -> Icons.Outlined.Dns
        SettingsArea.Appearance -> Icons.Outlined.Palette
        SettingsArea.Chat -> Icons.Outlined.ChatBubbleOutline
        SettingsArea.LiveActivity -> Icons.Outlined.Bolt
        SettingsArea.Composer -> Icons.Outlined.EditNote
        SettingsArea.VoiceInput -> Icons.Outlined.Mic
        SettingsArea.Sessions -> Icons.AutoMirrored.Outlined.ViewList
        SettingsArea.Sharing -> Icons.Outlined.Share
        SettingsArea.Notifications -> Icons.Outlined.Notifications
        SettingsArea.Terminal -> Icons.Outlined.Terminal
        SettingsArea.Updates -> Icons.Outlined.SystemUpdate
        SettingsArea.About -> Icons.Outlined.Info
    }

/**
 * The page of a category. [showBack] is false beside the first page on wide windows, where Back
 * leaves Settings. A search result's option is scrolled into view and briefly highlighted, or the
 * option that brings it back when its parent hides it.
 */
@Composable
internal fun CategoryScreen(
    area: SettingsArea,
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    showBack: Boolean,
    wide: Boolean,
    demo: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val scroll = rememberScrollState()
    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = area.label(),
                leading = if (showBack) {
                    {
                        OcIconButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(UiRes.string.oc_back),
                            onClick = { onEvent(SettingsEvent.CloseArea) },
                            modifier = Modifier.testTag("category_back_button"),
                        )
                    }
                } else {
                    null
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = OcSpacing.gutter, end = OcSpacing.gutter, top = OcSpacing.lg, bottom = OcSpacing.xxl)
                    .testTag("category_${area.name.lowercase()}"),
            ) {
                when (area) {
                    SettingsArea.Servers -> {
                        ServersSection(state, onEvent, demo, titled = false)
                        if (state.activeServerId != null) UsageSection(onEvent)
                    }
                    SettingsArea.About -> {
                        AboutSection(state, onEvent, showUpdateNotice = false)
                        ResetSection(onEvent)
                    }
                    else -> CategoryOptions(area, state, onEvent, wide, scroll)
                }
            }
        }
    }
}

@Composable
private fun CategoryOptions(area: SettingsArea, state: SettingsState, onEvent: (SettingsEvent) -> Unit, wide: Boolean, scroll: ScrollState) {
    val c = OcTheme.colors
    val target = state.focus?.takeIf { it.area == area }?.anchor(state.settings, state.notificationsOn)
    val highlight = remember(target) { Animatable(if (target != null) 1f else 0f) }
    // The options and the target, once laid out: where the target is below the top of the options.
    var page by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var placed by remember(target) { mutableStateOf<LayoutCoordinates?>(null) }
    val margin = with(LocalDensity.current) { OcSpacing.xl.toPx() }
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        val top = snapshotFlow { page?.let { p -> placed?.let { p.localPositionOf(it, Offset.Zero).y } } }.filterNotNull().first()
        // Near the top of the page, with its group title in sight.
        scroll.animateScrollTo((top - margin).roundToInt().coerceAtLeast(0))
        delay(HIGHLIGHT_HOLD_MS)
        highlight.animateTo(0f, tween(HIGHLIGHT_FADE_MS))
    }
    Column(Modifier.onGloballyPositioned { page = it }) {
        visibleGroups(area, state.settings, state.notificationsOn, wide).forEach { group ->
            val content: @Composable () -> Unit = {
                OcCard {
                    group.options.forEachIndexed { i, option ->
                        val indent = NestIndent * option.depthIn(group)
                        // A nested option's divider starts at its indent, so it reads as part of its parent.
                        if (i > 0) OcDivider(Modifier.padding(start = indent))
                        val focused = option == target
                        Box(
                            Modifier
                                .then(
                                    if (focused) {
                                        Modifier.onGloballyPositioned { placed = it }
                                    } else {
                                        Modifier
                                    },
                                )
                                .background(if (focused) c.info.bg.copy(alpha = c.info.bg.alpha * highlight.value) else Color.Transparent)
                                .padding(start = indent)
                                .testTag("settings_option_${option.name.lowercase()}"),
                        ) {
                            SettingOptionControl(option, state, onEvent)
                        }
                    }
                }
            }
            val title = group.title
            if (title != null) Section(stringResource(title), content) else Column(Modifier.padding(bottom = OcSpacing.lg)) { content() }
        }
    }
}

/** Indent of an option under the one it depends on, per level. */
private val NestIndent = OcSpacing.lg

private const val HIGHLIGHT_HOLD_MS = 1200L
private const val HIGHLIGHT_FADE_MS = 1200
