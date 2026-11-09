package io.github.beppe2880.opencodemobile.core.data.integration

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertFailsWith

class OidcValidationTest {
    private fun token(changes: Map<String, JsonElement?> = emptyMap()): String {
        val values = mutableMapOf<String, JsonElement?>(
            "iss" to JsonPrimitive("https://issuer.example"), "aud" to JsonPrimitive("client"),
            "sub" to JsonPrimitive("user"), "nonce" to JsonPrimitive("nonce"), "exp" to JsonPrimitive(2000),
        ) + changes
        val claims = buildJsonObject { values.forEach { (key, value) -> if (value != null) put(key, value) } }
        return "header.${base64(claims.toString().encodeToByteArray())}.signature"
    }
    private fun validate(token: String) = validateIdClaims(token, "https://issuer.example", "client", "nonce", nowSeconds = 1000)

    @Test fun requiredClaimsMustExistAndBeNonempty() {
        validate(token())
        listOf("iss", "aud", "sub", "nonce", "exp").forEach { key ->
            assertFailsWith<IllegalArgumentException> { validate(token(mapOf(key to null))) }
        }
        assertFailsWith<IllegalArgumentException> { validate(token(mapOf("sub" to JsonPrimitive("  ")))) }
        assertFailsWith<IllegalArgumentException> { validate(token(mapOf("sub" to JsonPrimitive(123)))) }
        assertFailsWith<IllegalArgumentException> { validate(token(mapOf("exp" to JsonPrimitive("2000")))) }
    }

    @Test fun multipleAudiencesRequireMatchingAuthorizedParty() {
        val audiences = JsonArray(listOf(JsonPrimitive("client"), JsonPrimitive("other")))
        assertFailsWith<IllegalArgumentException> { validate(token(mapOf("aud" to audiences))) }
        validate(token(mapOf("aud" to audiences, "azp" to JsonPrimitive("client"))))
        assertFailsWith<IllegalArgumentException> { validate(token(mapOf("azp" to JsonPrimitive("other")))) }
    }

    @Test fun expiredAndNotYetValidTokensAreRejected() {
        assertFailsWith<IllegalArgumentException> { validate(token(mapOf("exp" to JsonPrimitive(1000)))) }
        assertFailsWith<IllegalArgumentException> { validate(token(mapOf("nbf" to JsonPrimitive(1001)))) }
        assertFailsWith<IllegalArgumentException> { validate(token(mapOf("iat" to JsonPrimitive(1061)))) }
        validate(token(mapOf("nbf" to JsonPrimitive(1000), "iat" to JsonPrimitive(1000))))
    }
}
