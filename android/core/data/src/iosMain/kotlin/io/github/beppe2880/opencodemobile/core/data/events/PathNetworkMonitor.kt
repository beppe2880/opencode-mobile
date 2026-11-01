package io.github.beppe2880.opencodemobile.core.data.events

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.nw_interface_get_name
import platform.Network.nw_path_enumerate_interfaces
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfiable
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

/**
 * [NetworkMonitor] backed by `NWPathMonitor`: emits the first path update (the current state),
 * every usable/unusable change and a switch to other interfaces. A satisfiable path (an
 * on-demand VPN) counts as usable: connecting is what brings it up.
 */
@OptIn(ExperimentalForeignApi::class)
internal class PathNetworkMonitor : NetworkMonitor {
    override val connected: Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        // A serial queue: updates arrive one at a time and in order.
        val queue = dispatch_queue_create("io.github.beppe2880.opencodemobile.network", null)
        val reporter = PathReporter()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            val interfaces = mutableSetOf<String>()
            nw_path_enumerate_interfaces(path) { item ->
                nw_interface_get_name(item)?.toKString()?.let(interfaces::add)
                true
            }
            val usable = status == nw_path_status_satisfied || status == nw_path_status_satisfiable
            reporter.onPath(usable, interfaces)?.let(::trySend)
        }
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }
}
