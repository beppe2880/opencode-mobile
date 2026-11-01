package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.feature.settings.updatedBy
import kotlinx.coroutines.launch

/** The chat customizer: a preview drawn from the saved settings, and the options of the tapped part. */
class CustomizeViewModel(
    private val settings: SettingsRepository,
) : BaseViewModel<CustomizeState, CustomizeEvent, CustomizeEffect>(CustomizeState(settings = settings.settings.value)) {

    init {
        viewModelScope.launch { settings.settings.collect { s -> updateState { it.copy(settings = s) } } }
    }

    override fun onEvent(event: CustomizeEvent) {
        when (event) {
            CustomizeEvent.Back -> sendEffect(CustomizeEffect.NavigateBack)
            is CustomizeEvent.Select -> updateState { it.copy(selected = event.element) }
            CustomizeEvent.CloseOptions -> updateState { it.copy(selected = null) }
            is CustomizeEvent.MarksChanged -> updateState { it.copy(marks = event.visible) }
            // Saved right away, like in the settings list: the preview follows the saved settings.
            is CustomizeEvent.Setting -> viewModelScope.launch { settings.update { it.updatedBy(event.event) ?: it } }
        }
    }
}
