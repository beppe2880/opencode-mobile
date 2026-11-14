package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wifi
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoAssistantText
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ---- Notifications ----

@Composable
private fun FNotification(app: String, title: String, text: String, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    val c = OcTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.xl))
            .background(c.bgBase)
            .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.xl))
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(c.bgAccent))
            Text("$app · now", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
        Text(title, style = MaterialTheme.typography.titleSmall, color = c.textBase, maxLines = 1)
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1)
        actions()
    }
}

/** A permission request arrives as a notification and is allowed from it; then a session finishes. */
@Composable
internal fun NotificationsDemo(active: Boolean) {
    val c = OcTheme.colors
    var first by remember { mutableStateOf(true) }
    var second by remember { mutableStateOf(true) }
    var actions by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        first = true
        second = true
        actions = true
        finger.hide()
    }) {
        first = false
        second = false
        actions = true
        finger.hide()
        delay(700)
        first = true
        delay(1400)
        finger.show(targets.center("allow") + Offset(80f, 180f))
        finger.moveTo(targets.center("allow"))
        finger.tap()
        actions = false
        finger.hide()
        delay(900)
        second = true
        delay(3200)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.layer02)) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.sm), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            val enter = slideInVertically { -it } + fadeIn()
            val exit = slideOutVertically { -it } + fadeOut()
            AnimatedVisibility(first, enter = enter, exit = exit) {
                FNotification("OpenCode", "Fix login bug", "Permission required: edit src/auth/session.ts") {
                    FVisible(actions) {
                        Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.lg)) {
                            Text(
                                "Allow once",
                                style = MaterialTheme.typography.labelLarge,
                                color = c.textAccent,
                                modifier = Modifier.then(targets.target("allow")).padding(top = OcSpacing.xs),
                            )
                            Text("Reject", style = MaterialTheme.typography.labelLarge, color = c.textAccent, modifier = Modifier.padding(top = OcSpacing.xs))
                        }
                    }
                }
            }
            AnimatedVisibility(second, enter = enter, exit = exit) {
                FNotification("OpenCode", "Refactor payments", "Finished")
            }
        }
        finger.Draw()
    }
}

// ---- Pairing ----

