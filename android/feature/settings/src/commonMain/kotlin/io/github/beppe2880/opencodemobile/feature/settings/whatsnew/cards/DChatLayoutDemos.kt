package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcLogo
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoSessionRow
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay

// ---- The folder, branch and changed files in the chat header ----

@Composable
private fun VcsPart(icon: ImageVector, text: String) {
    val c = OcTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
        Icon(icon, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, softWrap = false)
    }
}

private val CHANGED_FILES = listOf(
    Triple("LoginRoute.kt", "+12", "−3"),
    Triple("AuthRedirect.kt", "+8", "−2"),
    Triple("AuthRedirectTest.kt", "+21", "−0"),
)

/** The header line counts the files the agent changed; a tap on it opens them. */
@Composable
internal fun HeaderDemo(active: Boolean) {
    val c = OcTheme.colors
    var files by remember { mutableIntStateOf(3) }
    var sheet by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        files = 3
        sheet = true
        finger.hide()
    }) {
        files = 2
        sheet = false
        finger.hide()
        delay(1000)
        files = 3
        delay(1000)
        finger.show(targets.center("vcs") + Offset(160f, 240f))
        finger.moveTo(targets.center("vcs"))
        finger.tap()
        sheet = true
        delay(3000)
        sheet = false
        delay(800)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(c.bgBase).padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text("Fix login redirect", style = MaterialTheme.typography.titleSmall, color = c.textBase, maxLines = 1)
                    Row(
                        Modifier.then(targets.target("vcs")),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        VcsPart(Icons.Outlined.Folder, "web-app")
                        VcsPart(Icons.Outlined.AccountTree, "fix/login")
                        VcsPart(Icons.Outlined.Difference, files.toString())
                    }
                }
                Icon(Icons.Outlined.Difference, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
            }
            OcDivider()
            Column(Modifier.padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
                DemoUserBubble("Fix the login redirect")
                Text(
                    "I made the redirect keep the query string and added a test.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textBase,
                )
            }
        }
        AnimatedVisibility(
            sheet,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = OcRadius.sheet, topEnd = OcRadius.sheet))
                    .background(c.bgBase)
                    .border(1.dp, c.borderBase, RoundedCornerShape(topStart = OcRadius.sheet, topEnd = OcRadius.sheet))
                    .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Text("Changes", style = MaterialTheme.typography.titleSmall, color = c.textBase, modifier = Modifier.weight(1f))
                    OcChip("Whole session", onClick = {}, selected = true)
                    OcChip("Last turn", onClick = {})
                }
                CHANGED_FILES.forEach { (name, added, removed) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                        Text(name, style = OcTheme.mono.codeSmall, color = c.textBase, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(added, style = OcTheme.mono.codeSmall, color = c.diff.added)
                        Text(removed, style = OcTheme.mono.codeSmall, color = c.diff.removed)
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- The offline interactive demo ----

private val DEMO_SESSIONS = listOf(
    Triple("Explain this repository", "dotfiles", "2h"),
    Triple("Add a dark mode toggle", "web-app", "5h"),
    Triple("Fix the failing test", "api", "1d"),
)

/** On the Connect screen a tap on "Try without a server" opens sample sessions. */
@Composable
internal fun OfflineDemo(active: Boolean) {
    val c = OcTheme.colors
    var screen by remember { mutableIntStateOf(1) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        screen = 1
        finger.hide()
    }) {
        screen = 0
        finger.hide()
        delay(1400)
        finger.show(targets.center("try") + Offset(140f, 220f))
        finger.moveTo(targets.center("try"))
        finger.tap()
        screen = 1
        finger.hide()
        delay(3200)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Crossfade(screen, Modifier.fillMaxSize()) { shown ->
            if (shown == 0) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = OcSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    OcLogo(size = 28.dp)
                    Text(
                        "Connect to OpenCode",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textBase,
                        modifier = Modifier.padding(top = OcSpacing.xs, bottom = OcSpacing.sm),
                    )
                    Text(
                        "http://192.168.1.20:4096",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textFaint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(OcRadius.md))
                            .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
                            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                    )
                    Text(
                        "No server yet? Look around with sample sessions. Nothing leaves your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = OcSpacing.sm),
                    )
                    Box(Modifier.then(targets.target("try"))) {
                        OcButton(
                            text = "Try without a server",
                            onClick = {},
                            variant = OcButtonVariant.Ghost,
                            leadingIcon = Icons.Outlined.Science,
                            compact = true,
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        Text("Sessions", style = MaterialTheme.typography.titleSmall, color = c.textBase)
                        OcBadge("Demo", c.info)
                    }
                    OcDivider()
                    DEMO_SESSIONS.forEach { (title, project, time) -> DemoSessionRow(title, project, time) }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Side-by-side session list and chat ----

private class SplitSession(val title: String, val project: String, val prompt: String, val reply: String)

private val SPLIT_SESSIONS = listOf(
    SplitSession("Fix login redirect", "web-app", "Fix the login redirect", "The redirect now keeps the query string."),
    SplitSession("Add dark mode", "web-app", "Add a dark mode toggle", "Added a toggle in Settings and saved the choice."),
    SplitSession("Update the README", "docs", "Update the README", "Rewrote the install steps and the badges."),
)

/** Unfolded: the list next to the chat; a tap switches the chat, a drag on the divider resizes the list. */
@Composable
internal fun SplitDemo(active: Boolean) {
    val c = OcTheme.colors
    val density = LocalDensity.current
    var selected by remember { mutableIntStateOf(1) }
    var listWidth by remember { mutableStateOf(150.dp) }
    var dragging by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    val handleHalf = 6.dp
    fun fingerWidth(): Dp = with(density) { finger.position.x.toDp() }.minus(handleHalf).coerceIn(100.dp, 190.dp)
    DemoLoop(active, finalFrame = {
        selected = 1
        listWidth = 150.dp
        dragging = false
        finger.hide()
    }) {
        selected = 0
        listWidth = 112.dp
        dragging = false
        finger.hide()
        delay(1000)
        finger.show(targets.center("row1") + Offset(60f, 200f))
        finger.moveTo(targets.center("row1"))
        finger.tap()
        selected = 1
        delay(1200)
        finger.moveTo(targets.center("handle"))
        dragging = true
        finger.drag(targets.center("handle") + Offset(with(density) { 44.dp.toPx() }, 0f))
        listWidth = fingerWidth()
        dragging = false
        delay(400)
        finger.hide()
        delay(2000)
    }
    val width = if (dragging) fingerWidth() else listWidth
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(width).fillMaxHeight().background(c.layer01)) {
                Text(
                    "Sessions",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textMuted,
                    modifier = Modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                )
                SPLIT_SESSIONS.forEachIndexed { index, session ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(if (index == selected) c.layer03 else c.layer01)
                            .then(targets.target("row$index")),
                    ) {
                        DemoSessionRow(session.title, session.project, "")
                    }
                }
            }
            Box(
                Modifier.width(handleHalf * 2).fillMaxHeight().background(c.bgBase).then(targets.target("handle")),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxHeight().width(1.dp).background(c.borderBase))
                Box(Modifier.size(width = 4.dp, height = 32.dp).clip(RoundedCornerShape(2.dp)).background(c.iconMuted))
            }
            Crossfade(selected, Modifier.weight(1f).fillMaxHeight().background(c.bgBase)) { index ->
                val session = SPLIT_SESSIONS[index]
                Column(Modifier.fillMaxSize()) {
                    Text(
                        session.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textBase,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.md),
                    )
                    OcDivider()
                    Column(Modifier.padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
                        DemoUserBubble(session.prompt)
                        Text(session.reply, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                    }
                }
            }
        }
        finger.Draw()
    }
}
