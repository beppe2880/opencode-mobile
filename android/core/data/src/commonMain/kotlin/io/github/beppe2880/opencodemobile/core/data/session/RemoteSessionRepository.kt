package io.github.beppe2880.opencodemobile.core.data.session

import io.github.beppe2880.opencodemobile.api.apis.SessionApi
import io.github.beppe2880.opencodemobile.api.models.LocationPublicRef
import io.github.beppe2880.opencodemobile.api.models.SessionCreateRequest
import io.github.beppe2880.opencodemobile.api.models.SessionForkRequest
import io.github.beppe2880.opencodemobile.api.models.SessionUpdateRequest
import io.github.beppe2880.opencodemobile.api.models.SessionViewRequest
import io.github.beppe2880.opencodemobile.core.data.ConcurrentMap
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.cache.ExportCache
import io.github.beppe2880.opencodemobile.core.data.cache.ListCache
import io.github.beppe2880.opencodemobile.core.data.cache.NativeExport
import io.github.beppe2880.opencodemobile.core.data.cache.SessionListCodec
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.AppVersion
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionExportProgress
import io.github.beppe2880.opencodemobile.core.domain.SessionExportStage
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.UsageStats
import io.github.beppe2880.opencodemobile.core.network.MessageCodec
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.awaitSuccess
import io.github.beppe2880.opencodemobile.core.network.mapping.toApi
import io.github.beppe2880.opencodemobile.core.network.mapping.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.TimeSource

