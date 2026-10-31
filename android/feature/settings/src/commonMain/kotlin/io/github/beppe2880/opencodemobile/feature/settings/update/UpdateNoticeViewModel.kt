package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.UpdateRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Announces a newer release of the app while "Check for updates" is on. */
class UpdateNoticeViewModel(
    private val updates: UpdateRepository,
    settings: SettingsRepository,
    private val appInfo: AppInfo,
) : BaseViewModel<UpdateNoticeState, UpdateNoticeEvent, UpdateNoticeEffect>(UpdateNoticeState()) {

    init {
        viewModelScope.launch {
            settings.settings.map { it.checkForUpdates }.distinctUntilChanged().collectLatest { enabled ->
                val release = if (enabled) updates.availableUpdate(appInfo.versionName) else null
                updateState { it.copy(release = release, action = release?.let(appInfo.updateTarget::actionFor)) }
            }
        }
    }

    override fun onEvent(event: UpdateNoticeEvent) {
        val release = currentState.release ?: return
        when (event) {
            UpdateNoticeEvent.Open -> currentState.action?.let { sendEffect(UpdateNoticeEffect.OpenUrl(it.url)) }
            UpdateNoticeEvent.Dismiss -> {
                updateState { it.copy(release = null, action = null) }
                viewModelScope.launch { updates.dismiss(release) }
            }
        }
    }
}
