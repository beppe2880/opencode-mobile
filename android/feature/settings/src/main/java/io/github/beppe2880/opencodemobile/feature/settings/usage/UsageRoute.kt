package io.github.beppe2880.opencodemobile.feature.settings.usage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UsageRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: UsageViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            UsageEffect.NavigateBack -> onBack()
        }
    }
    UsageScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}
