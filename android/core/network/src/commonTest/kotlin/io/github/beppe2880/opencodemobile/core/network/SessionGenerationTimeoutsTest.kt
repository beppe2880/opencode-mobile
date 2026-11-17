package io.github.beppe2880.opencodemobile.core.network

import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionGenerationTimeoutsTest {
    @Test
    fun generationCanWaitForTheCompleteAnswer() {
        val request = request("https://example.test/api/session/ses_test/generate")
        request.applySessionGenerationTimeout()
        val timeout = request.getCapabilityOrNull(HttpTimeoutCapability)
        assertEquals(300_000L, timeout?.requestTimeoutMillis)
        assertEquals(300_000L, timeout?.socketTimeoutMillis)
        assertNull(timeout?.connectTimeoutMillis)
    }

    @Test
    fun reverseProxyPrefixAndQueryDoNotChangeTheTimeout() {
        val request = request("https://example.test/proxy/api/session/ses_test/generate?extra=true")
        request.applySessionGenerationTimeout()
        assertEquals(300_000L, request.getCapabilityOrNull(HttpTimeoutCapability)?.socketTimeoutMillis)
    }

    @Test
    fun unrelatedRequestsKeepTheirNormalTimeouts() {
        for (path in listOf("/api/session/ses_test/prompt", "/api/session", "/api/experimental/generate")) {
            val request = request("https://example.test$path")
            request.applySessionGenerationTimeout()
            assertNull(request.getCapabilityOrNull(HttpTimeoutCapability))
        }
        val get = request("https://example.test/api/session/ses_test/generate").apply { method = HttpMethod.Get }
        get.applySessionGenerationTimeout()
        assertNull(get.getCapabilityOrNull(HttpTimeoutCapability))
    }

    private fun request(address: String) = HttpRequestBuilder().apply {
        url(address)
        method = HttpMethod.Post
    }
}
