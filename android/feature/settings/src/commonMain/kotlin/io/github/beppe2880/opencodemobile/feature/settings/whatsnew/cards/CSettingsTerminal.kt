package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import kotlinx.coroutines.delay

// ---- Settings categories and search ----

private class Category(val icon: ImageVector, val title: String, val sub: String)

private val CATEGORIES = listOf(
    Category(Icons.Outlined.Palette, "Appearance", "Theme, OpenCode colors and text size"),
    Category(Icons.Outlined.ChatBubbleOutline, "Chat", "Header, thinking, tools, cards and swiping"),
    Category(Icons.Outlined.Mic, "Voice input", "Microphone, speech to text, cloud provider"),
    Category(Icons.Outlined.Terminal, "Terminal", "Key bar, keyboard and full screen"),
)

private const val QUERY = "voice"

@Composable
private fun IconTile(icon: ImageVector) {
    val c = OcTheme.colors
    Box(Modifier.size(26.dp).clip(RoundedCornerShape(OcRadius.sm)).background(c.layer03), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CategoryRow(icon: ImageVector, title: String, sub: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().height(40.dp).padding(horizontal = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        IconTile(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OptionRow(title: String, value: String) {
    val c = OcTheme.colors
    Row(Modifier.fillMaxWidth().height(36.dp).padding(horizontal = OcSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}

/** The list of categories with a search field; a category opens its own page; a search jumps to settings. */
@Composable
internal fun SettingsCategoriesDemo(active: Boolean) {
    val c = OcTheme.colors
    var page by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        page = false
        query = ""
        typing = false
        finger.hide()
    }) {
        page = false
        query = ""
        typing = false
        finger.hide()
        delay(900)
        finger.show(targets.center("voice") + Offset(80f, 140f))
        finger.moveTo(targets.center("voice"))
        finger.tap()
        page = true
        delay(1700)
        finger.moveTo(targets.center("back"))
        finger.tap()
        page = false
        delay(600)
        finger.moveTo(targets.center("search"))
        finger.tap()
        typing = true
        for (n in 1..QUERY.length) {
            query = QUERY.take(n)
            delay(170)
        }
        finger.hide()
        delay(2400)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Crossfade(page, Modifier.padding(OcSpacing.sm), label = "settings_page") { onPage ->
            Column {
                if (onPage) {
                    Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).then(targets.target("back")), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                        }
                        Text("Voice input", style = MaterialTheme.typography.titleSmall, color = c.textBase)
                    }
                    OptionRow("Speech to text", "Cloud")
                    OptionRow("Provider", "OpenAI")
                    OptionRow("Recording style", "Waveform")
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(OcRadius.md))
                            .background(c.bgBase)
                            .border(1.dp, if (typing) c.borderFocus else c.borderBase, RoundedCornerShape(OcRadius.md))
                            .padding(horizontal = OcSpacing.md)
                            .then(targets.target("search")),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
                        Text(
                            query.ifEmpty { "Search settings" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (query.isEmpty()) c.textFaint else c.textBase,
                        )
                    }
                    Spacer(Modifier.height(OcSpacing.xs))
                    if (query.isEmpty()) {
                        CATEGORIES.forEach { category ->
                            val modifier = if (category.title == "Voice input") targets.target("voice") else Modifier
                            CategoryRow(category.icon, category.title, category.sub, modifier)
                        }
                    } else {
                        CategoryRow(Icons.Outlined.Mic, "Speech to text", "Voice input")
                        CategoryRow(Icons.Outlined.Mic, "Recording style", "Voice input")
                        CategoryRow(Icons.Outlined.Mic, "Send the app language", "Voice input")
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Terminal full screen ----

private val TERMINAL_LINES = listOf(
    "$ bun test",
    "✓ parser › nested lists",
    "✓ parser › code fences",
    "✓ client › reconnects",
    "✓ client › backs off",
    "✗ upload › retries",
    "  expected 3, got 1",
    "$ git status",
    "M  src/upload.ts",
    "M  src/client.ts",
    "$ bun test upload",
    "✓ upload › retries",
    "$ ",
)

@Composable
private fun KeyCap(label: String) {
    val c = OcTheme.colors
    Box(
        Modifier
            .widthIn(min = 30.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(OcRadius.sm))
            .background(c.layer03),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = OcTheme.mono.codeSmall, color = c.textBase, modifier = Modifier.padding(horizontal = OcSpacing.sm))
    }
}

/** The colors of a terminal line, as the shell prints them. */
@Composable
private fun terminalLineColor(line: String) = OcTheme.colors.let { c ->
    when {
        line.startsWith("$") -> c.textAccent
        line.startsWith("✓") -> c.success.fg
        line.startsWith("✗") -> c.danger.fg
        else -> c.textMuted
    }
}

/** How much of the failing line is selected: nothing, the long-pressed word, or up to its end. */
private enum class TerminalSelection { None, Word, Line }

/** Text that may be selected: highlighted, with a handle under each end. */
@Composable
private fun SelectableText(text: String, selected: Boolean, color: Color, modifier: Modifier = Modifier, handles: Boolean = selected) {
    val c = OcTheme.colors
    Box(modifier) {
        Text(
            text,
            style = OcTheme.mono.codeSmall,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.background(if (selected) c.bgAccent.copy(alpha = 0.3f) else Color.Transparent),
        )
        if (handles) {
            Box(Modifier.align(Alignment.BottomStart).offset(x = (-4).dp, y = 9.dp).size(8.dp).clip(CircleShape).background(c.bgAccent))
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 9.dp).size(8.dp).clip(CircleShape).background(c.bgAccent))
        }
    }
}

/**
 * A long press on a word of the output selects it; a drag of its handle extends the selection,
 * Copy puts it on the clipboard.
 */
@Composable
internal fun TerminalSelectionDemo(active: Boolean) {
    val c = OcTheme.colors
    var selection by remember { mutableStateOf(TerminalSelection.Line) }
    var copied by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        selection = TerminalSelection.Line
        copied = false
        finger.hide()
    }) {
        selection = TerminalSelection.None
        copied = false
        finger.hide()
        delay(900)
        finger.show(targets.center("word") + Offset(60f, 140f))
        finger.moveTo(targets.center("word"))
        finger.longPress()
        selection = TerminalSelection.Word
        delay(500)
        val end = targets.bounds("word")?.bottomRight ?: Offset.Zero
        finger.moveTo(end + Offset(0f, 24f), 350)
        val lineEnd = targets.bounds("rest")?.bottomRight ?: Offset.Zero
        finger.drag(lineEnd + Offset(0f, 24f), 700)
        selection = TerminalSelection.Line
        delay(500)
        finger.moveTo(targets.center("copy"))
        finger.tap()
        selection = TerminalSelection.None
        copied = true
        finger.hide()
        delay(1600)
        copied = false
        delay(600)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgBase)) {
        Column(Modifier.fillMaxSize()) {
            DemoTopBar("Terminal", subtitle = "~/api")
            TerminalSelectionOutput(selection, copied, targets, Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().background(c.layer01).padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KeyCap("Esc")
                KeyCap("Tab")
                KeyCap("Ctrl")
                KeyCap("↑")
            }
        }
        finger.Draw()
    }
}

/** The output of the selection demo, with the selection toolbar and the Copied notice over it. */
@Composable
private fun TerminalSelectionOutput(selection: TerminalSelection, copied: Boolean, targets: DemoTargets, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Box(modifier.fillMaxWidth().clipToBounds().background(c.bgDeep)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)) {
            TERMINAL_LINES.take(9).forEach { line ->
                if (line.startsWith("✗")) {
                    val color = terminalLineColor(line)
                    Row {
                        Text("✗ ", style = OcTheme.mono.codeSmall, color = color, maxLines = 1, softWrap = false)
                        if (selection == TerminalSelection.Line) {
                            SelectableText("upload › retries", selected = true, color = color)
                        } else {
                            SelectableText("upload", selected = selection == TerminalSelection.Word, color = color, modifier = targets.target("word"))
                            SelectableText(" › retries", selected = false, color = color, modifier = targets.target("rest"))
                        }
                    }
                } else {
                    Text(line, style = OcTheme.mono.codeSmall, color = terminalLineColor(line), maxLines = 1, softWrap = false)
                }
            }
        }
        AnimatedVisibility(
            selection != TerminalSelection.None,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(OcSpacing.sm),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(OcRadius.md))
                    .background(c.layer02)
                    .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
                    .padding(horizontal = OcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
            ) {
                Text(
                    "Copy",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textBase,
                    modifier = Modifier.padding(OcSpacing.sm).then(targets.target("copy")),
                )
                Text("Select all", style = MaterialTheme.typography.labelLarge, color = c.textBase, modifier = Modifier.padding(OcSpacing.sm))
                Icon(Icons.Outlined.Close, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(18.dp))
            }
        }
        AnimatedVisibility(copied, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).padding(OcSpacing.md)) {
            Text(
                "Copied",
                style = MaterialTheme.typography.labelLarge,
                color = c.textInverse,
                modifier = Modifier.clip(RoundedCornerShape(OcRadius.md)).background(c.bgInverse).padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
            )
        }
    }
}

