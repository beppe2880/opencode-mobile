package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberTextClipboard
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_copied
import io.github.beppe2880.opencodemobile.core.ui.text.asString
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_dictation_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_dictation_no_speech
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_dictation_permission
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_discarded
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_voice_undo
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    // Cloud voice input: one recording per chat, sent to the provider chosen in Settings.
    val voiceModel: VoiceInputViewModel = koinViewModel(key = "voice-$sessionId")
    val voiceState by voiceModel.state.collectAsStateWithLifecycle()
    val cloud = voiceState.engine == VoiceEngine.Cloud
    // Leaving the chat or the app ends the recording; what was said is still transcribed.
    LifecycleStartEffect(voiceModel) {
        onStopOrDispose { voiceModel.onEvent(VoiceInputEvent.Stop) }
    }
    CollectEffect(voiceModel.effect) { effect ->
        when (effect) {
            VoiceInputEffect.NoSpeech -> snackbar.showSnackbar(getString(Res.string.chat_dictation_no_speech))
            // Its own coroutine: the snackbar stays while other effects arrive, and closes when undo is no longer possible.
            VoiceInputEffect.Discarded -> scope.launch {
                val message = getString(Res.string.chat_voice_discarded)
                val undo = getString(Res.string.chat_voice_undo)
                val result = withTimeoutOrNull(VOICE_UNDO_MS) {
                    snackbar.showSnackbar(message, actionLabel = undo, duration = SnackbarDuration.Indefinite)
                }
                if (result == SnackbarResult.ActionPerformed) voiceModel.onEvent(VoiceInputEvent.Undo)
            }
        }
    }
    // Asked on the first tap of the microphone; when denied, the keyboard's own voice typing still works.
    val micPermission = rememberMicPermission { granted ->
        if (granted) {
            if (cloud) voiceModel.onEvent(VoiceInputEvent.Start) else startDictation()
        } else {
            scope.launch { snackbar.showSnackbar(getString(Res.string.chat_dictation_permission)) }
        }
    }
    val startCloud = { event: VoiceInputEvent ->
        if (!voiceModel.needsPermission || micPermission.granted()) voiceModel.onEvent(event) else micPermission.request()
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
        voice = when {
            !state.dictation -> null
            cloud -> {
                val phase = voiceState.phase
                VoiceInput(
                    listening = phase is CloudVoicePhase.Recording,
                    level = 0f,
                    cloud = phase,
                    provider = voiceState.provider.label,
                    transcript = voiceState.transcript,
                    levels = voiceModel.level,
                    style = voiceState.style,
                    holdToRecord = voiceState.holdToRecord,
                )
            }
            dictation.available -> VoiceInput(dictation.listening, dictation.level)
            else -> null
        },
        onDictate = {
            when {
                cloud -> when (voiceState.phase) {
                    is CloudVoicePhase.Recording -> voiceModel.onEvent(VoiceInputEvent.Stop)
                    CloudVoicePhase.Transcribing -> Unit
                    else -> startCloud(VoiceInputEvent.Start)
                }
                dictation.listening -> dictation.stop()
                micPermission.granted() -> startDictation()
                else -> micPermission.request()
            }
        },
        onVoice = voiceModel::onEvent,
        // Without the permission yet, the dialog takes the press: after it a tap records.
        onHoldVoice = { startCloud(VoiceInputEvent.Hold) },
        showBack = showBack,
        snackbarHostState = snackbar,
        listState = listState,
    )
}
