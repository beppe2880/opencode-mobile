package io.github.beppe2880.opencodemobile.core.data.server

import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.ConnectTarget
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.network.BasicCredentials
import io.github.beppe2880.opencodemobile.core.network.ConnectLinkParser
import io.github.beppe2880.opencodemobile.core.network.OpenCodeClientFactory
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.mapping.toDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Saved servers backed by a [ServerStore]. Credentials are validated against the server before
 * anything is saved; saving a server whose address is already known updates that entry.
 * The store is read on [ioDispatcher] in [scope] (it may touch the Keystore); [loaded] turns true
 * when done and changes wait for it.
 */
internal class StoredServerRepository(
    private val store: ServerStore,
    private val factory: OpenCodeClientFactory,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    scope: CoroutineScope = CoroutineScope(ioDispatcher),
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    /** Drops data kept for a removed server, such as cached transcripts. */
    private val onRemoved: suspend (serverId: String) -> Unit = {},
) : ServerRepository {

    private val mutex = Mutex()
    private var stored = StoredServers()

    private val _servers = MutableStateFlow(emptyList<ServerProfile>())
    override val servers: StateFlow<List<ServerProfile>> = _servers.asStateFlow()

    private val _activeServer = MutableStateFlow<ServerProfile?>(null)
    override val activeServer: StateFlow<ServerProfile?> = _activeServer.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    override val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        scope.launch(ioDispatcher) {
            try {
                val saved = store.load().let { raw ->
                    raw.copy(activeId = raw.activeId?.takeIf { id -> raw.profiles.any { it.id == id } })
                }
                mutex.withLock { publish(saved) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // An unreadable store (for example a Keystore failure) must not keep
                // the app from starting; the user pairs again instead.
            } finally {
                _loaded.value = true
            }
        }
    }

    override suspend fun probe(baseUrl: String, password: String?): Result<ServerInfo> = resultOf {
        fetchInfo(normalize(baseUrl), password?.let { BasicCredentials(password = it) })
    }

    override suspend fun pair(baseUrl: String, code: String, name: String?): Result<ServerProfile> = resultOf {
        val url = normalize(baseUrl)
        val token = factory.redeemPairing(url, code.trim())
        fetchInfo(url, BasicCredentials(password = token))
        saveAndActivate(url, token, CredentialKind.PairingToken, name)
    }

    override suspend fun addWithPassword(baseUrl: String, password: String, name: String?): Result<ServerProfile> = resultOf {
        val url = normalize(baseUrl)
        fetchInfo(url, BasicCredentials(password = password))
        saveAndActivate(url, password, CredentialKind.Password, name)
    }

    override suspend fun activate(serverId: String) = update { current ->
        if (current.profiles.none { it.id == serverId }) current else current.copy(activeId = serverId)
    }

    override suspend fun rename(serverId: String, name: String) = update { current ->
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@update current
        current.copy(profiles = current.profiles.map { if (it.id == serverId) it.copy(name = trimmed) else it })
    }

    override suspend fun remove(serverId: String) {
        update { current ->
            val remaining = current.profiles.filterNot { it.id == serverId }
            val activeId = current.activeId.takeIf { it != serverId } ?: remaining.maxByOrNull { it.addedAt }?.id
            StoredServers(remaining, activeId)
        }
        onRemoved(serverId)
    }

    override fun parseConnectTarget(raw: String): ConnectTarget? = ConnectLinkParser.parse(raw)

    private suspend fun fetchInfo(baseUrl: String, credentials: BasicCredentials?): ServerInfo {
        val api = factory.create(baseUrl, credentials).server
        return awaitBody { api.serverInfo() }.toDomain()
    }

    private suspend fun saveAndActivate(
        baseUrl: String,
        secret: String,
        credential: CredentialKind,
        name: String?,
    ): ServerProfile {
        lateinit var saved: ServerProfile
        update { current ->
            val existing = current.profiles.firstOrNull { it.baseUrl == baseUrl }
            val label = name?.trim()?.takeIf { it.isNotEmpty() }
            saved = existing?.copy(name = label ?: existing.name, secret = secret, credential = credential)
                ?: ServerProfile(
                    id = newId(),
                    name = label ?: baseUrl.substringAfter("://"),
                    baseUrl = baseUrl,
                    secret = secret,
                    credential = credential,
                    addedAt = clock(),
                )
            StoredServers(current.profiles.filterNot { it.id == saved.id } + saved, saved.id)
        }
        return saved
    }

    private suspend fun update(transform: (StoredServers) -> StoredServers) {
        _loaded.first { it }
        mutex.withLock {
            val next = transform(stored)
            if (next == stored) return@withLock
            withContext(ioDispatcher) { store.save(next) }
            publish(next)
        }
    }

    private fun publish(next: StoredServers) {
        stored = next
        _servers.value = next.sortedProfiles()
        _activeServer.value = next.activeProfile()
    }

    private fun normalize(baseUrl: String): String =
        ConnectLinkParser.normalizeBaseUrl(baseUrl) ?: throw OpenCodeError.Unreachable("Invalid server address")

    private fun StoredServers.sortedProfiles() = profiles.sortedByDescending { it.addedAt }
    private fun StoredServers.activeProfile() = profiles.firstOrNull { it.id == activeId }
}
