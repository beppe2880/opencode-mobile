package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.beppe2880.opencodemobile.core.domain.SessionShortcut
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.core.ui.platform.sessionNotificationsSupported
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_archive
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_delete
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_down
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_mute
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_pin
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_read
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_rename
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_up
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionShortcutsControl(
    title: String,
    subtitle: String,
    shortcuts: List<SessionShortcut>,
    onChange: (List<SessionShortcut>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        OcListRow(
            title = title,
            subtitle = subtitle,
            titleMaxLines = Int.MAX_VALUE,
            subtitleMaxLines = Int.MAX_VALUE,
            modifier = modifier,
            onClick = { expanded = !expanded },
            trailing = { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null) },
        )
        if (expanded) {
            val eligible = SessionShortcut.entries.filter { it != SessionShortcut.Mute || sessionNotificationsSupported }
            val enabled = shortcuts.filter { it in eligible }
            (enabled + (eligible - enabled.toSet())).forEach { shortcut ->
                key(shortcut) {
                    val label = stringResource(shortcut.label)
                    val index = enabled.indexOf(shortcut)
                    val tag = "settings_session_shortcut_${shortcut.name.lowercase()}"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OcSwitchRow(
                            title = label,
                            checked = index >= 0,
                            onCheckedChange = { onChange(if (it) shortcuts + shortcut else shortcuts - shortcut) },
                            modifier = Modifier.weight(1f).testTag(tag),
                        )
                        if (index >= 0 && enabled.size > 1) {
                            OcIconButton(
                                icon = Icons.Outlined.ExpandLess,
                                contentDescription = stringResource(Res.string.settings_shortcut_up, label),
                                enabled = index > 0,
                                onClick = { onChange(shortcuts.swap(shortcut, enabled[index - 1])) },
                                modifier = Modifier.testTag("${tag}_up"),
                            )
                            OcIconButton(
                                icon = Icons.Outlined.ExpandMore,
                                contentDescription = stringResource(Res.string.settings_shortcut_down, label),
                                enabled = index < enabled.lastIndex,
                                onClick = { onChange(shortcuts.swap(shortcut, enabled[index + 1])) },
                                modifier = Modifier.testTag("${tag}_down"),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun List<SessionShortcut>.swap(first: SessionShortcut, second: SessionShortcut): List<SessionShortcut> =
    toMutableList().apply {
        this[this@swap.indexOf(first)] = second
        this[this@swap.indexOf(second)] = first
    }

private val SessionShortcut.label
    get() = when (this) {
        SessionShortcut.Pin -> Res.string.settings_shortcut_pin
        SessionShortcut.Read -> Res.string.settings_shortcut_read
        SessionShortcut.Mute -> Res.string.settings_shortcut_mute
        SessionShortcut.Archive -> Res.string.settings_shortcut_archive
        SessionShortcut.Rename -> Res.string.settings_shortcut_rename
        SessionShortcut.Delete -> Res.string.settings_shortcut_delete
    }
