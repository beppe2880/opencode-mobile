package io.github.beppe2880.opencodemobile.core.data

import com.russhwolf.settings.MapSettings
import io.github.beppe2880.opencodemobile.core.data.integration.AndroidOAuthPlatform
import io.github.beppe2880.opencodemobile.core.data.integration.BrowserOAuth
import io.github.beppe2880.opencodemobile.core.data.integration.OAuthHttpError
import io.github.beppe2880.opencodemobile.core.data.integration.OAuthLoopback
import io.github.beppe2880.opencodemobile.core.data.integration.OAuthPlatform
import io.github.beppe2880.opencodemobile.core.data.integration.OpenAiBrowser
import io.github.beppe2880.opencodemobile.core.data.integration.base64
import io.github.beppe2880.opencodemobile.core.data.integration.now
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStage
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey

/** Real RSA signatures, fake authorization service; no account or internet required. */
internal class OAuthFixture(private val native: OAuthPlatform = AndroidOAuthPlatform()) : OAuthPlatform by native {
    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val settings = MapSettings()
    val foreground = MutableStateFlow(true)
    var authorization: Url? = null
    var tokenForm: Map<String, String>? = null
    var closed = false
    var responded = false
    private var pendingResponse = false
    var responseError: ProviderOAuthError? = null
    var block = false
    var callbackState: String? = null
    var callbackError = false
    var missingClient = false
    var missingScope = false
    var invalidSignature = false
    var badNonce = false
    var badAudience = false
    var badIssuer = false
    var expired = false
    var httpFailure = false
    var expiry: JsonElement? = JsonPrimitive(3600)
    var tokenHttpStatus: Int? = null
    var jwksHttpStatus: Int? = null
    var missingIdToken = false
    var exchanges = 0
    var jwksRequests = 0
    var backgroundAfterExchange = false
    var onSignatureVerified: (() -> Unit)? = null
    private var state = ""

    override fun listen(state: String): OAuthLoopback {
        this.state = state
        responded = false
        responseError = null
        pendingResponse = false
        return object : OAuthLoopback {
            override val redirect = "http://127.0.0.1:54321/auth/callback"
            override suspend fun receive(): Map<String, String> {
                if (block) CompletableDeferred<Unit>().await()
                pendingResponse = true
                return buildMap {
                    put("state", callbackState ?: state)
                    if (callbackError) put("error", "access_denied") else put("code", "test-code")
                    if (!missingClient) put("client_id", "issued-client")
                }
            }
            override fun close() { closed = true }
            override suspend fun respond(error: ProviderOAuthError?) {
                if (pendingResponse) {
                    responded = true
                    responseError = error
                    pendingResponse = false
                }
            }
        }
    }

    fun opened(url: String) { authorization = Url(url) }

    override fun verifySignature(token: String, jwks: JsonObject) {
        check(responded && responseError == null)
        native.verifySignature(token, jwks)
        onSignatureVerified?.invoke()
    }

    override suspend fun json(url: String, form: Map<String, String>?): JsonObject {
        check(responded && responseError == null)
        check(foreground.value) { "Network attempted in background" }
        check(!httpFailure)
        if (form == null) {
            jwksRequests++
            jwksHttpStatus?.let { throw OAuthHttpError(it) }
            return jwks()
        }
        tokenHttpStatus?.let { throw OAuthHttpError(it) }
        exchanges++
        if (backgroundAfterExchange) foreground.value = false
        tokenForm = form
        return buildJsonObject {
            put("access_token", "test-access")
            put("refresh_token", "test-refresh")
            expiry?.let { put("expires_in", it) }
            put("scope", if (missingScope) "openid" else OpenAiBrowser.scopes)
            if (!missingIdToken) put("id_token", token())
        }
    }

