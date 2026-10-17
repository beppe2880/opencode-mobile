package io.github.beppe2880.opencodemobile.core.data.chat

import io.github.beppe2880.opencodemobile.api.apis.SessionApi
import io.github.beppe2880.opencodemobile.api.models.FormReply
import io.github.beppe2880.opencodemobile.api.models.PermissionReply
import io.github.beppe2880.opencodemobile.api.models.SessionCommandRequest
import io.github.beppe2880.opencodemobile.api.models.SessionCompactRequest
import io.github.beppe2880.opencodemobile.api.models.SessionInboxDelivery
import io.github.beppe2880.opencodemobile.api.models.SessionInboxUpdateRequest
import io.github.beppe2880.opencodemobile.api.models.SessionMessageInfo
import io.github.beppe2880.opencodemobile.api.models.SessionPermissionReplyRequest
import io.github.beppe2880.opencodemobile.api.models.SessionPromptRequest
import io.github.beppe2880.opencodemobile.api.models.SessionRevertStageRequest
import io.github.beppe2880.opencodemobile.api.models.SessionShellRequest
import io.github.beppe2880.opencodemobile.api.models.SessionSwitchAgentRequest
import io.github.beppe2880.opencodemobile.api.models.SessionSwitchModelRequest
import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.cache.CachedEntry
import io.github.beppe2880.opencodemobile.core.data.cache.MessageCache
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import io.github.beppe2880.opencodemobile.core.domain.QueuedPrompt
import io.github.beppe2880.opencodemobile.core.network.MessageCodec
import io.github.beppe2880.opencodemobile.core.network.awaitBody
import io.github.beppe2880.opencodemobile.core.network.awaitSuccess
import io.github.beppe2880.opencodemobile.core.network.mapping.toApi
import io.github.beppe2880.opencodemobile.core.network.mapping.toDomain
import io.github.beppe2880.opencodemobile.core.network.mapping.toJson
import io.github.beppe2880.opencodemobile.core.network.mapping.toQueuedPrompt
import io.github.beppe2880.opencodemobile.core.network.receivedCount
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap

/**
 * Chat operations on the active server. Transcripts are mirrored in [cache]; [maxMessages]
 * bounds how much history one session keeps.
 */
