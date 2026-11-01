package io.github.beppe2880.opencodemobile.core.data.update

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppUpdate
import io.github.beppe2880.opencodemobile.core.domain.AppVersion
import io.github.beppe2880.opencodemobile.core.domain.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Checks [fetchReleases] (the GitHub API, one request for the recent releases) at most once
 * every [CHECK_INTERVAL_MS] and keeps the releases found in [prefs], with the version dismissed
 * by the user. A check that fails is not counted, so an offline launch is retried at the next one.
 */
internal class GitHubUpdateRepository(
    private val fetchReleases: suspend () -> List<AppRelease>,
    private val prefs: Settings,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : UpdateRepository {

    private val mutex = Mutex()

    override suspend fun availableUpdate(installedVersion: String): AppUpdate? = mutex.withLock {
        // Builds without a release version (local or CI builds) never look for updates.
        if (AppVersion.parse(installedVersion) == null) return null
        val known = if (checkDue()) check() else stored()
        val newer = known
            .filter { AppVersion.isUpdate(it.version, installedVersion) }
            .sortedByDescending { AppVersion.parse(it.version) }
        val latest = newer.firstOrNull()?.takeUnless(::isDismissed) ?: return null
        AppUpdate(latest, newer)
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

    private suspend fun check(): List<AppRelease> {
        val releases = try {
            fetchReleases()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return stored()
        }
        prefs.putLong(KEY_CHECKED_AT, now())
        prefs.remove(KEY_VERSION)
        prefs.remove(KEY_URL)
        if (releases.isEmpty()) {
            prefs.remove(KEY_RELEASES)
        } else {
            val stored = releases.map { StoredRelease(it.version, it.url, it.notes) }
            prefs.putString(KEY_RELEASES, StoreJson.encodeToString(StoredReleases, stored))
        }
        return releases
    }

    private fun stored(): List<AppRelease> {
        val json = prefs.getStringOrNull(KEY_RELEASES)
        if (json == null) {
            // Saved by versions that only kept the latest release, without its notes.
            val version = prefs.getStringOrNull(KEY_VERSION) ?: return emptyList()
            val url = prefs.getStringOrNull(KEY_URL) ?: return emptyList()
            return listOf(AppRelease(version, url))
        }
        return try {
            StoreJson.decodeFromString(StoredReleases, json).map { AppRelease(it.version, it.url, it.notes) }
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private fun isDismissed(release: AppRelease): Boolean {
        val dismissed = prefs.getStringOrNull(KEY_DISMISSED) ?: return false
        val dismissedVersion = AppVersion.parse(dismissed) ?: return false
        return AppVersion.parse(release.version)?.let { it <= dismissedVersion } ?: false
    }

    @Serializable
    private class StoredRelease(val version: String, val url: String, val notes: String = "")

    internal companion object {
        const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
        const val KEY_CHECKED_AT = "update_checked_at"
        const val KEY_RELEASES = "update_releases"
        const val KEY_VERSION = "update_latest_version"
        const val KEY_URL = "update_latest_url"
        const val KEY_DISMISSED = "update_dismissed_version"
        private val StoreJson = Json { ignoreUnknownKeys = true }
        private val StoredReleases = ListSerializer(StoredRelease.serializer())
    }
}
