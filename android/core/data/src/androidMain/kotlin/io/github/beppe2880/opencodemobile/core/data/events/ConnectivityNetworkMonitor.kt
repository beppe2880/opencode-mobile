package io.github.beppe2880.opencodemobile.core.data.events

import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** [NetworkMonitor] backed by a default network callback, registered only while collected. */
internal class ConnectivityNetworkMonitor(private val connectivity: ConnectivityManager) : NetworkMonitor {
    override val connected: Flow<Boolean> = callbackFlow {
        // Null when there is no default network or it is blocked for this app.
        val tracker = DefaultNetworkTracker(connectivity.activeNetwork)
        trySend(tracker.connected)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                tracker.onAvailable(network)?.let(::trySend)
            }

            override fun onLost(network: Network) {
                tracker.onLost(network)?.let(::trySend)
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                tracker.onBlockedStatusChanged(network, blocked)?.let(::trySend)
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }
}

/**
 * State of the default network as reported by its callbacks, which run on another thread.
 * Each callback receives a new [Network] instance, so networks are compared with `equals`
 * (same network id), never by identity. Each method returns the state to report, or null.
 */
internal class DefaultNetworkTracker<N : Any>(initial: N?) {
    private var current: N? = initial
    private var blocked = false

    val connected: Boolean @Synchronized get() = current != null && !blocked

    /** A new default network is worth reporting even when one was present: retries can start at once. */
    @Synchronized
    fun onAvailable(network: N): Boolean? {
        if (network == current) return null
        current = network
        blocked = false
        return true
    }

    /** Only the loss of the current default network matters; an old one may be reported after its successor. */
    @Synchronized
    fun onLost(network: N): Boolean? {
        if (network != current) return null
        current = null
        blocked = false
        return false
    }

    @Synchronized
    fun onBlockedStatusChanged(network: N, blocked: Boolean): Boolean? {
        if (network != current || blocked == this.blocked) return null
        this.blocked = blocked
        return !blocked
    }
}
