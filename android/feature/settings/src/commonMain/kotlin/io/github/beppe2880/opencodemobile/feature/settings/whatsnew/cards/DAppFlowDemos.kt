package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcLogo
import io.github.beppe2880.opencodemobile.core.ui.components.OcSliderRow
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoComposer
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoSessionRow
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ---- Share from other apps ----

private val SHARE_TARGETS = listOf("Messages", "Mail", "OpenCode", "Notes")

/** A page is shared from another app, OpenCode is picked and a new session starts with the link. */
@Composable
internal fun ShareDemo(active: Boolean) {
    val c = OcTheme.colors
    var phase by remember { mutableIntStateOf(2) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        phase = 2
        finger.hide()
    }) {
        phase = 0
        finger.hide()
        delay(1000)
        finger.show(targets.center("share") + Offset(-160f, 240f))
        finger.moveTo(targets.center("share"))
        finger.tap()
        phase = 1
        delay(800)
        finger.moveTo(targets.center("opencode"))
        finger.tap()
        phase = 2
        finger.hide()
        delay(3200)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Crossfade(phase, Modifier.fillMaxSize()) { shown ->
            if (shown == 2) {
                Column(Modifier.fillMaxSize()) {
                    DemoTopBar("New session", subtitle = "Shared with OpenCode")
                    Box(Modifier.weight(1f))
                    DemoComposer(
                        Modifier.padding(OcSpacing.md),
                        text = "https://opencode.ai/docs/share",
                        attachments = {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(OcRadius.md)).background(DEMO_PICTURE))
                        },
                    )
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                            Text(
                                "opencode.ai/docs/share",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.textMuted,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(OcRadius.md))
                                    .background(c.layer02)
                                    .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                            )
                            Box(Modifier.size(32.dp).then(targets.target("share")), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Share, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(OcRadius.md)).background(DEMO_LANDSCAPE))
                        Box(Modifier.fillMaxWidth(0.9f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.layer03))
                        Box(Modifier.fillMaxWidth(0.6f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.layer03))
                    }
                    AnimatedVisibility(
                        shown == 1,
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
                                .padding(OcSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                        ) {
                            Text("Share", style = MaterialTheme.typography.labelLarge, color = c.textBase)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                SHARE_TARGETS.forEach { name ->
                                    Column(
                                        Modifier.then(if (name == "OpenCode") targets.target("opencode") else Modifier),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(OcSpacing.xxs),
                                    ) {
                                        Box(
                                            Modifier.size(40.dp).clip(CircleShape).background(c.bgBase).border(1.dp, c.borderBase, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (name == "OpenCode") {
                                                OcLogo(size = 24.dp)
                                            } else {
                                                Text(name.take(1), style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                                            }
                                        }
                                        Text(name, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        finger.Draw()
    }
}

// ---- Settings with a live preview ----

private const val TEXT_SIZE_MIN = 0.8f
private const val TEXT_SIZE_MAX = 1.4f

/** The text size slider is dragged and the text of a preview of the chat grows with it. */
@Composable
internal fun SettingsPreviewDemo(active: Boolean) {
    val c = OcTheme.colors
    val density = LocalDensity.current
    var size by remember { mutableFloatStateOf(1.3f) }
    var dragging by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    fun fingerSize(): Float {
        val row = targets.bounds("slider") ?: return size
        return TEXT_SIZE_MIN + (TEXT_SIZE_MAX - TEXT_SIZE_MIN) * sliderFraction(row, finger.position.x, density)
    }
    DemoLoop(active, finalFrame = {
        size = 1.3f
        dragging = false
        finger.hide()
    }) {
        size = 1f
        dragging = false
        finger.hide()
        delay(1000)
        val row = targets.bounds("slider")
        if (row != null) {
            val from = (1f - TEXT_SIZE_MIN) / (TEXT_SIZE_MAX - TEXT_SIZE_MIN)
            finger.show(sliderThumb(row, from, density) + Offset(0f, 120f))
            finger.moveTo(sliderThumb(row, from, density))
            dragging = true
            finger.drag(sliderThumb(row, 0.85f, density), durationMillis = 1200)
            size = fingerSize()
            dragging = false
        }
        delay(300)
        finger.hide()
        delay(2600)
    }
    val shown = if (dragging) fingerSize() else size
    Column(Modifier.fillMaxSize().then(targets.root())) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = OcSpacing.md)
                .padding(top = OcSpacing.md)
                .clip(RoundedCornerShape(OcRadius.lg))
                .background(c.bgBase)
                .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.lg))
                .height(84.dp)
                .padding(OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            Text(
                "Add a dark mode toggle",
                fontSize = (14f * shown).sp,
                lineHeight = (19f * shown).sp,
                color = c.userBubbleText,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.clip(RoundedCornerShape(OcRadius.md)).background(c.userBubbleBg).padding(horizontal = OcSpacing.sm),
            )
            Text(
                "Done. The toggle is in Settings.",
                fontSize = (14f * shown).sp,
                lineHeight = (19f * shown).sp,
                color = c.textBase,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        Box(Modifier.then(targets.target("slider"))) {
            OcSliderRow(
                title = "Chat text size",
                value = shown,
                onValueChange = {},
                valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
                valueLabel = "${(shown * 100).roundToInt()}%",
            )
        }
    }
    Box(Modifier.fillMaxSize()) { finger.Draw() }
}

// ---- Update sheet ----

private val UPDATE_NOTES = listOf("Math formulas in answers", "Thumbnails for attachments", "Scrolling stays put while streaming")

/** An update sheet; "View changelog" lists what is new before the update. */
@Composable
internal fun UpdateDemo(active: Boolean) {
    val c = OcTheme.colors
    var sheet by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        sheet = true
        notes = true
        finger.hide()
    }) {
        sheet = false
        notes = false
        finger.hide()
        delay(700)
        sheet = true
        delay(1300)
        finger.show(targets.center("changelog") + Offset(60f, 160f))
        finger.moveTo(targets.center("changelog"))
        finger.tap()
        notes = true
        finger.hide()
        delay(3200)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().alpha(0.5f)) {
            Text("Sessions", style = MaterialTheme.typography.titleSmall, color = c.textBase, modifier = Modifier.padding(OcSpacing.lg))
            DemoSessionRow("Explain this repository", "dotfiles", "2h")
            DemoSessionRow("Add a dark mode toggle", "web-app", "5h")
        }
        Box(Modifier.fillMaxSize().background(c.scrim.copy(alpha = if (sheet) 0.4f else 0f)))
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
                    .animateContentSize()
                    .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Icon(Icons.Outlined.NewReleases, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(20.dp))
                    Text("Update available", style = MaterialTheme.typography.titleSmall, color = c.textBase)
                }
                Text("0.15.0 → 0.16.0", style = OcTheme.mono.codeSmall, color = c.textMuted, modifier = Modifier.padding(top = OcSpacing.xxs))
                AnimatedVisibility(notes) {
                    Column(Modifier.padding(top = OcSpacing.sm), verticalArrangement = Arrangement.spacedBy(OcSpacing.xxs)) {
                        Text("In this update", style = MaterialTheme.typography.labelMedium, color = c.textBase)
                        UPDATE_NOTES.forEach { note ->
                            Text("•  $note", style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1)
                        }
                    }
                }
                Row(Modifier.padding(top = OcSpacing.md), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    OcButton("Update", onClick = {}, compact = true, modifier = Modifier.weight(1f))
                    OcButton(
                        if (notes) "Hide changelog" else "View changelog",
                        onClick = {},
                        variant = OcButtonVariant.Secondary,
                        compact = true,
                        modifier = Modifier.weight(1f).then(targets.target("changelog")),
                    )
                }
            }
        }
        finger.Draw()
    }
}
