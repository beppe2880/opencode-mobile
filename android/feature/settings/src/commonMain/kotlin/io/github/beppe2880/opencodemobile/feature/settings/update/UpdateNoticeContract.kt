package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.ReleaseSection
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle

@Immutable
data class UpdateNoticeState(
    /** The newer release to announce; null hides the banner and the sheet. */
    val release: AppRelease? = null,
    /** What the notice's main button does for [release]. */
    val action: UpdateAction? = null,
    /** How [release] is announced, from Settings. */
    val style: UpdateNoticeStyle = UpdateNoticeStyle.Window,
    /** The version of this app. */
    val installedVersion: String = "",
    /** What changed in each release since [installedVersion] that has notes, newest first. */
    val changes: List<VersionChanges> = emptyList(),
    /**
     * The sheet is open: on its own at most once per app start, with [UpdateNoticeStyle.Window]
     * and [changes] to show; on demand from the banner or "Check for updates".
     */
    val sheetVisible: Boolean = false,
    /** The sheet lists [changes]: at once when it opens as the window, else after "View changelog". */
    val changelogVisible: Boolean = false,
    /** Where "Check for updates" in Settings is. */
    val check: UpdateCheck = UpdateCheck.Idle,
) {
    /**
     * The banner above the sessions: with [UpdateNoticeStyle.Banner], and with
     * [UpdateNoticeStyle.Window] once the window was closed (or while there are no notes to show
     * in it), as a reminder until the version is skipped or installed. Never while the sheet is open.
     */
    val bannerVisible: Boolean
        get() = release != null && action != null && style != UpdateNoticeStyle.Off && !sheetVisible
}

/** "Check for updates" in Settings. */
sealed interface UpdateCheck {
    /** Not asked yet, or the update it found is in the sheet. */
    data object Idle : UpdateCheck

    /** Asking GitHub. */
    data object Checking : UpdateCheck

    /** The installed version is the newest. */
    data object UpToDate : UpdateCheck

    /** GitHub could not be reached. */
    data object Failed : UpdateCheck
}

/** The release notes of one version, read from its GitHub release. */
@Immutable
data class VersionChanges(val version: String, val sections: List<ReleaseSection>)

sealed interface UpdateNoticeEvent {
    /** The main action: download, release notes or store. */
    data object Open : UpdateNoticeEvent

    /** Skip this version: hidden until a newer one. */
    data object Dismiss : UpdateNoticeEvent

    /** Close the sheet; with a banner style the banner stays as a reminder. */
    data object Later : UpdateNoticeEvent

    /** The banner was tapped: open the sheet. */
    data object ShowSheet : UpdateNoticeEvent

    /** Show or hide what changed in the sheet. */
    data object ToggleChangelog : UpdateNoticeEvent

    /** Ask GitHub now, whatever the notice style, the daily limit or a skipped version. */
    data object CheckNow : UpdateNoticeEvent
}

sealed interface UpdateNoticeEffect {
    data class OpenUrl(val url: String) : UpdateNoticeEffect
}