    fun jwks(): JsonObject {
        val public = keys.public as RSAPublicKey
        return buildJsonObject {
            put(
                "keys",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("kid", "test")
                            put("kty", "RSA")
                            put("alg", "RS256")
                            put("use", "sig")
                            put("n", base64(public.modulus.toByteArray()))
                            put("e", base64(public.publicExponent.toByteArray()))
                        },
                    ),
                ),
            )
        }
    }

    fun token(): String {
        val header = base64("""{"alg":"RS256","kid":"test"}""".encodeToByteArray())
        val claims = buildJsonObject {
            put("iss", if (badIssuer) "https://evil.example" else OpenAiBrowser.issuer)
            put("aud", if (badAudience) "other-client" else "issued-client")
            put("sub", "test-user")
            put("nonce", if (badNonce) "other-nonce" else requireNotNull(authorization).parameters["nonce"]!!)
            put("exp", now() / 1000 + if (expired) -60 else 3600)
        }
        val signed = "$header.${base64(claims.toString().encodeToByteArray())}"
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(keys.private)
            update(signed.encodeToByteArray())
            sign().also { if (invalidSignature) it[0] = (it[0].toInt() xor 1).toByte() }
        }
        return "$signed.${base64(signature)}"
    }
}

class BrowserOAuthTest {
    @Test fun `callback is acknowledged in background but exchange waits until foreground`() = runTest {
        val fixture = OAuthFixture()
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        var complete = false
        launch {
            engine.authorize(OpenAiBrowser) { url -> fixture.opened(url); fixture.foreground.value = false }
            complete = true
        }
        runCurrent()
        assertTrue(fixture.responded)
        assertEquals(null, fixture.responseError)
        assertTrue(fixture.closed)
        advanceTimeBy(15_000)
        runCurrent()
        assertEquals(0, fixture.exchanges)
        assertEquals(0, fixture.jwksRequests)
        assertFalse(complete)
        fixture.foreground.value = true
        advanceUntilIdle()
        assertTrue(complete)
        assertEquals(1, fixture.exchanges)
        assertEquals(1, fixture.jwksRequests)
    }

    @Test fun `JWKS request waits again if app backgrounds after token exchange`() = runTest {
        val fixture = OAuthFixture().apply { backgroundAfterExchange = true }
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        var complete = false
        launch { engine.authorize(OpenAiBrowser, fixture::opened); complete = true }
        runCurrent()
        assertEquals(1, fixture.exchanges)
        assertEquals(0, fixture.jwksRequests)
        assertFalse(complete)
        fixture.foreground.value = true
        advanceUntilIdle()
        assertTrue(complete)
        assertEquals(1, fixture.exchanges)
        assertEquals(1, fixture.jwksRequests)
    }

    @Test fun `cancelling after callback while background never exchanges the code later`() = runTest {
        val fixture = OAuthFixture().apply { foreground.value = false }
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val job = launch { engine.authorize(OpenAiBrowser, fixture::opened) }
        runCurrent()
        assertTrue(fixture.responded)
        job.cancelAndJoin()
        fixture.foreground.value = true
        advanceUntilIdle()
        assertEquals(0, fixture.exchanges)
        assertTrue(fixture.closed)
    }

    @Test fun `not returning to app expires foreground wait without network or browser success`() = runTest {
        val fixture = OAuthFixture().apply { foreground.value = false }
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        var error: ProviderOAuthError? = null
        launch { error = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as? ProviderOAuthError }
        runCurrent()
        assertTrue(fixture.responded)
        advanceUntilIdle()
        assertEquals(ProviderOAuthStage.Expired, error?.stage)
        assertEquals(0, fixture.exchanges)
        assertTrue(fixture.closed)
    }

    @Test fun `denied callback fails immediately even while app is background`() = runTest {
        val fixture = OAuthFixture().apply { foreground.value = false; callbackError = true }
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val error = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
        assertEquals(ProviderOAuthStage.Callback, error.stage)
        assertEquals(ProviderOAuthStage.Callback, fixture.responseError?.stage)
        assertEquals(0, fixture.exchanges)
        assertTrue(fixture.closed)
    }

