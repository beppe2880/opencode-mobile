package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoComposer
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay

private val Below = Offset(0f, 110f)

// ---- Open and Stop from the running chips popover ----

/** The chips open one popover: Open goes to the subagent's session, Stop ends it. */
@Composable
internal fun RunningPopoverDemo(active: Boolean) {
    var open by remember { mutableStateOf(true) }
    var agent by remember { mutableStateOf(true) }
    var shell by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            open = true
            agent = true
            shell = true
            toast = false
            finger.hide()
        },
    ) {
        open = false
        agent = true
        shell = true
        toast = false
        finger.hide()
        delay(900)
        finger.show(targets.center("chipAgent") + Below)
        finger.moveTo(targets.center("chipAgent"))
        finger.tap()
        open = true
        delay(900)
        finger.moveTo(targets.center("openAgent"))
        finger.tap()
        toast = true
        delay(1300)
        toast = false
        delay(300)
        finger.moveTo(targets.center("stopAgent"))
        finger.tap()
        agent = false
        delay(1400)
        finger.hide()
        delay(1000)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.align(Alignment.BottomStart).padding(OcSpacing.sm)) {
            AnimatedVisibility(open, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    PopoverCard {
                        AnimatedVisibility(agent, enter = expandVertically(), exit = shrinkVertically() + fadeOut()) {
                            Column {
                                PopoverHeading("Agents")
                                PopoverRow("Explore the auth module", "0:42", mono = false, openModifier = targets.target("openAgent"))
                                if (shell) OcDivider(Modifier.padding(vertical = OcSpacing.xs))
                            }
                        }
                        if (shell) {
                            PopoverHeading("Shells")
                            PopoverRow("npm run dev", "1:05", mono = true, openModifier = Modifier)
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm, Alignment.End),
                        ) {
                            if (agent) BStopButton("Stop agent", targets.target("stopAgent"))
                            if (shell) BStopButton("Stop shell")
                        }
                    }
                    Spacer(Modifier.height(OcSpacing.xs))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                AnimatedVisibility(agent) { BChip("1 agent", targets.target("chipAgent")) }
                if (shell) BChip("1 shell")
            }
        }
        AnimatedVisibility(
            toast,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = OcSpacing.sm),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) { BToast("Opening session…") }
        finger.Draw()
    }
}

@Composable
private fun PopoverCard(content: @Composable () -> Unit) {
    val c = OcTheme.colors
    val shape = RoundedCornerShape(OcRadius.xl)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.bgBase, shape)
            .border(1.dp, c.borderBase, shape)
            .padding(vertical = OcSpacing.xs),
    ) { content() }
}

@Composable
private fun PopoverHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = OcTheme.colors.textBase,
        modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = 2.dp),
    )
}

@Composable
private fun PopoverRow(label: String, time: String, mono: Boolean, openModifier: Modifier) {
    val c = OcTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp).padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        BRunningDots(c.textMuted)
        Text(
            label,
            style = if (mono) OcTheme.mono.code else MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.weight(1f),
        )
        Text(time, style = OcTheme.mono.label, color = c.textMuted)
        Text(
            "Open",
            style = MaterialTheme.typography.labelLarge,
            color = c.textBase,
            modifier = openModifier.padding(horizontal = OcSpacing.xs, vertical = OcSpacing.xs),
        )
    }
}

// ---- Running shells and subagents as chips above the message box ----

