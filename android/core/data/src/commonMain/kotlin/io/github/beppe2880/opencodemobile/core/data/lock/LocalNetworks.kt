package io.github.beppe2880.opencodemobile.core.data.lock

import io.github.beppe2880.opencodemobile.core.domain.WifiNetworks

/** An IPv4 subnet of one of the phone's networks: [address] with [prefixLength] network bits. */
internal class Subnet(val address: ByteArray, val prefixLength: Int) {
    operator fun contains(ip: ByteArray): Boolean {
        if (ip.size != 4 || address.size != 4) return false
        var bits = prefixLength.coerceIn(0, 32)
        for (i in 0 until 4) {
            if (bits <= 0) return true
            val mask = if (bits >= 8) 0xFF else (0xFF shl (8 - bits)) and 0xFF
            if ((ip[i].toInt() and mask) != (address[i].toInt() and mask)) return false
            bits -= 8
        }
        return true
    }
}

/** The phone's local networks: Wi-Fi and Ethernet, never VPN tunnels or mobile data. */
internal interface LocalNetworks : WifiNetworks {
    /** The IPv4 subnets of the Wi-Fi and Ethernet interfaces. */
    fun subnets(): List<Subnet>
}

/** The four bytes of a dotted IPv4 address, or null for anything else (host names, IPv6). */
internal fun ipv4Bytes(host: String): ByteArray? {
    val parts = host.split('.')
    if (parts.size != 4) return null
    val bytes = parts.map { part -> part.toIntOrNull()?.takeIf { it in 0..255 && part.length in 1..3 } ?: return null }
    return ByteArray(4) { bytes[it].toByte() }
}

/** The host of an `http(s)://host[:port][/path]` address. */
internal fun hostOf(baseUrl: String): String = baseUrl.substringAfter("://").substringBefore('/').substringBefore('?').let { authority ->
    if (authority.startsWith("[")) authority.substringBefore(']').removePrefix("[") else authority.substringBefore(':')
}