/** Sessions from the active server; the unsearched lists and projects are also kept per server in [lists]. */
internal class RemoteSessionRepository(
    private val clients: OpenCodeClients,
    private val lists: ListCache = ListCache.None,
    private val exports: ExportCache = ExportCache(),
) : SessionRepository {

    /** Archiving, with the archived list, exists since OpenCode 2.1.0. */
    private val archiveSince = requireNotNull(AppVersion.parse("2.1.0"))

    /** Sessions last seen, by server and session id, so a chat can show its header at once. */
    private val seen = ConcurrentMap<Pair<String, String>, Session>()

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

    override suspend fun list(search: String?, directory: String?, limit: Int, archived: Boolean?): Result<List<Session>> = resultOf {
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
                archived = archived?.let { if (it) SessionApi.ArchivedSessionList.TRUE else SessionApi.ArchivedSessionList.FALSE },
                directory = directory,
            )
        }.data.map { it.toDomain() }
        remember(profile.id, sessions)
        if (query == null && archived != true) {
            lists.write(profile.id, ListCache.sessionsKey(directory), SessionListCodec.encodeSessions(sessions))
        }
        sessions
    }

    override suspend fun listUntil(
        search: String?,
        directory: String?,
        limit: Int,
        archived: Boolean?,
        notBefore: Long,
    ): Result<List<Session>> = resultOf {
        val profile = clients.activeProfile()
        val api = clients.forProfile(profile).sessions
        val query = search?.trim()?.takeIf { it.isNotEmpty() }
        val pageSize = minOf(limit, PAGE_SIZE)
        val seen = HashSet<String>()
        val sessions = mutableListOf<Session>()
        var cursor: String? = null
        var pages = 0
        do {
            val page = awaitBody {
                api.sessionList(
                    limit = pageSize.toString(),
                    order = SessionApi.OrderSessionList.DESC,
                    search = query,
                    parentID = ROOT_SESSIONS,
                    archived = archived?.let { if (it) SessionApi.ArchivedSessionList.TRUE else SessionApi.ArchivedSessionList.FALSE },
                    directory = directory,
                    cursor = cursor,
                )
            }
            val batch = page.data.map { it.toDomain() }
            // A server without cursor support repeats the first page; the ids show when to stop.
            val added = batch.filter { seen.add(it.id) }
            sessions += added
            cursor = page.cursor.next
            pages++
        } while (
            added.isNotEmpty() && cursor != null && sessions.size < limit && pages < MAX_PAGES &&
            batch.lastOrNull()?.updatedAt?.let { it >= notBefore } == true
        )
        val result = sessions.take(limit)
        remember(profile.id, result)
        if (query == null && archived != true) {
            lists.write(profile.id, ListCache.sessionsKey(directory), SessionListCodec.encodeSessions(result))
        }
        result
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
        exports.invalidate(profile.id, sessionId)
        seen[profile.id to sessionId]?.let { seen[profile.id to sessionId] = it.copy(title = title) }
    }

    override suspend fun delete(sessionId: String): Result<Unit> = resultOf {
        val profile = clients.activeProfile()
        val api = clients.forProfile(profile).sessions
        awaitSuccess { api.sessionRemove(sessionId) }
        exports.invalidate(profile.id, sessionId)
        lists.removeSession(profile.id, sessionId)
        seen.remove(profile.id to sessionId)
    }

    override suspend fun setArchived(sessionId: String, archived: Boolean): Result<Unit> = resultOf {
        val profile = clients.activeProfile()
        val api = clients.forProfile(profile).sessions
        awaitSuccess { api.sessionUpdate(sessionId, SessionUpdateRequest(archived = archived)) }
        exports.invalidate(profile.id, sessionId)
        seen[profile.id to sessionId]?.let { session ->
            seen[profile.id to sessionId] = session.copy(
                archivedAt = if (archived) Clock.System.now().toEpochMilliseconds() else null,
            )
        }
        if (archived) lists.removeSession(profile.id, sessionId)
    }

    override suspend fun archiveSupported(): Boolean = resultOf {
        val profile = clients.activeProfile()
        awaitBody { clients.forProfile(profile).server.serverInfo() }.version
    }.map { version -> AppVersion.parse(version)?.let { it >= archiveSince } ?: true }
        .getOrDefault(true)

    override suspend fun markViewed(sessionId: String, idleAt: Long): Result<Unit> = resultOf {
        val profile = clients.activeProfile()
        val api = clients.forProfile(profile).sessions
        // The server ignores an idle it no longer has and never moves `viewed` backwards.
        awaitSuccess { api.sessionView(sessionId, SessionViewRequest(idle = idleAt.toDouble())) }
        exports.invalidate(profile.id, sessionId)
        seen[profile.id to sessionId]?.let { seen[profile.id to sessionId] = it.copy(viewedAt = idleAt) }
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

    override suspend fun export(sessionId: String, onProgress: suspend (SessionExportProgress) -> Unit): Result<List<Message>> = resultOf {
        val native = nativeExport(sessionId, onProgress)
        onProgress(SessionExportProgress(SessionExportStage.Decoding))
        val document = native.document ?: Json.parseToJsonElement(native.json) as JsonObject
        val messages = document["messages"] as JsonArray
        val result = mutableListOf<Message>()
        var lastUpdate = TimeSource.Monotonic.markNow()
        onProgress(SessionExportProgress(SessionExportStage.PreparingMessages, 0, messages.size.toLong()))
        messages.forEachIndexed { index, element ->
            MessageCodec.decode(element.toString())?.toDomain()?.let(result::add)
            if (index == messages.lastIndex || lastUpdate.elapsedNow().inWholeMilliseconds >= 50) {
                onProgress(SessionExportProgress(SessionExportStage.PreparingMessages, index + 1L, messages.size.toLong()))
                lastUpdate = TimeSource.Monotonic.markNow()
            }
        }
        result
    }

    override suspend fun exportJson(sessionId: String, onProgress: suspend (SessionExportProgress) -> Unit): Result<String> = resultOf {
        nativeExport(sessionId, onProgress).json.also { onProgress(SessionExportProgress(SessionExportStage.Preparing)) }
    }

    private suspend fun nativeExport(sessionId: String, onProgress: suspend (SessionExportProgress) -> Unit): NativeExport {
        val profile = clients.activeProfile()
        val client = clients.forProfile(profile)
        return exports.load(profile.id, sessionId) {
            onProgress(SessionExportProgress(SessionExportStage.WaitingForServer))
            // Reuse the generated request and HTTP error handling, but do not decode through DTOs:
            // older/newer provider metadata and unknown message kinds must survive a JSON export.
            val body = client.downloadSessionExport(
                sessionId,
                onDownload = { received, total -> onProgress(SessionExportProgress(SessionExportStage.Downloading, received, total)) },
                onDownloaded = { onProgress(SessionExportProgress(SessionExportStage.Decoding)) },
            )
            val data = body["data"] as? JsonObject
            if (data == null || data["info"] !is JsonObject || data["messages"] !is JsonArray) {
                throw OpenCodeError.Server(status = 0, tag = null, message = "Invalid session export document")
            }
            // Remove only the HTTP envelope. This is the native document accepted by session import.
            onProgress(SessionExportProgress(SessionExportStage.Preparing))
            NativeExport(data.toString(), data)
        }
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

        /** Sessions read per page while filling a date range. */
        const val PAGE_SIZE = 200

        /** A server that ignores the cursor would repeat a page; the id check stops first. */
        const val MAX_PAGES = 10
    }
}
