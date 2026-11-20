package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.text.UiText
import io.github.beppe2880.opencodemobile.core.ui.text.asString
import io.github.beppe2880.opencodemobile.feature.sessions.resources.Res
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_archived_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_restored_message
import io.github.beppe2880.opencodemobile.feature.sessions.resources.sessions_undo
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SessionsRoute(
    onOpenChat: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    selectedSessionId: String? = null,
    /** Opens the new session sheet once, for a shortcut outside the app such as the widget. */
    openNewSession: Boolean = false,
    onNewSessionOpened: () -> Unit = {},
    banner: @Composable () -> Unit = {},
    /** Drawn at the top of the list header (see [SessionsScreen]). */
    headerTop: @Composable () -> Unit = {},
    /** The offline demo: sample sessions, no server behind them. */
    demo: Boolean = false,
    /** Shown in the drawer over a chat (see [SessionsScreen]). */
    inDrawer: Boolean = false,
) {
    val viewModel: SessionsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(viewModel) {
        viewModel.onEvent(SessionsEvent.Visible(true))
        onStopOrDispose { viewModel.onEvent(SessionsEvent.Visible(false)) }
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(openNewSession) {
        if (openNewSession) {
            viewModel.onEvent(SessionsEvent.StartNewSession)
            onNewSessionOpened()
        }
    }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is SessionsEffect.OpenChat -> onOpenChat(effect.sessionId)
            SessionsEffect.OpenSettings -> onOpenSettings()
            is SessionsEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString())
            is SessionsEffect.ShowArchiveUndo -> {
                // Its own coroutine: the snackbar must not make CollectEffect await it, or every
                // later effect (opening a session, for one) waits until it disappears.
                scope.launch {
                    val result = withTimeoutOrNull(ARCHIVE_UNDO_MS) {
                        snackbar.showSnackbar(
                            message = UiText.Resource(
                                if (effect.archived) Res.string.sessions_archived_message else Res.string.sessions_restored_message,
                            ).asString(),
                            actionLabel = UiText.Resource(Res.string.sessions_undo).asString(),
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(SessionsEvent.SetArchived(effect.session, !effect.archived))
                    }
                }
            }
        }
    }

    SessionsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        selectedSessionId = selectedSessionId,
        snackbarHostState = snackbar,
        banner = banner,
        headerTop = headerTop,
        demo = demo,
        inDrawer = inDrawer,
    )
}

/** How long the archive confirmation offers Undo; matches the voice discard window. */
private const val ARCHIVE_UNDO_MS = 5_000L