@Composable
private fun FQrCode(modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color.White).padding(6.dp)) {
        val n = 21
        val cell = size.width / n
        fun finder(x: Int, y: Int) = (x < 7 && y < 7) || (x >= n - 7 && y < 7) || (x < 7 && y >= n - 7)
        for (x in 0 until n) {
            for (y in 0 until n) {
                val on = if (finder(x, y)) {
                    val fx = if (x >= n - 7) x - (n - 7) else x
                    val fy = if (y >= n - 7) y - (n - 7) else y
                    fx == 0 || fx == 6 || fy == 0 || fy == 6 || (fx in 2..4 && fy in 2..4)
                } else {
                    (x * 7 + y * 13 + x * y) % 3 == 0
                }
                if (on) drawRect(Color(0xFF111111), Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
        }
    }
}

/** The Connect screen: scan the QR code of `opencode pair`, paste the link, or tap a server found on the Wi-Fi. */
@Composable
internal fun PairDemo(active: Boolean) {
    val c = OcTheme.colors
    var phase by remember { mutableIntStateOf(1) }
    val scan = remember { Animatable(0f) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        phase = 1
        finger.hide()
    }) {
        phase = 0
        scan.snapTo(0f)
        finger.hide()
        delay(900)
        phase = 1
        delay(1000)
        finger.show(targets.center("scan") + Offset(60f, 160f))
        finger.moveTo(targets.center("scan"))
        finger.tap()
        finger.hide()
        phase = 2
        scan.animateTo(1f, tween(1100))
        scan.animateTo(0f, tween(900))
        phase = 3
        delay(1800)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgDeep)) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            Text("Connect to OpenCode", style = MaterialTheme.typography.titleMedium, color = c.textBase)
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                OcButton(
                    "Scan QR code",
                    {},
                    Modifier.weight(1f).then(targets.target("scan")),
                    variant = OcButtonVariant.Inverse,
                    leadingIcon = Icons.Outlined.QrCodeScanner,
                    compact = true,
                )
                OcButton("Paste link", {}, Modifier.weight(1f), variant = OcButtonVariant.Secondary, leadingIcon = Icons.Outlined.ContentPaste, compact = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Found on this network", style = MaterialTheme.typography.labelMedium, color = c.textMuted, modifier = Modifier.weight(1f))
                if (phase == 0) OcSpinner()
            }
            OcCard(Modifier.fillMaxWidth()) {
                if (phase == 0) {
                    Text(
                        "Looking for OpenCode servers on this network…",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        modifier = Modifier.padding(OcSpacing.md),
                    )
                } else {
                    OcListRow(
                        title = "192.168.1.10:4096",
                        subtitle = "Tap, then enter a pairing code or the password",
                        leading = { Icon(Icons.Outlined.Wifi, null, tint = c.iconMuted, modifier = Modifier.size(20.dp)) },
                    )
                }
            }
        }
        AnimatedVisibility(phase == 2, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0xF2101010)), contentAlignment = Alignment.Center) {
                Text(
                    "Scan pairing QR",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = OcSpacing.md),
                )
                Box(Modifier.size(120.dp).padding(top = OcSpacing.sm)) {
                    FQrCode(Modifier.fillMaxSize())
                    Box(Modifier.size(120.dp, 2.dp).offset { IntOffset(0, (scan.value * 112.dp.toPx()).roundToInt()) }.background(c.bgAccent))
                }
            }
        }
        AnimatedVisibility(phase == 3, enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.fillMaxSize().background(c.bgDeep), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Outlined.CheckCircle, null, tint = c.success.fg, modifier = Modifier.size(40.dp))
                Text("Connected", style = MaterialTheme.typography.titleMedium, color = c.textBase, modifier = Modifier.padding(top = OcSpacing.sm))
                Text("Reachable · OpenCode 1.4", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
        finger.Draw()
    }
}

// ---- Live sessions ----

@Composable
private fun FToolRow(name: String, path: String, done: Boolean) {
    val c = OcTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.layer01, RoundedCornerShape(OcRadius.md))
            .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
            .padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(Icons.Outlined.Description, null, tint = c.iconMuted, modifier = Modifier.size(14.dp))
        Text(name, style = MaterialTheme.typography.labelMedium, color = c.textBase, fontWeight = FontWeight.Medium)
        Text(path, style = OcTheme.mono.codeSmall, color = c.textMuted, maxLines = 1, modifier = Modifier.weight(1f))
        if (done) Icon(Icons.Outlined.CheckCircle, null, tint = c.success.fg, modifier = Modifier.size(14.dp)) else OcSpinner(size = 12.dp)
    }
}

private const val LIVE_REPLY = "I'll wrap `request()` in a **retry with backoff**."

