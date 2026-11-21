package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.network.BasicCredentials
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClient
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClientFactory
import io.github.beppe2880.opencodemobile.core.network.toOpenCodeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Caches one authenticated client per saved server, rebuilt when its address or secret changes. */
internal class OpenCodeClients(
    private val factory: OpenCodeClientFactory,
    private val servers: ServerRepository,
) {
    private val cache = ConcurrentMap<String, Pair<ServerProfile, OpenCodeClient>>()

    fun forProfile(profile: ServerProfile): OpenCodeClient {
        val previous = cache[profile.id]
        previous?.let { (cached, client) ->
            if (cached.baseUrl == profile.baseUrl && cached.secret == profile.secret) return client
        }
        val client = factory.create(profile.baseUrl, BasicCredentials(profile.username, profile.secret))
        cache[profile.id] = profile to client
        // The old address or secret is no longer valid; its calls fail like a dropped connection.
        previous?.second?.close()
        return client
    }

    /** Client for the active server; fails with [OpenCodeError.Unauthorized] when none is selected. */
    fun active(): OpenCodeClient = forProfile(activeProfile())

    /** Resolves an explicitly saved server rather than whichever one navigation made active. */
    fun forServer(serverId: String): OpenCodeClient = forProfile(
        servers.servers.value.firstOrNull { it.id == serverId } ?: throw OpenCodeError.NotFound("Server is no longer saved"),
    )

    /** The active server profile; fails like [active] when none is selected. */
    fun activeProfile(): ServerProfile =
        servers.activeServer.value ?: throw OpenCodeError.Unauthorized("No server selected")

    fun activeIdOrNull(): String? = servers.activeServer.value?.id
}

/**
 * Runs a remote operation, reporting every failure except cancellation as an [OpenCodeError].
 * Runs off the main thread: decoding and mapping transcripts or the model catalog is heavy
 * enough to drop frames while a chat opens.
 */
internal suspend fun <T> resultOf(block: suspend () -> T): Result<T> = try {
    Result.success(withContext(Dispatchers.Default) { block() })
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toOpenCodeError())
}