    @Test fun `browser acknowledges callback without claiming validation and app retains later failures`() = runTest {
        val fixture = OAuthFixture()
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        engine.authorize(OpenAiBrowser, fixture::opened)
        assertTrue(fixture.responded)
        assertEquals(null, fixture.responseError)
        fixture.badNonce = true
        val failure = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
        assertEquals(ProviderOAuthStage.Claims, failure.stage)
        assertEquals(null, fixture.responseError)
        fixture.badNonce = false
        fixture.tokenHttpStatus = 400
        val exchange = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
        assertEquals(ProviderOAuthStage.TokenExchange, exchange.stage)
        assertEquals(null, fixture.responseError)
        assertTrue(fixture.closed)
    }

    @Test fun `missing expiry uses server compatible one hour default after full token validation`() = runTest {
        val fixture = OAuthFixture().apply { expiry = null }
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val before = now()
        val credential = engine.authorize(OpenAiBrowser, fixture::opened)
        assertTrue(credential.expires in (before + 3_600_000)..(now() + 3_600_000))
        assertEquals("issued-client", credential.clientId)
        assertTrue(fixture.closed)
        fixture.badNonce = true
        val failure = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
        assertEquals(ProviderOAuthStage.Claims, failure.stage)
    }

    @Test fun `malformed zero negative and overflowing expiry are not treated as absent`() = runTest {
        val fixture = OAuthFixture()
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        listOf(JsonNull, JsonPrimitive(0), JsonPrimitive(-1), JsonPrimitive("3600"), JsonPrimitive(1.5), JsonPrimitive(Long.MAX_VALUE)).forEach {
            fixture.expiry = it
            val failure = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
            assertEquals(ProviderOAuthStage.TokenResponse, failure.stage)
            assertTrue(fixture.closed)
        }
        fixture.expiry = JsonPrimitive(172800)
        assertTrue(engine.authorize(OpenAiBrowser, fixture::opened).expires > now() + 86_400_000)
    }

    @Test fun `provider HTTP statuses identify exchange or JWKS phase without payloads`() = runTest {
        val fixture = OAuthFixture()
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        fixture.tokenHttpStatus = 400
        val exchange = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
        assertEquals("OAUTH_TokenExchange_HTTP_400", exchange.diagnosticCode)
        assertEquals(null, exchange.cause)
        fixture.tokenHttpStatus = null
        fixture.jwksHttpStatus = 503
        val keys = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
        assertEquals("OAUTH_Jwks_HTTP_503", keys.diagnosticCode)
        assertEquals(null, keys.cause)
        assertTrue(fixture.closed)
    }

    @Test fun `token validation failure identifies signature claims scope or missing ID token`() = runTest {
        val cases = listOf<Pair<(OAuthFixture) -> Unit, ProviderOAuthStage>>(
            { f: OAuthFixture -> f.invalidSignature = true } to ProviderOAuthStage.Signature,
            { f: OAuthFixture -> f.badNonce = true } to ProviderOAuthStage.Claims,
            { f: OAuthFixture -> f.missingScope = true } to ProviderOAuthStage.Scope,
            { f: OAuthFixture -> f.missingIdToken = true } to ProviderOAuthStage.TokenResponse,
        )
        cases.forEach { (change, stage) ->
            val fixture = OAuthFixture().also(change)
            val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
            val failure = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
            assertEquals(stage, failure.stage)
            assertFalse(failure.toString().contains("test-code"))
            assertTrue(fixture.closed)
        }
    }

