package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

private class AImageSize(val label: String, val width: Dp, val height: Dp)

private val AImageSizes = listOf(
    AImageSize("Thumbnails", 56.dp, 56.dp),
    AImageSize("Small", 84.dp, 56.dp),
    AImageSize("Medium", 108.dp, 72.dp),
    AImageSize("Large", 136.dp, 90.dp),
)

/** The Attached images setting grows the pictures of a prompt from thumbnails to large. */
@Composable
internal fun AttachedImagesSizeDemo(active: Boolean) {
    var selected by remember { mutableIntStateOf(3) }
    val width by animateDpAsState(AImageSizes[selected].width, tween(350), label = "tips-attached-width")
    val height by animateDpAsState(AImageSizes[selected].height, tween(350), label = "tips-attached-height")
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        selected = 3
        finger.hide()
    }) {
        selected = 0
        delay(1000)
        val medium = targets.center("size2")
        finger.show(medium + Offset(0f, -140f))
        finger.moveTo(medium)
        finger.tap()
        selected = 2
        delay(1300)
        finger.moveTo(targets.center("size3"))
        finger.tap()
        selected = 3
        delay(2400)
        finger.hide()
        delay(300)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.md)) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    repeat(2) { index ->
                        APicture(index, Modifier.size(width, height).clip(RoundedCornerShape(OcRadius.md)), sun = 12.dp)
                    }
                }
                DemoUserBubble("Compare these two designs")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                AImageSizes.forEachIndexed { index, size ->
                    val modifier = if (index >= 2) targets.target("size$index") else Modifier
                    OcChip(size.label, onClick = {}, selected = selected == index, modifier = modifier)
                }
            }
        }
        finger.Draw()
    }
}

/** A prompt with images shows at once, dimmed with "Sending…", then "Queued", then normal. */
@Composable
internal fun PendingPromptDemo(active: Boolean) {
    val c = OcTheme.colors
    var sent by remember { mutableStateOf(true) }
    var phase by remember { mutableIntStateOf(2) }
    val dim by animateFloatAsState(if (phase in 1..2) 0.6f else 1f, tween(300), label = "tips-pending-dim")
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        sent = true
        phase = 2
        finger.hide()
    }) {
        sent = false
        phase = 0
        delay(1000)
        val send = targets.center("send")
        finger.show(send + Offset(-70f, -130f))
        finger.moveTo(send)
        finger.tap()
        sent = true
        phase = 1
        delay(1700)
        phase = 2
        delay(1300)
        phase = 3
        finger.hide()
        delay(2500)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                this@Column.AnimatedVisibility(sent, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut()) {
                    Column(
                        Modifier.fillMaxWidth().animateContentSize(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                    ) {
                        Column(
                            Modifier.alpha(dim),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                                repeat(2) { APicture(it, Modifier.size(64.dp, 44.dp).clip(RoundedCornerShape(OcRadius.md)), sun = 10.dp) }
                            }
                            DemoUserBubble("What changed between these?")
                        }
                        PendingLabel(phase)
                        AnimatedVisibility(phase == 3, enter = fadeIn()) {
                            Text(
                                "Comparing the two screenshots…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textMuted,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OcRadius.lg))
                    .background(c.bgBase)
                    .border(1.dp, c.borderFocus, RoundedCornerShape(OcRadius.lg))
                    .padding(OcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                if (!sent) {
                    Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        repeat(2) { APicture(it, Modifier.size(32.dp).clip(RoundedCornerShape(OcRadius.sm)), sun = 8.dp) }
                    }
                }
                Text(
                    if (sent) "Ask anything…" else "What changed between these?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sent) c.textFaint else c.textBase,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(c.bgAccent).then(targets.target("send")),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
        finger.Draw()
    }
}

@Composable
private fun PendingLabel(phase: Int) {
    val c = OcTheme.colors
    AnimatedVisibility(phase in 1..2, enter = fadeIn(), exit = fadeOut()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            if (phase == 1) {
                OcSpinner(size = 12.dp, color = c.textFaint)
            } else {
                Icon(Icons.Outlined.Schedule, null, tint = c.textFaint, modifier = Modifier.size(12.dp))
            }
            Text(if (phase == 1) "Sending…" else "Queued", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
        }
    }
}
