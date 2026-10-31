package io.github.beppe2880.opencodemobile.core.data.catalog

import io.github.beppe2880.opencodemobile.api.apis.FilesystemApi
import io.github.beppe2880.opencodemobile.core.data.ConcurrentMap
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.nowMillis
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.Command
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.mapping.configuredModel
import io.github.beppe2880.opencodemobile.core.network.mapping.toDomain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class RemoteCatalogRepository(
    private val clients: OpenCodeClients,
    private val clock: () -> Long = ::nowMillis,
) : CatalogRepository {

    /**
     * Agents, models and commands rarely change and every chat needs them: they are kept per
     * server, folder and kind for [CATALOG_TTL_MS], so switching chats does not reload them.
     */
    private val memo = ConcurrentMap<String, Pair<Long, Any>>()

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> remembered(kind: String, directory: String?, load: suspend () -> T): Result<T> {
        val key = "${clients.activeIdOrNull()}|$kind|${directory.orEmpty()}"
        memo[key]?.let { (at, value) -> if (clock() - at < CATALOG_TTL_MS) return Result.success(value as T) }
        return resultOf { load() }.onSuccess { memo[key] = clock() to it }
    }

    override suspend fun agents(directory: String?): Result<List<Agent>> = remembered("agents", directory) {
        val api = clients.active().agents
        awaitBody { api.agentList(locationDirectory = directory) }.data.map { it.toDomain() }
    }

    override suspend fun models(directory: String?): Result<List<ModelInfo>> = remembered("models", directory) {
        coroutineScope {
            val client = clients.active()
            val names = async { providerNames(directory) }
            val models = awaitBody { client.models.modelList(locationDirectory = directory) }.data
            val providers = names.await()
            models
                .map { it.toDomain(providers) }
                .filter { it.enabled }
                .sortedByDescending { it.releasedAt }
        }
    }

    override suspend fun configuredModel(directory: String?): Result<ModelRef?> = remembered("config-model", directory) {
        val api = clients.active().config
        listOf(awaitBody { api.configGet(locationDirectory = directory) }.configuredModel())
    }.map { it.single() }

    override suspend fun commands(directory: String?): Result<List<Command>> = remembered("commands", directory) {
        val api = clients.active().commands
        awaitBody { api.commandList(locationDirectory = directory) }.data
            .map { Command(it.name, it.description) }
            .sortedBy { it.name }
    }

    override suspend fun findFiles(directory: String, query: String): Result<List<String>> = resultOf {
        val api = clients.active().files
        awaitBody {
            api.fsFind(query = query, locationDirectory = directory, type = FilesystemApi.TypeFsFind.FILE, limit = FILE_LIMIT.toString())
        }.data.mapNotNull { it.path }
    }

    /**
     * Provider display names; models stay usable with raw IDs when the lookup fails.
     * The response also carries provider settings such as API keys: keep only id and name.
     */
    private suspend fun providerNames(directory: String?): Map<String, String> = try {
        val api = clients.active().providers
        awaitBody { api.providerList(locationDirectory = directory) }.data
            .mapNotNull { provider -> provider.id?.let { it to provider.name } }
            .toMap()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyMap()
    }

    private companion object {
        /** Mention suggestions shown at once. */
        const val FILE_LIMIT = 20

        const val CATALOG_TTL_MS = 5 * 60_000L
    }
}
