package io.github.beppe2880.opencodemobile.core.data.session

import io.github.beppe2880.opencodemobile.api.apis.SessionApi
import io.github.beppe2880.opencodemobile.api.models.LocationPublicRef
import io.github.beppe2880.opencodemobile.api.models.SessionCreateRequest
import io.github.beppe2880.opencodemobile.api.models.SessionForkRequest
import io.github.beppe2880.opencodemobile.api.models.SessionUpdateRequest
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.cache.ListCache
import io.github.beppe2880.opencodemobile.core.data.cache.SessionListCodec
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.UsageStats
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.awaitSuccess
import io.github.beppe2880.opencodemobile.core.network.mapping.toApi
import io.github.beppe2880.opencodemobile.core.network.mapping.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Sessions from the active server; the unsearched lists and projects are also kept per server in [lists]. */
internal class RemoteSessionRepository(
    private val clients: OpenCodeClients,
    private val lists: ListCache = ListCache.None,
) : SessionRepository {

    /** Sessions last seen, by server and session id, so a chat can show its header at once. */
    private val seen = ConcurrentHashMap<Pair<String, String>, Session>()

    override fun cached(sessionId: String): Session? =
        clients.activeIdOrNull()?.let { seen[it to sessionId] }

    @Volatile
    private var browseOrder: List<String> = emptyList()

    override fun browseOrder(): List<String> = browseOrder

    override fun setBrowseOrder(sessionIds: List<String>) {
        browseOrder = sessionIds
    }

    private fun remember(sessions: List<Session>) {
        remember(clients.activeIdOrNull() ?: return, sessions)
    }

    private fun remember(serverId: String, sessions: List<Session>) {
        sessions.forEach { seen[serverId to it.id] = it }
    }

    override suspend fun list(search: String?, directory: String?, limit: Int): Result<List<Session>> = resultOf {
        // Resolved once, so a server switch during the request never files the list under the new server.
        val profile = clients.activeProfile()
        val api = clients.forProfile(profile).sessions
        val query = search?.trim()?.takeIf { it.isNotEmpty() }
        val sessions = awaitBody {
            api.sessionList(
                limit = limit.toString(),
                order = SessionApi.OrderSessionList.DESC,
                search = query,
                parentID = ROOT_SESSIONS,
                directory = directory,
            )
        }.data.map { it.toDomain() }
        remember(profile.id, sessions)
        if (query == null) lists.write(profile.id, ListCache.sessionsKey(directory), SessionListCodec.encodeSessions(sessions))
        sessions
    }

    override suspend fun cachedList(serverId: String, directory: String?): List<Session>? {
        val raw = lists.read(serverId, ListCache.sessionsKey(directory)) ?: return null
        return withContext(Dispatchers.Default) { SessionListCodec.decodeSessions(raw) }?.also { remember(serverId, it) }
    }

    override suspend fun cachedProjects(serverId: String): List<Project>? {
        val raw = lists.read(serverId, ListCache.PROJECTS_KEY) ?: return null
        return withContext(Dispatchers.Default) { SessionListCodec.decodeProjects(raw) }
    }

    override suspend fun active(): Result<Set<String>> = resultOf {
        val api = clients.active().sessions
        awaitBody { api.sessionActive() }.data.keys
    }

    override suspend fun get(sessionId: String): Result<Session> = resultOf {
        val api = clients.active().sessions
        awaitBody { api.sessionGet(sessionId) }.data.toDomain().also { remember(listOf(it)) }
    }

    override suspend fun create(directory: String, agent: String?, model: ModelRef?): Result<Session> = resultOf {
        val api = clients.active().sessions
        val request = SessionCreateRequest(location = LocationPublicRef(directory), agent = agent, model = model?.toApi())
        awaitBody { api.sessionCreate(request) }.data.toDomain().also { remember(listOf(it)) }
    }

    override suspend fun rename(sessionId: String, title: String): Result<Unit> = resultOf {
        val profile = clients.activeProfile()
        val api = clients.forProfile(profile).sessions
        awaitSuccess { api.sessionUpdate(sessionId, SessionUpdateRequest(title = title)) }
        seen[profile.id to sessionId]?.let { seen[profile.id to sessionId] = it.copy(title = title) }
    }

    override suspend fun delete(sessionId: String): Result<Unit> = resultOf {
        val profile = clients.activeProfile()
        val api = clients.forProfile(profile).sessions
        awaitSuccess { api.sessionRemove(sessionId) }
        seen.remove(profile.id to sessionId)
    }

    override suspend fun fork(sessionId: String, beforeMessageId: String?): Result<Session> = resultOf {
        val api = clients.active().sessions
        awaitBody { api.sessionFork(sessionId, SessionForkRequest(before = beforeMessageId)) }.data.toDomain()
            .also { remember(listOf(it)) }
    }

    override suspend fun projects(): Result<List<Project>> = resultOf {
        val profile = clients.activeProfile()
        val api = clients.forProfile(profile).projects
        awaitBody { api.projectList() }.map { it.toDomain() }.sortedByDescending { it.updatedAt }
            .also { lists.write(profile.id, ListCache.PROJECTS_KEY, SessionListCodec.encodeProjects(it)) }
    }

    override suspend fun usage(from: Long, to: Long, timeZone: String): Result<UsageStats> = resultOf {
        val api = clients.active().sessions
        awaitBody {
            api.experimentalSessionStats(
                from = from.toString(),
                to = to.toString(),
                timezone = timeZone,
                tools = SessionApi.ToolsExperimentalSessionStats.NONE,
            )
        }.data.toDomain()
    }

    private companion object {
        /** `parentID=null` asks the server for root sessions only. */
        const val ROOT_SESSIONS = "null"
    }
}
