package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay

// ---- Agent and model changes apply with the next message ----

private val MODELS = listOf("Sonnet 4.5", "Opus 4.1", "Haiku 4.5")

@Composable
private fun WorkingLine(label: String) {
    val c = OcTheme.colors
    Row(
        Modifier.padding(horizontal = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        OcSpinner(size = 12.dp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}

/** The model is changed while a reply runs; the change shows as a note and applies to the next prompt. */
@Composable
internal fun NextMessageDemo(active: Boolean) {
    val c = OcTheme.colors
    var model by remember { mutableStateOf(MODELS[1]) }
    var menu by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(true) }
    var second by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        model = MODELS[1]
        menu = false
        note = true
        second = false
        finger.hide()
    }) {
        model = MODELS[0]
        menu = false
        note = false
        second = false
        finger.hide()
        delay(1100)
        finger.show(targets.center("model") + Offset(100f, 120f))
        finger.moveTo(targets.center("model"))
        finger.tap()
        menu = true
        delay(700)
        finger.moveTo(targets.center("opus"))
        finger.tap()
        model = MODELS[1]
        menu = false
        note = true
        finger.hide()
        delay(2300)
        note = false
        second = true
        delay(2500)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).padding(OcSpacing.md)) {
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            AnimatedVisibility(!second, enter = fadeIn(), exit = fadeOut() + shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    DemoUserBubble("Refactor the parser")
                    WorkingLine("Sonnet 4.5 · Working…")
                }
            }
            AnimatedVisibility(second, enter = fadeIn() + expandVertically(), exit = fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    DemoUserBubble("Now add tests")
                    WorkingLine("$model · Working…")
                }
            }
        }
        Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            AnimatedVisibility(note, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = c.info.fg, modifier = Modifier.size(14.dp))
                    Text("Applies to your next message", style = MaterialTheme.typography.labelMedium, color = c.info.fg)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                OcChip("build", onClick = {})
                OcChip(model, onClick = {}, trailingIcon = Icons.Outlined.ExpandMore, modifier = targets.target("model"))
            }
        }
        AnimatedVisibility(
            menu,
            modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 44.dp, start = 64.dp),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(OcRadius.md))
                    .background(c.layer02)
                    .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
                    .padding(vertical = OcSpacing.xs),
            ) {
                MODELS.forEach { name ->
                    val mod = if (name == MODELS[1]) targets.target("opus") else Modifier
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textBase,
                        modifier = mod.width(120.dp).padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                    )
                }
            }
        }
        finger.Draw()
    }
}

// ---- Consecutive thoughts grouped into one row ----

private val THOUGHTS = listOf(
    "Checking how the parser handles nesting",
    "Looking at the existing tests",
    "Planning the smallest fix",
)

@Composable
private fun DisclosureLine(text: String, expanded: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        Icon(
            if (expanded) Icons.Outlined.ExpandMore else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = c.iconMuted,
            modifier = Modifier.size(16.dp),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1)
    }
}

/** The setting is switched on: three thought rows merge into one that opens into the thoughts. */
@Composable
internal fun ThoughtsGroupDemo(active: Boolean) {
    val c = OcTheme.colors
    var grouped by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        grouped = true
        expanded = false
        finger.hide()
    }) {
        grouped = false
        expanded = false
        finger.hide()
        delay(1500)
        finger.show(targets.center("switch") + Offset(-120f, 120f))
        finger.moveTo(targets.center("switch"))
        finger.tap()
        grouped = true
        delay(1300)
        finger.moveTo(targets.center("group"))
        finger.tap()
        expanded = true
        finger.hide()
        delay(3200)
    }
    Column(Modifier.fillMaxSize().then(targets.root()).padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Group consecutive thoughts", style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
            Switch(checked = grouped, onCheckedChange = null, modifier = Modifier.height(28.dp).then(targets.target("switch")))
        }
        Column(Modifier.fillMaxWidth().animateContentSize()) {
            AnimatedVisibility(!grouped, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    THOUGHTS.forEach { _ -> DisclosureLine("Thought for 2s", expanded = false) }
                }
            }
            AnimatedVisibility(grouped, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    DisclosureLine("Thought 3 times · 6s", expanded, Modifier.then(targets.target("group")))
                    AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Column(Modifier.padding(start = OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            THOUGHTS.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Found it: the nested list resets the indent.", style = MaterialTheme.typography.bodyMedium, color = c.textBase)
    }
}
