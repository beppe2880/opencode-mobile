package io.github.beppe2880.opencodemobile.core.network

import io.github.beppe2880.opencodemobile.api.models.SessionGenerateResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Servers before 2.3 answer /generate with the text only, without `tokens` and `cost`. The app
 * shows only the text, so missing usage is filled with zeros instead of failing the answer.
 */
internal fun decodeGenerateResponse(json: Json, body: String): SessionGenerateResponse {
    val root = json.parseToJsonElement(body).jsonObject
    val data = root["data"] as? JsonObject
        ?: return json.decodeFromJsonElement(SessionGenerateResponse.serializer(), root)
    val zero = JsonPrimitive(0)
    val usage = JsonObject(
        mapOf("input" to zero, "output" to zero, "reasoning" to zero, "cache" to JsonObject(mapOf("read" to zero, "write" to zero))),
    )
    val filled = JsonObject(mapOf("tokens" to usage, "cost" to zero) + data)
    return json.decodeFromJsonElement(SessionGenerateResponse.serializer(), JsonObject(root + ("data" to filled)))
}
