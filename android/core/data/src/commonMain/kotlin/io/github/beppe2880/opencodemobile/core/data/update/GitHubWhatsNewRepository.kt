package io.github.beppe2880.opencodemobile.core.data.update

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.WhatsNewHistory
import io.github.beppe2880.opencodemobile.core.domain.WhatsNewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * The release history from [fetchReleases] (the GitHub API, one request for the recent releases),
 * kept in [prefs] after every successful request so the What's New page also opens offline, with
 * the versions the user saw on the page and hid the button at. It has its own keys: the update
 * check ([GitHubUpdateRepository]) keeps fewer releases and its own daily limit.
 */
internal class GitHubWhatsNewRepository(
    private val fetchReleases: suspend () -> List<AppRelease>,
    private val prefs: Settings,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : WhatsNewRepository {

    private val mutex = Mutex()

    private val state = MutableStateFlow(
        WhatsNewHistory(
            releases = stored(),
            seenVersion = prefs.getStringOrNull(KEY_SEEN),
            hiddenVersion = prefs.getStringOrNull(KEY_HIDDEN),
        ),
    )

    override val history: StateFlow<WhatsNewHistory> = state.asStateFlow()

    override suspend fun refresh(): Result<List<AppRelease>> = mutex.withLock { fetch() }

    override suspend fun refreshIfDue() {
        mutex.withLock { if (checkDue()) fetch() }
    }

    override suspend fun markSeen() {
        mutex.withLock {
            val latest = state.value.latestVersion
            if (latest != null && latest != state.value.seenVersion) {
                prefs.putString(KEY_SEEN, latest)
                state.update { it.copy(seenVersion = latest) }
            }
        }
    }

    override suspend fun hide(installedVersion: String) {
        mutex.withLock {
            val hidden = state.value.latestVersion ?: installedVersion
            prefs.putString(KEY_HIDDEN, hidden)
            state.update { it.copy(hiddenVersion = hidden) }
        }
    }

    private suspend fun fetch(): Result<List<AppRelease>> {
        val releases = try {
            fetchReleases()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Not counted as the day's request: the next one tries again.
            return Result.failure(e)
        }
        prefs.putLong(KEY_CHECKED_AT, now())
        if (releases.isEmpty()) {
            prefs.remove(KEY_RELEASES)
        } else {
            val stored = releases.map { StoredRelease(it.version, it.url, it.notes, it.publishedAt) }
            prefs.putString(KEY_RELEASES, StoreJson.encodeToString(StoredReleases, stored))
        }
        state.update { it.copy(releases = releases) }
        return Result.success(releases)
    }

    private fun checkDue(): Boolean {
        val last = prefs.getLongOrNull(KEY_CHECKED_AT) ?: return true
        val elapsed = now() - last
        // A clock moved back counts as due rather than postponing requests indefinitely.
        return elapsed !in 0 until CHECK_INTERVAL_MS
    }

    private fun stored(): List<AppRelease> {
        val json = prefs.getStringOrNull(KEY_RELEASES) ?: return emptyList()
        return try {
            StoreJson.decodeFromString(StoredReleases, json).map { AppRelease(it.version, it.url, it.notes, it.publishedAt) }
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    @Serializable
    private class StoredRelease(val version: String, val url: String, val notes: String = "", val publishedAt: Long? = null)

    internal companion object {
        const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
        const val KEY_CHECKED_AT = "whats_new_checked_at"
        const val KEY_RELEASES = "whats_new_releases"
        const val KEY_SEEN = "whats_new_seen_version"
        const val KEY_HIDDEN = "whats_new_hidden_version"
        private val StoreJson = Json { ignoreUnknownKeys = true }
        private val StoredReleases = ListSerializer(StoredRelease.serializer())
    }
}
