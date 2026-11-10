package io.github.beppe2880.opencodemobile.core.data.integration

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64

internal fun jwtPart(part: String) = Json.parseToJsonElement(
    Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(part).decodeToString(),
).jsonObject

/** Call only after verifying the signature against the adapter's pinned issuer JWKS. */
internal fun validateIdClaims(token: String, issuer: String, clientId: String, nonce: String, nowSeconds: Long = now() / 1000) {
    val parts = token.split('.')
    require(parts.size == 3 && token.length <= 65_536)
    val claims = jwtPart(parts[1])
    require(claims.string("iss") == issuer && claims.string("nonce") == nonce)
    claims.string("sub")
    val audiences = when (val aud = claims["aud"]) {
        is JsonArray -> aud.map {
            require(it.jsonPrimitive.isString)
            it.jsonPrimitive.content
        }
        else -> listOf(claims.string("aud"))
    }
    require(clientId in audiences)
    if (audiences.size > 1 || claims["azp"] != null) require(claims.string("azp") == clientId)
    require(requireNotNull(claims["exp"]?.jsonPrimitive?.takeUnless { it.isString }?.longOrNull) > nowSeconds)
    claims["nbf"]?.let {
        require(!it.jsonPrimitive.isString && requireNotNull(it.jsonPrimitive.longOrNull) <= nowSeconds)
    }
    claims["iat"]?.let {
        require(!it.jsonPrimitive.isString && requireNotNull(it.jsonPrimitive.longOrNull) <= nowSeconds + 60)
    }
}
