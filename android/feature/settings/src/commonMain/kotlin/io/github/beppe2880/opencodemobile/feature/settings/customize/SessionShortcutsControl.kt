package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
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
import androidx.compose.ui.unit.dp
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
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_read_disabled
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_rename
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_shortcut_up
import org.jetbrains.compose.resources.stringResource

private val ReorderButtonWidth = 48.dp

@Composable
internal fun SessionShortcutsControl(
    title: String,
    subtitle: String,
    shortcuts: List<SessionShortcut>,
    unreadEnabled: Boolean,
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
            val reorderable = enabled.filter { it != SessionShortcut.Read || unreadEnabled }
            (enabled + (eligible - enabled.toSet())).forEach { shortcut ->
                key(shortcut) {
                    val label = stringResource(shortcut.label)
                    val available = shortcut != SessionShortcut.Read || unreadEnabled
                    val index = reorderable.indexOf(shortcut)
                    val tag = "settings_session_shortcut_${shortcut.name.lowercase()}"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OcSwitchRow(
                            title = label,
                            checked = shortcut in enabled,
                            onCheckedChange = { onChange(if (it) shortcuts + shortcut else shortcuts - shortcut) },
                            modifier = Modifier.weight(1f).testTag(tag),
                            subtitle = if (available) null else stringResource(Res.string.settings_shortcut_read_disabled),
                            enabled = available,
                        )
                        if (index >= 0 && reorderable.size > 1) {
                            OcIconButton(
                                icon = Icons.Outlined.ArrowUpward,
                                contentDescription = stringResource(Res.string.settings_shortcut_up, label),
                                enabled = index > 0,
                                onClick = { onChange(shortcuts.swap(shortcut, reorderable[index - 1])) },
                                modifier = Modifier.width(ReorderButtonWidth).testTag("${tag}_up"),
                            )
                            OcIconButton(
                                icon = Icons.Outlined.ArrowDownward,
                                contentDescription = stringResource(Res.string.settings_shortcut_down, label),
                                enabled = index < reorderable.lastIndex,
                                onClick = { onChange(shortcuts.swap(shortcut, reorderable[index + 1])) },
                                modifier = Modifier.width(ReorderButtonWidth).testTag("${tag}_down"),
                            )
                        } else {
                            // Keep the switches in one column without adding inactive accessibility controls.
                            repeat(2) { Spacer(Modifier.width(ReorderButtonWidth)) }
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
