package io.github.beppe2880.opencodemobile.core.data.server

import io.github.beppe2880.opencodemobile.core.data.lock.LocalNetworks
import io.github.beppe2880.opencodemobile.core.domain.ServerDiscovery
import io.github.beppe2880.opencodemobile.core.network.LanScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Scans the default OpenCode port on the IPv4 subnets of the phone's Wi-Fi and Ethernet
 * networks. VPN tunnels are skipped: their peers are not on a subnet the phone can enumerate.
 */
internal class LanServerDiscovery(
    private val networks: LocalNetworks,
    private val scanner: LanScanner = LanScanner(),
) : ServerDiscovery {

    override fun discover(): Flow<String> = flow {
        emitAll(scanner.scan(networks.subnets().flatMap { LanScanner.subnetHosts(it.address, it.prefixLength) }.distinct()))
    }
}
