package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.components.LocalEditMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcConfirmDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcCrossfade
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.editable
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.components.rememberLastNonNull
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_close
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_running
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMotion
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_agent_untitled
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_chip_agents
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_chip_agents_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_chip_shells
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_chip_shells_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_copy_command
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_list_agents
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_list_shells
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_no_output
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_open
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_open_agent_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_open_session
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_open_shell_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_share
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_status_ended
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_status_exited
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_status_finished
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_status_running
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_agent_message
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_agent_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_agents_label
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_all
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_all_agents_message
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_all_agents_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_all_commands_message
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_all_commands_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_everything_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_everything_message
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_everything_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_message
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_shell
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_shells_label
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_subagent
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_stop
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/*
 * Running work above the composer (setting): one chip per kind that runs, its list in a popover
 * anchored above the composer, and one item in a sheet with its live output and Stop. The data,
 * polling and stopping are the Running sheet's (ChatViewModel); only what is shown differs.
 */

/**
 * The chips, left aligned; the popover of the open list is anchored above them. The bar grows in
 * above the composer when work starts and folds away when it ends.
 */
@Composable
internal fun RunningChipsBar(state: ChatState, onEvent: (ChatEvent) -> Unit, modifier: Modifier = Modifier) {
    val chips = runningChips(state.work, state.runningChips)
    val motion = OcTheme.motion
    // The last chips stay drawn while the bar folds away. Always composed, so that it grows in.
    val shown = rememberLastNonNull(chips.takeIf { it.isNotEmpty() })
    AnimatedVisibility(
        visible = chips.isNotEmpty(),
        modifier = modifier,
        enter = OcMotion.expandEnter(motion),
        exit = OcMotion.collapseExit(motion),
        label = "running_chips",
    ) {
        if (shown != null) RunningChipsAnchor(state, shown, onEvent)
    }
}

