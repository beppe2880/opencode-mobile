package io.github.beppe2880.opencodemobile.feature.connect

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.platform.rememberTextClipboard
import io.github.beppe2880.opencodemobile.core.ui.text.asString
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConnectRoute(
    onConnected: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
    onTryDemo: (() -> Unit)? = null,
) {
    val viewModel: ConnectViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val focus = remember { FocusRequester() }
    val clipboard = rememberTextClipboard()
    val scope = rememberCoroutineScope()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            ConnectEffect.Connected -> onConnected()
            ConnectEffect.FocusSecret -> runCatching { focus.requestFocus() }
            is ConnectEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString())
        }
    }

    ConnectScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onPaste = {
            scope.launch {
                viewModel.onEvent(ConnectEvent.LinkPasted(clipboard.getText()))
            }
        },
        modifier = modifier,
        showBack = showBack,
        onBack = onBack,
        onTryDemo = onTryDemo,
        snackbarHostState = snackbar,
        secretFocusRequester = focus,
    )
}
