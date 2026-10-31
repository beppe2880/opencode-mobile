package io.github.beppe2880.opencodemobile.core.data.catalog

import com.russhwolf.settings.Settings
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.RECENT_MODELS
import io.github.beppe2880.opencodemobile.core.domain.RecentModelRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Recently used models in app-private preferences, one list per saved server. */
internal class PreferencesRecentModelRepository(
    private val prefs: Settings,
    private val clients: OpenCodeClients,
) : RecentModelRepository {

    override fun recent(): List<ModelRef> {
        val stored = clients.activeIdOrNull()?.let { prefs.getStringOrNull(key(it)) } ?: return emptyList()
        return try {
            Json.decodeFromString(Entries, stored).mapNotNull { entry ->
                val (providerId, id) = entry.takeIf { it.size == 3 } ?: return@mapNotNull null
                if (providerId == null || id == null) null else ModelRef(id, providerId, entry[2])
            }
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    override fun used(model: ModelRef) {
        val serverId = clients.activeIdOrNull() ?: return
        val next = (listOf(model) + recent()).distinctBy { it.key }.take(RECENT_MODELS)
        prefs.putString(key(serverId), Json.encodeToString(Entries, next.map { listOf(it.providerId, it.id, it.variant) }))
    }

    companion object {
        private const val PREFIX = "recent_models/"
        private val Entries = ListSerializer(ListSerializer(String.serializer().nullable))

        private fun key(serverId: String) = "$PREFIX$serverId"

        /** Forgets the models used on [serverId], e.g. when the server is removed. */
        fun clearServer(prefs: Settings, serverId: String) = prefs.remove(key(serverId))
    }
}
