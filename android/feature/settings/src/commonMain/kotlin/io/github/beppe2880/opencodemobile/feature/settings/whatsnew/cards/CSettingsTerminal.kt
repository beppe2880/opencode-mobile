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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
