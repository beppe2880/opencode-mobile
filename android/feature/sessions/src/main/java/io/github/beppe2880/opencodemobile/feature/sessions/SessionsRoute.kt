package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
) {
    val viewModel: SessionsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

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
            is SessionsEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(context))
        }
    }

    SessionsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        selectedSessionId = selectedSessionId,
        snackbarHostState = snackbar,
    )
}
