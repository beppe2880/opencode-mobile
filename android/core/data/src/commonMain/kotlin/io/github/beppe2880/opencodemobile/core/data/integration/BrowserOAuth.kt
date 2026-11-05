package io.github.beppe2880.opencodemobile.core.data.integration

import com.russhwolf.settings.Settings
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Native operations only; authorization policy and orchestration remain shared. */
internal interface OAuthPlatform {
    val supported: Boolean
    fun randomBytes(): ByteArray
    fun sha256(bytes: ByteArray): ByteArray
    fun listen(state: String): OAuthLoopback
    suspend fun json(url: String, form: Map<String, String>? = null): JsonObject
    fun verifySignature(token: String, jwks: JsonObject)
}

internal interface OAuthLoopback {
    val redirect: String
    suspend fun receive(): Map<String, String>
    fun close()
}

/** Deliberately not a data class: tokens must never appear in generated toString(). */
internal class BrowserCredential(val access: String, val refresh: String, val expires: Long, val clientId: String, val scopes: List<String>)

internal class OAuthProvider(
    val integrationId: String,
    val methodId: String,
    val authorizeUrl: String,
    val tokenUrl: String,
    val issuer: String,
    val jwksUrl: String,
    val registrationClientId: String,
    val scopes: String,
    val requiredScope: String,
    val parameters: Map<String, String>,
    val tokenParameters: Map<String, String>,
    val issuedClientId: Boolean = false,
    val hostParameter: String? = null,
)

internal val OpenAiBrowser = OAuthProvider(
    integrationId = "openai",
    methodId = "chatgpt-token-sharing",
    authorizeUrl = "https://auth.openai.com/api/accounts/authorize",
    tokenUrl = "https://auth.openai.com/api/accounts/oauth/token",
    issuer = "https://auth.openai.com",
    jwksUrl = "https://auth.openai.com/.well-known/jwks.json",
    registrationClientId = "dynamic_agent_client",
    scopes = "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct",
    requiredScope = "chatgpt.tokens.use.direct",
    parameters = mapOf("agent_name_hint" to "OpenCode", "resource" to "https://api.openai.com/v1"),
    tokenParameters = mapOf("resource" to "https://api.openai.com/v1"),
    issuedClientId = true,
    hostParameter = "ext_agent_host_id",
)

/** Common engine: no server credentials, no persistence of OAuth codes or tokens, no refreshes. */
internal class BrowserOAuth(
    private val platform: OAuthPlatform,
    private val settings: Settings,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val supported: Boolean get() = platform.supported

    suspend fun authorize(provider: OAuthProvider, openBrowser: suspend (String) -> Unit): BrowserCredential = withContext(dispatcher) {
        withTimeoutOrNull(300_000) {
            check(supported)
            val verifier = base64(platform.randomBytes())
            val state = base64(platform.randomBytes())
            val nonce = base64(platform.randomBytes())
            // This is the local authorizing host, never the server's plugin host ID. Import does not touch server storage.
            val hostKey = "oauth.local-host-id"
            val hostId = settings.getStringOrNull(hostKey) ?: "urn:uuid:${Uuid.random()}".also { settings.putString(hostKey, it) }
            val callback = platform.listen(state)
            try {
                val url = URLBuilder(provider.authorizeUrl).apply {
                    (
                        provider.parameters + mapOf(
                            "client_id" to provider.registrationClientId,
                            "response_type" to "code",
                            "redirect_uri" to callback.redirect,
                            "scope" to provider.scopes,
                            "state" to state,
                            "nonce" to nonce,
                            "code_challenge_method" to "S256",
                            "code_challenge" to base64(platform.sha256(verifier.encodeToByteArray())),
                        )
                        ).forEach { (key, value) -> parameters.append(key, value) }
                    provider.hostParameter?.let { parameters.append(it, hostId) }
                }.buildString()
                openBrowser(url)
                val response = callback.receive()
                require(response["state"] == state && response["error"] == null)
                val code = requireNotNull(response["code"]).also { require(it.isNotBlank()) }
                val clientId = (if (provider.issuedClientId) requireNotNull(response["client_id"]) else provider.registrationClientId)
                    .also { require(it.isNotBlank() && it.length <= 1024) }
                val tokens = platform.json(
                    provider.tokenUrl,
                    provider.tokenParameters + mapOf(
                        "grant_type" to "authorization_code", "client_id" to clientId, "code" to code,
                        "code_verifier" to verifier, "redirect_uri" to callback.redirect,
                    ),
                )
                val scopes = tokens.string("scope").split(' ').filter { it.isNotBlank() }
                require(provider.requiredScope in scopes)
                val idToken = tokens.string("id_token")
                platform.verifySignature(idToken, platform.json(provider.jwksUrl))
                validateIdClaims(idToken, provider.issuer, clientId, nonce)
                val seconds = requireNotNull(tokens["expires_in"]?.jsonPrimitive?.longOrNull)
                require(seconds in 1..86_400)
                BrowserCredential(tokens.string("access_token"), tokens.string("refresh_token"), now() + seconds * 1000, clientId, scopes)
            } finally {
                callback.close()
            }
        } ?: error("Browser authorization expired")
    }
}

internal fun JsonObject.string(key: String): String =
    requireNotNull(get(key)?.jsonPrimitive?.takeIf { it.isString }?.contentOrNull).also { require(it.isNotBlank()) }
internal fun base64(bytes: ByteArray): String = Base64.UrlSafe.encode(bytes).trimEnd('=')
internal fun now(): Long = Clock.System.now().toEpochMilliseconds()

/** Never infer VPN protection from a private address. Only TLS or literal local loopback is accepted. */
internal fun protectedOAuthTransport(address: String): Boolean = runCatching {
    val url = Url(address)
    url.user.isNullOrEmpty() && url.password.isNullOrEmpty() && (
        url.protocol.name == "https" || (url.protocol.name == "http" && url.host in setOf("127.0.0.1", "localhost", "[::1]", "::1"))
        )
}.getOrDefault(false)
