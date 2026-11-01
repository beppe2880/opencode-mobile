package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.domain.SettingsArea
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.ui.components.EditMode
import io.github.beppe2880.opencodemobile.core.ui.components.LocalEditMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.platform.OcBackHandler
import io.github.beppe2880.opencodemobile.core.ui.text.label
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent
import io.github.beppe2880.opencodemobile.feature.settings.SettingsState
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_edit_close
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_edit_done
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_edit_hint
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_edit_mode
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_edit_more
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_use_hint
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_use_mode
import io.github.beppe2880.opencodemobile.feature.settings.updatedBy
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The live demo's edit mode around the real screens of [content]. A bar at the bottom, in thumb
 * reach, switches between Edit (marked parts open their options when tapped; everything else,
 * like Back, keeps working) and Use (the demo as usual), and Done leaves. The options dock above
 * the bar and push the screen up instead of covering it, so the edited part, which scrolls into
 * view, stays in sight while it changes. Changes are saved to the settings at once.
 * [onOpenArea] leaves for the page of the edited part's category in Settings, with its other options.
 */
@Composable
fun LiveEditHost(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenArea: ((SettingsArea) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val repository = koinInject<SettingsRepository>()
    val settings by repository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf<SettingsPart?>(null) }
    val state = SettingsState(settings = settings)
    val onEvent: (SettingsEvent) -> Unit = { event -> scope.launch { repository.update { it.updatedBy(event) ?: it } } }
    OcBackHandler(enabled = selected != null) { selected = null }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelMax = maxHeight * PANEL_SHARE
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars)) {
                CompositionLocalProvider(LocalEditMode provides EditMode(active = editing, selected = selected, onEdit = { selected = it })) {
                    content()
                }
            }
            AnimatedVisibility(visible = selected != null, enter = expandVertically(), exit = shrinkVertically()) {
                selected?.let { part -> OptionsPanel(part, state, onEvent, onClose = { selected = null }, onOpenArea, Modifier.heightIn(max = panelMax)) }
            }
            EditBar(
                editing = editing,
                onEditing = {
                    editing = it
                    if (!it) selected = null
                },
                onDone = onDone,
            )
        }
    }
}

/** At most this share of the height for the options: the edited part keeps the rest. */
private const val PANEL_SHARE = 0.4f

@Composable
private fun OptionsPanel(
    part: SettingsPart,
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onClose: () -> Unit,
    onOpenArea: ((SettingsArea) -> Unit)?,
    modifier: Modifier,
) {
    val c = OcTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth().testTag("edit_options"),
        shape = RoundedCornerShape(topStart = OcRadius.sheet, topEnd = OcRadius.sheet),
        color = c.bgDeep,
        shadowElevation = 8.dp,
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = OcSpacing.lg, end = OcSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    part.label(),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textBase,
                    modifier = Modifier.weight(1f).semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                OcIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.settings_edit_close),
                    onClick = onClose,
                    tint = c.iconMuted,
                    modifier = Modifier.testTag("edit_options_close"),
                )
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.md)) {
                PartOptionsCard(part, state, onEvent, highlighted = false, inPreview = false)
                if (onOpenArea != null) {
                    OcButton(
                        text = stringResource(Res.string.settings_edit_more, part.area.label()),
                        onClick = { onOpenArea(part.area) },
                        variant = OcButtonVariant.Ghost,
                        compact = true,
                        modifier = Modifier.padding(top = OcSpacing.xs).testTag("edit_options_more"),
                    )
                }
            }
        }
    }
}

/** Edit or Use, with a hint of what a tap does, and Done; above the navigation bar. */
@Composable
private fun EditBar(editing: Boolean, onEditing: (Boolean) -> Unit, onDone: () -> Unit) {
    val c = OcTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.bgDeep)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm)
            .testTag("edit_bar"),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        Text(
            stringResource(if (editing) Res.string.settings_edit_hint else Res.string.settings_use_hint),
            style = MaterialTheme.typography.labelSmall,
            color = c.textMuted,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            OcSegmentedControl(
                segments = listOf(
                    OcSegment(true, stringResource(Res.string.settings_edit_mode), "edit_mode_edit"),
                    OcSegment(false, stringResource(Res.string.settings_use_mode), "edit_mode_use"),
                ),
                selected = editing,
                onSelect = onEditing,
                modifier = Modifier.weight(1f),
            )
            OcButton(
                text = stringResource(Res.string.settings_edit_done),
                onClick = onDone,
                variant = OcButtonVariant.Inverse,
                modifier = Modifier.testTag("edit_mode_done"),
            )
        }
    }
}
