package io.github.beppe2880.opencodemobile.core.data.session

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.domain.UnreadSessionRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Session ids marked unread in app-private preferences, one JSON list per saved server. A missing
 * or unreadable list reads as no marks; the last mark removed deletes the entry.
 */
internal class PreferencesUnreadSessionRepository(
    private val prefs: Settings,
    private val clients: OpenCodeClients,
) : UnreadSessionRepository {
    private val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val changes: Flow<Unit> = changed

    override fun unread(): Set<String> = clients.activeIdOrNull()?.let(::read).orEmpty()

    override fun setUnread(sessionId: String, unread: Boolean) {
        val serverId = clients.activeIdOrNull() ?: return
        val current = read(serverId)
        val next = if (unread) current + sessionId else current - sessionId
        if (next == current) return
        if (next.isEmpty()) prefs.remove(key(serverId)) else prefs.putString(key(serverId), Json.encodeToString(Entries, next.toList()))
        changed.tryEmit(Unit)
    }

    private fun read(serverId: String): Set<String> {
        val stored = prefs.getStringOrNull(key(serverId)) ?: return emptySet()
        return try {
            Json.decodeFromString(Entries, stored).toSet()
        } catch (_: SerializationException) {
            emptySet()
        } catch (_: IllegalArgumentException) {
            emptySet()
        }
    }

    companion object {
        private const val PREFIX = "unread_sessions/"
        private val Entries = ListSerializer(String.serializer())

        private fun key(serverId: String) = "$PREFIX$serverId"

        /** Forgets the unread marks of [serverId], e.g. when the server is removed. */
        fun clearServer(prefs: Settings, serverId: String) = prefs.remove(key(serverId))
    }
}
