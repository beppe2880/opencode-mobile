package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.PartPreview
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.text.label
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent
import io.github.beppe2880.opencodemobile.feature.settings.SettingsState
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_marks_hide
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_marks_show
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_option_needs
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_part_hidden
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_collapse
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_expand
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_finished
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_running
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.min
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/**
 * A group of settings: its live preview and the options of each of its parts. Tapping a part
 * of the preview scrolls its options into view; scrolling the options selects the part at the
 * top, which the preview scrolls to.
 *
 * Phones in portrait (the main case) stack them: the preview takes about 40% of the height and
 * shrinks to a strip while the options scroll up, growing back when they return to the top or
 * when its handle is dragged or tapped; the options stay in the lower, reachable half. Wide or
 * landscape windows show them side by side.
 */
@Composable
internal fun AreaScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    content: AreaPreviewContent,
    modifier: Modifier = Modifier,
) {
    val area = state.area ?: return
    val c = OcTheme.colors
    val marks = state.settings.showEditMarks
    val preview = PartPreview(
        settings = state.settings,
        marks = marks,
        selected = state.selected,
        running = state.running,
        onCustomize = { onEvent(SettingsEvent.SelectPart(it)) },
    )
    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = area.label(),
                leading = {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiRes.string.oc_back),
                        onClick = { onEvent(SettingsEvent.CloseArea) },
                        modifier = Modifier.testTag("area_back_button"),
                    )
                },
                actions = {
                    if (area == SettingsArea.Chat) {
                        OcIconButton(
                            icon = if (state.running) Icons.Outlined.StopCircle else Icons.Outlined.PlayCircle,
                            contentDescription = stringResource(
                                if (state.running) Res.string.settings_preview_finished else Res.string.settings_preview_running,
                            ),
                            onClick = { onEvent(SettingsEvent.PreviewRunning(!state.running)) },
                            modifier = Modifier.testTag("area_running_button"),
                        )
                    }
                    OcIconButton(
                        icon = if (marks) Icons.Outlined.EditOff else Icons.Outlined.Edit,
                        contentDescription = stringResource(if (marks) Res.string.settings_marks_hide else Res.string.settings_marks_show),
                        onClick = { onEvent(SettingsEvent.EditMarksChanged(!marks)) },
                        modifier = Modifier.testTag("area_marks_button"),
                    )
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            val parts = remember(area) { SettingsPart.of(area) }
            val listState = rememberLazyListState()
            OptionsFollowPreview(parts, state.selected, listState, onSelect = { onEvent(SettingsEvent.SelectPart(it)) })
            val frame: @Composable (Modifier) -> Unit = { frameModifier ->
                PreviewFrame(area, preview, content, state, frameModifier)
            }
            // Room for the last parts' options to reach the top, where they select their part.
            val tail = maxHeight / 2
            if (maxWidth >= ExpandedWidth || (maxWidth > maxHeight && maxWidth >= SideBySideMinWidth)) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(start = OcSpacing.md, end = OcSpacing.md, top = OcSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
                ) {
                    frame(Modifier.weight(1f).fillMaxHeight().padding(bottom = OcSpacing.md))
                    OptionsList(parts, state, onEvent, listState, tail, Modifier.weight(1f).widthIn(max = 520.dp).fillMaxHeight())
                }
            } else {
                StackedLayout(
                    available = maxHeight,
                    frame = frame,
                    options = { optionsModifier -> OptionsList(parts, state, onEvent, listState, tail, optionsModifier) },
                    area = area,
                )
            }
        }
    }
}

private val SideBySideMinWidth = 560.dp
private val ExpandedWidth = 840.dp

@Composable
private fun StackedLayout(
    available: Dp,
    frame: @Composable (Modifier) -> Unit,
    options: @Composable (Modifier) -> Unit,
    area: SettingsArea,
) {
    val c = OcTheme.colors
    val density = LocalDensity.current
    val expanded = (available * 0.42f).coerceIn(160.dp, 440.dp)
    val collapsed = (available * 0.2f).coerceIn(96.dp, 160.dp)
    var height by rememberSaveable(area) { mutableStateOf(expanded.value) }
    val currentHeight = height.dp.coerceIn(collapsed, expanded)
    val shrink by rememberUpdatedState(collapsed)
    val grow by rememberUpdatedState(expanded)
    // Scrolling the options up first shrinks the preview; pulling them down at the top grows it.
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = with(density) { available.y.toDp() }
                if (delta >= 0.dp || height.dp <= shrink) return Offset.Zero
                val used = max(delta.value, shrink.value - height)
                height += used
                return Offset(0f, with(density) { used.dp.toPx() })
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = with(density) { available.y.toDp() }
                if (delta <= 0.dp || height.dp >= grow) return Offset.Zero
                val used = min(delta.value, grow.value - height)
                height += used
                return Offset(0f, with(density) { used.dp.toPx() })
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        frame(Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, top = OcSpacing.sm).fillMaxWidth().height(currentHeight))
        val isExpanded = currentHeight > (collapsed + expanded) / 2
        val toggleLabel = stringResource(if (isExpanded) Res.string.settings_preview_collapse else Res.string.settings_preview_expand)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        height = (height + with(density) { delta.toDp() }.value).coerceIn(shrink.value, grow.value)
                    },
                )
                .clickable(onClickLabel = toggleLabel, role = Role.Button) {
                    height = if (isExpanded) collapsed.value else expanded.value
                }
                .testTag("area_preview_handle"),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = 32.dp, height = 4.dp).background(c.layer04, RoundedCornerShape(2.dp)))
        }
        options(Modifier.weight(1f).fillMaxWidth().nestedScroll(connection))
    }
}

