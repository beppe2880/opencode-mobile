package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.lifecycle.viewModelScope
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppVersion
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.domain.WhatsNewRepository
import io.github.beppe2880.opencodemobile.core.mvi.BaseViewModel
import io.github.beppe2880.opencodemobile.feature.settings.AppInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

/**
 * The What's New page: the releases kept on the device at once, then the ones GitHub answers
 * with. Opening it marks the newest release as seen, which clears the dot of the button above the
 * session list.
 */
class WhatsNewViewModel(
    private val repository: WhatsNewRepository,
    private val appInfo: AppInfo,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : BaseViewModel<WhatsNewState, WhatsNewEvent, WhatsNewEffect>(WhatsNewState(installedVersion = appInfo.versionName)) {

    private var loadJob: Job? = null

    init {
        show(repository.history.value.releases)
        load()
    }

    override fun onEvent(event: WhatsNewEvent) {
        when (event) {
            WhatsNewEvent.Back -> sendEffect(WhatsNewEffect.NavigateBack)
            WhatsNewEvent.Refresh -> load()
            // Release notes only link GitHub; anything else is not opened.
            is WhatsNewEvent.OpenChange -> if (event.url.startsWith(GITHUB)) sendEffect(WhatsNewEffect.OpenUrl(event.url))
            WhatsNewEvent.Update -> currentState.update?.let { sendEffect(WhatsNewEffect.OpenUrl(it.url)) }
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        updateState { it.copy(loading = true, failed = false) }
        loadJob = viewModelScope.launch {
            val result = repository.refresh()
            // The days first, so the page never shows as empty in between.
            result.onSuccess(::show)
            updateState { it.copy(loading = false, failed = result.isFailure) }
        }
    }

    private fun show(releases: List<AppRelease>) {
        val installed = appInfo.versionName
        val newest = releases
            .filter { AppVersion.isUpdate(it.version, installed) }
            .maxWithOrNull(compareBy { AppVersion.parse(it.version) })
        updateState {
            it.copy(
                days = whatsNewTimeline(releases, installed, zone()),
                update = newest?.let(appInfo.updateTarget::actionFor),
            )
        }
        if (releases.isNotEmpty()) viewModelScope.launch { repository.markSeen() }
    }

    private companion object {
        const val GITHUB = "https://github.com/"
    }
}

/**
 * The "What's New" button above the session list: shown until it is hidden, and again once a
 * newer release is published; with a dot while there are releases the page was not opened for.
 * Looks for new releases at most once a day, and never while the update notice is off.
 */
class WhatsNewButtonViewModel(
    private val repository: WhatsNewRepository,
    settings: SettingsRepository,
    private val appInfo: AppInfo,
) : BaseViewModel<WhatsNewButtonState, WhatsNewButtonEvent, WhatsNewButtonEffect>(
    repository.history.value.let { WhatsNewButtonState(visible = it.buttonVisible, unseen = it.hasUnseen) },
) {

    init {
        viewModelScope.launch {
            repository.history.collect { history ->
                updateState { it.copy(visible = history.buttonVisible, unseen = history.hasUnseen) }
            }
        }
        viewModelScope.launch {
            settings.settings.map { it.updateNotice }.distinctUntilChanged().collectLatest { style ->
                if (style != UpdateNoticeStyle.Off) repository.refreshIfDue()
            }
        }
    }

    override fun onEvent(event: WhatsNewButtonEvent) {
        when (event) {
            WhatsNewButtonEvent.Open -> sendEffect(WhatsNewButtonEffect.OpenPage)
            WhatsNewButtonEvent.Hide -> viewModelScope.launch { repository.hide(appInfo.versionName) }
        }
    }
}
