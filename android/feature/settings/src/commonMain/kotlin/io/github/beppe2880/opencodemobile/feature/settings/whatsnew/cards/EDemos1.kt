package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.theme.OcColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.terminalAnsi
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay

// ---- Home screen widget ----

/** A home screen with the widget; refreshing it moves a session from running to recent. */
@Composable
internal fun WidgetDemo(active: Boolean) {
    val c = OcTheme.colors
    var running by remember { mutableIntStateOf(2) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        running = 2
        finger.hide()
    }) {
        running = 2
        finger.hide()
        delay(1600)
        finger.show(targets.center("refresh") + Offset(-40f, 110f))
        finger.moveTo(targets.center("refresh"))
        finger.tap()
        running = 1
        delay(300)
        finger.hide()
        delay(2600)
    }
    Box(
        Modifier
            .fillMaxSize()
            .then(targets.root())
            .background(Brush.verticalGradient(listOf(Color(0xFF3B5B8C), Color(0xFF8E5C8A), Color(0xFFE8956B))))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.bgBase)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("OpenCode", style = MaterialTheme.typography.titleSmall, color = c.textBase)
                    Text("home-server · $running running", style = MaterialTheme.typography.labelSmall, color = c.textMuted, maxLines = 1)
                }
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = c.textMuted,
                    modifier = Modifier.padding(end = 6.dp).size(20.dp).then(targets.target("refresh")),
                )
                Row(
                    Modifier.height(30.dp).clip(CircleShape).background(c.bgAccent).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(16.dp))
                    Text("New session", style = MaterialTheme.typography.labelMedium, color = c.onAccent)
                }
            }
            WidgetRow("Fix flaky login test", "Running", true)
            WidgetRow("Add dark mode toggle", if (running >= 2) "Running" else "Just now", running >= 2)
            WidgetRow("Update the README", "2h ago", false)
        }
    }
}

@Composable
private fun WidgetRow(title: String, detail: String, running: Boolean) {
    val c = OcTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.layer02).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (running) c.success.fg else Color.Transparent))
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, color = c.textBase, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = if (running) c.success.fg else c.textMuted)
        }
    }
}

// ---- File viewer ----

private val CODE = listOf(
    "package app",
    "",
    "fun main() {",
    "    val port = 4096",
    "    val name = \"opencode\"",
    "    // start the server",
    "    serve(name, port)",
    "}",
)

private val KEYWORDS = setOf("package", "fun", "val", "var", "if", "else", "return", "import", "class")
private val TOKEN = Regex("\"[^\"]*\"|\\w+|\\W")

private fun highlight(line: String, c: OcColors): AnnotatedString {
    val syntax = c.syntax
    val dark = c.isDark
    val keyword = syntax?.keyword ?: if (dark) Color(0xFFC678DD) else Color(0xFFA626A2)
    val string = syntax?.string ?: if (dark) Color(0xFF98C379) else Color(0xFF50A14F)
    val number = syntax?.number ?: if (dark) Color(0xFFD19A66) else Color(0xFF986801)
    val comment = syntax?.comment ?: if (dark) Color(0xFF7F848E) else Color(0xFFA0A1A7)
    val function = syntax?.function ?: if (dark) Color(0xFF61AFEF) else Color(0xFF4078F2)
    val commentAt = line.indexOf("//")
    val code = if (commentAt >= 0) line.substring(0, commentAt) else line
    return buildAnnotatedString {
        for (match in TOKEN.findAll(code)) {
            val token = match.value
            val color = when {
                token.startsWith("\"") -> string
                token in KEYWORDS -> keyword
                token.firstOrNull()?.isDigit() == true -> number
                token.firstOrNull()?.isLetter() == true && code.getOrNull(match.range.last + 1) == '(' -> function
                else -> c.textBase
            }
            withStyle(SpanStyle(color = color)) { append(token) }
        }
        if (commentAt >= 0) withStyle(SpanStyle(color = comment)) { append(line.substring(commentAt)) }
    }
}

