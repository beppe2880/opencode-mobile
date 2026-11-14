package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import org.koin.compose.viewmodel.koinViewModel

/** The What's New page, opened from the button above the session list or from Settings. */
@Composable
fun WhatsNewRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: WhatsNewViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            WhatsNewEffect.NavigateBack -> onBack()
            is WhatsNewEffect.OpenUrl -> runCatching { uriHandler.openUri(effect.url) }
        }
    }
    WhatsNewScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}
