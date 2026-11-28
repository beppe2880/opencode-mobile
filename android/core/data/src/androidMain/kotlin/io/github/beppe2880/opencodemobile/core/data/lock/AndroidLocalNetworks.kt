package io.github.beppe2880.opencodemobile.core.data.lock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address

/** Wi-Fi and Ethernet networks from [ConnectivityManager]; the Wi-Fi name needs the location permission. */
internal class AndroidLocalNetworks(private val context: Context) : LocalNetworks {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    @Suppress("DEPRECATION") // allNetworks: the callback API is overkill for a one-shot read.
    override fun subnets(): List<Subnet> = connectivity.allNetworks
        .filter { network ->
            val caps = connectivity.getNetworkCapabilities(network) ?: return@filter false
            !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        }
        .flatMap { connectivity.getLinkProperties(it)?.linkAddresses.orEmpty() }
        .filter { it.address is Inet4Address && !it.address.isLoopbackAddress }
        .map { Subnet(it.address.address, it.prefixLength) }

    // WifiManager.connectionInfo is deprecated but still the simplest read of the current SSID;
    // the system returns "<unknown ssid>" without the fine location permission or with location off.
    @Suppress("DEPRECATION")
    override suspend fun currentName(): String? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return@withContext null
        }
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return@withContext null
        wifi.connectionInfo?.ssid
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID && it != "<unknown ssid>" }
    }
}
