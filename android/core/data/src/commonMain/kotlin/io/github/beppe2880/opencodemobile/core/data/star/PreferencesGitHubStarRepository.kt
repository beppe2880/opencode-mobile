package io.github.beppe2880.opencodemobile.core.data.star

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.GitHubStarChoice
import io.github.beppe2880.opencodemobile.core.domain.GitHubStarPrompt
import io.github.beppe2880.opencodemobile.core.domain.GitHubStarRepository
import io.github.beppe2880.opencodemobile.core.domain.GitHubStarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * The answer to the GitHub star invitation and the days the app was used, in app preferences.
 * The banner is marked as shown as soon as it appears, so it is shown in one run of the app only,
 * even when it is neither tapped nor closed.
 */
internal class PreferencesGitHubStarRepository(
    private val prefs: Settings,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : GitHubStarRepository {

    private val mutex = Mutex()

    /**
     * Other preferences were there before the invitation looked at them: the app was used with a
     * version without it. Read once, before anything in this run writes its own keys.
     */
    private val usedBefore = prefs.keys.any { !it.startsWith(KEY_PREFIX) }

    private val current = MutableStateFlow(GitHubStarState(choice = storedChoice()))

    override val state: StateFlow<GitHubStarState> = current.asStateFlow()

    override suspend fun recordUse() {
        mutex.withLock {
            val today = now() / DAY_MS
            val firstDay = prefs.getLongOrNull(KEY_FIRST_DAY) ?: firstUse(today)
            var daysUsed = prefs.getInt(KEY_DAYS_USED, 0)
            if (prefs.getLongOrNull(KEY_LAST_DAY) != today) {
                daysUsed++
                prefs.putInt(KEY_DAYS_USED, daysUsed)
                prefs.putLong(KEY_LAST_DAY, today)
            }
            val shown = prefs.getBoolean(KEY_BANNER_SHOWN, false)
            if (GitHubStarPrompt.bannerDue(current.value.choice, shown, daysUsed, firstDay, today)) {
                prefs.putBoolean(KEY_BANNER_SHOWN, true)
                current.update { it.copy(bannerVisible = true) }
            }
        }
    }

    override suspend fun starred() {
        mutex.withLock { save(GitHubStarChoice.Starred) }
    }

    override suspend fun dismissBanner() {
        mutex.withLock {
            if (current.value.choice != GitHubStarChoice.Starred) save(GitHubStarChoice.Dismissed) else current.update { it.copy(bannerVisible = false) }
        }
    }

    /**
     * Today, for a new install. Someone updating from a version without the invitation has used
     * the app for a while already: counted as due, so the banner shows at once.
     */
    private fun firstUse(today: Long): Long {
        val first = if (usedBefore) today - GitHubStarPrompt.DAYS_SINCE_FIRST_USE else today
        if (usedBefore) prefs.putInt(KEY_DAYS_USED, GitHubStarPrompt.DAYS_USED - 1)
        prefs.putLong(KEY_FIRST_DAY, first)
        return first
    }

    private fun save(choice: GitHubStarChoice) {
        prefs.putString(KEY_CHOICE, choice.name)
        current.value = GitHubStarState(choice = choice, bannerVisible = false)
    }

    private fun storedChoice(): GitHubStarChoice =
        GitHubStarChoice.entries.firstOrNull { it.name == prefs.getStringOrNull(KEY_CHOICE) } ?: GitHubStarChoice.None

    internal companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val KEY_PREFIX = "github_star_"
        const val KEY_CHOICE = "github_star_choice"
        const val KEY_FIRST_DAY = "github_star_first_day"
        const val KEY_LAST_DAY = "github_star_last_day"
        const val KEY_DAYS_USED = "github_star_days_used"
        const val KEY_BANNER_SHOWN = "github_star_banner_shown"
    }
}
