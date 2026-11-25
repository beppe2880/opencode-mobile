package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.api.models.SessionMessageCompaction
import io.github.beppe2880.opencodemobile.api.models.SessionMessageInfo
import io.github.beppe2880.opencodemobile.api.models.SessionMessageShell
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Wire JSON of stored transcript entries. The message cache keeps the server payload rather
 * than domain models, so mapping fixes also apply to cached history.
 */
object MessageCodec {

    /** A message id with its wire JSON. */
    data class Encoded(val id: String, val json: String)

    /**
     * Inline payloads are slimmed for storage: a tool that read a screenshot carries the image
     * as a multi-megabyte `data:` URI, and SQLite cannot read back a row bigger than its
     * cursor window (about 2 MB). The transcript never shows those payloads.
     */
    fun encode(message: SessionMessageInfo): Encoded {
        val element = slim(OpenCodeJson.encodeToJsonElement(SessionMessageInfo.serializer(), message))
        return Encoded(element.jsonObject["id"]?.jsonPrimitive?.content.orEmpty(), element.toString())
    }

    private fun isBase64Char(c: Char) = c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '+' || c == '/' || c == '='

    /** Longest string kept as is in a stored message. */
    const val MAX_STORED_STRING = 64 * 1024

    internal fun slim(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (_, value) -> slim(value) })
        is JsonArray -> JsonArray(element.map(::slim))
        is JsonPrimitive -> {
            val text = element.contentOrNull
            when {
                !element.isString || text == null || text.length <= MAX_STORED_STRING -> element
                // Keeps the media type so the attachment is still recognizable.
                text.startsWith("data:") -> JsonPrimitive(text.substringBefore(',') + ",")
                // A raw base64 payload (prompt attachments): cut, it would decode to garbage.
                text.all(::isBase64Char) -> JsonPrimitive("")
                else -> JsonPrimitive(text.take(MAX_STORED_STRING) + "\n… [${text.length - MAX_STORED_STRING} characters not stored]")
            }
        }
        else -> element
    }

    /** Null when [json] no longer matches the generated contract (e.g. after an API update). */
    fun decode(json: String): SessionMessageInfo? = try {
        OpenCodeJson.decodeFromString(SessionMessageInfo.serializer(), json)
    } catch (e: SerializationException) {
        null
    }

    /** An export already has a JSON tree; don't serialize and parse every message again. */
    fun decode(element: JsonElement): SessionMessageInfo? = try {
        OpenCodeJson.decodeFromJsonElement(SessionMessageInfo.serializer(), element)
    } catch (_: SerializationException) {
        null
    }

    /** False while the server may still change the message: a running step, shell or compaction. */
    fun isSettled(message: SessionMessageInfo): Boolean = when (message) {
        is SessionMessageInfo.SessionMessageAssistantValue -> message.value.time.completed != null
        is SessionMessageInfo.SessionMessageShellValue -> message.value.status != SessionMessageShell.Status.RUNNING
        is SessionMessageInfo.SessionMessageCompactionValue -> message.value !is SessionMessageCompaction.RunningWrapper
        else -> true
    }
}