@Composable
private fun PreviewFrame(area: SettingsArea, preview: PartPreview, content: AreaPreviewContent, state: SettingsState, modifier: Modifier) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.xl)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, c.borderBase, shape)
            .background(c.bgBase)
            .testTag("area_preview"),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = OcSpacing.sm)) {
            AreaPreview(area, preview, content, updateTarget = state.updateTarget)
        }
    }
}

/**
 * Keeps the options list on the selected part: a part picked in the preview scrolls its options
 * to the top; options scrolled by hand select the part at the top.
 */
@Composable
private fun OptionsFollowPreview(parts: List<SettingsPart>, selected: SettingsPart?, listState: LazyListState, onSelect: (SettingsPart) -> Unit) {
    var fromList by remember { mutableStateOf<SettingsPart?>(null) }
    var scrolling by remember { mutableStateOf(false) }
    val currentSelected by rememberUpdatedState(selected)
    LaunchedEffect(selected) {
        val index = parts.indexOf(selected)
        if (index < 0 || selected == fromList) return@LaunchedEffect
        scrolling = true
        try {
            listState.animateScrollToItem(index)
        } finally {
            scrolling = false
        }
    }
    LaunchedEffect(listState, parts) {
        snapshotFlow { readingIndex(listState) to listState.isScrollInProgress }.collect { (index, inProgress) ->
            if (!inProgress || scrolling || index == null) return@collect
            val part = parts.getOrNull(index) ?: return@collect
            if (part != currentSelected) {
                fromList = part
                onSelect(part)
            }
        }
    }
}

/** The item at a quarter of the list's height, where the eye reads: the part being looked at. */
private fun readingIndex(listState: LazyListState): Int? {
    val info = listState.layoutInfo
    val line = info.viewportStartOffset + (info.viewportEndOffset - info.viewportStartOffset) / 4
    return info.visibleItemsInfo.firstOrNull { it.offset <= line && line < it.offset + it.size }?.index
}

@Composable
private fun OptionsList(
    parts: List<SettingsPart>,
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    listState: LazyListState,
    tail: Dp,
    modifier: Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.testTag("area_options"),
        contentPadding = PaddingValues(start = OcSpacing.md, end = OcSpacing.md, top = OcSpacing.xs, bottom = tail),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        items(parts, key = { it.name }) { part -> PartOptions(part, state, onEvent) }
    }
}

/** The name of [part] and its options as in the rest of Settings; options switched off by another one say which. */
@Composable
private fun PartOptions(part: SettingsPart, state: SettingsState, onEvent: (SettingsEvent) -> Unit) {
    val c = OcTheme.colors
    val selected = state.selected == part
    val options = part.options
    val shown = options.filter { it.appliesTo(state.settings, state.notificationsOn) }
    // The switch to turn on first: past the ones that do not apply themselves (live kinds before the interval).
    val needs = options.filterNot { it in shown }.mapNotNull { hidden ->
        var parent = hidden.requires
        while (parent != null && !parent.appliesTo(state.settings, state.notificationsOn)) parent = parent.requires ?: break
        parent
    }.distinct()
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs), modifier = Modifier.testTag("area_part_${part.name.lowercase()}")) {
        Text(
            part.label(),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) c.textAccent else c.textMuted,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onEvent(SettingsEvent.SelectPart(part)) }
                .heightIn(min = 40.dp)
                .padding(start = OcSpacing.xs, top = OcSpacing.sm)
                .semantics { heading() },
        )
        OcCard(borderColor = if (selected) c.textAccent else c.borderBase) {
            if (!part.shownWith(state.settings)) {
                Text(
                    stringResource(Res.string.settings_part_hidden),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.fillMaxWidth().padding(start = OcSpacing.lg, end = OcSpacing.lg, top = OcSpacing.md),
                )
            }
            shown.forEachIndexed { i, option ->
                if (i > 0) OcDivider()
                Box(Modifier.background(if (state.focus == option) c.info.bg else Color.Transparent)) {
                    SettingOptionControl(option, state, onEvent)
                }
            }
            needs.forEach { parent ->
                if (shown.isNotEmpty()) OcDivider()
                Text(
                    stringResource(Res.string.settings_option_needs, stringResource(parent.titleRes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
                )
            }
        }
    }
}
