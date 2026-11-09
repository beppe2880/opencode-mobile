package io.github.beppe2880.opencodemobile.core.data.integration

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OAuthSecurityTest {
    @Test fun onlyTlsOrLiteralLoopbackAllowsCredentialImport() {
        listOf("https://server.example:4096", "http://127.0.0.1:4096", "http://localhost:4096", "http://[::1]:4096").forEach {
            assertTrue(protectedOAuthTransport(it), it)
        }
        listOf(
            "http://192.168.1.2:4096", "http://100.64.0.2:4096", "http://10.0.2.2:4096", "http://127.0.0.1.evil.example",
            "http://2130706433", "https://user:secret@server.example", "file:///local", "ftp://localhost",
        ).forEach { assertFalse(protectedOAuthTransport(it), it) }
    }

    @Test fun unavailablePlatformNeverAdvertisesBrowserSupport() {
        assertFalse(UnsupportedOAuthPlatform().supported)
    }
}
