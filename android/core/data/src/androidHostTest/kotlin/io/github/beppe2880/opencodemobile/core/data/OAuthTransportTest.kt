package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.integration.AndroidOAuthPlatform
import io.github.beppe2880.opencodemobile.core.data.integration.OAuthTransportError
import io.github.beppe2880.opencodemobile.core.data.integration.oauthTransportKind
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ProtocolException
import java.net.ServerSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

class OAuthTransportTest {
    @Test fun `network classification inspects types never sensitive messages`() {
        val certificate = SSLHandshakeException("secret-url").apply { initCause(CertificateException("secret-code")) }
        val cases = listOf(
            certificate to ProviderOAuthTransport.Certificate,
            SSLPeerUnverifiedException("secret") to ProviderOAuthTransport.Certificate,
            SSLHandshakeException("secret") to ProviderOAuthTransport.TLS,
            UnknownHostException("secret-host") to ProviderOAuthTransport.DNS,
            ConnectException("secret") to ProviderOAuthTransport.Connect,
            NoRouteToHostException("secret") to ProviderOAuthTransport.Connect,
            SocketTimeoutException("secret") to ProviderOAuthTransport.Timeout,
            InterruptedIOException("secret") to ProviderOAuthTransport.Interrupted,
            SocketException("secret") to ProviderOAuthTransport.ConnectionIO,
            ProtocolException("secret") to ProviderOAuthTransport.Protocol,
            IOException("secret says SSL timeout DNS") to ProviderOAuthTransport.IO,
        )
        cases.forEach { (error, expected) -> assertEquals(expected, oauthTransportKind(error)) }
    }

    @Test fun `native DNS failure exposes category and actual elapsed time without host or exception`() = runBlocking {
        val client = OkHttpClient.Builder().dns { throw UnknownHostException("secret-host secret-code") }.retryOnConnectionFailure(false).build()
        val failure = runCatching { AndroidOAuthPlatform(client).json("https://oauth.invalid/token", mapOf("code" to "secret-code")) }
            .exceptionOrNull() as OAuthTransportError
        assertEquals(ProviderOAuthTransport.DNS, failure.kind)
        assertTrue(failure.elapsedMillis >= 0)
        assertNull(failure.cause)
        assertNull(failure.httpStatus)
        assertFalse(failure.toString().contains("secret"))
    }

    @Test fun `refused native connection is distinct from TLS and timeout`() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).callTimeout(3, TimeUnit.SECONDS).build()
        val failure = runCatching { AndroidOAuthPlatform(client).json("https://127.0.0.1:$port/token", emptyMap()) }
            .exceptionOrNull() as OAuthTransportError
        assertEquals(ProviderOAuthTransport.Connect, failure.kind)
        assertNull(failure.cause)
        assertTrue(failure.elapsedMillis >= 0)
    }

    @Test fun `native TLS handshake timeout is timed without retries or certificate bypass`() = runBlocking {
        val server = ServerSocket(0)
        val release = CompletableDeferred<Unit>()
        val accepted = CompletableDeferred<Unit>()
        val peer = async(Dispatchers.IO) {
            server.accept().use {
                accepted.complete(Unit)
                release.await()
            }
        }
        try {
            val client = OkHttpClient.Builder().retryOnConnectionFailure(false).readTimeout(200, TimeUnit.MILLISECONDS)
                .callTimeout(3, TimeUnit.SECONDS).build()
            val failure = runCatching { AndroidOAuthPlatform(client).json("https://127.0.0.1:${server.localPort}/token", emptyMap()) }
                .exceptionOrNull() as OAuthTransportError
            withTimeout(5000) { accepted.await() }
            assertEquals(ProviderOAuthTransport.Timeout, failure.kind)
            assertTrue(failure.elapsedMillis >= 100)
            assertNull(failure.cause)
        } finally {
            release.complete(Unit)
            server.close()
            withTimeout(5000) { peer.await() }
        }
    }

    @Test fun `native invalid TLS response is not mislabeled as OAuth rejection`() = runBlocking {
        val server = ServerSocket(0)
        val peer = async(Dispatchers.IO) {
            server.accept().use { socket ->
                socket.soTimeout = 3000
                val input = java.io.DataInputStream(socket.getInputStream())
                val header = ByteArray(5)
                input.readFully(header)
                val length = ((header[3].toInt() and 255) shl 8) or (header[4].toInt() and 255)
                input.readFully(ByteArray(length))
                socket.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".toByteArray())
            }
        }
        try {
            val client = OkHttpClient.Builder().retryOnConnectionFailure(false).callTimeout(3, TimeUnit.SECONDS).build()
            val failure = runCatching { AndroidOAuthPlatform(client).json("https://127.0.0.1:${server.localPort}/token", emptyMap()) }
                .exceptionOrNull() as OAuthTransportError
            assertEquals(ProviderOAuthTransport.TLS, failure.kind)
            assertNull(failure.httpStatus)
            assertNull(failure.cause)
        } finally {
            server.close()
            withTimeout(5000) { peer.await() }
        }
    }

    @Test fun `OkHttp call deadline is distinguished from generic interrupted IO`() = runBlocking {
        val server = ServerSocket(0)
        val release = CompletableDeferred<Unit>()
        val peer = async(Dispatchers.IO) { server.accept().use { release.await() } }
        try {
            val client = OkHttpClient.Builder().retryOnConnectionFailure(false).readTimeout(5, TimeUnit.SECONDS)
                .callTimeout(250, TimeUnit.MILLISECONDS).build()
            val failure = runCatching { AndroidOAuthPlatform(client).json("https://127.0.0.1:${server.localPort}/token", emptyMap()) }
                .exceptionOrNull() as OAuthTransportError
            assertEquals(ProviderOAuthTransport.Timeout, failure.kind)
            assertTrue(failure.elapsedMillis >= 100)
            assertNull(failure.cause)
        } finally {
            release.complete(Unit)
            server.close()
            withTimeout(5000) { peer.await() }
        }
    }
}
