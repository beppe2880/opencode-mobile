package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.theme.OcColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcDarkColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcLightColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.OpenCodeThemePreview
import io.github.beppe2880.opencodemobile.core.ui.theme.OpenCodeThemes
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_opencode_theme_none
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_opencode_theme_none_sub
import org.jetbrains.compose.resources.stringResource

/** One choice of the picker: none (the app's own colors) or an OpenCode theme. */
private class ThemeChoice(val id: String?, val name: String, val subtitle: String?, val preview: OpenCodeThemePreview)

/**
 * The OpenCode theme: a row with the current choice, which opens a sheet listing None and every
 * theme with its colors in the app's current light or dark mode.
 */
@Composable
internal fun OpenCodeThemeControl(title: String, subtitle: String, selected: String?, onEvent: (SettingsEvent) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val dark = c.isDark
    var open by remember { mutableStateOf(false) }
    val none = ThemeChoice(
        id = null,
        name = stringResource(Res.string.settings_opencode_theme_none),
        subtitle = stringResource(Res.string.settings_opencode_theme_none_sub),
        preview = (if (dark) OcDarkColors else OcLightColors).swatch(),
    )
    val choices = listOf(none) + OpenCodeThemes.all.map { ThemeChoice(it.id, it.displayName, null, it.preview(dark)) }
    val current = choices.firstOrNull { it.id == selected } ?: none

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.DropdownList, onClick = { open = true })
            .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md)
            .testTag("settings_opencode_theme"),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            ThemeSwatch(current.preview)
            Text(
                current.name,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).testTag("settings_opencode_theme_value"),
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
        }
    }

    if (open) {
        OcBottomSheet(onDismiss = { open = false }, testTag = "settings_opencode_theme_sheet") {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = c.textBase,
                modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).selectableGroup(),
                contentPadding = PaddingValues(bottom = OcSpacing.lg + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                items(choices, key = { it.id ?: "" }) { choice ->
                    val isSelected = choice.id == current.id
                    OcListRow(
                        title = choice.name,
                        subtitle = choice.subtitle,
                        leading = { ThemeSwatch(choice.preview) },
                        trailing = {
                            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                if (isSelected) Icon(Icons.Outlined.Check, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(20.dp))
                            }
                        },
                        modifier = Modifier
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = {
                                    open = false
                                    if (!isSelected) onEvent(SettingsEvent.OpenCodeThemeChanged(choice.id))
                                },
                            )
                            .testTag("settings_opencode_theme_option_${choice.id ?: "none"}"),
                    )
                }
            }
        }
    }
}

/** The app's own colors, shown like a theme for the None choice. */
private fun OcColors.swatch() = OpenCodeThemePreview(
    background = bgDeep,
    surface = layer02,
    text = textBase,
    primary = bgAccent,
    strong = textAccent,
    code = textCodePath,
)

/** The colors of a theme side by side: background, surface, text, primary, strong and code. */
@Composable
private fun ThemeSwatch(preview: OpenCodeThemePreview) {
    val shape = RoundedCornerShape(OcRadius.sm)
    Row(
        modifier = Modifier
            .width(48.dp)
            .height(24.dp)
            .clip(shape)
            .border(1.dp, OcTheme.colors.borderBase, shape),
    ) {
        listOf(preview.background, preview.surface, preview.text, preview.primary, preview.strong, preview.code).forEach { color ->
            Box(Modifier.weight(1f).fillMaxSize().background(color))
        }
    }
}