@Composable
private fun RunningChipsAnchor(state: ChatState, chips: List<RunningChip>, onEvent: (ChatEvent) -> Unit) {
    val focus = LocalFocusManager.current
    val motion = OcTheme.motion
    var anchorWidth by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .fillMaxWidth()
            .editable(SettingsPart.PendingCards, LocalEditMode.current)
            .onSizeChanged { anchorWidth = it.width },
    ) {
        RunningChipRow(chips) { group ->
            // The keyboard would cover the list (and on iOS fight the sheet): it closes first.
            focus.clearFocus()
            onEvent(ChatEvent.ShowRunningList(group))
        }
        val listGroup = state.runningPanel?.takeIf { it.item == null }?.group
        // The popover stays composed while it closes, showing the group it was opened for.
        val shownGroup = rememberLastNonNull(listGroup)
        val popover = remember { MutableTransitionState(false) }
        popover.targetState = listGroup != null
        if (shownGroup != null && (popover.currentState || popover.targetState)) {
            val gap = with(LocalDensity.current) { OcSpacing.xs.roundToPx() }
            // A Stop's confirmation shows over the list: touching it must not close the list.
            val confirming = state.confirmingStop != null || state.confirmingStopAgent != null || state.confirmingStopAll != null
            // Closing, it no longer takes focus or touches: the chat is usable at once.
            val interactive = !confirming && listGroup != null
            // Above the chips at their start edge: it grows out of them and shrinks back into them.
            val origin = OcMotion.anchoredOrigin(above = true, layoutDirection = LocalLayoutDirection.current)
            Popup(
                popupPositionProvider = remember(gap) { AboveAnchor(gap) },
                onDismissRequest = { if (listGroup != null) onEvent(ChatEvent.ShowRunningList(null)) },
                properties = PopupProperties(focusable = interactive, dismissOnBackPress = interactive, dismissOnClickOutside = interactive),
            ) {
                AnimatedVisibility(
                    visibleState = popover,
                    enter = OcMotion.popoverEnter(origin, motion),
                    exit = OcMotion.popoverExit(origin, motion),
                    label = "running_popover",
                ) {
                    RunningListPopover(
                        state = state,
                        group = shownGroup,
                        width = with(LocalDensity.current) { anchorWidth.toDp() },
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

/**
 * The chips alone, also drawn by the Settings preview. A chip that starts or stops while the
 * other runs fades and slides in or out at its place, the other one moving over.
 */
@Composable
internal fun RunningChipRow(chips: List<RunningChip>, onClick: (RunningGroup) -> Unit) {
    val motion = OcTheme.motion
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Always in this order (runningChips), so each group keeps its place.
        RUNNING_CHIP_ORDER.forEachIndexed { index, group ->
            val chip = chips.firstOrNull { it.group == group }
            // Composed even while its group does not run, so that its chip slides in when it starts.
            val shown = rememberLastNonNull(chip)
            AnimatedVisibility(
                visible = chip != null,
                enter = OcMotion.inlineEnter(motion, from = Alignment.Start),
                exit = OcMotion.inlineExit(motion, towards = Alignment.Start),
                label = "running_chip_${group.tagName}",
            ) {
                // The gap before a chip belongs to it, so it comes and goes with the chip before it.
                val gap by animateDpAsState(
                    if (index > 0 && chips.any { it.group != group }) OcSpacing.sm else 0.dp,
                    OcMotion.spec(OcMotion.SHORT_MS, motion),
                    label = "running_chip_gap",
                )
                if (shown != null) {
                    Box(Modifier.padding(start = gap)) {
                        // A chip sliding away no longer opens its (now empty) list.
                        RunningChipView(shown, onClick = { if (chip != null) onClick(shown.group) })
                    }
                }
            }
        }
    }
}

private val RUNNING_CHIP_ORDER = listOf(RunningGroup.Commands, RunningGroup.Agents)

@Composable
private fun RunningChipView(chip: RunningChip, onClick: () -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    val (label, description, tag) = when (chip.group) {
        RunningGroup.Commands -> Triple(
            pluralStringResource(Res.plurals.chat_running_chip_shells, chip.count, chip.count),
            pluralStringResource(Res.plurals.chat_running_chip_shells_description, chip.count, chip.count),
            "running_chip_shells",
        )
        RunningGroup.Agents -> Triple(
            pluralStringResource(Res.plurals.chat_running_chip_agents, chip.count, chip.count),
            pluralStringResource(Res.plurals.chat_running_chip_agents_description, chip.count, chip.count),
            "running_chip_agents",
        )
    }
    Row(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(c.layer01, shape)
            .border(BorderStroke(1.dp, c.borderBase), shape)
            .testTag(tag)
            .semantics { contentDescription = description }
            .clickable(onClickLabel = stringResource(chip.group.listTitle), role = Role.Button, onClick = onClick)
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        RunningDots(c.textMuted)
        // Read as the description above, not twice.
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textBase, maxLines = 1, modifier = Modifier.clearAndSetSemantics { })
    }
}

private val RunningGroup.listTitle: StringResource
    get() = when (this) {
        RunningGroup.Commands -> Res.string.chat_running_list_shells
        RunningGroup.Agents -> Res.string.chat_running_list_agents
    }

private val RunningGroup.tagName: String
    get() = when (this) {
        RunningGroup.Commands -> "shells"
        RunningGroup.Agents -> "agents"
    }

/** Above the anchor (the chips), at its start edge, kept inside the window. */
private class AboveAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left else anchorBounds.right - popupContentSize.width
        val y = anchorBounds.top - popupContentSize.height - gap
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceAtLeast(0),
        )
    }
}

/**
 * Everything that runs in separate sections (Agents, Shells: own heading and rows, a divider
 * between them), the tapped [group] first; each row opens its
 * item's sheet, its Open the subagent's session (or the shell's sheet); the Stops at the end.
 */
