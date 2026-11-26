package io.github.beppe2880.opencodemobile.core.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.URLBuilder
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.posix.AF_UNSPEC
import platform.posix.EINPROGRESS
import platform.posix.F_GETFL
import platform.posix.F_SETFL
import platform.posix.IPPROTO_TCP
import platform.posix.O_NONBLOCK
import platform.posix.POLLOUT
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_ERROR
import platform.posix.addrinfo
import platform.posix.close
import platform.posix.connect
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.freeaddrinfo
import platform.posix.getaddrinfo
import platform.posix.getsockopt
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.socket
import platform.posix.socklen_tVar
import kotlin.concurrent.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * NSURLSession has no connect timeout: an unreachable server only fails after the socket
 * timeout (30 s), while OkHttp gives up after the connect timeout. Before calling a host that
 * has not answered recently, this opens a plain TCP connection with the request's connect
 * timeout and fails fast with [ConnectTimeoutException] when nothing answers.
 */
internal val ConnectProbe = createClientPlugin("ConnectProbe") {
    val reachable = ReachableHosts()
    on(Send) { request ->
        val timeout = request.getCapabilityOrNull(HttpTimeoutCapability)?.connectTimeoutMillis
        val host = request.url.host
        val port = request.url.connectPort()
        val key = "$host:$port"
        if (timeout != null && !reachable.recently(key) && !tcpConnects(host, port, timeout)) {
            throw ConnectTimeoutException("Connect timeout has expired [url=${request.url}, connect_timeout=$timeout ms]")
        }
        // Any answer, even an error status, proves the server is reachable.
        proceed(request).also { reachable.mark(key) }
    }
}

/**
 * The TCP port of the request. [URLBuilder.port] is 0 when the URL has none (for example
 * `https://host`); the built [io.ktor.http.Url] falls back to the scheme's default port.
 */
internal fun URLBuilder.connectPort(): Int = build().port

/** Hosts that answered within [TTL]; their calls skip the probe. */
private class ReachableHosts {
    private val seen = AtomicReference(emptyMap<String, Instant>())

    fun recently(key: String): Boolean {
        val at = seen.value[key] ?: return false
        return Clock.System.now() - at < TTL
    }

    fun mark(key: String) {
        while (true) {
            val current = seen.value
            if (seen.compareAndSet(current, current + (key to Clock.System.now()))) return
        }
    }

    private companion object {
        val TTL = 30.seconds
    }
}

/** True when a TCP connection to [host]:[port] opens within [timeoutMillis]. */
@OptIn(ExperimentalForeignApi::class)
private suspend fun tcpConnects(host: String, port: Int, timeoutMillis: Long): Boolean = withContext(Dispatchers.IO) {
    memScoped {
        val hints = alloc<addrinfo>().apply {
            ai_family = AF_UNSPEC
            ai_socktype = SOCK_STREAM
            ai_protocol = IPPROTO_TCP
        }
        val result = allocPointerTo<addrinfo>()
        if (getaddrinfo(host, port.toString(), hints.ptr, result.ptr) != 0) return@memScoped false
        try {
            var info = result.value
            val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMillis
            while (info != null) {
                val address = info.pointed
                val left = deadline - Clock.System.now().toEpochMilliseconds()
                if (left <= 0) return@memScoped false
                if (connectOnce(address.ai_family, address.ai_addr, address.ai_addrlen, left)) return@memScoped true
                info = address.ai_next
            }
            false
        } finally {
            freeaddrinfo(result.value)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun connectOnce(
    family: Int,
    address: kotlinx.cinterop.CPointer<platform.posix.sockaddr>?,
    length: platform.posix.socklen_t,
    timeoutMillis: Long,
): Boolean {
    val fd = socket(family, SOCK_STREAM, IPPROTO_TCP)
    if (fd < 0) return false
    try {
        fcntl(fd, F_SETFL, fcntl(fd, F_GETFL, 0) or O_NONBLOCK)
        if (connect(fd, address, length) == 0) return true
        if (errno != EINPROGRESS) return false
        return memScoped {
            val poller = alloc<pollfd>().apply {
                this.fd = fd
                events = POLLOUT.convert()
            }
            if (poll(poller.ptr, 1u, timeoutMillis.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) <= 0) return@memScoped false
            val error = alloc<IntVar>()
            val size = alloc<socklen_tVar>().apply { value = 4u }
            getsockopt(fd, SOL_SOCKET, SO_ERROR, error.ptr, size.ptr) == 0 && error.value == 0
        }
    } finally {
        close(fd)
    }
}
