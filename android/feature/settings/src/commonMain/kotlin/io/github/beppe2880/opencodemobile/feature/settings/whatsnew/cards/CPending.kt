package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay

// ---- Question and permission cards collapse to a bar ----

@Composable
private fun PillButton(label: String, filled: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Box(
        modifier
            .height(32.dp)
            .clip(RoundedCornerShape(OcRadius.md))
            .background(if (filled) c.bgAccent else c.layer02)
            .padding(horizontal = OcSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (filled) c.onAccent else c.textBase)
    }
}

/** A permission card is folded with its arrow so the chat can be read, then opened from the bar. */
@Composable
internal fun PendingBarDemo(active: Boolean) {
    val c = OcTheme.colors
    var collapsed by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        collapsed = true
        finger.hide()
    }) {
        collapsed = false
        finger.hide()
        delay(1500)
        finger.show(targets.center("collapse") + Offset(-100f, -110f))
        finger.moveTo(targets.center("collapse"))
        finger.tap()
        collapsed = true
        delay(2200)
        finger.moveTo(targets.center("bar"))
        finger.tap()
        collapsed = false
        finger.hide()
        delay(1800)
    }
    Column(Modifier.fillMaxSize().then(targets.root()).padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        Column(Modifier.weight(1f).fillMaxWidth().clipToBounds(), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            DemoUserBubble("Run the tests and fix what fails")
            Text(
                "I'll run the suite first. Two tests in upload fail because the retry count is never reset between calls, so I'll patch the helper and run them again.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textBase,
            )
        }
        AnimatedVisibility(!collapsed, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OcRadius.lg))
                    .background(c.bgBase)
                    .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.lg))
                    .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
            ) {
                Box(Modifier.padding(top = 2.dp).size(width = 32.dp, height = 4.dp).background(c.borderStrong, CircleShape).align(Alignment.CenterHorizontally))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.warning.fg, modifier = Modifier.size(18.dp))
                    Text("Permission required", style = MaterialTheme.typography.titleSmall, color = c.textBase, modifier = Modifier.weight(1f))
                    Box(Modifier.size(32.dp).then(targets.target("collapse")), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.iconMuted)
                    }
                }
                Text(
                    "bun test upload",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = c.textCodePath,
                    modifier = Modifier.clip(RoundedCornerShape(OcRadius.xs)).background(c.bgCodePath).padding(horizontal = OcSpacing.sm, vertical = 2.dp),
                )
                Row(Modifier.padding(vertical = OcSpacing.sm), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    PillButton("Reject", filled = false)
                    PillButton("Always", filled = false)
                    PillButton("Allow once", filled = true)
                }
            }
        }
        AnimatedVisibility(collapsed, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(OcRadius.lg))
                    .background(c.bgBase)
                    .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.lg))
                    .padding(start = OcSpacing.md, end = OcSpacing.sm)
                    .then(targets.target("bar")),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.warning.fg, modifier = Modifier.size(18.dp))
                Text(
                    "Permission required · bun test upload",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textBase,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.ExpandLess, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ---- Running subagents and background commands ----

private fun elapsedLabel(seconds: Int): String = "Elapsed: " + (seconds / 60) + ":" + (seconds % 60).toString().padStart(2, '0')

@Composable
private fun SheetHeader(title: String) {
    val c = OcTheme.colors
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.weight(1f))
        Text("Stop all", style = MaterialTheme.typography.labelLarge, color = c.textAccent)
    }
}

/** The running chip opens a sheet with a subagent and a command, their timers ticking. */
@Composable
internal fun RunningWorkDemo(active: Boolean) {
    val c = OcTheme.colors
    var sheet by remember { mutableStateOf(true) }
    var seconds by remember { mutableIntStateOf(12) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        sheet = true
        seconds = 12
        finger.hide()
    }) {
        sheet = false
        seconds = 12
        finger.hide()
        delay(1300)
        finger.show(targets.center("chip") + Offset(120f, -110f))
        finger.moveTo(targets.center("chip"))
        finger.tap()
        sheet = true
        finger.hide()
        repeat(5) {
            delay(1000)
            seconds++
        }
        delay(600)
        sheet = false
        delay(600)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            DemoUserBubble("Start the dev server and explore the API routes")
            Text("Working on it in the background…", style = MaterialTheme.typography.bodyMedium, color = c.textBase)
        }
        Row(Modifier.align(Alignment.BottomStart).padding(OcSpacing.md)) {
            OcChip("1 agent · 1 shell", onClick = {}, dotColor = c.success.fg, modifier = targets.target("chip"))
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
                    .background(c.layer02)
                    .border(1.dp, c.borderBase, RoundedCornerShape(topStart = OcRadius.sheet, topEnd = OcRadius.sheet))
                    .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
            ) {
                SheetHeader("Subagents")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Explore API routes", style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                        Text("explore · " + elapsedLabel(seconds), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                    Text("Stop", style = MaterialTheme.typography.labelLarge, color = c.textAccent)
                }
                Spacer(Modifier.height(OcSpacing.xs))
                SheetHeader("Commands")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("bun run dev", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = c.textBase)
                        Text(elapsedLabel(seconds + 22), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        Text(
                            "ready on http://localhost:3000",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = c.textMuted,
                        )
                    }
                    Text("Stop", style = MaterialTheme.typography.labelLarge, color = c.textAccent)
                }
            }
        }
        finger.Draw()
    }
}
