package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TabletAndroid
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcModelName
import io.github.beppe2880.opencodemobile.core.ui.components.OcProviderInitial
import io.github.beppe2880.opencodemobile.core.ui.components.OcSectionHeader
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** A chat is long-pressed, Pin is chosen and it moves to a Pinned group above Today. */
@Composable
internal fun PinSessionsDemo(active: Boolean) {
    val c = OcTheme.colors
    var pinned by remember { mutableStateOf(true) }
    var menu by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        pinned = true
        menu = false
        finger.hide()
    }) {
        pinned = false
        menu = false
        delay(900)
        val row = targets.center("row")
        finger.show(row + Offset(80f, 120f))
        finger.moveTo(row)
        finger.longPress(700)
        menu = true
        delay(500)
        finger.moveTo(targets.center("pin"))
        finger.tap()
        menu = false
        delay(300)
        finger.hide()
        delay(2400)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().padding(top = OcSpacing.xs)) {
            AnimatedVisibility(pinned, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    OcSectionHeader("Pinned")
                    ASessionRow(ASessions[2], pinned = true)
                }
            }
            OcSectionHeader("Today")
            ASessions.take(3).forEachIndexed { index, session ->
                AnimatedVisibility(!(pinned && index == 2), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    ASessionRow(session, if (index == 2) targets.target("row") else Modifier)
                }
            }
        }
        AnimatedVisibility(
            menu,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut(),
            modifier = Modifier.offset {
                val row = targets.bounds("row")
                IntOffset(
                    ((row?.center?.x ?: 0f) - 40.dp.toPx()).roundToInt(),
                    ((row?.top ?: 0f) - 100.dp.toPx()).coerceAtLeast(0f).roundToInt(),
                )
            },
        ) {
            Column(
                Modifier
                    .width(140.dp)
                    .clip(RoundedCornerShape(OcRadius.md))
                    .background(c.bgBase)
                    .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
                    .padding(vertical = OcSpacing.xs),
            ) {
                AMenuItem("Pin", Icons.Filled.PushPin, Modifier.then(targets.target("pin")))
                AMenuItem("Rename", Icons.Outlined.Edit)
                AMenuItem("Delete", Icons.Outlined.Delete, tint = c.danger.fg)
            }
        }
        finger.Draw()
    }
}

