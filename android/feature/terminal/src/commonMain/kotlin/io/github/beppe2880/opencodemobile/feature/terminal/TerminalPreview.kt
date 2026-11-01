package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.components.OcCustomizable
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.components.PartPreview
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.terminalAnsi
import io.github.beppe2880.opencodemobile.feature.terminal.resources.Res
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_preview_fullscreen
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_preview_keyboard_off
import io.github.beppe2880.opencodemobile.feature.terminal.resources.terminal_preview_keyboard_on
import org.jetbrains.compose.resources.stringResource

/**
 * The terminal for Settings: the title bar (gone in full screen), a sample session, the real key
 * bar with the saved keys and the on-screen keyboard that opens with it, or not.
 */
@Composable
fun TerminalPreview(preview: PartPreview, modifier: Modifier = Modifier) {
    val s = preview.settings
    val c = OcTheme.colors
    Column(modifier.fillMaxWidth().padding(OcSpacing.sm).testTag("terminal_preview"), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        OcCustomizable(SettingsPart.TerminalFullscreen, preview) {
            Column {
                if (s.terminalFullscreenByDefault) {
                    Caption(stringResource(Res.string.terminal_preview_fullscreen))
                } else {
                    OcTopBar(
                        title = "~/code/shop",
                        leading = { OcIconButton(icon = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, onClick = {}) },
                        applyStatusBarInset = false,
                    )
                }
                val ansi = c.terminalAnsi
                val mono = OcTheme.mono.code
                Column(
                    Modifier.fillMaxWidth().background(c.layer01, RoundedCornerShape(OcRadius.md)).padding(OcSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(OcSpacing.xxs),
                ) {
                    listOf("npm test" to "  ✓ cart (14 tests)", "git status" to "  modified: src/cart/Coupon.kt").forEach { (command, output) ->
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = ansi[2])) { append("shop $ ") }
                                append(command)
                            },
                            style = mono,
                            color = c.textBase,
                        )
                        Text(output, style = mono, color = c.textMuted)
                    }
                    Text(buildAnnotatedString { withStyle(SpanStyle(color = ansi[2])) { append("shop $ ") } }, style = mono)
                }
            }
        }
        OcCustomizable(SettingsPart.KeyBar, preview) {
            KeyBar(
                rows = BarKeys.fromSettings(s.terminalKeys),
                modifierState = { ModifierState.Off },
                onPress = {},
                onEdit = {},
                fullscreen = s.terminalFullscreenByDefault,
            )
        }
        OcCustomizable(SettingsPart.TerminalKeyboard, preview) {
            if (s.terminalOpenKeyboard) {
                Column {
                    Caption(stringResource(Res.string.terminal_preview_keyboard_on))
                    KeyboardSketch()
                }
            } else {
                Caption(stringResource(Res.string.terminal_preview_keyboard_off))
            }
        }
    }
}

/** Rows of keys like the system keyboard's, without the labels that would vary with the language. */
@Composable
private fun KeyboardSketch() {
    val c = OcTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.layer02, RoundedCornerShape(OcRadius.md)).padding(OcSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(10, 9, 7).forEach { keys ->
            Row(Modifier.fillMaxWidth(keys / 10f), horizontalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
                repeat(keys) { Box(Modifier.weight(1f).height(22.dp).background(c.bgBase, RoundedCornerShape(4.dp))) }
            }
        }
        Box(Modifier.fillMaxWidth(0.5f).height(22.dp).background(c.bgBase, RoundedCornerShape(4.dp)))
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = OcTheme.colors.textMuted,
        modifier = Modifier.padding(horizontal = OcSpacing.xs, vertical = OcSpacing.xs),
    )
}