/** A shell and a subagent start: a chip each appears above the message box; one opens its list. */
@Composable
internal fun RunningChipsDemo(active: Boolean) {
    var shell by remember { mutableStateOf(true) }
    var agent by remember { mutableStateOf(true) }
    var open by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            shell = true
            agent = true
            open = true
            finger.hide()
        },
    ) {
        shell = false
        agent = false
        open = false
        finger.hide()
        delay(800)
        shell = true
        delay(1000)
        agent = true
        delay(1100)
        finger.show(targets.center("chipShell") + Below)
        finger.moveTo(targets.center("chipShell"))
        finger.tap()
        open = true
        delay(2400)
        finger.moveTo(targets.center("chipShell"))
        finger.tap()
        open = false
        finger.hide()
        delay(900)
    }
    val c = OcTheme.colors
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Text(
            "Starting the dev server and exploring the auth module…",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            modifier = Modifier.padding(OcSpacing.md),
        )
        Column(Modifier.align(Alignment.BottomCenter).padding(OcSpacing.sm)) {
            AnimatedVisibility(open && shell, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    PopoverCard {
                        PopoverHeading("Shells")
                        PopoverRow("npm run dev", "0:12", mono = true, openModifier = Modifier)
                    }
                    Spacer(Modifier.height(OcSpacing.xs))
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                AnimatedVisibility(shell, enter = fadeIn() + expandHorizontally()) {
                    BChip("1 shell", targets.target("chipShell"))
                }
                AnimatedVisibility(agent, enter = fadeIn() + expandHorizontally()) { BChip("1 agent") }
            }
            Spacer(Modifier.height(OcSpacing.xs))
            DemoComposer()
        }
        finger.Draw()
    }
}

// ---- Stop all background commands and subagents from the Running sheet ----

/** The Running sheet groups work under Subagents and Commands, each with its Stop all. */
@Composable
internal fun StopAllDemo(active: Boolean) {
    var agents by remember { mutableStateOf(true) }
    var commands by remember { mutableStateOf(true) }
    var stoppingAgents by remember { mutableStateOf(false) }
    var stoppingCommands by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            agents = true
            commands = true
            stoppingAgents = false
            stoppingCommands = false
            finger.hide()
        },
    ) {
        agents = true
        commands = true
        stoppingAgents = false
        stoppingCommands = false
        finger.hide()
        delay(1000)
        finger.show(targets.center("stopCommands") + Below)
        finger.moveTo(targets.center("stopCommands"))
        finger.tap()
        stoppingCommands = true
        delay(800)
        commands = false
        delay(700)
        finger.moveTo(targets.center("stopAgents"))
        finger.tap()
        stoppingAgents = true
        delay(800)
        agents = false
        finger.hide()
        delay(1400)
    }
    val c = OcTheme.colors
    val shape = RoundedCornerShape(topStart = OcRadius.sheet, topEnd = OcRadius.sheet)
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(shape).background(c.bgBase, shape).padding(bottom = OcSpacing.xs)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = OcSpacing.xs)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(c.borderStrong),
            )
            AnimatedVisibility(agents, exit = fadeOut() + shrinkVertically()) {
                Column {
                    SheetSection("Subagents", stoppingAgents, targets.target("stopAgents"))
                    SheetRow("Explore the auth module", "0:42", mono = false, stopping = stoppingAgents)
                    SheetRow("Review the API changes", "1:18", mono = false, stopping = stoppingAgents)
                }
            }
            AnimatedVisibility(commands, exit = fadeOut() + shrinkVertically()) {
                Column {
                    SheetSection("Commands", stoppingCommands, targets.target("stopCommands"))
                    SheetRow("npm run dev", "3:05", mono = true, stopping = stoppingCommands)
                    SheetRow("pytest -q tests/", "0:31", mono = true, stopping = stoppingCommands)
                }
            }
        }
        AnimatedVisibility(
            !agents && !commands,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut(),
        ) { Text("Nothing is running", style = MaterialTheme.typography.bodyMedium, color = c.textMuted) }
        finger.Draw()
    }
}

@Composable
private fun SheetSection(title: String, stopping: Boolean, stopModifier: Modifier) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = OcTheme.colors.textBase, modifier = Modifier.weight(1f))
        BStopButton("Stop all", stopModifier, spinner = stopping)
    }
}

@Composable
private fun SheetRow(label: String, time: String, mono: Boolean, stopping: Boolean) {
    val c = OcTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 30.dp).padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        BRunningDots(c.textMuted)
        Text(
            label,
            style = if (mono) OcTheme.mono.code else MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.weight(1f),
        )
        Text(time, style = OcTheme.mono.label, color = c.textMuted)
        Text(
            "Stop",
            style = MaterialTheme.typography.labelLarge,
            color = if (stopping) c.textFaint else c.danger.fg,
            modifier = Modifier.padding(horizontal = OcSpacing.xs),
        )
    }
}
