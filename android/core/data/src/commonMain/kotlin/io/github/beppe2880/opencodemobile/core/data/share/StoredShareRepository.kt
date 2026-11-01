package io.github.beppe2880.opencodemobile.core.data.share

import io.github.beppe2880.opencodemobile.core.domain.PendingShare
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.ShareRepository
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Where the waiting share is kept: app-private files on Android (it holds what the user shared:
 * never backed up, never logged), memory where nothing can be shared. Blocking.
 */
interface ShareStore {
    /** Null when nothing is stored or what is stored cannot be read. */
    fun read(): PendingShare?

    /** Null deletes the stored share. */
    fun write(share: PendingShare?)
}

class MemoryShareStore : ShareStore {
    private var share: PendingShare? = null

    override fun read(): PendingShare? = share

    override fun write(share: PendingShare?) {
        this.share = share
    }
}

/**
 * The waiting share in memory, mirrored to [store] so it survives the app being stopped.
 * [scope] (the app scope) only reads the stored share once at start, so [pending] shows it
 * without a caller; every change is a suspend call owned by its caller.
 */
class StoredShareRepository(
    private val store: ShareStore,
    scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ShareRepository {

    private val mutex = Mutex()
    private val _pending = MutableStateFlow<PendingShare?>(null)
    override val pending: StateFlow<PendingShare?> = _pending.asStateFlow()
    private var loaded = false

    init {
        scope.launch { mutex.withLock { load() } }
    }

    override suspend fun offer(content: SharedContent) = update { PendingShare(id = clock(), content = content) }

    override suspend fun assign(sessionId: String, serverId: String?) = update { it?.copy(sessionId = sessionId, serverId = serverId) }

    override suspend fun take(sessionId: String): SharedContent? = mutex.withLock {
        load()
        val share = _pending.value?.takeIf { it.sessionId == sessionId } ?: return null
        save(null)
        share.content
    }

    override suspend fun discard() = update { null }

    private suspend fun update(transform: (PendingShare?) -> PendingShare?) {
        mutex.withLock {
            load()
            save(transform(_pending.value))
        }
    }

    /** Called under [mutex]: what is stored is read once, before the first change. */
    private suspend fun load() {
        if (loaded) return
        _pending.value = withContext(io) { store.read() }
        loaded = true
    }

    private suspend fun save(share: PendingShare?) {
        _pending.value = share
        withContext(io) { store.write(share) }
    }
}

/**
 * The stored form of a share: [encode] gives the description (JSON) and the attachment URIs,
 * kept apart so a large image is one file of its own.
 */
object ShareCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(share: PendingShare): Pair<String, List<String>> {
        val content = share.content
        val stored = StoredShare(
            id = share.id,
            text = content.text,
            skipped = content.skipped,
            truncated = content.truncated,
            sessionId = share.sessionId,
            serverId = share.serverId,
            attachments = content.attachments.map { StoredAttachment(it.name, it.mime) },
        )
        return json.encodeToString(StoredShare.serializer(), stored) to content.attachments.map { it.uri }
    }

    /** Null when [description] is not a share or does not match [uris]. */
    fun decode(description: String, uris: List<String>): PendingShare? {
        val stored = try {
            json.decodeFromString(StoredShare.serializer(), description)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (stored.attachments.size != uris.size) return null
        val attachments = stored.attachments.zip(uris) { a, uri -> PromptAttachment(uri = uri, name = a.name, mime = a.mime) }
        return PendingShare(
            id = stored.id,
            content = SharedContent(stored.text, attachments, stored.skipped, stored.truncated),
            sessionId = stored.sessionId,
            serverId = stored.serverId,
        )
    }

    @Serializable
    private data class StoredShare(
        val id: Long,
        val text: String,
        val skipped: Int = 0,
        val truncated: Boolean = false,
        val sessionId: String? = null,
        val serverId: String? = null,
        val attachments: List<StoredAttachment> = emptyList(),
    )

    @Serializable
    private data class StoredAttachment(val name: String, val mime: String? = null)
}