    @Test fun `signature verification rejects unsigned tokens algorithm downgrades and unknown keys`() {
        val fixture = OAuthFixture()
        fixture.opened("https://auth.example/authorize?nonce=test")
        val token = fixture.token()
        val platform = AndroidOAuthPlatform()
        platform.verifySignature(token, fixture.jwks())
        val suffix = token.substringAfter('.')
        listOf(
            """{"alg":"none","kid":"test"}""",
            """{"alg":"HS256","kid":"test"}""",
            """{"alg":"RS256","kid":"unknown"}""",
            """{"alg":"RS256","kid":"test","crit":["b64"],"b64":false}""",
        ).forEach { header ->
            assertTrue(runCatching { platform.verifySignature("${base64(header.encodeToByteArray())}.$suffix", fixture.jwks()) }.isFailure)
        }
    }

    @Test fun `OpenAI registration exchanges exact redirect and PKCE then validates signed ID token`() = runTest {
        val fixture = OAuthFixture()
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, testScheduler.let { kotlinx.coroutines.test.StandardTestDispatcher(it) })
        val credential = engine.authorize(OpenAiBrowser, fixture::opened)
        val url = requireNotNull(fixture.authorization)
        val form = requireNotNull(fixture.tokenForm)
        assertEquals("dynamic_agent_client", url.parameters["client_id"])
        assertEquals("OpenCode", url.parameters["agent_name_hint"])
        assertEquals("issued-client", form["client_id"])
        assertEquals(url.parameters["redirect_uri"], form["redirect_uri"])
        assertEquals(url.parameters["code_challenge"], base64(fixture.sha256(form.getValue("code_verifier").encodeToByteArray())))
        assertEquals("S256", url.parameters["code_challenge_method"])
        assertEquals("test-access", credential.access)
        assertEquals("test-refresh", credential.refresh)
        assertTrue(credential.expires > now())
        assertTrue(fixture.closed)
        assertEquals(setOf("oauth.local-host-id"), fixture.settings.keys)
        val host = url.parameters["ext_agent_host_id"]
        engine.authorize(OpenAiBrowser, fixture::opened)
        assertEquals(host, fixture.authorization!!.parameters["ext_agent_host_id"])
        assertNotEquals(url.parameters["state"], fixture.authorization!!.parameters["state"])
    }

    @Test fun `wrong state missing client and denial never exchange tokens`() = runTest {
        listOf<(OAuthFixture) -> Unit>({ it.callbackState = "wrong" }, { it.missingClient = true }, { it.callbackError = true }).forEach { change ->
            val fixture = OAuthFixture().also(change)
            val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
            val failure = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as ProviderOAuthError
            assertEquals(ProviderOAuthStage.Callback, failure.stage)
            assertEquals(0, fixture.exchanges)
            assertTrue(fixture.closed)
        }
    }

    @Test fun `invalid signature nonce audience issuer expiry and missing sharing scope fail closed`() = runTest {
        val changes = listOf<(OAuthFixture) -> Unit>(
            { it.invalidSignature = true }, { it.badNonce = true }, { it.badAudience = true },
            { it.badIssuer = true }, { it.expired = true }, { it.missingScope = true }, { it.httpFailure = true },
        )
        changes.forEach { change ->
            val fixture = OAuthFixture().also(change)
            val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
            assertTrue(runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.isFailure)
            assertTrue(fixture.closed)
        }
    }

    @Test fun `cancellation and expiry always close callback without storing tokens`() = runTest {
        val fixture = OAuthFixture().apply { block = true }
        val engine = BrowserOAuth(fixture, fixture.settings, fixture.foreground, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val job = launch { engine.authorize(OpenAiBrowser, fixture::opened) }
        runCurrent()
        job.cancelAndJoin()
        assertTrue(fixture.closed)
        assertEquals(0, fixture.exchanges)
        fixture.closed = false
        var expiration: ProviderOAuthError? = null
        launch { expiration = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.exceptionOrNull() as? ProviderOAuthError }
        advanceUntilIdle()
        assertEquals(ProviderOAuthStage.Expired, expiration?.stage)
        assertTrue(fixture.closed)
        assertEquals(0, fixture.exchanges)
        assertFalse(fixture.settings.toString().contains("test-refresh"))
    }
}
