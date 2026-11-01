package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CustomizeRoute(
    preview: ChatPreviewContent,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CustomizeViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            CustomizeEffect.NavigateBack -> onBack()
        }
    }
    CustomizeScreen(state = state, onEvent = viewModel::onEvent, preview = preview, modifier = modifier)
}
