package io.github.beppe2880.opencodemobile.feature.chat

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.text.asString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import io.github.beppe2880.opencodemobile.core.ui.R as UiR

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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    // Per session: in list + detail the same route shows another session after a swipe.
    val listState = rememberSaveable(sessionId, saver = LazyListState.Saver) { LazyListState() }

    val readAttachments: (List<Uri>) -> Unit = { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val read = withContext(Dispatchers.IO) { uris.map { AttachmentReader.read(context.contentResolver, it) } }
                if (read.any { it == null }) viewModel.onEvent(ChatEvent.AttachmentFailed)
                read.filterNotNull().takeIf { it.isNotEmpty() }?.let { viewModel.onEvent(ChatEvent.AddAttachments(it)) }
            }
        }
    }
    val dictation = rememberDictation()
    val startDictation = {
        val typed = viewModel.state.value.draft
        dictation.start(
            onText = { spoken -> viewModel.onEvent(ChatEvent.DraftChanged(appendDictated(typed, spoken))) },
            onFailure = { failure ->
                val message = when (failure) {
                    DictationFailure.NoSpeech -> R.string.chat_dictation_no_speech
                    DictationFailure.Permission -> R.string.chat_dictation_permission
                    DictationFailure.Unavailable, DictationFailure.Other -> R.string.chat_dictation_failed
                }
                scope.launch { snackbar.showSnackbar(context.getString(message)) }
            },
        )
    }
    // Asked on the first tap of the microphone; when denied, the keyboard's own voice typing still works.
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startDictation()
        } else {
            scope.launch { snackbar.showSnackbar(context.getString(R.string.chat_dictation_permission)) }
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICKED), readAttachments)
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), readAttachments)

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is ChatEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(context))
            ChatEffect.NavigateBack -> onBack()
            ChatEffect.PromptSent -> {
                dictation.cancel()
                listState.scrollToItem(0)
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
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("OpenCode", text)))
                snackbar.showSnackbar(context.getString(UiR.string.oc_copied))
            }
        },
        modifier = modifier,
        onAttachPhoto = {
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onAttachFile = { filePicker.launch(arrayOf("*/*")) },
        voice = if (state.dictation && dictation.available) VoiceInput(dictation.listening, dictation.level) else null,
        onDictate = {
            when {
                dictation.listening -> dictation.stop()
                context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> startDictation()
                else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        showBack = showBack,
        snackbarHostState = snackbar,
        listState = listState,
    )
}

private const val MAX_PICKED = 5
