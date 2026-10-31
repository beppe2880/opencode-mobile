package io.github.beppe2880.opencodemobile.core.data.update

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppVersion
import io.github.beppe2880.opencodemobile.core.domain.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * Checks [fetchLatest] (the GitHub API) at most once every [CHECK_INTERVAL_MS] and keeps the
 * release found in [prefs], with the version dismissed by the user. A check that fails is not
 * counted, so an offline launch is retried at the next one.
 */
internal class GitHubUpdateRepository(
    private val fetchLatest: suspend () -> AppRelease?,
    private val prefs: Settings,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : UpdateRepository {

    private val mutex = Mutex()

    override suspend fun availableUpdate(installedVersion: String): AppRelease? = mutex.withLock {
        // Builds without a release version (local or CI builds) never look for updates.
        if (AppVersion.parse(installedVersion) == null) return null
        val latest = if (checkDue()) check() else stored()
        latest?.takeIf { AppVersion.isUpdate(it.version, installedVersion) && !isDismissed(it) }
    }

    override suspend fun dismiss(release: AppRelease) = mutex.withLock {
        prefs.putString(KEY_DISMISSED, release.version)
    }

    private fun checkDue(): Boolean {
        val last = prefs.getLongOrNull(KEY_CHECKED_AT) ?: return true
        val elapsed = now() - last
        // A clock moved back counts as due rather than postponing checks indefinitely.
        return elapsed !in 0 until CHECK_INTERVAL_MS
    }

    private suspend fun check(): AppRelease? {
        val latest = try {
            fetchLatest()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return stored()
        }
        prefs.putLong(KEY_CHECKED_AT, now())
        if (latest == null) {
            prefs.remove(KEY_VERSION)
            prefs.remove(KEY_URL)
        } else {
            prefs.putString(KEY_VERSION, latest.version)
            prefs.putString(KEY_URL, latest.url)
        }
        return latest
    }

    private fun stored(): AppRelease? {
        val version = prefs.getStringOrNull(KEY_VERSION) ?: return null
        val url = prefs.getStringOrNull(KEY_URL) ?: return null
        return AppRelease(version, url)
    }

    private fun isDismissed(release: AppRelease): Boolean {
        val dismissed = prefs.getStringOrNull(KEY_DISMISSED) ?: return false
        val dismissedVersion = AppVersion.parse(dismissed) ?: return false
        return AppVersion.parse(release.version)?.let { it <= dismissedVersion } ?: false
    }

    internal companion object {
        const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
        const val KEY_CHECKED_AT = "update_checked_at"
        const val KEY_VERSION = "update_latest_version"
        const val KEY_URL = "update_latest_url"
        const val KEY_DISMISSED = "update_dismissed_version"
    }
}
