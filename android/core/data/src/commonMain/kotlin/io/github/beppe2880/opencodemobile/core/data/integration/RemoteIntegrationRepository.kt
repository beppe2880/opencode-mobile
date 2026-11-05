package io.github.beppe2880.opencodemobile.core.data.integration

import io.github.beppe2880.opencodemobile.api.models.IntegrationConnectKeyRequest
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.IntegrationRepository
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.awaitSuccess
import io.github.beppe2880.opencodemobile.core.network.mapping.toJson
import io.github.beppe2880.opencodemobile.core.network.mapping.toKeyIntegration

internal class RemoteIntegrationRepository(private val clients: OpenCodeClients, private val servers: ServerRepository) : IntegrationRepository {
    private fun client(serverId: String) = clients.forProfile(
        servers.servers.value.firstOrNull { it.id == serverId } ?: throw OpenCodeError.NotFound("Server removed"),
    )

    override suspend fun listKeys(serverId: String, directory: String) = resultOf {
        val api = client(serverId).integrations
        awaitBody { api.integrationList(locationDirectory = directory) }.data.mapNotNull { it.toKeyIntegration() }.sortedBy { it.name.lowercase() }
    }

    override suspend fun connectKey(serverId: String, directory: String, integrationId: String, key: String, answers: Map<String, FormAnswer>) = resultOf {
        val api = client(serverId).integrations
        awaitSuccess {
            api.integrationConnectKey(integrationId, IntegrationConnectKeyRequest(key, answers.mapValues { it.value.toJson() }), directory)
        }
    }
}