@Composable
private fun RunningListPopover(state: ChatState, group: RunningGroup, width: Dp, onEvent: (ChatEvent) -> Unit) {
    val c = OcTheme.colors
    val now = rememberNow()
    val shape = RoundedCornerShape(OcRadius.xl)
    Column(
        Modifier
            .then(if (width > 0.dp) Modifier.width(width) else Modifier.fillMaxWidth())
            .clip(shape)
            .background(c.bgBase, shape)
            .border(BorderStroke(1.dp, c.borderBase), shape)
            .exposeTestTags()
            .testTag("running_popover_${group.tagName}")
            .padding(vertical = OcSpacing.sm),
    ) {
        LazyColumn(Modifier.heightIn(max = 320.dp)) {
            // Agents and shells stay apart: each its own heading and rows, a divider between them.
            state.work.listGroups(group).forEachIndexed { index, section ->
                item(key = "section:${section.name}") {
                    Column(Modifier.testTag("running_section_${section.tagName}")) {
                        if (index > 0) OcDivider(Modifier.padding(vertical = OcSpacing.xs))
                        Text(
                            stringResource(section.listTitle),
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textBase,
                            modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.xs).semantics { heading() },
                        )
                    }
                }
                items(state.work.entries(section), key = { "${section.name}:${it.id}" }) { entry ->
                    val showItem = { onEvent(ChatEvent.ShowRunningItem(entry.group, entry.id)) }
                    RunningRow(
                        entry,
                        state,
                        now,
                        onClick = showItem,
                        onOpen = if (entry.group == RunningGroup.Agents) ({ onEvent(ChatEvent.OpenSession(entry.id)) }) else showItem,
                    )
                }
            }
        }
        RunningStops(state, onEvent)
    }
}

@Composable
private fun RunningRow(entry: RunningEntry, state: ChatState, now: Long, onClick: () -> Unit, onOpen: () -> Unit) {
    val c = OcTheme.colors
    val shell = entry.group == RunningGroup.Commands
    val label = entryLabel(entry)
    val openDescription = stringResource(
        if (shell) Res.string.chat_running_open_shell_description else Res.string.chat_running_open_agent_description,
        label,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("running_row_${entry.id}")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        RunningDots(c.textMuted)
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = if (shell) OcTheme.mono.code else MaterialTheme.typography.bodyMedium,
                color = c.textBase,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
            entry.agent?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = c.textMuted, maxLines = 1) }
        }
        runningElapsedMs(entry, null, now, state.clockOffsetMs)?.let {
            Text(formatElapsedClock(it), style = OcTheme.mono.label, color = c.textMuted, maxLines = 1)
        }
        OcButton(
            text = stringResource(Res.string.chat_running_open),
            onClick = onOpen,
            variant = OcButtonVariant.Ghost,
            compact = true,
            modifier = Modifier.testTag("running_open_${entry.id}").semantics { contentDescription = openDescription },
        )
    }
}

/** The footer's Stops ([runningStops]); the pressed one shows a spinner while it stops. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RunningStops(state: ChatState, onEvent: (ChatEvent) -> Unit) {
    val stops = runningStops(state.work)
    if (stops.isEmpty()) return
    // Without it, Stop all stopping everything would also spin the groups' Stops.
    var pressed by remember { mutableStateOf<String?>(null) }
    val stopAllDescription = stringResource(Res.string.chat_running_stop_everything_description)
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        stops.forEach { stop ->
            val tag = when (stop) {
                is RunningStop.One -> "running_stop"
                RunningStop.All -> "running_stop_all"
                is RunningStop.Group -> when (stop.group) {
                    RunningGroup.Agents -> "running_stop_agents"
                    RunningGroup.Commands -> "running_stop_shells"
                }
            }
            val text = when (stop) {
                is RunningStop.One -> stringResource(Res.string.chat_stop)
                RunningStop.All -> stringResource(Res.string.chat_running_stop_all)
                is RunningStop.Group -> stringResource(
                    when (stop.group) {
                        RunningGroup.Agents -> if (stop.count == 1) Res.string.chat_running_stop_subagent else Res.string.chat_running_stop_agents_label
                        RunningGroup.Commands -> if (stop.count == 1) Res.string.chat_running_stop_shell else Res.string.chat_running_stop_shells_label
                    },
                )
            }
            val stopping = stop.stopping(state)
            RunningStopButton(
                text = text,
                stopping = stopping,
                spinner = stopping && (pressed == null || pressed == tag),
                onClick = {
                    pressed = tag
                    onEvent(stop.event)
                },
                modifier = Modifier
                    .testTag(tag)
                    .then(if (stop == RunningStop.All) Modifier.semantics { contentDescription = stopAllDescription } else Modifier),
            )
        }
    }
}

/** A destructive Stop, disabled while [stopping]; [spinner] replaces its text. */
@Composable
private fun RunningStopButton(text: String, stopping: Boolean, spinner: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Button(
        onClick = onClick,
        enabled = !stopping,
        shape = RoundedCornerShape(OcRadius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.danger.bg,
            contentColor = c.danger.fg,
            disabledContainerColor = c.layer02,
            disabledContentColor = c.textFaint,
        ),
        elevation = null,
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        OcCrossfade(spinner, label = "running_stop") { busy ->
            if (busy) OcSpinner(color = c.danger.fg) else Text(text, maxLines = 1)
        }
    }
}

