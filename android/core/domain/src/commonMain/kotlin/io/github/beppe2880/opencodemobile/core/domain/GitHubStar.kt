package io.github.beppe2880.opencodemobile.core.domain

import kotlinx.coroutines.flow.StateFlow

/** The repository of the app on GitHub, where a star helps other people find it. */
const val APP_REPOSITORY_URL = "https://github.com/beppe2880/opencode-mobile"

/** What the user did with the invitation to star the project on GitHub; kept on the device. */
enum class GitHubStarChoice {
    /** Not answered yet. */
    None,

    /** Opened the repository to leave a star. The app cannot know whether they did. */
    Starred,

    /** Closed the banner: it never comes back. The quiet places keep the invitation. */
    Dismissed,
}

data class GitHubStarState(
    val choice: GitHubStarChoice = GitHubStarChoice.None,
    /** The banner above the sessions is shown in this run of the app: its only one. */
    val bannerVisible: Boolean = false,
)

/** When the banner asks for a star: once, after the app has been used for a while. */
object GitHubStarPrompt {
    /** Different days the app was opened on. */
    const val DAYS_USED = 4

    /** Days since the app was first opened. */
    const val DAYS_SINCE_FIRST_USE = 7

    /**
     * Never answered and never shown, on the [DAYS_USED]th different day of use at least
     * [DAYS_SINCE_FIRST_USE] days after the first one. Days are counted from the epoch.
     */
    fun bannerDue(choice: GitHubStarChoice, shown: Boolean, daysUsed: Int, firstDay: Long, today: Long): Boolean =
        choice == GitHubStarChoice.None && !shown && daysUsed >= DAYS_USED && today - firstDay >= DAYS_SINCE_FIRST_USE
}

/**
 * The invitation to star the project on GitHub: a banner above the sessions once, after a while
 * of use, and quiet places (Tips & new features, Settings) that are always there.
 */
interface GitHubStarRepository {
    val state: StateFlow<GitHubStarState>

    /**
     * The session list was opened: counts today as a day of use and, when the banner is due
     * ([GitHubStarPrompt.bannerDue]), shows it for this run of the app and never again.
     */
    suspend fun recordUse()

    /** The repository was opened to leave a star; hides the banner. */
    suspend fun starred()

    /** The banner was closed: it never comes back. Ignored once a star was left. */
    suspend fun dismissBanner()
}
