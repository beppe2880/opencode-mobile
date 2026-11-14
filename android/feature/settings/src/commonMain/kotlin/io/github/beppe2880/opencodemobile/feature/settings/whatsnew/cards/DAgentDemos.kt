package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.components.OcSliderRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ---- MCP status and connection controls ----

private class McpRow(val name: String, val status: Int)

/** A switch connects an MCP server: Disconnected, Connecting, Connected. */
@Composable
internal fun McpDemo(active: Boolean) {
    val c = OcTheme.colors
    var state by remember { mutableIntStateOf(2) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        state = 2
        finger.hide()
    }) {
        state = 0
        finger.hide()
        delay(1000)
        finger.show(targets.center("toggle") + Offset(-120f, 220f))
        finger.moveTo(targets.center("toggle"))
        finger.tap()
        state = 1
        delay(1200)
        state = 2
        delay(500)
        finger.hide()
        delay(2600)
    }
    val rows = listOf(McpRow("github", 2), McpRow("filesystem", state), McpRow("sentry", 3))
    Column(Modifier.fillMaxSize().then(targets.root()).background(c.bgBase).padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MCP servers", style = MaterialTheme.typography.titleSmall, color = c.textBase, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.padding(top = OcSpacing.sm), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            rows.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                        Text(
                            when (row.status) {
                                2 -> "Connected"
                                1 -> "Connecting"
                                3 -> "Authentication required"
                                else -> "Disconnected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                        )
                    }
                    if (row.status == 1) OcSpinner()
                    Box(Modifier.then(if (row.name == "filesystem") targets.target("toggle") else Modifier)) {
                        Switch(
                            checked = row.status == 1 || row.status == 2,
                            onCheckedChange = null,
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
            }
        }
    }
    Box(Modifier.fillMaxSize()) { finger.Draw() }
}

// ---- Follow the agent's work live ----

private val SHELL_OUTPUT = listOf(
    "> Task :app:testDebugUnitTest",
    "AuthRedirectTest > keepsQuery FAILED",
    "  expected /home?tab=1 but was /home",
    "1 test failed",
)