@Composable
private fun entryLabel(entry: RunningEntry): String =
    entry.label.ifBlank { entry.agent ?: stringResource(Res.string.chat_running_agent_untitled) }

/**
 * One running item: the command (copy, share) or subagent (open its session) in the header, the
 * live output that follows the end while scrolled there, and how long it runs with its Stop.
 */
@Composable
internal fun RunningItemSheet(
    state: ChatState,
    panel: RunningPanel,
    item: RunningEntry,
    onEvent: (ChatEvent) -> Unit,
    onCopy: (String) -> Unit,
    onShare: ((String) -> Unit)?,
) {
    val c = OcTheme.colors
    val now = rememberNow()
    val shell = item.group == RunningGroup.Commands
    val label = entryLabel(item)
    OcBottomSheet(onDismiss = { onEvent(ChatEvent.CloseRunningItem) }, testTag = "running_item_sheet") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // As tall as its content, up to 85% of the screen: the footer always shows.
                .layout { measurable, constraints ->
                    val max = if (constraints.hasBoundedHeight) (constraints.maxHeight * 0.85f).toInt() else constraints.maxHeight
                    val placeable = measurable.measure(constraints.copy(maxHeight = max.coerceAtLeast(constraints.minHeight)))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                .padding(
                    PaddingValues(
                        start = OcSpacing.sm,
                        end = OcSpacing.sm,
                        bottom = OcSpacing.md + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                ),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OcIconButton(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(UiRes.string.oc_close),
                    onClick = { onEvent(ChatEvent.CloseRunningItem) },
                    modifier = Modifier.testTag("running_item_close"),
                )
                Text(
                    label,
                    style = if (shell) OcTheme.mono.code else MaterialTheme.typography.titleSmall,
                    color = c.textBase,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (shell) {
                    OcIconButton(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(Res.string.chat_running_copy_command),
                        onClick = { onCopy(item.label) },
                        modifier = Modifier.testTag("running_item_copy"),
                    )
                    if (onShare != null) {
                        OcIconButton(
                            Icons.Outlined.Share,
                            contentDescription = stringResource(Res.string.chat_running_share),
                            onClick = { onShare(item.label) },
                            modifier = Modifier.testTag("running_item_share"),
                        )
                    }
                } else {
                    OcIconButton(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = stringResource(Res.string.chat_running_open_session),
                        onClick = { onEvent(ChatEvent.OpenSession(item.id)) },
                        modifier = Modifier.testTag("running_item_open"),
                    )
                }
            }
            Box(Modifier.weight(1f, fill = false).padding(horizontal = OcSpacing.sm)) {
                if (shell) {
                    ShellOutput(state.workOutput[item.id], state.workOutputErrors[item.id]?.resolve())
                } else {
                    AgentActivity(item, onOpen = { onEvent(ChatEvent.OpenSession(item.id)) })
                }
            }
            ItemFooter(state, panel, item, now, onEvent)
        }
    }
}

