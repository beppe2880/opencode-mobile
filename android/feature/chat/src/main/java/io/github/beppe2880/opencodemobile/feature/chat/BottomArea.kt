package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.QueuedPrompt
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import kotlinx.coroutines.delay

/** Everything below the transcript: banners, permission and question cards, completions, composer. */
@Composable
internal fun BottomArea(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onAttachPhoto: () -> Unit,
    onAttachFile: () -> Unit,
) {
    val c = OcTheme.colors
    Column(
        modifier = Modifier
            .widthIn(max = OcSpacing.contentMaxWidth)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        val offline = when (state.connection) {
            ConnectionState.Reconnecting, ConnectionState.Disconnected -> R.string.chat_offline to c.warning.fg
            ConnectionState.Unauthorized -> R.string.chat_unauthorized to c.danger.fg
            ConnectionState.Connected, ConnectionState.Connecting -> null
        }
        offline?.let { (message, color) ->
            Row(
                modifier = Modifier.fillMaxWidth().testTag("chat_offline_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs, Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Outlined.CloudOff, null, tint = color, modifier = Modifier.size(14.dp))
                Text(stringResource(message), style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
        state.permissions.firstOrNull()?.let { request ->
            PermissionCard(
                request = request,
                pendingCount = state.permissions.size,
                replying = state.replyingPermission == request.id,
                onReply = { onEvent(ChatEvent.ReplyPermission(request.id, it)) },
            )
        }
        if (state.permissions.isEmpty()) {
            state.forms.firstOrNull()?.let { form ->
                FormCard(
                    form = form,
                    answers = state.formAnswers,
                    pendingCount = state.forms.size,
                    submitting = state.submittingForm,
                    onAnswer = { key, answer -> onEvent(ChatEvent.FormAnswerChanged(key, answer)) },
                    onSubmit = { onEvent(ChatEvent.SubmitForm) },
                    onDismiss = { onEvent(ChatEvent.DismissForm) },
                )
            }
        }
        state.backgroundable?.let { work ->
            BackgroundHint(work, onMove = { onEvent(ChatEvent.MoveToBackground) })
        }
        if (state.revertedCount > 0) {
            RevertBanner(state.revertedCount, onRestore = { onEvent(ChatEvent.RestoreRevert) })
        }
        if (state.queued.isNotEmpty()) {
            QueuedPrompts(state.queued, onEvent)
        }
        if (state.suggestions.isNotEmpty()) {
            SuggestionList(state.suggestions, onPick = { onEvent(ChatEvent.PickSuggestion(it)) })
        }
        Composer(state, onEvent, onAttachPhoto, onAttachFile)
    }
}

/** Appears once [work] has run for a few seconds, like the TUI's ctrl+b hint. */
@Composable
private fun BackgroundHint(work: BackgroundableWork, onMove: () -> Unit) {
    var visible by remember(work.key) { mutableStateOf(false) }
    LaunchedEffect(work.key) {
        val elapsed = work.startedAt?.let { System.currentTimeMillis() - it } ?: 0L
        delay((BackgroundableWork.HINT_DELAY_MS - elapsed).coerceAtLeast(0))
        visible = true
    }
    if (!visible) return
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.layer01, RoundedCornerShape(OcRadius.lg))
            .padding(start = OcSpacing.md, end = OcSpacing.xs)
            .testTag("chat_background_hint"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Terminal, null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.chat_background_hint),
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            modifier = Modifier.weight(1f).padding(horizontal = OcSpacing.sm),
        )
        OcButton(
            text = stringResource(R.string.chat_background_action),
            onClick = onMove,
            variant = OcButtonVariant.Ghost,
            compact = true,
            modifier = Modifier.testTag("chat_background_button"),
        )
    }
}

@Composable
private fun RevertBanner(count: Int, onRestore: () -> Unit) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.layer01, RoundedCornerShape(OcRadius.lg))
            .padding(start = OcSpacing.md, end = OcSpacing.xs)
            .testTag("chat_revert_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Outlined.Undo, null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
        Text(
            pluralStringResource(R.plurals.chat_reverted, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            modifier = Modifier.weight(1f).padding(horizontal = OcSpacing.sm),
        )
        OcButton(
            text = stringResource(R.string.chat_restore),
            onClick = onRestore,
            variant = OcButtonVariant.Ghost,
            compact = true,
            modifier = Modifier.testTag("chat_revert_restore_button"),
        )
    }
}

/** Prompts waiting for the agent to finish, each one can be sent now, edited or removed. */
@Composable
private fun QueuedPrompts(queued: List<QueuedPrompt>, onEvent: (ChatEvent) -> Unit) {
    val c = OcTheme.colors
    OcCard(modifier = Modifier.fillMaxWidth().testTag("chat_queue"), shape = RoundedCornerShape(OcRadius.lg)) {
        Row(
            modifier = Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, top = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            Icon(Icons.Outlined.Schedule, null, tint = c.iconMuted, modifier = Modifier.size(14.dp))
            Text(
                pluralStringResource(R.plurals.chat_queue_title, queued.size, queued.size),
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted,
            )
        }
        LazyColumn(Modifier.heightIn(max = 200.dp)) {
            items(queued, key = { it.id }) { prompt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = OcSpacing.md, end = OcSpacing.xxs)
                        .testTag("chat_queued_${prompt.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        prompt.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textBase,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = stringResource(R.string.chat_queue_steer),
                        onClick = { onEvent(ChatEvent.SteerQueued(prompt.id)) },
                        iconSize = 18.dp,
                        modifier = Modifier.testTag("chat_queued_steer_${prompt.id}"),
                    )
                    OcIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.chat_queue_edit),
                        onClick = { onEvent(ChatEvent.EditQueued(prompt.id)) },
                        iconSize = 18.dp,
                        modifier = Modifier.testTag("chat_queued_edit_${prompt.id}"),
                    )
                    OcIconButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.chat_queue_remove),
                        onClick = { onEvent(ChatEvent.RemoveQueued(prompt.id)) },
                        iconSize = 18.dp,
                        modifier = Modifier.testTag("chat_queued_remove_${prompt.id}"),
                    )
                }
            }
        }
    }
}

/** `@` file and `/` command completions; tapping one completes the draft. */
@Composable
private fun SuggestionList(suggestions: List<Suggestion>, onPick: (Suggestion) -> Unit) {
    val c = OcTheme.colors
    OcCard(modifier = Modifier.fillMaxWidth().testTag("chat_suggestions"), shape = RoundedCornerShape(OcRadius.lg)) {
        LazyColumn(Modifier.heightIn(max = 220.dp)) {
            items(suggestions, key = { it.key }) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(suggestion) }
                        .heightIn(min = 44.dp)
                        .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)
                        .testTag("chat_suggestion_${suggestion.key}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                ) {
                    when (suggestion) {
                        is Suggestion.File -> {
                            Icon(Icons.Outlined.Description, null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
                            Text(
                                suggestion.path,
                                style = OcTheme.mono.codeSmall,
                                color = c.textBase,
                                maxLines = 1,
                                overflow = TextOverflow.StartEllipsis,
                            )
                        }
                        is Suggestion.Slash -> {
                            Text("/${suggestion.name}", style = OcTheme.mono.code, color = c.textBase)
                            suggestion.description?.let {
                                Text(
                                    it.resolve(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
