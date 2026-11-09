package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.SessionStructuredError
import io.github.beppe2880.opencodemobile.api.models.TokenUsageInfo
import io.github.beppe2880.opencodemobile.api.models.ToolContent
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.ToolFile
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import io.github.beppe2880.opencodemobile.api.models.ModelRef as ApiModelRef
import io.github.beppe2880.opencodemobile.core.domain.ModelRef as DomainModelRef

/** The server reports "no variant chosen" as `default`; the domain uses null, which omits it on send. */
fun ApiModelRef.toDomain(): DomainModelRef =
    DomainModelRef(id = id, providerId = providerID.orEmpty(), variant = variant?.takeUnless { it == DEFAULT_VARIANT })

private const val DEFAULT_VARIANT = "default"

fun DomainModelRef.toApi(): ApiModelRef = ApiModelRef(id = id, providerID = providerId, variant = variant)

fun TokenUsageInfo.toDomain(): TokenUsage = TokenUsage(
    input = input.toLong(),
    output = output.toLong(),
    reasoning = reasoning.toLong(),
    cacheRead = cache.read.toLong(),
    cacheWrite = cache.write.toLong(),
)

internal fun SessionStructuredError.describe(): String = message ?: type ?: "Unknown error"

/** Text content of a tool result joined in order; file attachments are in [files]. */
internal fun List<ToolContent>.joinedText(): String =
    filterIsInstance<ToolContent.TextWrapper>().joinToString("\n") { it.value.text }

/** File attachments of a tool result (images and PDFs read by `read`, MCP media), in order. */
internal fun List<ToolContent>.files(): List<ToolFile> =
    filterIsInstance<ToolContent.FileWrapper>().map { ToolFile(uri = it.value.uri, mime = it.value.mime, name = it.value.name) }

/** Flattens a JSON object to display strings: primitives keep their content, others stay JSON. */
internal fun JsonObject.toStringMap(): Map<String, String> = mapValues { (_, value) -> value.displayText() }

internal fun JsonElement.displayText(): String = (this as? JsonPrimitive)?.content ?: toString()
