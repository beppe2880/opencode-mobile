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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.QueuedPrompt
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.components.LocalEditMode
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcExpandable
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.editable
import io.github.beppe2880.opencodemobile.core.ui.components.rememberLastNonNull
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMotion
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_background_action
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_background_hint
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_offline
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_queue_edit
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_queue_remove
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_queue_steer
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_queue_title
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_restore
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_reverted
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_unauthorized
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/** Everything below the transcript: banners, permission and question cards, completions, composer. */
@Composable
internal fun BottomArea(
    state: ChatState,
    onEvent: (ChatEvent) -> Unit,
    onAttachPhoto: () -> Unit,
    onAttachFile: () -> Unit,
    voice: VoiceInput? = null,
    onDictate: () -> Unit = {},
    onVoice: (VoiceInputEvent) -> Unit = {},
    onHoldVoice: () -> Unit = {},
) {
    val c = OcTheme.colors
    val editMode = LocalEditMode.current
    Column(
        modifier = Modifier
            .widthIn(max = OcSpacing.contentMaxWidth)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        val offline = when (state.connection) {
            ConnectionState.Reconnecting, ConnectionState.Disconnected -> Res.string.chat_offline to c.warning.fg
            ConnectionState.Unauthorized -> Res.string.chat_unauthorized to c.danger.fg
            ConnectionState.Connected, ConnectionState.Connecting -> null
        }
        // Each part above the composer grows in when it appears and folds away when it goes, so the
        // transcript makes room smoothly; what it showed stays drawn while it folds.
        val shownOffline = rememberLastNonNull(offline)
        OcExpandable(visible = offline != null, label = "chat_offline") {
            shownOffline?.let { (message, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("chat_offline_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs, Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Outlined.CloudOff, null, tint = color, modifier = Modifier.size(14.dp))
                    Text(stringResource(message), style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
        val permission = state.permissions.firstOrNull()
        val shownPermission = rememberLastNonNull(permission)
        OcExpandable(visible = permission != null, label = "chat_permission") {
            shownPermission?.let { request ->
                PermissionCard(
                    request = request,
                    pendingCount = state.permissions.size.coerceAtLeast(1),
                    replying = state.replyingPermission == request.id,
                    collapsed = state.isPermissionCollapsed(request.id),
                    onCollapse = { onEvent(ChatEvent.CollapsePermission(request.id, it)) },
                    // Folding away once answered, it no longer replies.
                    onReply = { if (permission != null) onEvent(ChatEvent.ReplyPermission(request.id, it)) },
                    modifier = Modifier.editable(SettingsPart.PendingCards, editMode),
                )
            }
        }
        val form = state.forms.firstOrNull()?.takeIf { state.permissions.isEmpty() }
        val shownForm = rememberLastNonNull(form)
        OcExpandable(visible = form != null, label = "chat_form") {
            shownForm?.let { shown ->
                FormCard(
                    form = shown,
                    answers = state.formAnswers,
                    pendingCount = state.forms.size.coerceAtLeast(1),
                    submitting = state.submittingForm,
                    collapsed = state.isFormCollapsed(shown.id),
                    onCollapse = { onEvent(ChatEvent.CollapseForm(shown.id, it)) },
                    onAnswer = { key, answer -> onEvent(ChatEvent.FormAnswerChanged(key, answer)) },
                    onSubmit = { if (state.forms.isNotEmpty()) onEvent(ChatEvent.SubmitForm) },
                    onDismiss = { if (state.forms.isNotEmpty()) onEvent(ChatEvent.DismissForm) },
                    modifier = Modifier.editable(SettingsPart.PendingCards, editMode),
                )
            }
        }
        val backgroundable = rememberLastNonNull(state.backgroundable)
        OcExpandable(visible = state.backgroundable != null, label = "chat_backgroundable") {
            backgroundable?.let { work ->
                BackgroundHint(work, onMove = { if (state.backgroundable != null) onEvent(ChatEvent.MoveToBackground) })
            }
        }
        val reverted = rememberLastNonNull(state.revertedCount.takeIf { it > 0 })
        OcExpandable(visible = state.revertedCount > 0, label = "chat_reverted") {
            RevertBanner(reverted ?: 0, onRestore = { onEvent(ChatEvent.RestoreRevert) })
        }
        val queued = rememberLastNonNull(state.queued.takeIf { it.isNotEmpty() })
        OcExpandable(visible = state.queued.isNotEmpty(), label = "chat_queue") {
            QueuedPrompts(queued.orEmpty(), onEvent)
        }
        val suggestions = rememberLastNonNull(state.suggestions.takeIf { it.isNotEmpty() })
        OcExpandable(visible = state.suggestions.isNotEmpty(), label = "chat_suggestions") {
            // A list folding away after a pick takes no second one.
            SuggestionList(suggestions.orEmpty(), onPick = { if (state.suggestions.isNotEmpty()) onEvent(ChatEvent.PickSuggestion(it)) })
        }
        // Part of this column, so the transcript above ends before them and is never covered.
        RunningChipsBar(state, onEvent)
        Composer(
            state,
            onEvent,
            onAttachPhoto,
            onAttachFile,
            modifier = Modifier.editable(SettingsPart.ComposerBox, editMode),
            voice = voice,
            onDictate = onDictate,
            onVoice = onVoice,
            onHoldVoice = onHoldVoice,
        )
    }
}

/** Appears once [work] has run for a few seconds, like the TUI's ctrl+b hint. */
@Composable
private fun BackgroundHint(work: BackgroundableWork, onMove: () -> Unit) {
    // Already due when the chat opens: shown at once, without growing in.
    var visible by remember(work.key) { mutableStateOf(hintDelayLeft(work) <= 0) }
    LaunchedEffect(work.key) {
        delay(hintDelayLeft(work))
        visible = true
    }
    OcExpandable(visible = visible, label = "chat_background_hint") { BackgroundHintRow(onMove) }
}

private fun hintDelayLeft(work: BackgroundableWork): Long {
    val elapsed = work.startedAt?.let { Clock.System.now().toEpochMilliseconds() - it } ?: 0L
    return (BackgroundableWork.HINT_DELAY_MS - elapsed).coerceAtLeast(0)
}

@Composable
private fun BackgroundHintRow(onMove: () -> Unit) {
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
            stringResource(Res.string.chat_background_hint),
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            modifier = Modifier.weight(1f).padding(horizontal = OcSpacing.sm),
        )
        OcButton(
            text = stringResource(Res.string.chat_background_action),
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
            pluralStringResource(Res.plurals.chat_reverted, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            modifier = Modifier.weight(1f).padding(horizontal = OcSpacing.sm),
        )
        OcButton(
            text = stringResource(Res.string.chat_restore),
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
    val motion = OcTheme.motion
    OcCard(modifier = Modifier.fillMaxWidth().testTag("chat_queue"), shape = RoundedCornerShape(OcRadius.lg)) {
        Row(
            modifier = Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, top = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            Icon(Icons.Outlined.Schedule, null, tint = c.iconMuted, modifier = Modifier.size(14.dp))
            Text(
                pluralStringResource(Res.plurals.chat_queue_title, queued.size, queued.size),
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted,
            )
        }
        LazyColumn(Modifier.heightIn(max = 200.dp)) {
            items(queued, key = { it.id }) { prompt ->
                Row(
                    modifier = Modifier
                        // A prompt sent or removed makes the others slide over instead of jumping.
                        .animateItem(
                            fadeInSpec = OcMotion.spec(OcMotion.SHORT_MS, motion),
                            placementSpec = OcMotion.spec(OcMotion.MEDIUM_MS, motion),
                            fadeOutSpec = OcMotion.spec(OcMotion.SHORT_MS, motion),
                        )
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
                        contentDescription = stringResource(Res.string.chat_queue_steer),
                        onClick = { onEvent(ChatEvent.SteerQueued(prompt.id)) },
                        iconSize = 18.dp,
                        modifier = Modifier.testTag("chat_queued_steer_${prompt.id}"),
                    )
                    OcIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(Res.string.chat_queue_edit),
                        onClick = { onEvent(ChatEvent.EditQueued(prompt.id)) },
                        iconSize = 18.dp,
                        modifier = Modifier.testTag("chat_queued_edit_${prompt.id}"),
                    )
                    OcIconButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = stringResource(Res.string.chat_queue_remove),
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
