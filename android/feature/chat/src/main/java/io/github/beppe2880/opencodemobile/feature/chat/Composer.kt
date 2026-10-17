package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcRoundIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcStatusDot
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** Bottom composer card: attachments, text input, agent + model selectors, send / stop. */
@Composable
internal fun Composer(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onAttachPhoto: () -> Unit,
    onAttachFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val haptics = LocalHapticFeedback.current
    // The outline takes the selected agent's color, like the agent dot in the selector.
    val agentColor by animateColorAsState(
        if (state.agentColorOutline) c.agents.forAgent(state.agent ?: state.agents.firstOrNull()?.id ?: "build") else c.borderStrong,
        label = "composer_outline",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OcRadius.xl),
        color = c.bgBase,
        border = BorderStroke(1.dp, agentColor),
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(start = OcSpacing.md, end = OcSpacing.xs, top = OcSpacing.sm, bottom = OcSpacing.xs)) {
            AnimatedVisibility(visible = state.running && state.draft.isNotBlank() && !state.isShell) {
                DeliveryToggle(state.delivery, onSelect = { onEvent(ChatEvent.DeliveryChanged(it)) })
            }
            AnimatedVisibility(visible = state.isShell) { ShellHint(state.directory) }
            if (state.uploads.isNotEmpty()) {
                AttachmentChips(
                    state.uploads,
                    onRemove = { onEvent(ChatEvent.RemoveAttachment(it)) },
                    onPreview = { onEvent(ChatEvent.PreviewAttachment(it)) },
                )
            }
            val placeholder = stringResource(R.string.chat_composer_placeholder)
            // Keeps the selection; when the ViewModel rewrites the draft (picked mention or
            // command, reverted prompt) the cursor moves to the end of the new text.
            var field by remember { mutableStateOf(TextFieldValue(state.draft, TextRange(state.draft.length))) }
            val value = if (field.text == state.draft) field else TextFieldValue(state.draft, TextRange(state.draft.length))
            LaunchedEffect(value) { field = value }
            BasicTextField(
                value = value,
                onValueChange = {
                    field = it
                    if (it.text != state.draft) onEvent(ChatEvent.DraftChanged(it.text))
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.textBase),
                cursorBrush = SolidColor(c.bgAccent),
                minLines = 1,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = OcSpacing.sm, top = OcSpacing.xs, bottom = OcSpacing.xs)
                    .heightIn(min = 40.dp)
                    .semantics { contentDescription = placeholder }
                    .testTag("chat_composer_field"),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (state.draft.isEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                        }
                        inner()
                    }
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                // Selectors take only the space the stop/send buttons leave; on narrow phones the
                // model chip shrinks and ellipsizes instead of pushing the buttons off screen.
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                ) {
                    AttachButton(onAttachPhoto, onAttachFile)
                    AgentSelector(state.agents, state.agent, onSelect = { onEvent(ChatEvent.SelectAgent(it)) })
                    OcChip(
                        text = (state.modelLabel?.name ?: stringResource(R.string.chat_model)) +
                            state.model?.variant?.let { " · $it" }.orEmpty(),
                        secondaryText = state.modelLabel?.provider,
                        onClick = { onEvent(ChatEvent.ModelSheet(true)) },
                        leadingIcon = Icons.Outlined.AutoAwesome,
                        trailingIcon = Icons.Outlined.ExpandMore,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 280.dp)
                            .testTag("chat_model_selector"),
                    )
                }
                if (state.running) {
                    OcRoundIconButton(
                        icon = Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.chat_stop),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(ChatEvent.Stop)
                        },
                        enabled = !state.stopping,
                        container = c.danger.bg,
                        content = c.danger.fg,
                        modifier = Modifier.testTag("chat_stop_button"),
                    )
                }
                if (!state.running || state.draft.isNotBlank()) {
                    OcRoundIconButton(
                        icon = if (state.running) Icons.AutoMirrored.Outlined.Send else Icons.Outlined.ArrowUpward,
                        contentDescription = stringResource(R.string.chat_send),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEvent(ChatEvent.Send)
                        },
                        enabled = state.canSend,
                        modifier = Modifier.testTag("chat_send_button"),
                    )
                }
            }
        }
    }
}

