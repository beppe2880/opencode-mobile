package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.AgentInfo
import io.github.beppe2880.opencodemobile.api.models.ConfigModelEntry
import io.github.beppe2880.opencodemobile.api.models.SessionInfo
import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import io.github.beppe2880.opencodemobile.api.models.ModelInfo as ApiModelInfo
import io.github.beppe2880.opencodemobile.api.models.PermissionRequest as ApiPermissionRequest
import io.github.beppe2880.opencodemobile.api.models.Project as ApiProject
import io.github.beppe2880.opencodemobile.api.models.ServerInfo as ApiServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo as DomainModelInfo

fun SessionInfo.toDomain(): Session = Session(
    id = id.orEmpty(),
    parentId = parentID,
    projectId = projectID.orEmpty(),
    title = title.orEmpty(),
    directory = location?.directory.orEmpty(),
    agent = agent,
    model = model?.toDomain(),
    cost = cost,
    tokens = tokens.toDomain(),
    outcome = when (outcome) {
        SessionInfo.Outcome.SUCCEEDED -> SessionOutcome.Succeeded
        SessionInfo.Outcome.FAILED -> SessionOutcome.Failed
        SessionInfo.Outcome.INTERRUPTED -> SessionOutcome.Interrupted
        null -> null
    },
    createdAt = time.created.toLong(),
    updatedAt = time.updated.toLong(),
    archivedAt = time.archived?.toLong(),
    revertMessageId = revert?.messageID,
)

fun ApiProject.toDomain(): Project = Project(
    id = id.orEmpty(),
    directory = canonical.orEmpty(),
    name = name,
    vcs = vcs,
    updatedAt = time.updated,
)

fun AgentInfo.toDomain(): Agent = Agent(
    id = id ?: name,
    name = name,
    description = description,
    mode = when (mode) {
        AgentInfo.Mode.PRIMARY -> AgentMode.Primary
        AgentInfo.Mode.SUBAGENT -> AgentMode.Subagent
        AgentInfo.Mode.ALL -> AgentMode.All
    },
    hidden = hidden,
    color = color,
    model = model?.toDomain(),
)

/**
 * The `model` set explicitly in the server configuration, like OpenCode Desktop: the last
 * config document that sets one wins. It is written `provider/model` (optionally `#variant`)
 * or as `{ providerID, model, variant }`. Null when no document sets it.
 */
fun List<ConfigModelEntry>.configuredModel(): ModelRef? {
    val model = lastOrNull { it.type == "document" && it.info?.model.let { m -> m != null && m !is JsonNull } }?.info?.model ?: return null
    return when (model) {
        is JsonPrimitive -> model.contentOrNull?.let { text ->
            val providerId = text.substringBefore('/', missingDelimiterValue = "")
            val rest = text.substringAfter('/')
            ModelRef(id = rest.substringBefore('#'), providerId = providerId, variant = rest.substringAfter('#', "").ifEmpty { null })
                .takeIf { providerId.isNotEmpty() && it.id.isNotEmpty() }
        }
        is JsonObject -> {
            val providerId = (model["providerID"] as? JsonPrimitive)?.contentOrNull
            val id = (model["model"] as? JsonPrimitive)?.contentOrNull
            if (providerId.isNullOrEmpty() || id.isNullOrEmpty()) null
            else ModelRef(id = id, providerId = providerId, variant = (model["variant"] as? JsonPrimitive)?.contentOrNull)
        }
        else -> null
    }
}

/** [providerNames] maps provider IDs to display names; unknown providers keep their ID. */
fun ApiModelInfo.toDomain(providerNames: Map<String, String> = emptyMap()): DomainModelInfo {
    val providerId = providerID.orEmpty()
    val modelId = id ?: modelID.orEmpty()
    return DomainModelInfo(
        ref = ModelRef(id = modelId, providerId = providerId),
        name = name ?: modelId,
        providerId = providerId,
        providerName = providerNames[providerId] ?: providerId,
        family = family,
        contextLimit = limit.context,
        supportsImages = "image" in capabilities.input,
        variants = variants.map { it.id },
        releasedAt = time.released.toLong(),
        enabled = enabled,
        status = status.value,
    )
}

fun ApiPermissionRequest.toDomain(): PermissionRequest = PermissionRequest(
    id = id,
    sessionId = sessionID,
    action = action,
    resources = resources,
    message = message,
    canSave = !save.isNullOrEmpty(),
)

fun ApiServerInfo.toDomain(): ServerInfo = ServerInfo(version = version.orEmpty(), urls = urls)