/** Monospace and selectable; follows new output while scrolled to the end. */
@Composable
private fun ShellOutput(output: String?, error: String?) {
    val c = OcTheme.colors
    val scroll = rememberScrollState()
    var follow by remember { mutableStateOf(true) }
    val slop = with(LocalDensity.current) { 8.dp.roundToPx() }
    LaunchedEffect(scroll) {
        // Only the user's scrolling decides; scrolling to the end by code keeps following.
        snapshotFlow { Triple(scroll.isScrollInProgress, scroll.value, scroll.maxValue) }.collect { (moving, value, max) ->
            if (moving) follow = value >= max - slop
        }
    }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.maxValue }.collect { if (follow) scroll.scrollTo(it) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(OcRadius.lg))
            .background(c.layer01)
            .verticalScroll(scroll)
            .padding(OcSpacing.md)
            .testTag("running_item_output"),
    ) {
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.danger.fg) }
        val text = output?.takeIf { it.isNotEmpty() }
        if (text == null) {
            Text(stringResource(Res.string.chat_running_no_output), style = OcTheme.mono.codeSmall, color = c.textMuted)
        } else {
            SelectionContainer { Text(text, style = OcTheme.mono.codeSmall, color = c.textBase) }
        }
    }
}

/** What the Running sheet knows of a subagent: its task and agent, and its session to follow it. */
@Composable
private fun AgentActivity(item: RunningEntry, onOpen: () -> Unit) {
    val c = OcTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        Text(entryLabel(item), style = MaterialTheme.typography.bodyMedium, color = c.textBase)
        item.agent?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = c.textMuted) }
        OcButton(
            text = stringResource(Res.string.chat_running_open_session),
            onClick = onOpen,
            variant = OcButtonVariant.Secondary,
            compact = true,
            modifier = Modifier.testTag("running_item_open_session"),
        )
    }
}

/** "Running · 5:59" and Stop; once ended "Exited · 6:12" (shells) or "Finished · 6:12" (subagents). */
@Composable
private fun ItemFooter(state: ChatState, panel: RunningPanel, item: RunningEntry, now: Long, onEvent: (ChatEvent) -> Unit) {
    val c = OcTheme.colors
    val shell = item.group == RunningGroup.Commands
    val ended = panel.endedAt != null
    val elapsed = runningElapsedMs(item, panel.endedAt, now, state.clockOffsetMs)?.let(::formatElapsedClock)
    val status = when {
        !ended -> elapsed?.let { stringResource(Res.string.chat_running_status_running, it) } ?: stringResource(UiRes.string.oc_running)
        elapsed == null -> stringResource(Res.string.chat_running_status_ended)
        shell -> stringResource(Res.string.chat_running_status_exited, elapsed)
        else -> stringResource(Res.string.chat_running_status_finished, elapsed)
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        if (!ended) RunningDots(c.textMuted)
        Text(status, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.testTag("running_item_status"))
        Spacer(Modifier.weight(1f))
        if (!ended) {
            val stopping = if (shell) item.id in state.stoppingCommands else item.id in state.stoppingAgents
            RunningStopButton(
                text = stringResource(if (shell) Res.string.chat_running_stop_shell else Res.string.chat_running_stop_subagent),
                stopping = stopping,
                spinner = stopping,
                onClick = { onEvent(if (shell) ChatEvent.StopCommand(item.id) else ChatEvent.StopAgent(item.id)) },
                modifier = Modifier.testTag("running_item_stop"),
            )
        }
    }
}

/**
 * Six dots of a braille cell lighting up in turn around its edge, like the TUI's spinner; still
 * when the system asks for less motion. Decorative: the text beside it says what runs.
 */
