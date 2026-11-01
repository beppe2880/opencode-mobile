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
    /** The sheet is open: at most once per app start, with [UpdateNoticeStyle.Window] and [changes] to show. */
    val sheetVisible: Boolean = false,
) {
    /** The banner, or in place of the window while no release has notes to show. */
    val bannerVisible: Boolean
        get() = release != null && action != null &&
            (style == UpdateNoticeStyle.Banner || (style == UpdateNoticeStyle.Window && changes.isEmpty()))
}

/** The release notes of one version, read from its GitHub release. */
@Immutable
data class VersionChanges(val version: String, val sections: List<ReleaseSection>)

sealed interface UpdateNoticeEvent {
    /** The main action: download, release notes or store. */
    data object Open : UpdateNoticeEvent

    /** Skip this version: hidden until a newer one. */
    data object Dismiss : UpdateNoticeEvent

    /** Close the sheet until the next app start. */
    data object Later : UpdateNoticeEvent
}

sealed interface UpdateNoticeEffect {
    data class OpenUrl(val url: String) : UpdateNoticeEffect
}