/** A tool row in the chat gets an open button; it opens the file, highlighted, read-only. */
@Composable
internal fun FileViewerDemo(active: Boolean) {
    val c = OcTheme.colors
    var viewer by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        viewer = true
        finger.hide()
    }) {
        viewer = false
        finger.hide()
        delay(1300)
        finger.show(targets.center("open") + Offset(-30f, 110f))
        finger.moveTo(targets.center("open"))
        finger.tap()
        viewer = true
        delay(300)
        finger.hide()
        delay(3600)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgBase)) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            DemoUserBubble("Show me the entry point")
            ToolRow("Read", "src/Main.kt", Modifier.then(targets.target("open")))
            ToolRow("Edit", "src/Server.kt", Modifier)
        }
        AnimatedVisibility(
            viewer,
            enter = slideInHorizontally { it / 2 } + fadeIn(),
            exit = slideOutHorizontally { it / 2 } + fadeOut(),
        ) {
            Column(Modifier.fillMaxSize().background(c.bgBase)) {
                DemoTopBar("Main.kt", subtitle = "src/main")
                Column(Modifier.fillMaxSize().background(c.layer01).padding(vertical = OcSpacing.sm)) {
                    CODE.forEachIndexed { index, line ->
                        Row {
                            Text(
                                "${index + 1}",
                                style = OcTheme.mono.codeSmall,
                                color = c.textFaint,
                                modifier = Modifier.width(36.dp).padding(end = OcSpacing.sm),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                            Text(highlight(line, c), style = OcTheme.mono.codeSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
        finger.Draw()
    }
}

@Composable
private fun ToolRow(label: String, path: String, openModifier: Modifier) {
    val c = OcTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.md))
            .background(c.layer01)
            .border(1.dp, c.borderMuted, RoundedCornerShape(OcRadius.md))
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(Icons.Outlined.Description, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.textBase)
        Text(
            path,
            style = OcTheme.mono.codeSmall,
            color = c.textCodePath,
            modifier = Modifier.clip(RoundedCornerShape(OcRadius.xs)).background(c.bgCodePath).padding(horizontal = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = c.iconBase, modifier = openModifier.size(18.dp))
    }
}

// ---- Terminal ----

private val TERMINAL_KEYS = listOf("Esc", "Tab", "Ctrl", "Alt", "←", "↑", "↓", "→", "↵")

/** A command is typed, Enter on the key bar runs it, the output comes back. */
@Composable
internal fun TerminalDemo(active: Boolean) {
    val c = OcTheme.colors
    var typed by remember { mutableStateOf("npm test") }
    var out by remember { mutableIntStateOf(3) }
    var hot by remember { mutableStateOf<String?>(null) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        typed = "npm test"
        out = 3
        hot = null
        finger.hide()
    }) {
        typed = ""
        out = 0
        hot = null
        finger.hide()
        delay(800)
        for (ch in "npm test") {
            typed += ch
            delay(120)
        }
        delay(350)
        finger.show(targets.center("↵") + Offset(0f, -100f))
        finger.moveTo(targets.center("↵"))
        hot = "↵"
        finger.tap()
        hot = null
        for (n in 1..3) {
            out = n
            delay(280)
        }
        finger.hide()
        delay(2800)
    }
    val ansi = c.terminalAnsi
    val green = ansi.getOrNull(2) ?: Color(0xFF98C379)
    val blue = ansi.getOrNull(4) ?: Color(0xFF61AFEF)
    val fg = Color(0xFFD7DAE0)
    val mono = OcTheme.mono.codeSmall
    Column(Modifier.fillMaxSize().then(targets.root()).background(c.bgBase)) {
        DemoTopBar("Terminal", subtitle = "~/app") {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF14161B)).padding(OcSpacing.md)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = blue)) { append("~/app ") }
                    withStyle(SpanStyle(color = green)) { append("$ ") }
                    withStyle(SpanStyle(color = fg)) { append(typed) }
                    if (out == 0) withStyle(SpanStyle(color = fg)) { append("▌") }
                },
                style = mono,
            )
            if (out >= 1) Text("> app@1.0.0 test", style = mono, color = Color(0xFF8B91A0))
            if (out >= 2) Text("PASS  src/login.test.ts", style = mono, color = green)
            if (out >= 3) {
                Text("Tests: 12 passed", style = mono, color = green)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = blue)) { append("~/app ") }
                        withStyle(SpanStyle(color = green)) { append("$ ") }
                        withStyle(SpanStyle(color = fg)) { append("▌") }
                    },
                    style = mono,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().background(c.layer01).padding(horizontal = OcSpacing.sm, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TERMINAL_KEYS.forEach { key ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(OcRadius.sm))
                        .background(if (hot == key) c.bgAccent else c.layer03)
                        .then(targets.target(key)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        key,
                        style = OcTheme.mono.label,
                        color = if (hot == key) c.onAccent else c.textBase,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
