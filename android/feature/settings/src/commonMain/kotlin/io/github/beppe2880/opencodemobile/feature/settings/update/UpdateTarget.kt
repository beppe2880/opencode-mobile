package io.github.beppe2880.opencodemobile.feature.settings.update

import io.github.beppe2880.opencodemobile.core.domain.AppRelease

/**
 * Where each app installs its updates from, chosen by the app module through
 * [io.github.beppe2880.opencodemobile.feature.settings.AppInfo].
 */
data class UpdateTarget(
    /** The GitHub release holds the installable build (the Android APK). */
    val releaseHasDownload: Boolean,
    /** Store page of the app; when set, the notice opens it instead of the GitHub release. */
    val storeUrl: String? = null,
) {
    /** What the notice offers for [release]. */
    fun actionFor(release: AppRelease): UpdateAction = when {
        storeUrl != null -> UpdateAction(UpdateActionKind.Store, storeUrl)
        releaseHasDownload -> UpdateAction(UpdateActionKind.Download, release.url)
        else -> UpdateAction(UpdateActionKind.ReleaseNotes, release.url)
    }

    companion object {
        /** The APK is attached to the GitHub release. */
        val Android = UpdateTarget(releaseHasDownload = true)

        /** Built with Xcode: the release only has notes. Set [storeUrl] once it is on the App Store. */
        val Ios = UpdateTarget(releaseHasDownload = false, storeUrl = null)
    }
}

enum class UpdateActionKind {
    /** The GitHub release page, to download the build ("View"). */
    Download,

    /** The GitHub release page, to read what changed ("Release notes"). */
    ReleaseNotes,

    /** The store page that installs the update ("Update"). */
    Store,
}

data class UpdateAction(val kind: UpdateActionKind, val url: String)
