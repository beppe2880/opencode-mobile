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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import kotlin.math.abs
import kotlin.math.sin

private const val SPOKEN = "Add a retry to the upload helper"
private const val BARS = 32

/** A speech-like level between 0 and 1, with pauses between words. */
private fun waveLevel(i: Int): Float {
    val word = (sin(i * 0.45) * 0.5 + 0.5).toFloat()
    val detail = abs(sin(i * 1.9 + 1.0)).toFloat()
    return (word * (0.25f + 0.75f * detail)).coerceIn(0.05f, 1f)
}

private fun clock(ms: Long): String = "0:" + (ms / 1000).toString().padStart(2, '0')

@Composable
private fun Waveform(levels: List<Float>, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(modifier.height(32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        levels.forEach { level ->
            Box(
                Modifier
                    .width(3.dp)
                    .height((3 + level * 28).dp)
                    .clip(CircleShape)
                    .background(if (level > 0f) c.textBase else c.textFaint),
            )
        }
    }
}

@Composable
private fun SendButton() {
    val c = OcTheme.colors
    Box(Modifier.size(32.dp).clip(CircleShape).background(c.bgAccent), contentAlignment = Alignment.Center) {
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
    }
}

/** The microphone is tapped: a live waveform and a timer, then the sentence lands as text. */
@Composable
internal fun VoiceRecordDemo(active: Boolean) {
    val c = OcTheme.colors
    var recording by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(SPOKEN) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val levels = remember { mutableStateListOf<Float>().apply { repeat(BARS) { add(0f) } } }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        recording = false
        text = SPOKEN
        finger.hide()
    }) {
        recording = false
        text = ""
        elapsed = 0
        for (i in levels.indices) levels[i] = 0f
        finger.hide()
        delay(900)
        finger.show(targets.center("mic") + Offset(-160f, -140f))
        finger.moveTo(targets.center("mic"))
        finger.tap()
        recording = true
        for (i in 0 until 44) {
            levels.removeAt(0)
            levels.add(waveLevel(i))
            elapsed = i * 80L
            delay(80)
        }
        finger.moveTo(targets.center("confirm"))
        finger.tap()
        recording = false
        text = SPOKEN
        finger.hide()
        delay(2400)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        DemoUserBubble("Fix the flaky upload test", Modifier.padding(OcSpacing.md))
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(OcSpacing.md)
                .fillMaxWidth()
                .clip(RoundedCornerShape(OcRadius.lg))
                .background(c.bgBase)
                .border(1.dp, c.borderFocus, RoundedCornerShape(OcRadius.lg))
                .padding(OcSpacing.md)
                .animateContentSize(),
        ) {
            if (recording) {
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                    Waveform(levels, Modifier.weight(1f).padding(horizontal = OcSpacing.sm))
                    Text(clock(elapsed), style = OcTheme.mono.codeSmall, color = c.textMuted, modifier = Modifier.padding(end = OcSpacing.sm))
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(c.bgAccent).then(targets.target("confirm")),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Text(
                    text.ifEmpty { "Ask anything…" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (text.isEmpty()) c.textFaint else c.textBase,
                    modifier = Modifier.padding(vertical = OcSpacing.sm),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = c.iconBase)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Outlined.Mic, contentDescription = null, tint = c.iconBase, modifier = targets.target("mic"))
                    Spacer(Modifier.size(OcSpacing.md))
                    SendButton()
                }
            }
        }
        finger.Draw()
    }
}

@Composable
private fun CloudRow(title: String, value: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
    }
}

/** Cloud is chosen with a provider and a model; a recording is then transcribed into the box. */
@Composable
internal fun VoiceCloudDemo(active: Boolean) {
    val c = OcTheme.colors
    var cloud by remember { mutableStateOf(true) }
    var phase by remember { mutableStateOf(2) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        cloud = true
        phase = 2
        finger.hide()
    }) {
        cloud = false
        phase = 0
        finger.hide()
        delay(900)
        finger.show(targets.center("cloud") + Offset(60f, 130f))
        finger.moveTo(targets.center("cloud"))
        finger.tap()
        cloud = true
        delay(1400)
        finger.moveTo(targets.center("mic"))
        finger.tap()
        phase = 1
        finger.hide()
        delay(1700)
        phase = 2
        delay(2600)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(
            Modifier
                .padding(OcSpacing.md)
                .fillMaxWidth()
                .clip(RoundedCornerShape(OcRadius.lg))
                .background(c.bgBase)
                .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.lg))
                .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
        ) {
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Speech to text", style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
                OcChip("Phone", onClick = {}, selected = !cloud)
                Spacer(Modifier.width(OcSpacing.xs))
                OcChip("Cloud", onClick = {}, selected = cloud, modifier = targets.target("cloud"))
            }
            AnimatedVisibility(cloud, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    CloudRow("Provider", "OpenAI")
                    CloudRow("Model", "gpt-4o-transcribe")
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(OcSpacing.md)
                .fillMaxWidth()
                .clip(RoundedCornerShape(OcRadius.lg))
                .background(c.bgBase)
                .border(1.dp, c.borderFocus, RoundedCornerShape(OcRadius.lg))
                .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm)
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (phase) {
                1 -> {
                    OcSpinner()
                    Spacer(Modifier.width(OcSpacing.sm))
                    Text("Transcribing…", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
                }
                2 -> Text(SPOKEN, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1, modifier = Modifier.weight(1f))
                else -> Text("Ask anything…", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.weight(1f))
            }
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = c.iconBase, modifier = targets.target("mic"))
        }
        finger.Draw()
    }
}
