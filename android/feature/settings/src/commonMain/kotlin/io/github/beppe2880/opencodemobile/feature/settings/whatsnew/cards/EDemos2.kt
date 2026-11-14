package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.markdown.OcMarkdown
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoAssistantText
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ---- Compaction summary ----

private const val COMPACTION_SUMMARY =
    "**Goal:** move the settings screen to Compose.\n\n" +
        "- Done: `SettingsViewModel`, state and events\n" +
        "- Next: write the tests, then the widget"

/** The "Context compacted" divider opens to show what the agent kept. */
@Composable
internal fun CompactionDemo(active: Boolean) {
    val c = OcTheme.colors
    val cache = rememberMarkdownCache()
    var expanded by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        expanded = true
        finger.hide()
    }) {
        expanded = false
        finger.hide()
        delay(1300)
        finger.show(targets.center("divider") + Offset(60f, 110f))
        finger.moveTo(targets.center("divider"))
        finger.tap()
        expanded = true
        delay(300)
        finger.hide()
        delay(3800)
    }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(250))
    Column(Modifier.fillMaxSize().then(targets.root()).padding(OcSpacing.md)) {
        DemoAssistantText("demo-compact", "Migrated the settings screen to Compose.", cache)
        Row(
            Modifier.fillMaxWidth().padding(vertical = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            HorizontalDivider(Modifier.weight(1f), color = c.borderMuted)
            Row(
                Modifier.then(targets.target("divider")).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Context compacted", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = c.iconMuted,
                    modifier = Modifier.size(18.dp).rotate(rotation),
                )
            }
            HorizontalDivider(Modifier.weight(1f), color = c.borderMuted)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OcRadius.md))
                    .background(c.layer01)
                    .border(1.dp, c.borderMuted, RoundedCornerShape(OcRadius.md))
                    .padding(OcSpacing.md),
            ) {
                OcMarkdown(id = "demo-compact-summary", content = COMPACTION_SUMMARY, cache = cache)
            }
        }
    }
    Box(Modifier.fillMaxSize()) { finger.Draw() }
}

// ---- Dictation ----

private val DICTATED = "Add a dark mode toggle to the settings screen".split(" ")

/** The microphone is tapped, the words appear in the message box as they are spoken. */
@Composable
internal fun DictationDemo(active: Boolean) {
    val c = OcTheme.colors
    var text by remember { mutableStateOf(DICTATED.joinToString(" ")) }
    var listening by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        text = DICTATED.joinToString(" ")
        listening = true
        finger.hide()
    }) {
        text = ""
        listening = false
        finger.hide()
        delay(800)
        finger.show(targets.center("mic") + Offset(-70f, -90f))
        finger.moveTo(targets.center("mic"))
        finger.tap()
        listening = true
        finger.hide()
        delay(500)
        for (word in DICTATED) {
            text = if (text.isEmpty()) word else "$text $word"
            delay(300)
        }
        delay(600)
        finger.show(targets.center("mic") + Offset(-70f, -90f))
        finger.moveTo(targets.center("mic"))
        finger.tap()
        listening = false
        finger.hide()
        delay(2400)
    }
    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "micPulse",
    )
    val micBackground by animateColorAsState(if (listening) c.bgAccent else Color.Transparent, tween(200))
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.md)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                ListeningPill(listening) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(c.layer02)
                            .border(1.dp, c.borderMuted, CircleShape)
                            .padding(horizontal = OcSpacing.md, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        OcPulsingDot(c.danger.fg)
                        Text("Listening", style = MaterialTheme.typography.labelMedium, color = c.textBase)
                        VoiceBars(c.bgAccent)
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OcRadius.lg))
                    .background(c.bgBase)
                    .border(1.dp, c.borderFocus, RoundedCornerShape(OcRadius.lg))
                    .padding(OcSpacing.md),
            ) {
                Text(
                    text.ifEmpty { "Ask anything…" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (text.isEmpty()) c.textFaint else c.textBase,
                    modifier = Modifier.heightIn(min = 44.dp).padding(vertical = OcSpacing.sm),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = c.iconBase)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(32.dp)
                            .scale(if (listening) pulse else 1f)
                            .clip(CircleShape)
                            .background(micBackground)
                            .then(targets.target("mic")),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null, tint = if (listening) c.onAccent else c.iconBase)
                    }
                    Spacer(Modifier.size(OcSpacing.md))
                    Box(Modifier.size(32.dp).clip(CircleShape).background(c.bgAccent), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        finger.Draw()
    }
}

@Composable
private fun VoiceBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "voice")
    Row(Modifier.height(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(380, 520, 440, 600).forEachIndexed { index, duration ->
            val h by transition.animateFloat(
                initialValue = 4f,
                targetValue = 14f,
                animationSpec = infiniteRepeatable(tween(duration, delayMillis = index * 60, easing = LinearEasing), RepeatMode.Reverse),
                label = "bar$index",
            )
            Box(Modifier.width(3.dp).height(h.dp).clip(CircleShape).background(color))
        }
    }
}

