package io.github.beppe2880.opencodemobile.core.data.events

/**
 * Turns successive network path updates (iOS `NWPathMonitor`) into [NetworkMonitor] reports:
 * the first state, every usable/unusable change, and a usable path that moved to other
 * interfaces (Wi-Fi to cellular), which is a new network worth retrying on at once.
 */
internal class PathReporter {
    private var last: Pair<Boolean, Set<String>>? = null

    /** Returns the state to report for this update, or null when nothing changed. */
    fun onPath(usable: Boolean, interfaces: Set<String>): Boolean? {
        val previous = last
        last = usable to interfaces
        return when {
            previous == null || usable != previous.first -> usable
            usable && interfaces != previous.second -> true
            else -> null
        }
    }
}