/** The reply streams in with its reasoning and tool calls, until a permission is asked and answered. */
@Composable
internal fun LiveDemo(active: Boolean) {
    val c = OcTheme.colors
    val cache = rememberMarkdownCache()
    var stage by remember { mutableIntStateOf(5) }
    var streamed by remember { mutableIntStateOf(LIVE_REPLY.length) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        stage = 5
        streamed = LIVE_REPLY.length
        finger.hide()
    }) {
        stage = 0
        streamed = 0
        finger.hide()
        delay(500)
        stage = 1
        delay(600)
        stage = 2
        delay(1000)
        stage = 3
        delay(900)
        stage = 4
        while (streamed < LIVE_REPLY.length) {
            streamed = minOf(LIVE_REPLY.length, streamed + 2)
            delay(35)
        }
        delay(500)
        stage = 5
        delay(1000)
        finger.show(targets.center("allow") + Offset(60f, 150f))
        finger.moveTo(targets.center("allow"))
        finger.tap()
        stage = 6
        finger.hide()
        delay(2600)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs, Alignment.Bottom)) {
            FVisible(stage >= 1) { DemoUserBubble("Add retries to the API client") }
            FVisible(stage >= 2) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    Icon(Icons.Outlined.Psychology, null, tint = c.iconMuted, modifier = Modifier.size(14.dp))
                    Text(if (stage == 2) "Thinking…" else "Thought for 2s", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                }
            }
            FVisible(stage >= 3) { FToolRow("Read", "src/api/client.ts", done = stage >= 4) }
            FVisible(stage >= 4) { DemoAssistantText("demo-live", LIVE_REPLY.take(streamed), cache) }
            FVisible(stage >= 5) { FToolRow("Edit", "src/api/client.ts", done = stage >= 6) }
            FVisible(stage == 5) {
                OcCard(
                    Modifier.fillMaxWidth(),
                    color = if (c.isDark) c.layer01 else c.warning.bg,
                    borderColor = c.warning.border,
                    shape = RoundedCornerShape(OcRadius.xl),
                ) {
                    Row(
                        Modifier.padding(OcSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        Icon(Icons.Outlined.Shield, null, tint = c.warning.fg, modifier = Modifier.size(18.dp))
                        Text("Permission required", style = MaterialTheme.typography.titleSmall, color = c.textBase, modifier = Modifier.weight(1f))
                        OcButton("Reject", {}, variant = OcButtonVariant.Secondary, compact = true)
                        OcButton("Allow once", {}, Modifier.then(targets.target("allow")), variant = OcButtonVariant.Inverse, compact = true)
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Steer, queue, commands and files ----

private class Suggestion(val title: String, val description: String?, val file: Boolean)

private val SLASH = listOf(
    Suggestion("/undo", "Undo the last prompt and its file changes", false),
    Suggestion("/redo", "Restore what was undone", false),
    Suggestion("/fork", "Copy this session to a new one", false),
)
private val FILES = listOf(Suggestion("src/app.ts", null, true), Suggestion("src/api/client.ts", null, true))

@Composable
private fun FSuggestions(items: List<Suggestion>, targets: DemoTargets) {
    val c = OcTheme.colors
    OcCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(OcRadius.lg)) {
        items.forEachIndexed { i, item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)
                    .then(if (i == 0) targets.target("suggestion") else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                if (item.file) {
                    Icon(Icons.Outlined.Description, null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
                    Text(item.title, style = OcTheme.mono.codeSmall, color = c.textBase, maxLines = 1)
                } else {
                    Text(item.title, style = OcTheme.mono.code, color = c.textBase)
                    Text(item.description.orEmpty(), style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1)
                }
            }
        }
    }
}

/** `/` lists commands, `@` lists files; while the agent works the prompt can steer it or wait in the queue. */
@Composable
internal fun SteerDemo(active: Boolean) {
    var text by remember { mutableStateOf("Use @src/app.ts to add retries") }
    var kind by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var queue by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        text = "Use @src/app.ts to add retries"
        kind = 0
        running = true
        queue = true
        finger.hide()
    }) {
        text = ""
        kind = 0
        running = false
        queue = false
        finger.hide()
        delay(600)
        finger.show(targets.center("input") + Offset(0f, 150f))
        finger.moveTo(targets.center("input"))
        finger.tap()
        fType("/") { text = it }
        kind = 1
        delay(1500)
        finger.moveTo(targets.center("suggestion"))
        finger.tap()
        text = "/undo "
        kind = 0
        delay(700)
        text = ""
        fType("Use @src/a") { text = it }
        kind = 2
        delay(900)
        finger.moveTo(targets.center("suggestion"))
        finger.tap()
        text = "Use @src/app.ts to add retries"
        kind = 0
        running = true
        delay(900)
        finger.moveTo(targets.center("queue"))
        finger.tap()
        queue = true
        finger.hide()
        delay(2600)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.sm), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs, Alignment.Bottom)) {
            FVisible(kind != 0) { FSuggestions(if (kind == 1) SLASH else FILES, targets) }
            FComposer(text, targets) {
                FVisible(running && text.isNotBlank()) {
                    Row(Modifier.padding(bottom = OcSpacing.xs), horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        OcChip("Steer now", {}, selected = !queue)
                        OcChip("Queue", {}, Modifier.then(targets.target("queue")), selected = queue)
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Fork ----

@Composable
private fun FMenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().background(if (highlight) c.layer02 else c.bgBase).padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(icon, null, tint = c.iconBase, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textBase)
    }
}

/** A long press on a prompt offers "Fork from here": a new session with the history before it. */
@Composable
internal fun ForkDemo(active: Boolean) {
    val c = OcTheme.colors
    val cache = rememberMarkdownCache()
    var menu by remember { mutableStateOf(true) }
    var forked by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        menu = true
        forked = false
        finger.hide()
    }) {
        menu = false
        forked = false
        finger.hide()
        delay(800)
        finger.show(targets.center("prompt") + Offset(0f, 170f))
        finger.moveTo(targets.center("prompt"))
        finger.longPress(650)
        menu = true
        delay(1000)
        finger.moveTo(targets.center("fork"))
        finger.tap()
        menu = false
        forked = true
        finger.hide()
        delay(3000)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize()) {
            DemoTopBar(if (forked) "Refactor auth (fork #1)" else "Refactor auth", subtitle = "my-api")
            OcDivider()
            Column(Modifier.padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                DemoUserBubble("Refactor the auth module", Modifier.then(targets.target("prompt")))
                DemoAssistantText("demo-fork", "Moved login and token code into `auth/`.", cache)
                FVisible(!forked) { DemoUserBubble("Now add tests") }
            }
        }
        if (menu) {
            val bottom = targets.bounds("prompt")?.bottom ?: 0f
            OcCard(
                Modifier.align(Alignment.TopEnd).offset { IntOffset(-12.dp.roundToPx(), bottom.roundToInt() + 4.dp.roundToPx()) }.size(190.dp, 102.dp),
                shape = RoundedCornerShape(OcRadius.md),
            ) {
                FMenuRow(Icons.Outlined.ContentCopy, "Copy text")
                FMenuRow(Icons.AutoMirrored.Outlined.Undo, "Undo from here")
                FMenuRow(Icons.Outlined.CallSplit, "Fork from here", Modifier.then(targets.target("fork")), highlight = true)
            }
        }
        finger.Draw()
    }
}

