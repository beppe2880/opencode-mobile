package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.ReleaseNotes
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.domain.UpdateRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The update sheet is offered once per app start: answered or not, it does not come back until
 * the process restarts. One instance for the whole app.
 */
class UpdateSheetSession {
    /** Read and written on the main thread by the ViewModels. */
    var shown: Boolean = false
        internal set
}

/**
 * Announces a newer release of the app as chosen in Settings: a sheet with what changed, once per
 * app start, or a banner. Nothing is checked while the notice is off.
 */
class UpdateNoticeViewModel(
    private val updates: UpdateRepository,
    settings: SettingsRepository,
    private val appInfo: AppInfo,
    private val session: UpdateSheetSession,
) : BaseViewModel<UpdateNoticeState, UpdateNoticeEvent, UpdateNoticeEffect>(UpdateNoticeState(installedVersion = appInfo.versionName)) {

    init {
        viewModelScope.launch {
            settings.settings.map { it.updateNotice }.distinctUntilChanged().collectLatest { style ->
                val update = if (style == UpdateNoticeStyle.Off) null else updates.availableUpdate(appInfo.versionName)
                val showSheet = update != null && style == UpdateNoticeStyle.Window && !session.shown
                if (showSheet) session.shown = true
                updateState { state ->
                    state.copy(
                        release = update?.latest,
                        action = update?.latest?.let(appInfo.updateTarget::actionFor),
                        style = style,
                        changes = update?.releases.orEmpty().map { VersionChanges(it.version, ReleaseNotes.parse(it.notes)) },
                        sheetVisible = showSheet || (state.sheetVisible && update != null && style == UpdateNoticeStyle.Window),
                    )
                }
            }
        }
    }

    override fun onEvent(event: UpdateNoticeEvent) {
        val release = currentState.release ?: return
        when (event) {
            UpdateNoticeEvent.Open -> {
                currentState.action?.let { sendEffect(UpdateNoticeEffect.OpenUrl(it.url)) }
                updateState { it.copy(sheetVisible = false) }
            }
            UpdateNoticeEvent.Later -> updateState { it.copy(sheetVisible = false) }
            UpdateNoticeEvent.Dismiss -> {
                updateState { it.copy(release = null, action = null, changes = emptyList(), sheetVisible = false) }
                viewModelScope.launch { updates.dismiss(release) }
            }
        }
    }
}
