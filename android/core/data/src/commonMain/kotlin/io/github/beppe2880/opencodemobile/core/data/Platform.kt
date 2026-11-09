package io.github.beppe2880.opencodemobile.core.data

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.time.Clock

/** Wall-clock time in epoch milliseconds. */
internal fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** A small thread-safe map for caches shared between coroutines on different threads. */
internal class ConcurrentMap<K, V> {
    private val lock = SynchronizedObject()
    private val map = HashMap<K, V>()

    operator fun get(key: K): V? = synchronized(lock) { map[key] }

    operator fun set(key: K, value: V) {
        synchronized(lock) { map[key] = value }
    }

    fun remove(key: K): V? = synchronized(lock) { map.remove(key) }
    fun removeMatching(predicate: (K) -> Boolean) { synchronized(lock) { map.keys.removeAll(predicate) } }
}
