package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay

// ---- !shell commands ----

/** Typing `!` turns the message box into a shell prompt; the command runs and its output lands in the chat. */
@Composable
internal fun ShellDemo(active: Boolean) {
    val c = OcTheme.colors
    var text by remember { mutableStateOf("") }
    var stage by remember { mutableIntStateOf(3) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        text = ""
        stage = 3
        finger.hide()
    }) {
        text = ""
        stage = 0
        finger.hide()
        delay(600)
        finger.show(targets.center("input") + Offset(0f, 160f))
        finger.moveTo(targets.center("input"))
        finger.tap()
        fType("!git status -s") { text = it }
        delay(700)
        finger.moveTo(targets.center("send"))
        finger.tap()
        text = ""
        stage = 1
        finger.hide()
        delay(900)
        stage = 2
        delay(400)
        stage = 3
        delay(3000)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(
            Modifier.fillMaxSize().padding(OcSpacing.sm),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.fillMaxWidth()) {
                FVisible(stage >= 1) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(c.layer01, RoundedCornerShape(OcRadius.md))
                            .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
                            .padding(OcSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                            Text("$ git status -s", style = OcTheme.mono.code, color = c.textBase, modifier = Modifier.weight(1f, fill = false))
                            if (stage == 1) OcSpinner()
                        }
                        if (stage >= 2) {
                            Text(" M src/app.ts\n?? notes.md", style = OcTheme.mono.codeSmall, color = c.textMuted)
                        }
                        if (stage >= 3) Text("exit 0", style = OcTheme.mono.codeSmall, color = c.textFaint)
                    }
                }
            }
            FComposer(text, targets) {
                FVisible(text.startsWith("!")) {
                    Row(
                        Modifier.padding(bottom = OcSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                    ) {
                        Icon(Icons.Outlined.Terminal, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(14.dp))
                        Text("Runs in the shell in ~/my-api", style = MaterialTheme.typography.labelSmall, color = c.textAccent, maxLines = 1)
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Queued prompts ----

private val QUEUED = listOf("Add tests for the parser", "Rename the helper", "Update the README")

/** Three prompts wait for the agent: one is edited, one removed, one is sent right now. */
@Composable
internal fun QueuedDemo(active: Boolean) {
    val c = OcTheme.colors
    val shown = remember { mutableStateListOf(0, 1, 2) }
    var sent by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        shown.clear()
        shown.addAll(listOf(0, 1, 2))
        sent = false
        editing = false
        finger.hide()
    }) {
        shown.clear()
        sent = false
        editing = false
        finger.hide()
        delay(500)
        for (i in 0..2) {
            shown.add(i)
            delay(400)
        }
        delay(700)
        finger.show(targets.center("remove2") + Offset(0f, 150f))
        finger.moveTo(targets.center("edit1"))
        finger.tap()
        editing = true
        delay(1100)
        editing = false
        finger.moveTo(targets.center("remove2"))
        finger.tap()
        shown.remove(2)
        delay(700)
        finger.moveTo(targets.center("send0"))
        finger.tap()
        shown.remove(0)
        sent = true
        finger.hide()
        delay(3200)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(
            Modifier.fillMaxSize().padding(OcSpacing.md),
            verticalArrangement = Arrangement.Bottom,
        ) {
            FVisible(sent) { DemoUserBubble(QUEUED[0], Modifier.padding(bottom = OcSpacing.sm)) }
            Row(
                Modifier.padding(bottom = OcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                OcPulsingDot(c.agents.build)
                Text("Working…", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            OcCard(Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(OcRadius.lg)) {
                Row(
                    Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, top = OcSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                ) {
                    Icon(Icons.Outlined.Schedule, null, tint = c.iconMuted, modifier = Modifier.size(14.dp))
                    Text(
                        if (shown.size == 1) "1 prompt queued · runs when the agent finishes" else "${shown.size} prompts queued · run when the agent finishes",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                QUEUED.forEachIndexed { i, prompt ->
                    FVisible(i in shown) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (i == 1 && editing) c.layer02 else c.bgBase)
                                .padding(start = OcSpacing.md, end = OcSpacing.xxs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (i == 1 && editing) "Rename the helper to parseConfig▏" else prompt,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textBase,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            OcIconButton(Icons.AutoMirrored.Outlined.Send, null, {
                            }, iconSize = 18.dp, modifier = Modifier.size(36.dp).then(targets.target("send$i")))
                            OcIconButton(Icons.Outlined.Edit, null, {}, iconSize = 18.dp, modifier = Modifier.size(36.dp).then(targets.target("edit$i")))
                            OcIconButton(Icons.Outlined.Close, null, {}, iconSize = 18.dp, modifier = Modifier.size(36.dp).then(targets.target("remove$i")))
                        }
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Changed files and their diffs ----

private enum class Kind { Hunk, Context, Added, Removed }

private class DiffRow(val kind: Kind, val old: String, val new: String, val code: String)

private val DIFF = listOf(
    DiffRow(Kind.Hunk, "", "", "@@ -12,5 +12,8 @@"),
    DiffRow(Kind.Removed, "13", "", "  return cache.get(id)"),
    DiffRow(Kind.Added, "", "13", "  const hit = cache.get(id)"),
    DiffRow(Kind.Added, "", "14", "  if (hit) return hit"),
    DiffRow(Kind.Context, "14", "15", "  return load(id)"),
)

@Composable
private fun FDiffStats(additions: Int, deletions: Int) {
    val c = OcTheme.colors
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = c.success.fg)) { append("+$additions") }
            append(" ")
            withStyle(SpanStyle(color = c.danger.fg)) { append("−$deletions") }
        },
        style = OcTheme.mono.codeSmall,
        maxLines = 1,
    )
}

@Composable
private fun FFileHeader(path: String, additions: Int, deletions: Int, expanded: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.md)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, c.borderBase, shape)
            .background(c.layer01)
            .padding(horizontal = OcSpacing.sm)
            .padding(vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Text(path, style = OcTheme.mono.codeSmall, color = c.textBase, maxLines = 1, modifier = Modifier.weight(1f))
        FDiffStats(additions, deletions)
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
    }
}

/** The Changes sheet: a tap on a file opens its red/green diff. */
@Composable
internal fun ChangesDemo(active: Boolean) {
    val c = OcTheme.colors
    var expanded by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        expanded = true
        finger.hide()
    }) {
        expanded = false
        finger.hide()
        delay(900)
        finger.show(targets.center("file0") + Offset(120f, 160f))
        finger.moveTo(targets.center("file0"))
        finger.tap()
        expanded = true
        finger.hide()
        delay(3800)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Changes", style = MaterialTheme.typography.titleSmall, color = c.textBase, modifier = Modifier.weight(1f))
                FDiffStats(14, 3)
            }
            Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                FFileHeader("src/auth/session.ts", 8, 2, expanded, Modifier.then(targets.target("file0")))
                if (expanded) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(OcRadius.md)).border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))) {
                        DIFF.forEach { row ->
                            val background = when (row.kind) {
                                Kind.Added -> c.diff.addedBackground
                                Kind.Removed -> c.diff.removedBackground
                                Kind.Hunk -> c.diff.hunkBackground
                                Kind.Context -> c.diff.contextBackground
                            }
                            Row(Modifier.fillMaxWidth().background(background).padding(horizontal = OcSpacing.sm)) {
                                if (row.kind == Kind.Hunk) {
                                    Text(row.code, style = OcTheme.mono.codeSmall, color = c.diff.hunkText, maxLines = 1)
                                } else {
                                    Text(
                                        row.old,
                                        style = OcTheme.mono.codeSmall,
                                        color = c.diff.lineNumber,
                                        modifier = Modifier.padding(end = OcSpacing.xs).size(width = 18.dp, height = 16.dp),
                                    )
                                    Text(
                                        row.new,
                                        style = OcTheme.mono.codeSmall,
                                        color = c.diff.lineNumber,
                                        modifier = Modifier.padding(end = OcSpacing.sm).size(width = 18.dp, height = 16.dp),
                                    )
                                    val (marker, color) = when (row.kind) {
                                        Kind.Added -> "+" to c.diff.added
                                        Kind.Removed -> "−" to c.diff.removed
                                        else -> " " to c.diff.text
                                    }
                                    Text(marker, style = OcTheme.mono.codeSmall, color = color)
                                    Text(row.code, style = OcTheme.mono.codeSmall, color = c.diff.text, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                FFileHeader("src/auth/token.ts", 6, 1, false)
            }
        }
        finger.Draw()
    }
}