/** The step in progress stays open: command output as it arrives, then the diff, then it folds. */
@Composable
internal fun FollowDemo(active: Boolean) {
    val c = OcTheme.colors
    var step by remember { mutableIntStateOf(1) }
    var lines by remember { mutableIntStateOf(SHELL_OUTPUT.size) }
    DemoLoop(active, finalFrame = {
        step = 1
        lines = SHELL_OUTPUT.size
    }) {
        step = 0
        lines = 0
        delay(900)
        step = 1
        repeat(SHELL_OUTPUT.size) {
            delay(650)
            lines += 1
        }
        delay(900)
        step = 2
        delay(2000)
        step = 3
        delay(900)
        step = 4
        delay(2800)
    }
    val open = step == 1 || step == 2
    val chevron by animateFloatAsState(if (open) 180f else 0f)
    Column(Modifier.fillMaxSize().padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
        DemoUserBubble("Run the tests and fix what fails")
        AnimatedVisibility(step >= 1) {
            Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    if (open) {
                        OcPulsingDot(c.info.fg)
                    } else {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = c.success.fg, modifier = Modifier.size(14.dp))
                    }
                    Text(
                        if (open) "Using Shell, Edit" else "Used Shell, Edit",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textMuted,
                    )
                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp).rotate(chevron))
                }
                AnimatedVisibility(open) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.lg))
                            .padding(OcSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                    ) {
                        ToolHeader(Icons.Outlined.Terminal, "Shell", "./gradlew test")
                        AnimatedVisibility(step == 1) {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(OcRadius.sm)).background(c.layer02).padding(OcSpacing.sm),
                            ) {
                                SHELL_OUTPUT.take(lines).forEachIndexed { index, line ->
                                    Text(
                                        line,
                                        style = OcTheme.mono.codeSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                                        color = if (index == 1 || index == 3) c.danger.fg else c.textMuted,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        AnimatedVisibility(step == 2) {
                            Column {
                                ToolHeader(Icons.Outlined.Edit, "Edit", "AuthRedirect.kt")
                                Column(
                                    Modifier.padding(top = OcSpacing.xs).fillMaxWidth().clip(RoundedCornerShape(OcRadius.sm)).background(c.layer02),
                                ) {
                                    DiffLine("- return Redirect(path)", c.diff.removed, c.diff.removedBackground)
                                    DiffLine("+ return Redirect(path + query)", c.diff.added, c.diff.addedBackground)
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(step >= 4) {
            Text("Fixed: the redirect keeps the query string and the tests pass.", style = MaterialTheme.typography.bodyMedium, color = c.textBase)
        }
    }
}

@Composable
private fun ToolHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, detail: String) {
    val c = OcTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        Icon(icon, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(14.dp))
        Text(name, style = MaterialTheme.typography.labelMedium, color = c.textBase)
        Text(detail, style = OcTheme.mono.codeSmall.copy(fontSize = 11.sp), color = c.textMuted, maxLines = 1)
    }
}

@Composable
private fun DiffLine(text: String, color: androidx.compose.ui.graphics.Color, background: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = OcTheme.mono.codeSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
        color = color,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth().background(background).padding(horizontal = OcSpacing.sm, vertical = 1.dp),
    )
}

// ---- Settings for tool output, refresh, text size and reset ----

private val REFRESH_LABELS = listOf("0.3 s", "0.7 s", "1.5 s", "3 s")
private const val LINES_MIN = 4f
private const val LINES_MAX = 40f
private const val DEFAULT_REFRESH = 2
private const val DEFAULT_LINES = 8f

/** Pick a refresh interval, drag the output lines, then reset everything to the defaults. */
@Composable
internal fun OutputSettingsDemo(active: Boolean) {
    val c = OcTheme.colors
    val density = LocalDensity.current
    var refresh by remember { mutableIntStateOf(3) }
    var lines by remember { mutableFloatStateOf(28f) }
    var dragging by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    fun fingerLines(): Float {
        val row = targets.bounds("slider") ?: return lines
        return (LINES_MIN + (LINES_MAX - LINES_MIN) * sliderFraction(row, finger.position.x, density)).roundToInt().toFloat()
    }
    DemoLoop(active, finalFrame = {
        refresh = 3
        lines = 28f
        dragging = false
        finger.hide()
    }) {
        refresh = DEFAULT_REFRESH
        lines = DEFAULT_LINES
        dragging = false
        finger.hide()
        delay(900)
        val control = targets.bounds("refresh")
        val slider = targets.bounds("slider")
        if (control != null && slider != null) {
            finger.show(Offset(control.right - control.width / 8f, control.bottom + with(density) { 60.dp.toPx() }))
            finger.moveTo(Offset(control.right - control.width / 8f, control.center.y))
            finger.tap()
            refresh = 3
            delay(300)
            val from = (DEFAULT_LINES - LINES_MIN) / (LINES_MAX - LINES_MIN)
            finger.moveTo(sliderThumb(slider, from, density))
            dragging = true
            finger.drag(sliderThumb(slider, 0.65f, density), durationMillis = 1000)
            lines = fingerLines()
            dragging = false
            delay(900)
            finger.moveTo(targets.center("reset"))
            finger.tap()
            repeat(8) { step ->
                lines -= (lines - DEFAULT_LINES) / (8 - step)
                delay(30)
            }
            refresh = DEFAULT_REFRESH
            delay(300)
        }
        finger.hide()
        delay(1400)
    }
    val shown = if (dragging) fingerLines() else lines
    Column(Modifier.fillMaxSize().then(targets.root())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            Text("Output refresh", style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
            Box(Modifier.width(190.dp).then(targets.target("refresh"))) {
                OcSegmentedControl(
                    segments = REFRESH_LABELS.mapIndexed { index, label -> OcSegment(index, label, "demo_refresh_$index") },
                    selected = refresh,
                    onSelect = {},
                )
            }
        }
        OcDivider()
        Box(Modifier.then(targets.target("slider"))) {
            OcSliderRow(
                title = "Output lines",
                value = shown,
                onValueChange = {},
                valueRange = LINES_MIN..LINES_MAX,
                valueLabel = shown.roundToInt().toString(),
            )
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.then(targets.target("reset"))) {
                OcButton(
                    "Reset all settings",
                    onClick = {},
                    variant = OcButtonVariant.Ghost,
                    leadingIcon = Icons.Outlined.RestartAlt,
                    compact = true,
                )
            }
        }
    }
    Box(Modifier.fillMaxSize()) { finger.Draw() }
}
