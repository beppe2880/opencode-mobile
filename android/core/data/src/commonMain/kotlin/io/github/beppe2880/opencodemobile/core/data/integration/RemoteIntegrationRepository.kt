package io.github.beppe2880.opencodemobile.core.data.integration

import io.github.beppe2880.opencodemobile.api.models.CredentialCreateInput
import io.github.beppe2880.opencodemobile.api.models.CredentialOAuth
import io.github.beppe2880.opencodemobile.api.models.CredentialValue
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
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStage
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStatus
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.deviceOAuthSupported
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.awaitSuccess
import io.github.beppe2880.opencodemobile.core.network.mapping.toJson
import io.github.beppe2880.opencodemobile.core.network.mapping.toKeyIntegration
import io.ktor.http.Url
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteIntegrationRepository(
    private val clients: OpenCodeClients,
    private val servers: ServerRepository,
    private val browser: BrowserOAuth? = null,
) : IntegrationRepository {
    override suspend fun connectBrowserOAuth(
        serverId: String,
        directory: String,
        integrationId: String,
        methodId: String,
        openBrowser: suspend (String) -> Unit,
    ) = resultOf {
        oauthStep(ProviderOAuthStage.Preparation) {
            val provider = OpenAiBrowser
            require(integrationId == provider.integrationId && methodId == provider.methodId)
            val profile = servers.servers.value.firstOrNull { it.id == serverId } ?: throw OpenCodeError.NotFound("Server removed")
            require(protectedOAuthTransport(profile.baseUrl))
            val targetClient = clients.forProfile(profile)
            val oauth = requireNotNull(browser)
            oauth.awaitForeground()
            // Recheck discovery: do not import credentials for a method the target server does not implement.
            val available = listKeys(serverId, directory).getOrThrow()
            require(available.any { it.id == integrationId && it.oauthMethods.any { method -> method.id == methodId && method.mobileSupported } })
            // OpenAI requires a stable destination-host ID before import. The existing plugin owns
            // that store and initializes it lazily on authorize; prepare then immediately cancel
            // an unopened server attempt. Never copy the phone's host ID into the server.
            val integrationApi = targetClient.integrations
            val prepared = awaitBody {
                integrationApi.integrationOauthConnect(integrationId, IntegrationOauthConnectRequest(methodId, emptyMap()), directory)
            }.data
            val preparedId = requireNotNull(prepared.attemptID)
            try {
                val preparedUrl = Url(requireNotNull(prepared.url))
                require(preparedUrl.parameters["ext_agent_host_id"]?.startsWith("urn:uuid:") == true)
            } finally {
                withContext(NonCancellable) { awaitSuccess { integrationApi.integrationOauthCancel(integrationId, preparedId, directory) } }
            }
            val credential = oauth.authorize(provider, openBrowser)
            oauth.awaitForeground()
            currentCoroutineContext().ensureActive()
            oauthStep(ProviderOAuthStage.ServerChanged) {
                require(servers.servers.value.firstOrNull { it.id == serverId } == profile && servers.activeServer.value?.id == serverId)
            }
            val api = targetClient.credentials
            oauthStep(ProviderOAuthStage.Import) {
                awaitSuccess {
                    api.credentialCreate(
                        CredentialCreateInput(
                            integrationID = integrationId,
                            value = CredentialValue.OauthWrapper(
                                CredentialOAuth(
                                    type = CredentialOAuth.Type.OAUTH, methodID = methodId,
                                    refresh = credential.refresh, access = credential.access, expires = credential.expires,
                                    metadata = buildJsonObject {
                                        put("clientID", credential.clientId)
                                        put("scopes", JsonArray(credential.scopes.map(::JsonPrimitive)))
                                    },
                                ),
                            ),
                            label = "OpenCode Mobile", activate = true,
                        ),
                    )
                }
            }
        }
    }
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
        val secure = protectedOAuthTransport(client(serverId).baseUrl)
        awaitBody { api.integrationList(locationDirectory = directory) }.data.mapNotNull { it.toKeyIntegration() }.map { integration ->
            integration.copy(
                oauthMethods = integration.oauthMethods.map { method ->
                    if (integration.id == OpenAiBrowser.integrationId && method.id == OpenAiBrowser.methodId) {
                        method.copy(
                            browser = true, mobileSupported = browser?.supported == true && secure,
                            requiresSecureTransport = browser?.supported == true && !secure,
                        )
                    } else method
                },
            )
        }.sortedBy { it.name.lowercase() }
    }

    override suspend fun connectKey(serverId: String, directory: String, integrationId: String, key: String, answers: Map<String, FormAnswer>) = resultOf {
        val api = client(serverId).integrations
        awaitSuccess {
            api.integrationConnectKey(integrationId, IntegrationConnectKeyRequest(key, answers.mapValues { it.value.toJson() }), directory)
        }
    }
}
