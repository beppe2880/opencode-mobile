package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.text.asString
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** A shell on the server in [directory]; leaving keeps it running for the next visit. */
@Composable
fun TerminalRoute(directory: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: TerminalViewModel = koinViewModel(key = "terminal-$directory") { parametersOf(directory) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            TerminalEffect.NavigateBack -> onBack()
            is TerminalEffect.ShowMessage -> snackbar.showSnackbar(effect.message.asString(context))
        }
    }
    TerminalScreen(
        state = state,
        surface = viewModel.surface,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        snackbarHostState = snackbar,
    )
}
