package io.github.beppe2880.opencodemobile.core.data.events

import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicReference

/** [NetworkMonitor] backed by a default network callback, registered only while collected. */
internal class ConnectivityNetworkMonitor(private val connectivity: ConnectivityManager) : NetworkMonitor {
    override val connected: Flow<Boolean> = callbackFlow {
        // The callback fires for the current network right after registration.
        val current = AtomicReference<Network?>(connectivity.activeNetwork)
        trySend(current.get() != null)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (current.getAndSet(network) != network) trySend(true)
            }

            override fun onLost(network: Network) {
                if (current.compareAndSet(network, null)) trySend(false)
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }
}
