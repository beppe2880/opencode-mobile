package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.terminal.resources.Res
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_fullscreen_exit
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_backspace
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_down
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_end
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_enter
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_escape
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_home
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_left
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_page_down
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_page_up
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_paste
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_right
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_tab
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_key_up
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_keys_edit
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_modifier_locked
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_modifier_off
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_modifier_once
import org.jetbrains.compose.resources.stringResource

/** The rows of keys above the keyboard; both rows scroll together so columns stay aligned. */
@Composable
internal fun KeyBar(
    rows: List<List<BarKey>>,
    modifierState: (TermModifier) -> ModifierState,
    onPress: (BarKey) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    fullscreen: Boolean = false,
    onExitFullscreen: () -> Unit = {},
) {
    val c = OcTheme.colors
    Column(modifier.fillMaxWidth().background(c.bgBase)) {
        OcDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(start = OcSpacing.xs, top = OcSpacing.xs, bottom = OcSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                rows.forEachIndexed { rowIndex, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        row.forEachIndexed { index, key ->
                            KeyButton(
                                key = key,
                                state = (key as? BarKey.Modifier)?.let { modifierState(it.modifier) },
                                onClick = { onPress(key) },
                                modifier = Modifier.testTag("terminal_key_${rowIndex}_${index}_${key.testId}"),
                            )
                        }
                    }
                }
            }
            if (fullscreen) {
                OcIconButton(
                    icon = Icons.Outlined.FullscreenExit,
                    contentDescription = stringResource(Res.string.terminal_fullscreen_exit),
                    onClick = onExitFullscreen,
                    modifier = Modifier.testTag("terminal_fullscreen_exit_button"),
                )
            }
            OcIconButton(
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(Res.string.terminal_keys_edit),
                onClick = onEdit,
                modifier = Modifier.testTag("terminal_keys_edit_button"),
            )
        }
    }
}

/** Stable, readable part of a key's test tag. */
internal val BarKey.testId: String
    get() = when (this) {
        is BarKey.Text -> "text_" + label.codePointList().joinToString("_")
        else -> spec
    }

@Composable
internal fun BarKey.displayLabel(): String = if (this == BarKey.Paste) stringResource(Res.string.terminal_key_paste) else label

@Composable
private fun BarKey.description(): String? = when (this) {
    is BarKey.Special -> when (key) {
        SpecialKey.Escape -> stringResource(Res.string.terminal_key_escape)
        SpecialKey.Tab -> stringResource(Res.string.terminal_key_tab)
        SpecialKey.Enter -> stringResource(Res.string.terminal_key_enter)
        SpecialKey.Backspace -> stringResource(Res.string.terminal_key_backspace)
        SpecialKey.Left -> stringResource(Res.string.terminal_key_left)
        SpecialKey.Down -> stringResource(Res.string.terminal_key_down)
        SpecialKey.Up -> stringResource(Res.string.terminal_key_up)
        SpecialKey.Right -> stringResource(Res.string.terminal_key_right)
        SpecialKey.Home -> stringResource(Res.string.terminal_key_home)
        SpecialKey.End -> stringResource(Res.string.terminal_key_end)
        SpecialKey.PageUp -> stringResource(Res.string.terminal_key_page_up)
        SpecialKey.PageDown -> stringResource(Res.string.terminal_key_page_down)
        else -> null
    }
    else -> null
}

/** Key labels are the names printed on keyboards, so most are not translated; [description] is. */
@Composable
internal fun KeyButton(
    key: BarKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: ModifierState? = null,
    selected: Boolean = false,
) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.sm)
    val active = state != null && state != ModifierState.Off
    val highlighted = active || selected
    val description = key.description()
    val stateText = when (state) {
        ModifierState.Once -> stringResource(Res.string.terminal_modifier_once)
        ModifierState.Locked -> stringResource(Res.string.terminal_modifier_locked)
        ModifierState.Off -> stringResource(Res.string.terminal_modifier_off)
        null -> null
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(40.dp)
            .widthIn(min = 44.dp)
            .background(if (highlighted) c.bgAccent else c.layer02, shape)
            .border(1.dp, if (highlighted) c.bgAccent else c.borderBase, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                if (description != null) contentDescription = description
                if (stateText != null) stateDescription = stateText
            }
            .padding(horizontal = OcSpacing.sm),
    ) {
        Text(
            text = key.displayLabel(),
            style = OcTheme.mono.label,
            color = if (highlighted) c.onAccent else c.textBase,
            maxLines = 1,
        )
        if (state == ModifierState.Locked) {
            // A bar under the label tells a locked modifier from one armed for a single key.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = OcSpacing.xs)
                    .width(16.dp)
                    .height(2.dp)
                    .background(c.onAccent, RoundedCornerShape(1.dp)),
            )
        }
    }
}
