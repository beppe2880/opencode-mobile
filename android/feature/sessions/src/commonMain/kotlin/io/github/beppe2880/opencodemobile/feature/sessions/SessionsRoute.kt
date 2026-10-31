package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.text.asString
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
    /** The offline demo: sample sessions, no server behind them. */
    demo: Boolean = false,
) {
    val viewModel: SessionsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

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
        }
    }

    SessionsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        selectedSessionId = selectedSessionId,
        snackbarHostState = snackbar,
        banner = banner,
        demo = demo,
    )
}
