package io.github.beppe2880.opencodemobile.core.data.session

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.MutedSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** One JSON list per saved server; corrupt or absent data means notifications are not muted. */
internal class PreferencesMutedSessionRepository(
    private val prefs: Settings,
    private val servers: ServerRepository,
    scope: CoroutineScope,
) : MutedSessionRepository {
    private val lock = SynchronizedObject()
    private val timed = MutableStateFlow(readTemporary(servers.activeServer.value?.id))
    private val ids = MutableStateFlow(read(servers.activeServer.value?.id) + timed.value.keys)
    override val muted = ids.asStateFlow()
    override val temporary = timed.asStateFlow()

    init {
        scope.launch {
            servers.activeServer.map { it?.id }.distinctUntilChanged().collect {
                synchronized(lock) { publish(servers.activeServer.value?.id) }
            }
        }
    }

    override fun setMuted(sessionId: String, muted: Boolean, untilIdleAfter: Long?) = synchronized(lock) {
        val serverId = servers.activeServer.value?.id ?: return@synchronized
        val next = if (muted && untilIdleAfter == null) read(serverId) + sessionId else read(serverId) - sessionId
        if (next.isEmpty()) prefs.remove(key(serverId)) else prefs.putString(key(serverId), Json.encodeToString(Entries, next.toList()))
        val temporary = readTemporary(serverId) - sessionId
        writeTemporary(serverId, if (muted && untilIdleAfter != null) temporary + (sessionId to untilIdleAfter) else temporary)
        publish(serverId)
    }

    override fun finishTemporary(sessionId: String, idleAt: Long?) = synchronized(lock) {
        val serverId = servers.activeServer.value?.id ?: return@synchronized
        val entries = readTemporary(serverId)
        val baseline = entries[sessionId] ?: return@synchronized
        if (idleAt == null || idleAt > baseline) {
            writeTemporary(serverId, entries - sessionId)
            publish(serverId)
        }
    }

    private fun publish(serverId: String?) {
        val next = readTemporary(serverId)
        ids.value = read(serverId) + next.keys
        timed.value = next
    }

    private fun readTemporary(serverId: String?): Map<String, Long> {
        val raw = serverId?.let { prefs.getStringOrNull(temporaryKey(it)) } ?: return emptyMap()
        return try {
            Json.decodeFromString(TemporaryEntries, raw)
        } catch (_: IllegalArgumentException) {
            emptyMap()
        }
    }

    private fun writeTemporary(serverId: String, entries: Map<String, Long>) {
        val key = temporaryKey(serverId)
        if (entries.isEmpty()) prefs.remove(key) else prefs.putString(key, Json.encodeToString(TemporaryEntries, entries))
    }

    private fun read(serverId: String?): Set<String> {
        val raw = serverId?.let { prefs.getStringOrNull(key(it)) } ?: return emptySet()
        return try {
            Json.decodeFromString(Entries, raw).toSet()
        } catch (_: SerializationException) {
            emptySet()
        } catch (_: IllegalArgumentException) {
            emptySet()
        }
    }

    companion object {
        private val Entries = ListSerializer(String.serializer())
        private val TemporaryEntries = MapSerializer(String.serializer(), Long.serializer())
        private fun key(serverId: String) = "muted_sessions/$serverId"
        private fun temporaryKey(serverId: String) = "muted_sessions_until_idle/$serverId"
        fun clearServer(prefs: Settings, serverId: String) {
            prefs.remove(key(serverId))
            prefs.remove(temporaryKey(serverId))
        }
    }
}
