package io.github.beppe2880.opencodemobile.core.data.integration

import io.github.beppe2880.opencodemobile.api.models.IntegrationAttemptEncoded
import io.github.beppe2880.opencodemobile.api.models.IntegrationAttemptStatus
import io.github.beppe2880.opencodemobile.api.models.IntegrationConnectKeyRequest
import io.github.beppe2880.opencodemobile.api.models.IntegrationOauthConnectRequest
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.IntegrationRepository
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthAttempt
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStatus
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.deviceOAuthSupported
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.awaitSuccess
import io.github.beppe2880.opencodemobile.core.network.mapping.toJson
import io.github.beppe2880.opencodemobile.core.network.mapping.toKeyIntegration
import io.ktor.http.Url
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal class RemoteIntegrationRepository(private val clients: OpenCodeClients, private val servers: ServerRepository) : IntegrationRepository {
    override suspend fun startOAuth(serverId: String, directory: String, integrationId: String, methodId: String, answers: Map<String, FormAnswer>) = resultOf {
        require(deviceOAuthSupported(integrationId, methodId))
        val api = client(serverId).integrations
        val attempt = awaitBody {
            api.integrationOauthConnect(integrationId, IntegrationOauthConnectRequest(methodId, answers.mapValues { it.value.toJson() }), directory)
        }.data
        val id = requireNotNull(attempt.attemptID)
        try {
            val url = Url(requireNotNull(attempt.url))
            require(
                attempt.mode == IntegrationAttemptEncoded.Mode.AUTO && url.protocol.name == "https" &&
                    url.user.isNullOrEmpty() && url.password.isNullOrEmpty(),
            )
            ProviderOAuthAttempt(id, url.toString(), attempt.instructions.orEmpty())
        } catch (error: Exception) {
            withContext(NonCancellable) { runCatching { awaitSuccess { api.integrationOauthCancel(integrationId, id, directory) } } }
            throw error
        }
    }

    override suspend fun oauthStatus(serverId: String, directory: String, integrationId: String, attemptId: String) = resultOf {
        val api = client(serverId).integrations
        when (awaitBody { api.integrationOauthStatus(integrationId, attemptId, directory) }.data) {
            is IntegrationAttemptStatus.IntegrationAttemptStatusAnyOfValue -> ProviderOAuthStatus.Pending
            is IntegrationAttemptStatus.IntegrationCommandAttemptStatusAnyOf1Value -> ProviderOAuthStatus.Complete
            is IntegrationAttemptStatus.IntegrationAttemptStatusAnyOf1Value -> ProviderOAuthStatus.Failed
            is IntegrationAttemptStatus.IntegrationCommandAttemptStatusAnyOf3Value -> ProviderOAuthStatus.Expired
        }
    }

    override suspend fun cancelOAuth(serverId: String, directory: String, integrationId: String, attemptId: String) = resultOf {
        val api = client(serverId).integrations
        awaitSuccess { api.integrationOauthCancel(integrationId, attemptId, directory) }
    }
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