@Composable
internal fun RunningDots(color: Color, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val reduced = OcTheme.reducedMotion
    val frame: State<Float> = if (reduced) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "runningDots").animateFloat(
            initialValue = 0f,
            targetValue = DOT_ORDER.size.toFloat(),
            animationSpec = infiniteRepeatable(tween(DOTS_CYCLE_MS, easing = LinearEasing)),
            label = "runningDotsFrame",
        )
    }
    Canvas(modifier.size(width = height * 2 / 3, height = height).clearAndSetSemantics { }) {
        val cellW = size.width / 2
        val cellH = size.height / 3
        val radius = minOf(cellW, cellH) * 0.34f
        val head = frame.value.toInt() % DOT_ORDER.size
        DOT_ORDER.forEachIndexed { i, (col, row) ->
            val alpha = if (reduced) {
                0.8f
            } else {
                when ((head - i + DOT_ORDER.size) % DOT_ORDER.size) {
                    0 -> 1f
                    1 -> 0.7f
                    2 -> 0.45f
                    else -> 0.2f
                }
            }
            drawCircle(color, radius, Offset(cellW * (col + 0.5f), cellH * (row + 0.5f)), alpha = alpha)
        }
    }
}

/** Column and row of each dot, clockwise from the top left. */
private val DOT_ORDER = listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2, 0 to 2, 0 to 1)
private const val DOTS_CYCLE_MS = 900

/** The device clock, ticking every second for live elapsed times. */
@Composable
internal fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = Clock.System.now().toEpochMilliseconds()
        }
    }
    return now
}

/** The confirmations of every Stop of running work: the Running sheet's and the chips' sheet's. */
@Composable
internal fun RunningStopDialogs(state: ChatState, onEvent: (ChatEvent) -> Unit) {
    state.confirmingStop?.let { id ->
        val command = (state.work.commands + state.unassignedCommands).firstOrNull { it.id == id }?.command.orEmpty()
        OcConfirmDialog(
            title = stringResource(Res.string.chat_running_stop_title),
            text = stringResource(Res.string.chat_running_stop_message, command),
            confirmLabel = stringResource(Res.string.chat_stop),
            onConfirm = { onEvent(ChatEvent.ConfirmStopCommand) },
            onDismiss = { onEvent(ChatEvent.DismissStopCommand) },
            destructive = true,
            testTagPrefix = "running_stop",
        )
    }
    state.confirmingStopAgent?.let { id ->
        val title = state.work.agents.firstOrNull { it.session.id == id }?.session?.title.orEmpty()
        OcConfirmDialog(
            title = stringResource(Res.string.chat_running_stop_agent_title),
            text = stringResource(Res.string.chat_running_stop_agent_message, title),
            confirmLabel = stringResource(Res.string.chat_stop),
            onConfirm = { onEvent(ChatEvent.ConfirmStopAgent) },
            onDismiss = { onEvent(ChatEvent.DismissStopAgent) },
            destructive = true,
            testTagPrefix = "running_stop_agent",
        )
    }
    state.confirmingStopAll?.let { target ->
        val (title, message, tagPrefix) = when (target) {
            StopAllTarget.Everything -> Triple(
                stringResource(Res.string.chat_running_stop_everything_title),
                stringResource(Res.string.chat_running_stop_everything_message),
                "running_stop_everything",
            )
            is StopAllTarget.Group -> when (target.group) {
                RunningGroup.Agents -> Triple(
                    pluralStringResource(Res.plurals.chat_running_stop_all_agents_title, state.work.agents.size, state.work.agents.size),
                    stringResource(Res.string.chat_running_stop_all_agents_message),
                    "running_stop_all_agents",
                )
                RunningGroup.Commands -> Triple(
                    pluralStringResource(Res.plurals.chat_running_stop_all_commands_title, state.work.commands.size, state.work.commands.size),
                    stringResource(Res.string.chat_running_stop_all_commands_message),
                    "running_stop_all_commands",
                )
            }
        }
        OcConfirmDialog(
            title = title,
            text = message,
            confirmLabel = stringResource(Res.string.chat_running_stop_all),
            onConfirm = { onEvent(ChatEvent.ConfirmStopAll) },
            onDismiss = { onEvent(ChatEvent.DismissStopAll) },
            destructive = true,
            testTagPrefix = tagPrefix,
        )
    }
}
