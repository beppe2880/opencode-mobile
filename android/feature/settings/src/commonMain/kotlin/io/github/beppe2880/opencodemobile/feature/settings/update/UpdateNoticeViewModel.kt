package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.AppUpdate
import io.github.beppe2880.opencodemobile.core.domain.ReleaseNotes
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.domain.UpdateRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * What the app knows about updates for the whole app run, shared by the ViewModels of the
 * session list and of Settings. One instance for the whole app.
 */
class UpdateSheetSession {
    /**
     * The sheet was offered: answered or not, it does not open on its own again until the
     * process restarts. Read and written on the main thread by the ViewModels.
     */
    var shown: Boolean = false
        internal set

    private val found = MutableStateFlow<AppUpdate?>(null)

    /**
     * The newer release found by "Check for updates" in this app run: announced by the banner
     * even when it was skipped before, until it is skipped again.
     */
    internal val checked: StateFlow<AppUpdate?> = found.asStateFlow()

    internal fun check(update: AppUpdate?) {
        found.value = update
    }
}

/**
 * Announces a newer release of the app as chosen in Settings: a sheet with what changed, once per
 * app start, then a banner as a reminder; or only the banner. Nothing is checked on its own while
 * the notice is off. Without any release notes (a release saved by an older version of the app,
 * or an empty body) the window would have nothing to show: the banner stands in until the next
 * check brings them. Tapping the banner opens the sheet.
 *
 * With [automatic] false (Settings) it never looks for updates or opens the sheet on its own: only
 * "Check for updates" does, asking GitHub at once.
 */
class UpdateNoticeViewModel(
    private val updates: UpdateRepository,
    settings: SettingsRepository,
    private val appInfo: AppInfo,
    private val session: UpdateSheetSession,
    private val automatic: Boolean = true,
) : BaseViewModel<UpdateNoticeState, UpdateNoticeEvent, UpdateNoticeEffect>(UpdateNoticeState(installedVersion = appInfo.versionName)) {

    init {
        viewModelScope.launch {
            val styles = settings.settings.map { it.updateNotice }.distinctUntilChanged()
            combine(styles, session.checked) { style, checked -> style to checked }.collectLatest { (style, checked) ->
                val update = when {
                    checked != null -> checked
                    !automatic || style == UpdateNoticeStyle.Off -> null
                    else -> updates.availableUpdate(appInfo.versionName)
                }
                announce(update, style)
            }
        }
    }

    private fun announce(update: AppUpdate?, style: UpdateNoticeStyle) {
        val changes = changesOf(update)
        val window = automatic && update != null && style == UpdateNoticeStyle.Window && changes.isNotEmpty() && !session.shown
        if (window) session.shown = true
        updateState { state ->
            state.copy(
                release = update?.latest,
                action = update?.latest?.let(appInfo.updateTarget::actionFor),
                style = style,
                changes = changes,
                sheetVisible = window || (state.sheetVisible && update != null),
                // The window is there to show what changed.
                changelogVisible = if (window) true else state.changelogVisible,
            )
        }
    }

    override fun onEvent(event: UpdateNoticeEvent) {
        if (event == UpdateNoticeEvent.CheckNow) return checkNow()
        val release = currentState.release ?: return
        when (event) {
            UpdateNoticeEvent.Open -> {
                currentState.action?.let { sendEffect(UpdateNoticeEffect.OpenUrl(it.url)) }
                updateState { it.copy(sheetVisible = false) }
            }
            UpdateNoticeEvent.Later -> updateState { it.copy(sheetVisible = false) }
            UpdateNoticeEvent.ShowSheet -> {
                session.shown = true
                updateState { it.copy(sheetVisible = true, changelogVisible = false) }
            }
            UpdateNoticeEvent.ToggleChangelog -> updateState { it.copy(changelogVisible = !it.changelogVisible) }
            UpdateNoticeEvent.Dismiss -> {
                updateState { it.copy(release = null, action = null, changes = emptyList(), sheetVisible = false) }
                viewModelScope.launch {
                    updates.dismiss(release)
                    if (session.checked.value?.latest == release) session.check(null)
                }
            }
            UpdateNoticeEvent.CheckNow -> Unit
        }
    }

    private fun checkNow() {
        if (currentState.check == UpdateCheck.Checking) return
        updateState { it.copy(check = UpdateCheck.Checking) }
        viewModelScope.launch {
            updates.checkNow(appInfo.versionName).fold(
                onSuccess = { update ->
                    if (update == null) {
                        updateState { it.copy(check = UpdateCheck.UpToDate) }
                    } else {
                        // Asked for: no window of its own on top of this one later.
                        session.shown = true
                        updateState {
                            it.copy(
                                check = UpdateCheck.Idle,
                                release = update.latest,
                                action = appInfo.updateTarget.actionFor(update.latest),
                                changes = changesOf(update),
                                sheetVisible = true,
                                changelogVisible = false,
                            )
                        }
                        // The session list shows it too, as a banner once the sheet is closed.
                        session.check(update)
                    }
                },
                onFailure = { updateState { it.copy(check = UpdateCheck.Failed) } },
            )
        }
    }

    /** Versions without notes are left out of the sheet. */
    private fun changesOf(update: AppUpdate?): List<VersionChanges> = update?.releases.orEmpty()
        .map { VersionChanges(it.version, ReleaseNotes.parse(it.notes)) }
        .filter { it.sections.isNotEmpty() }
}
