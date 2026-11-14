package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * The releases of the app known on this device and what the user did with the "What's New"
 * button of the session list.
 */
data class WhatsNewHistory(
    /** The releases found by the last successful request, newest version first; kept for offline use. */
    val releases: List<AppRelease> = emptyList(),
    /** The newest version there was when the What's New page was last opened; null before that. */
    val seenVersion: String? = null,
    /** The newest version there was when the button was hidden; null while it was never hidden. */
    val hiddenVersion: String? = null,
) {
    /** The newest known version; null when no release is known. */
    val latestVersion: String? get() = WhatsNew.latestVersion(releases)

    /** There is a release the user has not seen on the page yet: the button shows its dot. */
    val hasUnseen: Boolean get() = WhatsNew.hasUnseen(latestVersion, seenVersion)

    /** The button is shown above the sessions: never hidden, or a newer release came out since. */
    val buttonVisible: Boolean get() = WhatsNew.buttonVisible(latestVersion, hiddenVersion)
}

/** The decisions of the "What's New" button, on versions alone. */
object WhatsNew {

    /** The highest version among [releases]; null when there is none with a version. */
    fun latestVersion(releases: List<AppRelease>): String? = releases
        .mapNotNull { release -> AppVersion.parse(release.version)?.let { it to release.version } }
        .maxByOrNull { it.first }
        ?.second

    /**
     * [latest] was published after the page was last opened with [seen] as the newest. Before the
     * page was ever opened, any known release is new to the user.
     */
    fun hasUnseen(latest: String?, seen: String?): Boolean {
        val newest = latest?.let(AppVersion::parse) ?: return false
        val known = seen?.let(AppVersion::parse) ?: return true
        return newest > known
    }

    /**
     * The button was never hidden, or [latest] is newer than [hidden], the newest version when it
     * was hidden. Hidden while no release was known, it comes back with the first one found.
     */
    fun buttonVisible(latest: String?, hidden: String?): Boolean {
        if (hidden == null) return true
        val newest = latest?.let(AppVersion::parse) ?: return false
        val hiddenAt = AppVersion.parse(hidden) ?: return true
        return newest > hiddenAt
    }
}

/** The release history of the app from GitHub, for the What's New page and its button. */
interface WhatsNewRepository {
    /** The releases kept on this device with what the user saw and hid; survives restarts. */
    val history: StateFlow<WhatsNewHistory>

    /**
     * Asks GitHub for the recent releases now and keeps them for offline use. A failure when
     * GitHub cannot be reached: the releases kept before stay in [history].
     */
    suspend fun refresh(): Result<List<AppRelease>>

    /** [refresh] at most once a day, for the button's dot; a failure is retried at the next call. */
    suspend fun refreshIfDue()

    /** The page was opened: the newest known release is seen, which clears the button's dot. */
    suspend fun markSeen()

    /**
     * Hides the button until a release newer than the newest known one is published;
     * [installedVersion] stands in while no release is known.
     */
    suspend fun hide(installedVersion: String)
}
