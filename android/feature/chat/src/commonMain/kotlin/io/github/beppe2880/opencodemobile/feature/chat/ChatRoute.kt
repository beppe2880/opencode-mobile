package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberTextClipboard
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_copied
import io.github.beppe2880.opencodemobile.core.ui.text.asString
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_dictation_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_dictation_no_speech
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_dictation_permission
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
fun ChatRoute(
    sessionId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    onOpenSession: (String) -> Unit = {},
    /** Replaces this chat with another session of the list (swipe). */
    onSwitchSession: (String) -> Unit = {},
    /** Opens a shell in the folder of the session. */
    onOpenTerminal: (String) -> Unit = {},
    /** Opens a file read-only: the session folder and the path, absolute or relative to it. */
    onOpenFile: (directory: String, path: String) -> Unit = { _, _ -> },
) {
    val viewModel: ChatViewModel = koinViewModel(key = "chat-$sessionId") { parametersOf(sessionId) }
    // Live shell output follows only the visible chat; suspended readers keep their cursor.
    LifecycleResumeEffect(viewModel) {
        viewModel.setChatVisible(true)
        onPauseOrDispose { viewModel.setChatVisible(false) }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.onEvent(ChatEvent.Visible(true))
        onStopOrDispose { viewModel.onEvent(ChatEvent.Visible(false)) }
    }
    val snackbar = remember { SnackbarHostState() }
    val clipboard = rememberTextClipboard()
    val scope = rememberCoroutineScope()
    // Per session: in list + detail the same route shows another session after a swipe.
    val listState = rememberSaveable(sessionId, saver = LazyListState.Saver) { LazyListState() }

    val attachments = rememberAttachmentPicker { read ->
        if (read.any { it == null }) viewModel.onEvent(ChatEvent.AttachmentFailed)
        read.filterNotNull().takeIf { it.isNotEmpty() }?.let { viewModel.onEvent(ChatEvent.AddAttachments(it)) }
    }
    val dictation = rememberDictation()
    val startDictation = {
        val typed = viewModel.state.value.draft
        dictation.start(
            onText = { spoken -> viewModel.onEvent(ChatEvent.DraftChanged(appendDictated(typed, spoken))) },
            onFailure = { failure ->
                val message = when (failure) {
                    DictationFailure.NoSpeech -> Res.string.chat_dictation_no_speech
                    DictationFailure.Permission -> Res.string.chat_dictation_permission
                    DictationFailure.Unavailable, DictationFailure.Other -> Res.string.chat_dictation_failed
                }
                scope.launch { snackbar.showSnackbar(getString(message)) }
            },
        )
    }
    // Asked on the first tap of the microphone; when denied, the keyboard's own voice typing still works.
    val micPermission = rememberMicPermission { granted ->
        if (granted) {
            startDictation()
        } else {
            scope.launch { snackbar.showSnackbar(getString(Res.string.chat_dictation_permission)) }
        }
    }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is ChatEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString())
            ChatEffect.NavigateBack -> onBack()
            ChatEffect.PromptSent -> {
                dictation.cancel()
                listState.scrollToLatest()
            }
            is ChatEffect.OpenSession -> onOpenSession(effect.sessionId)
            is ChatEffect.SwitchSession -> onSwitchSession(effect.sessionId)
            is ChatEffect.OpenTerminal -> onOpenTerminal(effect.directory)
            is ChatEffect.OpenFile -> onOpenFile(effect.directory, effect.path)
        }
    }

    ChatScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onCopy = { text ->
            scope.launch {
                clipboard.setText(text)
                snackbar.showSnackbar(getString(UiRes.string.oc_copied))
            }
        },
        modifier = modifier,
        onAttachPhoto = attachments::pickPhotos,
        onAttachFile = attachments::pickFiles,
        voice = if (state.dictation && dictation.available) VoiceInput(dictation.listening, dictation.level) else null,
        onDictate = {
            when {
                dictation.listening -> dictation.stop()
                micPermission.granted() -> startDictation()
                else -> micPermission.request()
            }
        },
        showBack = showBack,
        snackbarHostState = snackbar,
        listState = listState,
    )
}
