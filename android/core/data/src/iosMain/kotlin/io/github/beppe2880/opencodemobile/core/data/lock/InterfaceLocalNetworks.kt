package io.github.beppe2880.opencodemobile.core.data.lock

import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.AF_INET
import platform.posix.IFF_LOOPBACK
import platform.posix.IFF_UP
import platform.posix.sockaddr_in

/**
 * The IPv4 subnets of the Wi-Fi and Ethernet interfaces (`en*`); VPN tunnels (`utun*`, `ipsec*`)
 * and cellular (`pdp_ip*`) are skipped. The Wi-Fi name needs the Access Wi-Fi Information
 * entitlement, which the app does not have yet: it is never known.
 */
internal class InterfaceLocalNetworks : LocalNetworks {

    override suspend fun currentName(): String? = null

    override fun subnets(): List<Subnet> = memScoped {
        val list = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(list.ptr) != 0) return@memScoped emptyList()
        val subnets = mutableListOf<Subnet>()
        try {
            var current = list.value
            while (current != null) {
                val entry = current.pointed
                val name = entry.ifa_name?.toKString().orEmpty()
                val flags = entry.ifa_flags.toInt()
                val address = entry.ifa_addr
                val netmask = entry.ifa_netmask
                if (name.startsWith("en") && flags and IFF_UP != 0 && flags and IFF_LOOPBACK == 0 &&
                    address != null && netmask != null && address.pointed.sa_family.toInt() == AF_INET
                ) {
                    val ip = address.reinterpret<sockaddr_in>().pointed.sin_addr.s_addr
                    val mask = netmask.reinterpret<sockaddr_in>().pointed.sin_addr.s_addr
                    subnets += Subnet(ip.networkBytes(), mask.networkBytes().prefixLength())
                }
                current = entry.ifa_next
            }
        } finally {
            freeifaddrs(list.value)
        }
        subnets
    }

    /** `in_addr_t` holds the address in network byte order; on little-endian iOS the first byte is the lowest. */
    private fun UInt.networkBytes(): ByteArray = ByteArray(4) { i -> (this shr (8 * i)).toByte() }

    private fun ByteArray.prefixLength(): Int = sumOf { (it.toInt() and 0xFF).countOneBits() }
}
