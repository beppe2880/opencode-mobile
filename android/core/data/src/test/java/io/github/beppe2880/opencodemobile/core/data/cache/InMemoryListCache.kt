package io.github.beppe2880.opencodemobile.core.data.cache

internal class InMemoryListCache : ListCache {
    val lists = mutableMapOf<Pair<String, String>, String>()

    override suspend fun read(serverId: String, key: String): String? = lists[serverId to key]

    override suspend fun write(serverId: String, key: String, json: String) {
        lists[serverId to key] = json
    }

    override suspend fun removeSession(serverId: String, sessionId: String) {
        lists.filterKeys { (id, key) -> id == serverId && key.startsWith("sessions:") }.forEach { (key, json) ->
            val sessions = SessionListCodec.decodeSessions(json) ?: return@forEach
            lists[key] = SessionListCodec.encodeSessions(sessions.filterNot { it.id == sessionId })
        }
    }

    override suspend fun clearServer(serverId: String) {
        lists.keys.removeAll { it.first == serverId }
    }
}