/** Shown while the draft is a `!command`: it runs in the shell instead of going to the agent. */
@Composable
private fun ShellHint(directory: String) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .padding(top = OcSpacing.xs, end = OcSpacing.sm)
            .testTag("chat_shell_hint"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        Icon(Icons.Outlined.Terminal, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(14.dp))
        Text(
            text = if (directory.isEmpty()) stringResource(R.string.chat_shell_hint) else stringResource(R.string.chat_shell_hint_in, directory),
            style = MaterialTheme.typography.labelSmall,
            color = c.textAccent,
            maxLines = 1,
            overflow = TextOverflow.StartEllipsis,
        )
    }
}

@Composable
private fun AttachButton(onAttachPhoto: () -> Unit, onAttachFile: () -> Unit) {
    val c = OcTheme.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        OcIconButton(
            icon = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.chat_attach),
            onClick = { open = true },
            modifier = Modifier.testTag("chat_attach_button"),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.bgBase, modifier = Modifier.exposeTestTags()) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_attach_photo), color = c.textBase) },
                leadingIcon = { Icon(Icons.Outlined.Image, null, tint = c.iconBase) },
                onClick = {
                    open = false
                    onAttachPhoto()
                },
                modifier = Modifier.testTag("chat_attach_photo"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_attach_file), color = c.textBase) },
                leadingIcon = { Icon(Icons.Outlined.AttachFile, null, tint = c.iconBase) },
                onClick = {
                    open = false
                    onAttachFile()
                },
                modifier = Modifier.testTag("chat_attach_file"),
            )
        }
    }
}

@Composable
private fun AttachmentChips(attachments: List<PromptAttachment>, onRemove: (String) -> Unit, onPreview: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = OcSpacing.xs, end = OcSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        attachments.forEach { attachment ->
            val image = attachment.mime?.startsWith("image/") == true
            OcChip(
                text = attachment.name,
                // Tapping an image opens it; the X removes the attachment.
                onClick = { if (image) onPreview(attachment.uri) },
                leadingIcon = if (image) Icons.Outlined.Image else Icons.Outlined.AttachFile,
                trailingIcon = Icons.Outlined.Close,
                onTrailingClick = { onRemove(attachment.uri) },
                trailingContentDescription = stringResource(R.string.chat_remove_attachment, attachment.name),
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .testTag("chat_attachment_${attachment.name}"),
            )
        }
    }
}

@Composable
private fun DeliveryToggle(selected: PromptDelivery, onSelect: (PromptDelivery) -> Unit) {
    Row(
        modifier = Modifier.padding(bottom = OcSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OcChip(
            text = stringResource(R.string.chat_steer),
            selected = selected == PromptDelivery.Steer,
            onClick = { onSelect(PromptDelivery.Steer) },
            modifier = Modifier.testTag("chat_delivery_steer"),
        )
        OcChip(
            text = stringResource(R.string.chat_queue),
            selected = selected == PromptDelivery.Queue,
            onClick = { onSelect(PromptDelivery.Queue) },
            modifier = Modifier.testTag("chat_delivery_queue"),
        )
    }
}

@Composable
private fun AgentSelector(agents: List<Agent>, selected: String?, onSelect: (String) -> Unit) {
    val c = OcTheme.colors
    var open by rememberSaveable { mutableStateOf(false) }
    val label = selected ?: agents.firstOrNull()?.id ?: "build"
    Box {
        OcChip(
            text = label.replaceFirstChar { it.uppercase() },
            onClick = {
                // With two agents a menu is one tap too many: switch straight to the other one.
                val other = agents.singleOrNull { it.id != label }
                when {
                    agents.size == 2 && other != null -> onSelect(other.id)
                    agents.isNotEmpty() -> open = true
                }
            },
            dotColor = c.agents.forAgent(label),
            modifier = Modifier.testTag("chat_agent_selector"),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.bgBase, modifier = Modifier.exposeTestTags()) {
            agents.forEach { agent ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(agent.name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                            agent.description?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    },
                    leadingIcon = { OcStatusDot(c.agents.forAgent(agent.id)) },
                    onClick = {
                        open = false
                        onSelect(agent.id)
                    },
                    modifier = Modifier.testTag("chat_agent_option_${agent.id}"),
                )
            }
        }
    }
}