@Composable
private fun AMenuItem(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = OcTheme.colors.textBase,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 32.dp).padding(horizontal = OcSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

/** Check for updates in Settings, then the banner under the search bar of the chat list. */
@Composable
internal fun CheckUpdatesDemo(active: Boolean) {
    val c = OcTheme.colors
    var stage by remember { mutableIntStateOf(1) }
    var checking by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        stage = 1
        checking = false
        banner = true
        finger.hide()
    }) {
        stage = 0
        checking = false
        banner = false
        delay(900)
        val row = targets.center("check")
        finger.show(row + Offset(60f, 130f))
        finger.moveTo(row)
        finger.tap()
        checking = true
        delay(1800)
        finger.hide()
        checking = false
        stage = 1
        delay(600)
        banner = true
        delay(3200)
    }
    Box(Modifier.fillMaxSize().background(c.bgBase).then(targets.root())) {
        Crossfade(stage, label = "tips-updates") { current ->
            if (current == 0) {
                Column(Modifier.fillMaxSize()) {
                    OcSectionHeader("Updates")
                    OcListRow(
                        title = "Check for updates",
                        subtitle = if (checking) "Checking for updates…" else null,
                        modifier = targets.target("check"),
                        trailing = if (checking) {
                            { OcSpinner() }
                        } else {
                            null
                        },
                    )
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(OcRadius.md))
                            .background(c.layer02)
                            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
                        Text("Search", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                    }
                    AnimatedVisibility(banner, enter = expandVertically() + fadeIn()) {
                        Row(
                            Modifier
                                .padding(top = OcSpacing.sm)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(OcRadius.lg))
                                .background(c.layer01)
                                .heightIn(min = 48.dp)
                                .padding(horizontal = OcSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                        ) {
                            Icon(Icons.Outlined.NewReleases, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(18.dp))
                            Text("Version 0.16.0 is available", style = MaterialTheme.typography.bodyMedium, color = c.textBase, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.Close, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    ASessions.take(3).forEach { ASessionRow(it, Modifier.padding(horizontal = 0.dp)) }
                }
            }
        }
        finger.Draw()
    }
}

/** Settings → Foldable and large screens: the section is opened, then the split view is switched on. */
@Composable
internal fun LargeScreensDemo(active: Boolean) {
    val c = OcTheme.colors
    var page by remember { mutableStateOf(true) }
    var split by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        page = true
        split = true
        finger.hide()
    }) {
        page = false
        split = false
        delay(900)
        val category = targets.center("category")
        finger.show(category + Offset(60f, 120f))
        finger.moveTo(category)
        finger.tap()
        page = true
        delay(1000)
        finger.moveTo(targets.center("split"))
        finger.tap()
        split = true
        delay(1000)
        finger.hide()
        delay(1800)
    }
    Box(Modifier.fillMaxSize().background(c.bgBase).then(targets.root())) {
        AnimatedContent(page, label = "tips-large-screens") { opened ->
            if (!opened) {
                Column(Modifier.fillMaxSize().padding(top = OcSpacing.xs)) {
                    AValueRow("Appearance", "")
                    AValueRow("Chat", "")
                    OcListRow(
                        title = "Foldable and large screens",
                        subtitle = "The list beside the chat, folding and unfolding, the divider and swiping",
                        modifier = targets.target("category"),
                        leading = { Icon(Icons.Outlined.TabletAndroid, null, tint = c.iconBase) },
                    )
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                        Text("Foldable and large screens", style = MaterialTheme.typography.titleSmall, color = c.textBase)
                    }
                    ASwitchRow("Chat beside the list", split, Modifier.then(targets.target("split")))
                    AValueRow("Unfolded layout", "Side by side")
                    OcSectionHeader("Folding and unfolding")
                    AValueRow("When folding", "Keep the open chat")
                }
            }
        }
        finger.Draw()
    }
}

private class AModel(val provider: String, val name: String)

private val AModels = listOf(AModel("Anthropic", "Claude Sonnet 4.5"), AModel("OpenAI", "GPT-5"), AModel("Google", "Gemini 2.5 Pro"))

/** With the switch off, the model picker stays open while models are tapped; Done closes it. */
@Composable
internal fun ModelPickerStayDemo(active: Boolean) {
    val c = OcTheme.colors
    var closeAfter by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(true) }
    var selected by remember { mutableIntStateOf(2) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        closeAfter = false
        open = true
        selected = 2
        finger.hide()
    }) {
        closeAfter = true
        open = false
        selected = 0
        delay(900)
        val switch = targets.center("switch")
        finger.show(switch + Offset(40f, 120f))
        finger.moveTo(switch)
        finger.tap()
        closeAfter = false
        delay(400)
        open = true
        delay(800)
        finger.moveTo(targets.center("model1"))
        finger.tap()
        selected = 1
        delay(600)
        finger.moveTo(targets.center("model2"))
        finger.tap()
        selected = 2
        delay(800)
        finger.moveTo(targets.center("done"))
        finger.tap()
        open = false
        delay(400)
        finger.hide()
        delay(1400)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize().padding(vertical = OcSpacing.xs)) {
            ASwitchRow("Close picker after choosing a model", closeAfter, Modifier.then(targets.target("switch")))
            AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(
                    Modifier
                        .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(OcRadius.lg))
                        .background(c.bgBase)
                        .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.lg))
                        .padding(vertical = OcSpacing.xs),
                ) {
                    AModels.forEachIndexed { index, model ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 30.dp)
                                .padding(horizontal = OcSpacing.md)
                                .then(if (index > 0) targets.target("model$index") else Modifier),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                        ) {
                            OcProviderInitial(model.provider)
                            Box(Modifier.weight(1f)) { OcModelName(model.name, null) }
                            if (selected == index) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().padding(horizontal = OcSpacing.md), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            "Done",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = c.textAccent,
                            modifier = Modifier.padding(vertical = OcSpacing.xs).then(targets.target("done")),
                        )
                    }
                }
            }
        }
        finger.Draw()
    }
}
