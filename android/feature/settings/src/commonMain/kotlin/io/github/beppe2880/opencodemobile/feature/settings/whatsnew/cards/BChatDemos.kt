package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Terminal
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcModelName
import io.github.beppe2880.opencodemobile.core.ui.components.OcProviderInitial
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoComposer
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay

private val Below = Offset(0f, 110f)

// ---- Show images and files read by tools inline ----

/** A tool reads a screenshot and a PDF: the image and the file show up under the tool row. */
@Composable
internal fun ToolMediaDemo(active: Boolean) {
    var step by remember { mutableIntStateOf(4) }
    DemoLoop(active, finalFrame = { step = 4 }) {
        step = 0
        delay(800)
        step = 1
        delay(1000)
        step = 2
        delay(1600)
        step = 3
        delay(900)
        step = 4
        delay(3000)
    }
    val c = OcTheme.colors
    Column(Modifier.fillMaxSize().padding(OcSpacing.sm)) {
        AnimatedVisibility(step >= 1, enter = fadeIn() + expandVertically()) {
            BToolRow(Icons.AutoMirrored.Outlined.Article, "Read", "design/login.png", Modifier.fillMaxWidth(), running = step < 2)
        }
        AnimatedVisibility(step >= 2, enter = fadeIn() + expandVertically()) {
            Box(
                Modifier
                    .padding(start = OcSpacing.xs, top = 2.dp, bottom = 4.dp)
                    .size(width = 132.dp, height = 68.dp)
                    .clip(RoundedCornerShape(OcRadius.lg))
                    .background(Brush.linearGradient(listOf(Color(0xFF3B6FD8), Color(0xFF7B4FD8))))
                    .border(1.dp, c.borderMuted, RoundedCornerShape(OcRadius.lg)),
            ) {
                Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(width = 56.dp, height = 8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.85f)))
                    Box(Modifier.size(width = 84.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.6f)))
                    Box(Modifier.size(width = 84.dp, height = 12.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.6f)))
                }
            }
        }
        AnimatedVisibility(step >= 3, enter = fadeIn() + expandVertically()) {
            BToolRow(Icons.AutoMirrored.Outlined.Article, "Read", "docs/spec.pdf", Modifier.fillMaxWidth(), running = step < 4)
        }
        AnimatedVisibility(step >= 4, enter = fadeIn() + expandVertically()) {
            val shape = RoundedCornerShape(OcRadius.lg)
            Row(
                Modifier
                    .padding(start = OcSpacing.xs, top = 2.dp)
                    .clip(shape)
                    .background(c.layer02)
                    .border(1.dp, c.borderMuted, shape)
                    .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(20.dp))
                Column {
                    Text("spec.pdf", style = MaterialTheme.typography.bodySmall, color = c.textBase)
                    Text("application/pdf · 214 KB", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                }
            }
        }
    }
}

// ---- /new to create a fresh session ----

/** Typing /new offers the command; tapping it opens a fresh session in the same directory. */
@Composable
internal fun NewCommandDemo(active: Boolean) {
    var typed by remember { mutableStateOf("") }
    var suggestion by remember { mutableStateOf(false) }
    var fresh by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            typed = "/new"
            suggestion = true
            fresh = false
            finger.hide()
        },
    ) {
        typed = ""
        suggestion = false
        fresh = false
        finger.hide()
        delay(900)
        val command = "/new"
        for (n in 1..command.length) {
            typed = command.take(n)
            suggestion = true
            delay(260)
        }
        delay(700)
        finger.show(targets.center("suggestion") + Below)
        finger.moveTo(targets.center("suggestion"))
        finger.tap()
        suggestion = false
        typed = ""
        fresh = true
        delay(2800)
        finger.hide()
    }
    val c = OcTheme.colors
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize()) {
            DemoTopBar(if (fresh) "New session" else "Fix login redirect", subtitle = "api-server")
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)) {
                AnimatedContent(fresh, transitionSpec = { fadeIn() togetherWith fadeOut() }) { isFresh ->
                    if (isFresh) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Outlined.AddComment, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(20.dp))
                            Text("/home/me/api-server", style = OcTheme.mono.codeSmall, color = c.textMuted, textAlign = TextAlign.Center)
                        }
                    } else {
                        DemoUserBubble("Fix the login redirect")
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    suggestion,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    OcCard(Modifier.fillMaxWidth().then(targets.target("suggestion")), shape = RoundedCornerShape(OcRadius.lg)) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                        ) {
                            Text("/new", style = OcTheme.mono.code, color = c.textBase)
                            Text(
                                "Start a new chat in this directory",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            DemoComposer(Modifier.padding(horizontal = OcSpacing.sm).padding(bottom = OcSpacing.sm), text = typed)
        }
        finger.Draw()
    }
}

// ---- Open GitHub links from tool rows ----

@Composable
private fun GitHubOpen(label: String, flash: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = c.success.fg,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(OcRadius.sm))
            .background(if (flash) c.success.bg else Color.Transparent)
            .padding(horizontal = OcSpacing.xs, vertical = OcSpacing.xs),
    )
}