// ---- Usage statistics ----

private fun lerp(from: Float, to: Float, p: Float) = from + (to - from) * p

private fun money(value: Float): String {
    val cents = (value * 100).roundToInt()
    return "$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

private fun tokens(value: Float): String {
    val v = value.roundToInt()
    return if (v >= 1_000_000) "${v / 1_000_000}.${(v / 100_000) % 10}M" else "${v / 1000}K"
}

private val WEEK_BARS = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 1f, 0.8f)

/** Switching the period to 7 days: the totals count up and the activity chart fills. */
@Composable
internal fun UsageDemo(active: Boolean) {
    val c = OcTheme.colors
    var week by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        week = true
        finger.hide()
    }) {
        week = false
        finger.hide()
        delay(1400)
        finger.show(targets.center("week") + Offset(40f, 100f))
        finger.moveTo(targets.center("week"))
        finger.tap()
        week = true
        delay(300)
        finger.hide()
        delay(3600)
    }
    val p by animateFloatAsState(if (week) 1f else 0f, tween(900))
    Column(Modifier.fillMaxSize().then(targets.root()).background(c.bgBase)) {
        DemoTopBar("Usage")
        Column(Modifier.padding(horizontal = OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                OcChip("Today", onClick = {}, selected = !week)
                OcChip("7 days", onClick = {}, selected = week, modifier = Modifier.then(targets.target("week")))
                OcChip("30 days", onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                StatCell("Cost", money(lerp(0.64f, 4.82f, p)), Modifier.weight(1f))
                StatCell("Prompts", lerp(6f, 38f, p).roundToInt().toString(), Modifier.weight(1f))
                StatCell("Input", tokens(lerp(142_000f, 1_240_000f, p)), Modifier.weight(1f))
                StatCell("Output", tokens(lerp(21_000f, 184_000f, p)), Modifier.weight(1f))
            }
            Text("Steps per day", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
            Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                WEEK_BARS.forEachIndexed { index, bar ->
                    val height = if (index == WEEK_BARS.lastIndex) lerp(0.3f, bar, p) else bar * p
                    Box(
                        Modifier
                            .weight(1f)
                            .height((4f + 40f * height).dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(c.bgAccent),
                    )
                }
            }
            Text("Models", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
            ModelRow("claude-sonnet-5", "31 steps", 0.8f)
            ModelRow("gpt-5-mini", "12 steps", 0.35f)
        }
    }
    Box(Modifier.fillMaxSize()) { finger.Draw() }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier) {
    val c = OcTheme.colors
    Column(modifier.clip(RoundedCornerShape(OcRadius.md)).background(c.layer02).padding(horizontal = OcSpacing.sm, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textMuted, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleSmall, color = c.textBase, maxLines = 1)
    }
}

@Composable
private fun ModelRow(name: String, steps: String, share: Float) {
    val c = OcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row {
            Text(name, style = MaterialTheme.typography.bodySmall, color = c.textBase, modifier = Modifier.weight(1f))
            Text(steps, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.layer03)) {
            Box(Modifier.fillMaxWidth(share).height(4.dp).clip(CircleShape).background(c.bgAccent))
        }
    }
}

// ---- Notification kinds ----

/** Under Session notifications, two of the three kinds are switched off. */
@Composable
internal fun NotificationKindsDemo(active: Boolean) {
    val c = OcTheme.colors
    var permissions by remember { mutableStateOf(true) }
    var questions by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        permissions = true
        questions = false
        finished = false
        finger.hide()
    }) {
        permissions = true
        questions = true
        finished = true
        finger.hide()
        delay(1200)
        finger.show(targets.center("questions") + Offset(-30f, 100f))
        finger.moveTo(targets.center("questions"))
        finger.tap()
        questions = false
        delay(600)
        finger.moveTo(targets.center("finished"))
        finger.tap()
        finished = false
        delay(400)
        finger.hide()
        delay(2800)
    }
    Column(Modifier.fillMaxSize().then(targets.root()).background(c.bgBase).padding(top = OcSpacing.md)) {
        Text(
            "Session notifications",
            style = MaterialTheme.typography.labelLarge,
            color = c.textAccent,
            modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
        )
        NotificationRow("Permission requests", permissions, "permissions", targets)
        NotificationRow("Questions", questions, "questions", targets)
        NotificationRow("Finished sessions", finished, "finished", targets)
    }
    Box(Modifier.fillMaxSize()) { finger.Draw() }
}

@Composable
private fun NotificationRow(title: String, checked: Boolean, name: String, targets: DemoTargets) {
    val c = OcTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.then(targets.target(name)),
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent,
                checkedTrackColor = c.bgAccent,
                checkedBorderColor = c.bgAccent,
                uncheckedThumbColor = c.iconMuted,
                uncheckedTrackColor = c.layer02,
                uncheckedBorderColor = c.borderStrong,
            ),
        )
    }
}

@Composable
private fun ListeningPill(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) { content() }
}
