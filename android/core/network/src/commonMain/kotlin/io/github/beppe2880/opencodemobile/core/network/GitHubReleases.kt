package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Reads the latest release of the app from the public GitHub API, without authentication. It
 * has its own HTTP client: nothing of the OpenCode connections (Basic auth) reaches GitHub.
 */
class GitHubReleases(
    private val http: HttpClient = defaultClient(),
    private val latestUrl: String = LATEST_RELEASE_URL,
) {

    /**
     * The latest published release, or null when there is none (no release yet, a draft or a
     * pre-release). Throws [IOException] when GitHub cannot be reached or does not answer.
     */
    suspend fun latest(): AppRelease? {
        val response = http.get(latestUrl) {
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
        if (response.status == HttpStatusCode.NotFound) return null
        if (!response.status.isSuccess()) throw IOException("GitHub answered ${response.status.value}")
        val release = try {
            ReleaseJson.decodeFromString(Release.serializer(), response.bodyAsText())
        } catch (e: SerializationException) {
            throw IOException("Unexpected GitHub release", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("Unexpected GitHub release", e)
        }
        if (release.draft || release.prerelease) return null
        // Only GitHub pages are opened from the banner.
        if (!release.htmlUrl.startsWith(RELEASES_PAGE)) return null
        return AppRelease(version = release.tagName.removePrefix("v").removePrefix("V"), url = release.htmlUrl)
    }

    @Serializable
    private class Release(
        @SerialName("tag_name") val tagName: String,
        @SerialName("html_url") val htmlUrl: String,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
    )

    private companion object {
        const val REPOSITORY = "beppe2880/opencode-mobile"
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"
        const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases/"
        const val USER_AGENT = "opencode-mobile"

        val ReleaseJson = Json { ignoreUnknownKeys = true }

        fun defaultClient(): HttpClient = HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
                requestTimeoutMillis = 20_000
            }
        }
    }
}