internal class RemoteChatRepository(
    private val clients: OpenCodeClients,
    private val cache: MessageCache,
    private val maxMessages: Int = 1_000,
) : ChatRepository {

    private val syncLock = Mutex()

    /**
     * Per server and session, where [olderMessages] continues once the cache is full. It is
     * relative to what that call returned, so it is reset by [cachedMessages], which a chat
     * calls when it starts showing a transcript again.
     */
    private val beyondCache = ConcurrentHashMap<Pair<String, String>, Beyond>()

    override suspend fun cachedMessages(sessionId: String): List<Message> = try {
        withContext(Dispatchers.Default) {
            val serverId = clients.activeProfile().id
            beyondCache.remove(serverId to sessionId)
            cache.read(serverId, sessionId).entries.decodeOrNull().orEmpty().toTranscript()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Brings the newest part of the cached transcript up to date and returns the cached
     * transcript. Entries up to the newest settled one (all older than any running step) no
     * longer change, so only messages after that anchor are fetched, newest first, until it
     * shows up. Without a usable cache, or when the anchor is not found within [maxMessages],
     * the cache restarts from the newest page and [olderMessages] fills in the history.
     */
    override suspend fun messages(sessionId: String): Result<List<Message>> = resultOf {
        val serverId = clients.activeProfile().id
        syncLock.withLock {
            val stored = cache.read(serverId, sessionId)
            val settled = stored.entries.decodeOrNull().orEmpty().takeWhile { MessageCodec.isSettled(it.info) }
            val anchor = settled.lastOrNull()?.id
            val fetched = fetchNewer(sessionId, anchor)
            val newer = fetched.newestFirst.asReversed()
            val kept = if (fetched.reachedAnchor) settled else emptyList()
            val olderCursor = if (fetched.reachedAnchor) stored.olderCursor else fetched.olderCursor
            val max = maxOf(maxMessages, newer.size)
            cache.write(serverId, sessionId, kept.lastOrNull()?.id, newer.toEntries(), olderCursor, max)
            (kept + newer).takeLast(max).toTranscript()
        }
    }

    override suspend fun messageImages(sessionId: String, messageId: String): Result<List<String>> = resultOf {
        val api = clients.active().sessions
        val user = awaitBody { api.sessionMessageGet(sessionId, messageId) }.data.toDomain() as? Message.User
        user?.images.orEmpty().mapNotNull { it.dataUri }
    }

    /**
     * Loads the page before what was returned so far; null once the history is complete.
     * Pages are stored while the cache has room. Beyond [maxMessages] they are only returned,
     * and the cursor to the rest is kept in memory ([beyondCache]) for this chat.
     */
    override suspend fun olderMessages(sessionId: String): Result<List<Message>?> = resultOf {
        val serverId = clients.activeProfile().id
        syncLock.withLock {
            val key = serverId to sessionId
            val stored = cache.read(serverId, sessionId)
            val api = clients.active().sessions
            val cursor = stored.olderCursor
            if (cursor == null) {
                // A cache below the cap without a cursor holds the complete history.
                if (stored.entries.size < maxMessages) return@withLock null
                return@withLock olderBeyondCache(sessionId, key, stored.entries)
            }
            val page = awaitBody { api.sessionMessageList(sessionId, limit = PAGE_SIZE.toString(), cursor = cursor) }
            val known = stored.entries.mapTo(HashSet()) { it.id }
            val older = page.data.asReversed().map { it.toStored() }.filter { it.id !in known }
            val next = page.cursor.next.takeIf { page.data.receivedCount == PAGE_SIZE }
            cache.prepend(serverId, sessionId, older.toEntries(), next, maxMessages)
            // Did not fit entirely: what follows is read without the cache.
            if (stored.entries.size + older.size > maxMessages) beyondCache[key] = Beyond(next) else beyondCache.remove(key)
            older.toTranscript()
        }
    }

    /**
     * History older than a full cache. The first time, the cursor before the oldest cached
     * entry is found by paging from the newest message (the cache cap was reached by a sync,
     * which only keeps the newest entries).
     */
    private suspend fun olderBeyondCache(sessionId: String, key: Pair<String, String>, cached: List<CachedEntry>): List<Message>? {
        val api = clients.active().sessions
        beyondCache[key]?.let { beyond ->
            val cursor = beyond.cursor ?: return null
            val page = awaitBody { api.sessionMessageList(sessionId, limit = PAGE_SIZE.toString(), cursor = cursor) }
            beyondCache[key] = Beyond(page.cursor.next.takeIf { page.data.receivedCount == PAGE_SIZE })
            return page.data.asReversed().map { it.toStored() }.toTranscript()
        }
        val oldestCached = cached.firstOrNull()?.id ?: return null
        var cursor: String? = null
        repeat(cached.size / PAGE_SIZE + 2) {
            val page = awaitBody { api.sessionMessageList(sessionId, limit = PAGE_SIZE.toString(), cursor = cursor) }
            val more = page.data.receivedCount == PAGE_SIZE && page.cursor.next != null
            val newestFirst = page.data.map { it.toStored() }
            val index = newestFirst.indexOfFirst { it.id == oldestCached }
            if (index >= 0) {
                beyondCache[key] = Beyond(page.cursor.next.takeIf { more })
                return newestFirst.drop(index + 1).asReversed().toTranscript()
            }
            if (!more) return null
            cursor = page.cursor.next
        }
        return null
    }

    /** Where history older than a full cache continues; a null [cursor] means it is complete. */
    private class Beyond(val cursor: String?)

    /**
     * Stores the last [lastMessages] of sessions that have less than that cached (and are not
     * complete), so they open with content.
     */
    override suspend fun prefetch(sessionIds: List<String>, lastMessages: Int) {
        val serverId = resultOf { clients.activeProfile().id }.getOrNull() ?: return
        for (sessionId in sessionIds) {
            resultOf {
                syncLock.withLock {
                    val stored = cache.read(serverId, sessionId)
                    val complete = stored.entries.isNotEmpty() && stored.olderCursor == null
                    if (stored.entries.size >= lastMessages || complete) return@withLock
                    val api = clients.active().sessions
                    val page = awaitBody { api.sessionMessageList(sessionId, limit = lastMessages.toString()) }
                    val next = page.cursor.next.takeIf { page.data.receivedCount == lastMessages }
                    cache.write(serverId, sessionId, null, page.data.asReversed().map { it.toStored() }.toEntries(), next, maxMessages)
                }
            }
        }
    }

    private class Stored(val id: String, val json: String, val info: SessionMessageInfo)

    /** [olderCursor] continues before [newestFirst] when the anchor was not reached. */
    private class Fetched(val newestFirst: List<Stored>, val reachedAnchor: Boolean, val olderCursor: String? = null)

    /**
     * Pages newest-first (the server default). Without [anchor] only the newest page is read;
     * otherwise paging continues until the anchor or the history cap.
     */
    private suspend fun fetchNewer(sessionId: String, anchor: String?): Fetched {
        val api = clients.active().sessions
        val newestFirst = mutableListOf<Stored>()
        var cursor: String? = null
        var pageSize = NEWEST_PAGE_SIZE
        var more: Boolean
        do {
            val page = awaitBody { api.sessionMessageList(sessionId, limit = pageSize.toString(), cursor = cursor) }
            for (info in page.data) {
                val stored = info.toStored()
                if (stored.id == anchor) return Fetched(newestFirst, reachedAnchor = true)
                newestFirst += stored
            }
            cursor = page.cursor.next
            more = page.data.receivedCount == pageSize && cursor != null
            pageSize = PAGE_SIZE
        } while (anchor != null && more && newestFirst.size < maxMessages)
        return Fetched(newestFirst, reachedAnchor = false, olderCursor = cursor.takeIf { more })
    }

    private fun SessionMessageInfo.toStored(): Stored = MessageCodec.encode(this).let { Stored(it.id, it.json, this) }

    private fun List<Stored>.toEntries(): List<CachedEntry> = map { CachedEntry(it.id, it.json) }

    /** Null when any entry no longer decodes; the caller then starts the cache over. */
    private fun List<CachedEntry>.decodeOrNull(): List<Stored>? = map { entry ->
        Stored(entry.id, entry.json, MessageCodec.decode(entry.json) ?: return null)
    }

    private fun List<Stored>.toTranscript(): List<Message> = mapNotNull { it.info.toDomain() }

    override suspend fun prompt(
        sessionId: String,
        text: String,
        delivery: PromptDelivery,
        attachments: List<PromptAttachment>,
    ): Result<Unit> = resultOf {
        val api = clients.active().sessions
        val request = SessionPromptRequest(text = text, files = attachments.toApi(text), delivery = delivery.toApi())
        awaitSuccess { api.sessionPrompt(sessionId, request) }
    }

    override suspend fun command(
        sessionId: String,
        name: String,
        arguments: String,
        delivery: PromptDelivery,
        attachments: List<PromptAttachment>,
    ): Result<Unit> = resultOf {
        val api = clients.active().sessions
        val request = SessionCommandRequest(
            name = name,
            text = arguments,
            files = attachments.toApi(arguments),
            delivery = delivery.toApi(),
        )
        awaitSuccess { api.sessionCommand(sessionId, request) }
    }

    /**
     * The server answers once the command exits. A command that outlasts the read timeout is
     * not stopped by it: the server keeps it running and the transcript shows its output.
     */
    override suspend fun shell(sessionId: String, command: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        try {
            awaitSuccess { api.sessionShell(sessionId, SessionShellRequest(command = command)) }
        } catch (e: OpenCodeError.Unreachable) {
            if (e.cause !is SocketTimeoutException) throw e
        }
    }

    override suspend fun inbox(sessionId: String): Result<List<QueuedPrompt>> = resultOf {
        val api = clients.active().sessions
        awaitBody { api.sessionInboxList(sessionId) }.data.mapNotNull { it.toQueuedPrompt() }
    }

    override suspend fun cancelQueued(sessionId: String, inboxId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionInboxCancel(sessionId, inboxId) }
    }

    override suspend fun steerQueued(sessionId: String, inboxId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionInboxUpdate(sessionId, inboxId, SessionInboxUpdateRequest(SessionInboxDelivery.STEER)) }
    }

    override suspend fun interrupt(sessionId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionInterrupt(sessionId) }
    }

    override suspend fun background(sessionId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionBackground(sessionId) }
    }

    override suspend fun compact(sessionId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionCompact(sessionId, SessionCompactRequest()) }
    }

    override suspend fun awaitIdle(sessionId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.experimentalSessionWait(sessionId) }
    }

    override suspend fun forms(sessionId: String): Result<List<Form>> = resultOf {
        val api = clients.active().sessions
        awaitBody { api.sessionFormList(sessionId) }.data.map { it.toDomain() }
    }

    override suspend fun replyForm(sessionId: String, formId: String, answers: Map<String, FormAnswer>): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionFormReply(sessionId, formId, FormReply(answers.mapValues { it.value.toJson() })) }
    }

    override suspend fun cancelForm(sessionId: String, formId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionFormCancel(sessionId, formId) }
    }

    override suspend fun revert(sessionId: String, messageId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionRevertStage(sessionId, SessionRevertStageRequest(messageID = messageId)) }
    }

    override suspend fun clearRevert(sessionId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionRevertClear(sessionId) }
    }

    override suspend fun commitRevert(sessionId: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionRevertCommit(sessionId) }
    }

    /**
     * The newest turn is the server default; the whole session runs from the turn of the first
     * prompt through that of the last one.
     */
    override suspend fun changes(sessionId: String, scope: ChangesScope): Result<List<FileDiff>> = resultOf {
        val api = clients.active().sessions
        val range = if (scope == ChangesScope.Session) {
            val first = firstUserMessage(sessionId, SessionApi.OrderSessionMessageList.ASC) ?: return@resultOf emptyList()
            first to firstUserMessage(sessionId, SessionApi.OrderSessionMessageList.DESC)
        } else {
            null
        }
        awaitBody { api.sessionDiff(sessionId, from = range?.first, to = range?.second, context = DIFF_CONTEXT_LINES.toString()) }
            .data.map { it.toDomain() }
    }

    private suspend fun firstUserMessage(sessionId: String, order: SessionApi.OrderSessionMessageList): String? {
        val api = clients.active().sessions
        val page = awaitBody { api.sessionMessageList(sessionId, limit = "1", order = order, type = SessionApi.TypeSessionMessageList.USER) }
        return page.data.firstOrNull()?.toDomain()?.id
    }

    private fun PromptDelivery.toApi(): SessionInboxDelivery = when (this) {
        PromptDelivery.Steer -> SessionInboxDelivery.STEER
        PromptDelivery.Queue -> SessionInboxDelivery.QUEUE
    }

    override suspend fun switchAgent(sessionId: String, agent: String): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionSwitchAgent(sessionId, SessionSwitchAgentRequest(agent)) }
    }

    override suspend fun switchModel(sessionId: String, model: ModelRef): Result<Unit> = resultOf {
        val api = clients.active().sessions
        awaitSuccess { api.sessionSwitchModel(sessionId, SessionSwitchModelRequest(model.toApi())) }
    }

    override suspend fun permissions(sessionId: String): Result<List<PermissionRequest>> = resultOf {
        val api = clients.active().permissions
        awaitBody { api.sessionPermissionList(sessionId) }.data.map { it.toDomain() }
    }

    override suspend fun replyPermission(
        sessionId: String,
        requestId: String,
        decision: PermissionDecision,
        message: String?,
    ): Result<Unit> = resultOf {
        val api = clients.active().permissions
        val reply = when (decision) {
            PermissionDecision.Once -> PermissionReply.ONCE
            PermissionDecision.Always -> PermissionReply.ALWAYS
            PermissionDecision.Reject -> PermissionReply.REJECT
        }
        awaitSuccess {
            api.sessionPermissionReply(sessionId, requestId, SessionPermissionReplyRequest(reply, message?.takeIf { it.isNotBlank() }))
        }
    }

    private companion object {
        /** Server maximum for one message page. */
        const val PAGE_SIZE = 200

        /** First page of every sync: enough to fill the screen, and usually all that is new. */
        const val NEWEST_PAGE_SIZE = 50

        /** Unchanged lines around each hunk; without them the server sends whole files. */
        const val DIFF_CONTEXT_LINES = 3
    }
}