/** Tool rows that touch GitHub get an Open action: tap opens the link, long press copies it. */
@Composable
internal fun GitHubOpenDemo(active: Boolean) {
    var rows by remember { mutableIntStateOf(3) }
    var flash by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf("") }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            rows = 3
            flash = ""
            toast = ""
            finger.hide()
        },
    ) {
        rows = 0
        flash = ""
        toast = ""
        finger.hide()
        delay(700)
        for (n in 1..3) {
            rows = n
            delay(450)
        }
        delay(500)
        finger.show(targets.center("openPr") + Below)
        finger.moveTo(targets.center("openPr"))
        finger.tap()
        flash = "pr"
        toast = "github.com/beppe2880/opencode-mobile/pull/156"
        delay(1500)
        flash = ""
        toast = ""
        delay(300)
        finger.moveTo(targets.center("openCommit"))
        finger.longPress()
        flash = "commit"
        toast = "Link copied"
        delay(1500)
        flash = ""
        toast = ""
        finger.hide()
        delay(800)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxWidth().padding(OcSpacing.sm), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            AnimatedVisibility(rows >= 1, enter = fadeIn() + expandVertically()) {
                BToolRow(
                    Icons.Outlined.Terminal,
                    "Shell",
                    "gh issue view 157",
                    Modifier.fillMaxWidth().heightIn(min = 40.dp),
                ) { GitHubOpen("Open #157", false) }
            }
            AnimatedVisibility(rows >= 2, enter = fadeIn() + expandVertically()) {
                BToolRow(
                    Icons.Outlined.Terminal,
                    "Shell",
                    "gh pr create",
                    Modifier.fillMaxWidth().heightIn(min = 40.dp),
                ) { GitHubOpen("Open PR #156", flash == "pr", targets.target("openPr")) }
            }
            AnimatedVisibility(rows >= 3, enter = fadeIn() + expandVertically()) {
                BToolRow(
                    Icons.Outlined.Terminal,
                    "Shell",
                    "git commit",
                    Modifier.fillMaxWidth().heightIn(min = 40.dp),
                ) { GitHubOpen("Open commit 4f2a9c1", flash == "commit", targets.target("openCommit")) }
            }
        }
        AnimatedVisibility(
            toast.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(OcSpacing.sm),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) { BToast(toast.ifEmpty { " " }) }
        finger.Draw()
    }
}

// ---- The model as provider mark, short name and variant ----

private class DemoModel(val full: String, val provider: String, val name: String, val variant: String?)

private val MODELS = listOf(
    DemoModel("Claude Haiku 5.5", "Anthropic", "Haiku 5.5", "Medium"),
    DemoModel("Claude Opus 5.5", "Anthropic", "Opus 5.5", "High"),
    DemoModel("GPT-5.2", "OpenAI", "GPT-5.2", "High"),
    DemoModel("Gemini 3 Pro", "Google", "Gemini 3 Pro", null),
)

@Composable
private fun DemoProviderMark(provider: String, size: Dp = 18.dp) {
    val tint = OcTheme.colors.iconBase
    if (provider == "Anthropic") {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            for (angle in listOf(0.0, 60.0, 120.0)) {
                val rad = angle * kotlin.math.PI / 180.0
                val dx = (r * 0.9f * kotlin.math.cos(rad)).toFloat()
                val dy = (r * 0.9f * kotlin.math.sin(rad)).toFloat()
                drawLine(tint, center - Offset(dx, dy), center + Offset(dx, dy), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    } else {
        OcProviderInitial(provider, size = size)
    }
}

/** The model button of the message box: provider mark, short name, variant in the muted color. */
@Composable
internal fun ModelButtonDemo(active: Boolean) {
    var index by remember { mutableIntStateOf(0) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = { index = 1; finger.hide() }) {
        index = 0
        finger.hide()
        delay(1200)
        finger.show(targets.center("model") + Below)
        for (next in 1..3) {
            finger.moveTo(targets.center("model"))
            finger.tap()
            index = next
            delay(1300)
        }
        finger.hide()
        delay(600)
    }
    val c = OcTheme.colors
    Box(Modifier.fillMaxSize().then(targets.root())) {
        DemoUserBubble("Summarize the last commit", Modifier.padding(OcSpacing.md))
        Column(Modifier.align(Alignment.BottomCenter).padding(OcSpacing.sm)) {
            Row(
                Modifier.padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                AnimatedContent(MODELS[index], transitionSpec = { fadeIn() togetherWith fadeOut() }) { model ->
                    val effort = model.variant?.let { " · effort ${it.lowercase()}" }.orEmpty()
                    Text(
                        "${model.provider} · ${model.full}$effort",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.textFaint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            DemoComposer(
                trailing = {
                    AnimatedContent(MODELS[index], transitionSpec = { fadeIn() togetherWith fadeOut() }) { model ->
                        Row(
                            Modifier.padding(end = OcSpacing.sm).then(targets.target("model")).heightIn(min = 32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                        ) {
                            DemoProviderMark(model.provider)
                            OcModelName(name = model.name, variant = model.variant, variantStyle = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
            )
        }
        finger.Draw()
    }
}
