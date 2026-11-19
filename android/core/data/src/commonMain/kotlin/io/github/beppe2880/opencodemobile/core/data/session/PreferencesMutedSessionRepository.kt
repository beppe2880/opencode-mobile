package io.github.beppe2880.opencodemobile.core.data.session

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.domain.MutedSessionRepository
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** One JSON list per saved server; corrupt or absent data means notifications are not muted. */
internal class PreferencesMutedSessionRepository(
    private val prefs: Settings,
    private val servers: ServerRepository,
    scope: CoroutineScope,
) : MutedSessionRepository {
    private val ids = MutableStateFlow(read(servers.activeServer.value?.id))
    override val muted = ids.asStateFlow()

    init {
        scope.launch {
            servers.activeServer.map { it?.id }.distinctUntilChanged().collect { ids.value = read(it) }
        }
    }

    override fun setMuted(sessionId: String, muted: Boolean) {
        val serverId = servers.activeServer.value?.id ?: return
        val current = read(serverId)
        val next = if (muted) current + sessionId else current - sessionId
        if (next.isEmpty()) prefs.remove(key(serverId)) else prefs.putString(key(serverId), Json.encodeToString(Entries, next.toList()))
        ids.value = next
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
        private fun key(serverId: String) = "muted_sessions/$serverId"
        fun clearServer(prefs: Settings, serverId: String) = prefs.remove(key(serverId))
    }
}
