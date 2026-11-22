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

    private val current = MutableStateFlow(GitHubStarState(choice = storedChoice()))

    override val state: StateFlow<GitHubStarState> = current.asStateFlow()

    override suspend fun recordUse() {
        mutex.withLock {
            val today = now() / DAY_MS
            val firstDay = prefs.getLongOrNull(KEY_FIRST_DAY) ?: today.also { prefs.putLong(KEY_FIRST_DAY, it) }
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

    private fun save(choice: GitHubStarChoice) {
        prefs.putString(KEY_CHOICE, choice.name)
        current.value = GitHubStarState(choice = choice, bannerVisible = false)
    }

    private fun storedChoice(): GitHubStarChoice =
        GitHubStarChoice.entries.firstOrNull { it.name == prefs.getStringOrNull(KEY_CHOICE) } ?: GitHubStarChoice.None

    internal companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val KEY_CHOICE = "github_star_choice"
        const val KEY_FIRST_DAY = "github_star_first_day"
        const val KEY_LAST_DAY = "github_star_last_day"
        const val KEY_DAYS_USED = "github_star_days_used"
        const val KEY_BANNER_SHOWN = "github_star_banner_shown"
    }
}
