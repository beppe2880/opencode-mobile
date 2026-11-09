package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.IntegrationInfo
import io.github.beppe2880.opencodemobile.api.models.IntegrationMethod
import io.github.beppe2880.opencodemobile.core.domain.KeyIntegration
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthMethod
import io.github.beppe2880.opencodemobile.core.domain.deviceOAuthSupported

fun IntegrationInfo.toKeyIntegration(): KeyIntegration? {
    val integrationId = id ?: return null
    // MCP integrations belong to their own panel, not to AI provider connection.
    if (metadata?.get("source")?.toString() == "\"mcp\"") return null
    val method = methods.filterIsInstance<IntegrationMethod.KeyWrapper>().firstOrNull()?.value
    val oauth = methods.filterIsInstance<IntegrationMethod.OauthWrapper>().mapNotNull { wrapper ->
        val value = wrapper.value
        val methodId = value.id ?: return@mapNotNull null
        val fields = value.form.orEmpty()
        ProviderOAuthMethod(
            methodId, value.label,
            KeyIntegration(
                integrationId, name, connections.isNotEmpty(), null, fields.map { it.toDomain() },
                fields.filter { it.isHidden() }.map { it.toDomain().key }.toSet(),
            ),
            deviceOAuthSupported(integrationId, methodId),
        )
    }
    if (method == null && oauth.isEmpty()) return null
    val fields = method?.form.orEmpty()
    return KeyIntegration(
        integrationId, name, connections.isNotEmpty(), method?.label,
        fields.map {
            it.toDomain()
        },
        fields.filter { it.isHidden() }.map { it.toDomain().key }.toSet(),
        hasKey = method != null, oauthMethods = oauth,
    )
}
