package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.domain.AppVersion
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Reads the recent releases of the app from the public GitHub API, without authentication. It
 * has its own HTTP client: nothing of the OpenCode connections (Basic auth) reaches GitHub.
 */
class GitHubReleases(
    private val http: HttpClient = defaultClient(),
    private val releasesUrl: String = RELEASES_URL,
) {

    /**
     * The [RECENT_COUNT] most recent published releases with their notes, newest version first,
     * in one request; empty when there is none yet. Drafts and pre-releases are left out. Throws
     * [IOException] when GitHub cannot be reached or does not answer.
     */
    suspend fun recent(): List<AppRelease> = try {
        fetchRecent()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Only the kind of failure: the caller keeps the last known release.
        logWarning(TAG, "Update check failed: ${e::class.simpleName}")
        throw e
    }

    private suspend fun fetchRecent(): List<AppRelease> {
        val response = http.get(releasesUrl) {
            parameter("per_page", RECENT_COUNT)
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
        if (response.status == HttpStatusCode.NotFound) return emptyList()
        if (!response.status.isSuccess()) throw IOException("GitHub answered ${response.status.value}")
        val releases = try {
            ReleaseJson.decodeFromString(ListSerializer(Release.serializer()), response.bodyAsText())
        } catch (e: SerializationException) {
            throw IOException("Unexpected GitHub releases", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("Unexpected GitHub releases", e)
        }
        return releases
            .filterNot { it.draft || it.prerelease }
            // Only GitHub pages are opened from the notice.
            .filter { it.htmlUrl.startsWith(RELEASES_PAGE) }
            .mapNotNull { release ->
                val version = release.tagName.removePrefix("v").removePrefix("V")
                AppVersion.parse(version)?.let { it to AppRelease(version, release.htmlUrl, release.body.orEmpty()) }
            }
            .sortedByDescending { it.first }
            .map { it.second }
    }

    @Serializable
    private class Release(
        @SerialName("tag_name") val tagName: String,
        @SerialName("html_url") val htmlUrl: String,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val body: String? = null,
    )

    companion object {
        /** Releases read by one check: enough for anyone who skipped a few. */
        const val RECENT_COUNT = 10
        private const val REPOSITORY = "beppe2880/opencode-mobile"
        private const val RELEASES_URL = "https://api.github.com/repos/$REPOSITORY/releases"
        private const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases/"
        private const val USER_AGENT = "opencode-mobile"
        private const val TAG = "GitHubReleases"

        private val ReleaseJson = Json { ignoreUnknownKeys = true }

        private fun defaultClient(): HttpClient = HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
                requestTimeoutMillis = 20_000
            }
        }
    }
}
