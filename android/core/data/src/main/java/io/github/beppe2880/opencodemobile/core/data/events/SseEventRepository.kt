package io.github.beppe2880.opencodemobile.core.data.events

import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.cache.ListCache
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClient
import io.github.beppe2880.opencodemobile.core.network.mapping.toDomain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

/**
 * One shared `/api/event` connection for the active server while the app is visible (or
 * [keepAliveInBackground] is on) and at least one collector exists. Failed, stalled or closed streams reconnect with jittered
 * exponential backoff, cut short when a network becomes available; every successful
 * (re)connect emits [ServerEvent.Connected] so screens reload. A 401 sets
 * [ConnectionState.Unauthorized] and stops retrying until the active server, its address or
 * its secret changes (or the stream restarts, for example when the app returns to the foreground).
 * A session deleted elsewhere also leaves the server's saved session lists, so it does not come
 * back from [lists] at the next start.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SseEventRepository(
    servers: ServerRepository,
    private val clients: OpenCodeClients,
    visibility: AppVisibility,
    private val network: NetworkMonitor,
    scope: CoroutineScope,
    private val backoff: Backoff = Backoff(),
    stopTimeoutMillis: Long = 5_000,
    private val readTimeoutMillis: Long = OpenCodeClient.STREAM_READ_TIMEOUT_MILLIS,
    private val lists: ListCache = ListCache.None,
) : EventRepository {

    private val _connection = MutableStateFlow(ConnectionState.Disconnected)
    override val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val keepAlive = MutableStateFlow(false)

    override fun keepAliveInBackground(enabled: Boolean) {
        keepAlive.value = enabled
    }

    override val events: Flow<ServerEvent> =
        combine(servers.activeServer, visibility.isVisible, keepAlive) { server, visible, alive ->
            server?.takeIf { visible || alive }
        }
            .distinctUntilChanged(::sameConnection)
            .flatMapLatest { server -> if (server == null) emptyFlow() else reconnecting(server) }
            .shareIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis))

    private fun reconnecting(server: ServerProfile): Flow<ServerEvent> = channelFlow {
        // Conflated: several network changes during one wait cut it only once.
        val networkBack = Channel<Unit>(Channel.CONFLATED)
        launch { network.networkAvailable.collect { networkBack.trySend(Unit) } }
        var attempt = 0
        while (true) {
            _connection.value = if (attempt == 0) ConnectionState.Connecting else ConnectionState.Reconnecting
            val failure = try {
                connectOnce(server, onOpen = { attempt = 0 }).collect { send(it) }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e
            }
            if (failure is OpenCodeError.Unauthorized) {
                _connection.value = ConnectionState.Unauthorized
                awaitCancellation()
            }
            // Any other failure, or the server closing the stream, schedules the next attempt.
            _connection.value = ConnectionState.Reconnecting
            withTimeoutOrNull(backoff.delayMillis(attempt++)) { networkBack.receive() }
        }
    }.onCompletion { _connection.value = ConnectionState.Disconnected }

    private fun connectOnce(server: ServerProfile, onOpen: () -> Unit): Flow<ServerEvent> = flow {
        var open = false
        clients.forProfile(server).events(readTimeoutMillis).collect { event ->
            if (!open) {
                open = true
                onOpen()
                _connection.value = ConnectionState.Connected
                emit(ServerEvent.Connected)
            }
            val mapped = event.toDomain()?.takeUnless { it == ServerEvent.Connected } ?: return@collect
            if (mapped is ServerEvent.SessionChanged && mapped.deleted) lists.removeSession(server.id, mapped.sessionId)
            emit(mapped)
        }
    }

    private fun sameConnection(a: ServerProfile?, b: ServerProfile?): Boolean =
        a?.id == b?.id && a?.baseUrl == b?.baseUrl && a?.secret == b?.secret
}

/**
 * Exponential reconnect delay: [initialMillis] doubled per failed attempt, capped at [maxMillis],
 * with "equal jitter" (a random value between half and all of it) so clients do not retry in step.
 */
internal class Backoff(
    private val initialMillis: Long = 500,
    private val maxMillis: Long = 10_000,
    private val random: Random = Random.Default,
) {
    fun delayMillis(attempt: Int): Long {
        val base = (initialMillis shl attempt.coerceAtMost(MAX_SHIFT)).coerceAtMost(maxMillis)
        val half = base / 2
        return half + random.nextLong(base - half + 1)
    }

    private companion object {
        const val MAX_SHIFT = 16
    }
}