// ---- Undo and redo ----

/** `/undo` hides the last prompt and restores the files; Restore brings it all back. */
@Composable
internal fun UndoDemo(active: Boolean) {
    val c = OcTheme.colors
    val cache = rememberMarkdownCache()
    var text by remember { mutableStateOf("") }
    var undone by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        text = ""
        undone = true
        finger.hide()
    }) {
        text = ""
        undone = false
        finger.hide()
        delay(800)
        finger.show(targets.center("input") + Offset(0f, 150f))
        finger.moveTo(targets.center("input"))
        finger.tap()
        fType("/undo") { text = it }
        delay(500)
        finger.moveTo(targets.center("suggestion"))
        finger.tap()
        text = ""
        undone = true
        delay(1800)
        finger.moveTo(targets.center("restore"))
        finger.tap()
        undone = false
        delay(1500)
        finger.hide()
        delay(500)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.sm), verticalArrangement = Arrangement.Bottom) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = OcSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xs, Alignment.Bottom),
            ) {
                DemoUserBubble("Add a /health endpoint")
                DemoAssistantText("demo-undo", "Added it in `src/server.ts`.", cache)
                FVisible(!undone) { DemoUserBubble("Rename everything to snake_case") }
            }
            FVisible(text.startsWith("/")) { FSuggestions(listOf(SLASH[0]), targets) }
            FVisible(undone) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = OcSpacing.xs)
                        .background(c.layer01, RoundedCornerShape(OcRadius.lg))
                        .padding(start = OcSpacing.md, end = OcSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
                    Text(
                        "1 message undone",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        modifier = Modifier.weight(1f).padding(horizontal = OcSpacing.sm),
                    )
                    OcButton("Restore", {}, Modifier.then(targets.target("restore")), variant = OcButtonVariant.Ghost, compact = true)
                }
            }
            FComposer(text, targets)
        }
        finger.Draw()
    }
}
