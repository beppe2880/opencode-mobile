package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ChatElement
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.text.label
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.customize_close
import io.github.beppe2880.opencodemobile.feature.settings.resources.customize_hint
import io.github.beppe2880.opencodemobile.feature.settings.resources.customize_marks_hide
import io.github.beppe2880.opencodemobile.feature.settings.resources.customize_marks_show
import io.github.beppe2880.opencodemobile.feature.settings.resources.customize_parts
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_customize_chat
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

/**
 * The chat customizer: a live [preview] of the chat and the options of the part tapped in it.
 * On narrow windows the options dock under the preview, which keeps showing the part being
 * changed; on wide ones (unfolded foldables, tablets, landscape) they sit beside it.
 */
@Composable
fun CustomizeScreen(
    state: CustomizeState,
    onEvent: (CustomizeEvent) -> Unit,
    preview: ChatPreviewContent,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    OcBackHandler(enabled = state.selected != null) { onEvent(CustomizeEvent.CloseOptions) }
    Scaffold(
        modifier = modifier,
        containerColor = c.bgDeep,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = stringResource(Res.string.settings_customize_chat),
                leading = {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiRes.string.oc_back),
                        onClick = { onEvent(CustomizeEvent.Back) },
                        modifier = Modifier.testTag("customize_back_button"),
                    )
                },
                actions = {
                    OcIconButton(
                        icon = if (state.marks) Icons.Outlined.EditOff else Icons.Outlined.Edit,
                        contentDescription = stringResource(if (state.marks) Res.string.customize_marks_hide else Res.string.customize_marks_show),
                        onClick = { onEvent(CustomizeEvent.MarksChanged(!state.marks)) },
                        modifier = Modifier.testTag("customize_marks_button"),
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
            val select: (ChatElement) -> Unit = { onEvent(CustomizeEvent.Select(it)) }
            val frame = Modifier
                .clip(RoundedCornerShape(OcRadius.xl))
                .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.xl))
            // Side by side on expanded windows and in landscape; stacked, the preview gets the full width.
            if (maxWidth >= ExpandedWidth || (maxWidth >= SideBySideMinWidth && maxWidth > maxHeight)) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(OcSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.lg),
                ) {
                    preview(state.settings, state.marks, state.selected, select, frame.weight(1f).fillMaxHeight())
                    Column(
                        modifier = Modifier.width(SidePanelWidth).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
                    ) {
                        val selected = state.selected
                        if (selected == null) {
                            Hint()
                            PartList(onSelect = select)
                        } else {
                            OptionsCard(selected, state, onEvent)
                        }
                    }
                }
            } else {
                val panelMax = maxHeight * 0.5f
                Column(Modifier.fillMaxSize().padding(top = OcSpacing.md)) {
                    preview(
                        state.settings,
                        state.marks,
                        state.selected,
                        select,
                        Modifier.padding(horizontal = OcSpacing.md).weight(1f).fillMaxWidth().then(frame),
                    )
                    AnimatedContent(targetState = state.selected, label = "customize_panel") { selected ->
                        if (selected == null) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = OcSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                            ) {
                                Hint(Modifier.padding(horizontal = OcSpacing.lg))
                                PartChips(onSelect = select)
                            }
                        } else {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = panelMax)
                                    .verticalScroll(rememberScrollState())
                                    .padding(OcSpacing.md),
                            ) {
                                OptionsCard(selected, state, onEvent)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Wide enough for the preview at phone width and the options beside it. */
private val SideBySideMinWidth = 600.dp
private val ExpandedWidth = 840.dp
private val SidePanelWidth = 340.dp

@Composable
private fun Hint(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.customize_hint),
        style = MaterialTheme.typography.bodySmall,
        color = OcTheme.colors.textMuted,
        modifier = modifier,
    )
}

/** The parts that can be changed, also those a setting hides from the preview (thinking turned off). */
private val listedParts = ChatElement.entries - ChatElement.Activity

/** One scrolling line, so the preview keeps the height. */
@Composable
private fun PartChips(onSelect: (ChatElement) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = OcSpacing.md)
            .testTag("customize_parts"),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        listedParts.forEach { element ->
            OcChip(
                text = element.label(),
                onClick = { onSelect(element) },
                // A bounded width: the chip label fills what is left after its icons.
                modifier = Modifier.widthIn(max = ChipMaxWidth).testTag("customize_part_${element.name.lowercase()}"),
            )
        }
    }
}

private val ChipMaxWidth = 320.dp

@Composable
private fun PartList(onSelect: (ChatElement) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        Text(
            stringResource(Res.string.customize_parts),
            style = MaterialTheme.typography.labelMedium,
            color = OcTheme.colors.textMuted,
            modifier = Modifier.padding(start = OcSpacing.xs).semantics { heading() },
        )
        OcCard(Modifier.testTag("customize_parts")) {
            listedParts.forEachIndexed { i, element ->
                if (i > 0) OcDivider()
                OcListRow(
                    title = element.label(),
                    onClick = { onSelect(element) },
                    modifier = Modifier.testTag("customize_part_${element.name.lowercase()}"),
                )
            }
        }
    }
}

/** The name of [element] with a close button, and its options as in the settings list. */
@Composable
private fun OptionsCard(element: ChatElement, state: CustomizeState, onEvent: (CustomizeEvent) -> Unit) {
    val c = OcTheme.colors
    OcCard(Modifier.fillMaxWidth().widthIn(max = 640.dp).testTag("customize_options")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = OcSpacing.lg, end = OcSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                element.label(),
                style = MaterialTheme.typography.titleSmall,
                color = c.textBase,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
            )
            OcIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = stringResource(Res.string.customize_close),
                onClick = { onEvent(CustomizeEvent.CloseOptions) },
                tint = c.iconMuted,
                modifier = Modifier.testTag("customize_options_close"),
            )
        }
        state.options.forEach { option ->
            OcDivider()
            ChatOptionControl(option, state.settings) { onEvent(CustomizeEvent.Setting(it)) }
        }
    }
}

@PreviewLightDark
@Composable
private fun CustomizeScreenPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        CustomizeScreen(
            state = CustomizeState(selected = ChatElement.Composer),
            onEvent = {},
            preview = { _, _, _, _, modifier -> androidx.compose.foundation.layout.Box(modifier) },
        )
    }
}
