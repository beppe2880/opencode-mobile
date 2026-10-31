package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.RunningCommand
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcConfirmDialog
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_running
import io.github.beppe2880.opencodemobile.core.ui.text.formatDuration
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_elapsed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_empty
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_error
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_no_output
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_refresh
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_message
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_stop_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_unassigned
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_running_unassigned_hint
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_stop
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
internal fun RunningSheet(state: ChatState, onEvent: (ChatEvent) -> Unit) {
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1_000)
        }
    }
    val colors = OcTheme.colors
    OcBottomSheet(onDismiss = { onEvent(ChatEvent.RunningSheet(false)) }, testTag = "chat_running_sheet") {
        LazyColumn(
            // As tall as its content, up to 70% of the screen.
            modifier = Modifier.fillMaxWidth().layout { measurable, constraints ->
                val max = if (constraints.hasBoundedHeight) (constraints.maxHeight * MAX_HEIGHT_FRACTION).toInt() else constraints.maxHeight
                val placeable = measurable.measure(constraints.copy(maxHeight = max.coerceAtLeast(constraints.minHeight)))
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            },
            contentPadding = PaddingValues(
                start = OcSpacing.lg,
                end = OcSpacing.lg,
                bottom = OcSpacing.lg + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(UiRes.string.oc_running), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (state.workLoading) OcSpinner()
                    OcIconButton(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(Res.string.chat_running_refresh),
                        onClick = { onEvent(ChatEvent.RefreshRunning) },
                        modifier = Modifier.testTag("chat_running_refresh"),
                    )
                }
            }
            state.workError?.let { error ->
                item(key = "error") {
                    OcErrorState(
                        title = stringResource(Res.string.chat_running_error),
                        message = error.resolve(),
                        onRetry = { onEvent(ChatEvent.RefreshRunning) },
                        retryTestTag = "chat_running_retry",
                    )
                }
            }
            if (!state.workLoading && state.workError == null && state.work.isEmpty) {
                item(key = "empty") { Text(stringResource(Res.string.chat_running_empty), color = colors.textMuted) }
            }
            items(state.work.agents, key = { "agent:${it.session.id}" }) { agent ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("running_agent_${agent.session.id}")
                        .clickable(role = Role.Button) { onEvent(ChatEvent.OpenSession(agent.session.id)) }
                        .padding(vertical = OcSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        Text(agent.session.title, style = MaterialTheme.typography.bodyMedium)
                        agent.session.agent?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = colors.textMuted) }
                        agent.startedAt?.let { Elapsed(it, now, state.clockOffsetMs) }
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.iconMuted)
                }
            }
            items(state.work.commands, key = { "command:${it.id}" }) { command ->
                CommandRow(command, state, now, onEvent)
            }
            if (state.unassignedCommands.isNotEmpty()) {
                item(key = "unassigned") {
                    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        Text(stringResource(Res.string.chat_running_unassigned), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(Res.string.chat_running_unassigned_hint), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    }
                }
                items(state.unassignedCommands, key = { "command:${it.id}" }) { command ->
                    CommandRow(command, state, now, onEvent)
                }
            }
        }
    }
    state.confirmingStop?.let { id ->
        val command = (state.work.commands + state.unassignedCommands).firstOrNull { it.id == id }?.command.orEmpty()
        OcConfirmDialog(
            title = stringResource(Res.string.chat_running_stop_title),
            text = stringResource(Res.string.chat_running_stop_message, command),
            confirmLabel = stringResource(Res.string.chat_stop),
            onConfirm = { onEvent(ChatEvent.ConfirmStopCommand) },
            onDismiss = { onEvent(ChatEvent.DismissStopCommand) },
            destructive = true,
            testTagPrefix = "running_stop",
        )
    }
}

@Composable
private fun CommandRow(command: RunningCommand, state: ChatState, now: Long, onEvent: (ChatEvent) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("running_command_${command.id}"), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        Text(command.command, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Elapsed(command.startedAt, now, state.clockOffsetMs) }
            TextButton(
                onClick = { onEvent(ChatEvent.StopCommand(command.id)) },
                enabled = command.id !in state.stoppingCommands,
                modifier = Modifier.testTag("running_stop_${command.id}"),
            ) {
                if (command.id in state.stoppingCommands) OcSpinner() else Text(stringResource(Res.string.chat_stop))
            }
        }
        state.workOutputErrors[command.id]?.let { Text(it.resolve(), color = OcTheme.colors.danger.fg) }
        Text(
            state.workOutput[command.id]?.takeIf { it.isNotEmpty() } ?: stringResource(Res.string.chat_running_no_output),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = OcTheme.colors.textMuted,
            modifier = Modifier.testTag("running_output_${command.id}"),
        )
    }
}

/** [startedAt] is on the server clock: [offsetMs] moves the device's [now] onto it. */
@Composable
private fun Elapsed(startedAt: Long, now: Long, offsetMs: Long) {
    Text(
        stringResource(Res.string.chat_running_elapsed, formatDuration(elapsedMs(startedAt, now, offsetMs))),
        style = MaterialTheme.typography.bodySmall,
        color = OcTheme.colors.textMuted,
    )
}

internal fun elapsedMs(startedAt: Long, deviceNow: Long, offsetMs: Long): Long = (deviceNow + offsetMs - startedAt).coerceAtLeast(0)

private const val MAX_HEIGHT_FRACTION = 0.7f
