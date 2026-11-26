package io.github.beppe2880.opencodemobile.core.network

import io.ktor.http.URLBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectProbeTest {
    @Test
    fun explicitPortIsUsed() {
        assertEquals(4096, URLBuilder("http://192.168.1.20:4096/api/info").connectPort())
    }

    @Test
    fun httpsWithoutPortUses443() {
        assertEquals(443, URLBuilder("https://abc.example.opentunnel.xyz/api/info").connectPort())
    }

    @Test
    fun httpWithoutPortUses80() {
        assertEquals(80, URLBuilder("http://server.tailnet.ts.net/api/info").connectPort())
    }
}
