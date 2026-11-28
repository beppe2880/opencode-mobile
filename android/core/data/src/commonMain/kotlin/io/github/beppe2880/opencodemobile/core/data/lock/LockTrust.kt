package io.github.beppe2880.opencodemobile.core.data.lock

import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Whether the phone is somewhere the app lock does not ask: on a trusted Wi-Fi network (by name),
 * or with the active server answering on the subnet of the phone's Wi-Fi with its credentials. A
 * VPN reaching the server from elsewhere does not count: the phone's own subnet is not the server's.
 */
internal class LockTrust(
    private val settings: SettingsRepository,
    private val servers: ServerRepository,
    private val clients: OpenCodeClients,
    private val networks: LocalNetworks,
) {
    suspend fun trusted(): Boolean {
        val s = settings.settings.value
        if (s.appLockTrustedWifi.isNotEmpty()) {
            val name = networks.currentName()
            if (name != null && name in s.appLockTrustedWifi) return true
        }
        return s.appLockTrustLocalServer && localServerAnswers()
    }

    private suspend fun localServerAnswers(): Boolean = withTimeoutOrNull(4.seconds) {
        servers.loaded.first { it }
        val profile = servers.activeServer.value ?: return@withTimeoutOrNull false
        val ip = ipv4Bytes(hostOf(profile.baseUrl)) ?: return@withTimeoutOrNull false
        if (networks.subnets().none { ip in it }) return@withTimeoutOrNull false
        resultOf { awaitBody { clients.forProfile(profile).server.serverInfo() } }.isSuccess
    } == true
}
