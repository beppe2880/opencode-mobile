package io.github.beppe2880.opencodemobile.core.data.integration

import kotlinx.serialization.json.JsonObject

/** iOS remains explicitly unsupported until a native loopback listener and verifier are tested there. */
internal class UnsupportedOAuthPlatform : OAuthPlatform {
    override val supported = false
    override fun randomBytes(): ByteArray = error("Browser OAuth unavailable")
    override fun sha256(bytes: ByteArray): ByteArray = error("Browser OAuth unavailable")
    override fun listen(state: String): OAuthLoopback = error("Browser OAuth unavailable")
    override suspend fun json(url: String, form: Map<String, String>?): JsonObject = error("Browser OAuth unavailable")
    override fun verifySignature(token: String, jwks: JsonObject): Unit = error("Browser OAuth unavailable")
}