/** The full screen button hides the status bar and the title bar: the terminal shows more lines. */
@Composable
internal fun TerminalFullscreenDemo(active: Boolean) {
    val c = OcTheme.colors
    var fullscreen by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        fullscreen = true
        finger.hide()
    }) {
        fullscreen = false
        finger.hide()
        delay(1300)
        finger.show(targets.center("fullscreen") + Offset(-120f, -150f))
        finger.moveTo(targets.center("fullscreen"))
        finger.tap()
        fullscreen = true
        delay(2800)
        finger.tap()
        fullscreen = false
        finger.hide()
        delay(900)
    }
    Column(Modifier.fillMaxSize().then(targets.root()).background(c.bgBase)) {
        AnimatedVisibility(!fullscreen, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column {
                Row(
                    Modifier.fillMaxWidth().height(18.dp).padding(horizontal = OcSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("9:41", style = MaterialTheme.typography.labelSmall, color = c.textMuted, modifier = Modifier.weight(1f))
                    Text("LTE  82%", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                }
                DemoTopBar("Terminal", subtitle = "~/api")
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(c.bgDeep)
                .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
        ) {
            TERMINAL_LINES.forEach { line ->
                val color = when {
                    line.startsWith("$") -> c.textAccent
                    line.startsWith("✓") -> c.success.fg
                    line.startsWith("✗") -> c.danger.fg
                    else -> c.textMuted
                }
                Text(line, style = OcTheme.mono.codeSmall, color = color, maxLines = 1, softWrap = false)
            }
        }
        Row(
            Modifier.fillMaxWidth().background(c.layer01).padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeyCap("Esc")
            KeyCap("Tab")
            KeyCap("Ctrl")
            KeyCap("↑")
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(32.dp).then(targets.target("fullscreen")), contentAlignment = Alignment.Center) {
                Icon(
                    if (fullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = null,
                    tint = c.iconBase,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
