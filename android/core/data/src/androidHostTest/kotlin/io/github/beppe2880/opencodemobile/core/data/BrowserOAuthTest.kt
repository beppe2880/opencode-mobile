package io.github.beppe2880.opencodemobile.core.data

import com.russhwolf.settings.MapSettings
import io.github.beppe2880.opencodemobile.core.data.integration.AndroidOAuthPlatform
import io.github.beppe2880.opencodemobile.core.data.integration.BrowserOAuth
import io.github.beppe2880.opencodemobile.core.data.integration.OAuthLoopback
import io.github.beppe2880.opencodemobile.core.data.integration.OAuthPlatform
import io.github.beppe2880.opencodemobile.core.data.integration.OpenAiBrowser
import io.github.beppe2880.opencodemobile.core.data.integration.base64
import io.github.beppe2880.opencodemobile.core.data.integration.now
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
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
internal class OAuthFixture : OAuthPlatform by AndroidOAuthPlatform() {
    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    val settings = MapSettings()
    var authorization: Url? = null
    var tokenForm: Map<String, String>? = null
    var closed = false
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
    var exchanges = 0
    private var state = ""

    override fun listen(state: String): OAuthLoopback {
        this.state = state
        return object : OAuthLoopback {
            override val redirect = "http://127.0.0.1:54321/auth/callback"
            override suspend fun receive(): Map<String, String> {
                if (block) CompletableDeferred<Unit>().await()
                return buildMap {
                    put("state", callbackState ?: state)
                    if (callbackError) put("error", "access_denied") else put("code", "test-code")
                    if (!missingClient) put("client_id", "issued-client")
                }
            }
            override fun close() { closed = true }
        }
    }

    fun opened(url: String) { authorization = Url(url) }

    override suspend fun json(url: String, form: Map<String, String>?): JsonObject {
        check(!httpFailure)
        if (form == null) return jwks()
        exchanges++
        tokenForm = form
        return buildJsonObject {
            put("access_token", "test-access")
            put("refresh_token", "test-refresh")
            put("expires_in", 3600)
            put("scope", if (missingScope) "openid" else OpenAiBrowser.scopes)
            put("id_token", token())
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
        val engine = BrowserOAuth(fixture, fixture.settings, testScheduler.let { kotlinx.coroutines.test.StandardTestDispatcher(it) })
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
            val engine = BrowserOAuth(fixture, fixture.settings, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
            assertTrue(runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.isFailure)
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
            val engine = BrowserOAuth(fixture, fixture.settings, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
            assertTrue(runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.isFailure)
            assertTrue(fixture.closed)
        }
    }

    @Test fun `cancellation and expiry always close callback without storing tokens`() = runTest {
        val fixture = OAuthFixture().apply { block = true }
        val engine = BrowserOAuth(fixture, fixture.settings, kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val job = launch { engine.authorize(OpenAiBrowser, fixture::opened) }
        runCurrent()
        job.cancelAndJoin()
        assertTrue(fixture.closed)
        assertEquals(0, fixture.exchanges)
        fixture.closed = false
        var failed = false
        launch { failed = runCatching { engine.authorize(OpenAiBrowser, fixture::opened) }.isFailure }
        advanceUntilIdle()
        assertTrue(failed)
        assertTrue(fixture.closed)
        assertEquals(0, fixture.exchanges)
        assertFalse(fixture.settings.toString().contains("test-refresh"))
    }
}
