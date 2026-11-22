package io.github.beppe2880.opencodemobile.core.data.cache

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.SQLiteException
import io.github.beppe2880.opencodemobile.core.data.nowMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.Uuid

/** Complete native exports, never the slimmed/paged transcript. Each row stays below CursorWindow limits. */
@Entity(tableName = "export_entry", primaryKeys = ["serverId", "sessionId"])
internal data class ExportEntry(
    val serverId: String,
    val sessionId: String,
    val revision: String,
    val createdAt: Long,
    val accessedAt: Long,
    val bytes: Long,
    val chunks: Int,
    val checksum: Int,
)

@Entity(tableName = "export_chunk", primaryKeys = ["serverId", "sessionId", "seq"])
internal data class ExportChunk(val serverId: String, val sessionId: String, val seq: Int, val text: String)

@Dao
internal abstract class ExportCacheDao {
    @Query("SELECT * FROM export_entry ORDER BY accessedAt DESC")
    abstract suspend fun entries(): List<ExportEntry>

    @Query("SELECT * FROM export_chunk WHERE serverId = :serverId AND sessionId = :sessionId ORDER BY seq")
    abstract suspend fun chunks(serverId: String, sessionId: String): List<ExportChunk>

    @Upsert
    abstract suspend fun put(entry: ExportEntry)

    @Upsert
    abstract suspend fun putChunks(chunks: List<ExportChunk>)

    @Query("DELETE FROM export_entry WHERE serverId = :serverId AND sessionId = :sessionId")
    abstract suspend fun deleteEntry(serverId: String, sessionId: String)

    @Query("DELETE FROM export_chunk WHERE serverId = :serverId AND sessionId = :sessionId")
    abstract suspend fun deleteChunks(serverId: String, sessionId: String)

    @Transaction
    open suspend fun remove(serverId: String, sessionId: String) {
        deleteChunks(serverId, sessionId)
        deleteEntry(serverId, sessionId)
    }

    @Transaction
    open suspend fun clearServer(serverId: String) {
        entries().filter { it.serverId == serverId }.forEach { remove(serverId, it.sessionId) }
    }

    /** Expiry is absolute, not extended by reads; eviction is global LRU plus a per-server count. */
    @Transaction
    open suspend fun prune(now: Long, maxBytes: Long, keepSessions: Int, ttl: Long) {
        var bytes = 0L
        val counts = mutableMapOf<String, Int>()
        entries().forEach { entry ->
            val count = counts[entry.serverId] ?: 0
            if (now < entry.createdAt || now - entry.createdAt >= ttl || count >= keepSessions || bytes + entry.bytes > maxBytes) {
                remove(entry.serverId, entry.sessionId)
            } else {
                counts[entry.serverId] = count + 1
                bytes += entry.bytes
            }
        }
    }

    @Transaction
    open suspend fun read(serverId: String, sessionId: String, revision: String, now: Long, maxBytes: Long, keepSessions: Int, ttl: Long): String? {
        prune(now, maxBytes, keepSessions, ttl)
        val entry = entries().firstOrNull { it.serverId == serverId && it.sessionId == sessionId } ?: return null
        if (entry.revision != revision) {
            remove(serverId, sessionId)
            return null
        }
        val chunks = chunks(serverId, sessionId)
        if (chunks.size != entry.chunks || chunks.withIndex().any { (i, chunk) -> chunk.seq != i }) {
            remove(serverId, sessionId)
            return null
        }
        val text = chunks.joinToString("") { it.text }
        if (text.hashCode() != entry.checksum || text.encodeToByteArray().size.toLong() != entry.bytes) {
            remove(serverId, sessionId)
            return null
        }
        put(entry.copy(accessedAt = now))
        return text
    }

    @Transaction
    open suspend fun write(serverId: String, sessionId: String, revision: String, text: String, now: Long, maxBytes: Long, keepSessions: Int, ttl: Long) {
        remove(serverId, sessionId)
        val bytes = text.encodeToByteArray().size.toLong()
        if (bytes <= maxBytes) {
            // At most 512 KiB UTF-8 per row, including non-ASCII and surrogate pairs.
            val chunks = mutableListOf<ExportChunk>()
            var start = 0
            while (start < text.length) {
                var end = minOf(start + 128 * 1024, text.length)
                if (end < text.length && text[end - 1].isHighSurrogate()) end--
                chunks += ExportChunk(serverId, sessionId, chunks.size, text.substring(start, end))
                start = end
            }
            putChunks(chunks)
            put(ExportEntry(serverId, sessionId, revision, now, now, bytes, chunks.size, text.hashCode()))
        }
        prune(now, maxBytes, keepSessions, ttl)
    }
}

/** Retains the already parsed cold response for Markdown, avoiding a second full-document parse. */
internal data class NativeExport(val json: String, val document: JsonObject? = null)

/**
 * App-scoped export cache. Reuse is trusted only during an uninterrupted SSE connection.
 * Restart/reconnect/switch discards trust, rather than assuming updatedAt covers all native metadata.
 * The small state lock also makes invalidation atomic with cache reads/writes; network IO never holds it.
 * A fetch lock coalesces simultaneous exports (including JSON and Markdown) without owning a new scope.
 * Native JSON includes private context omitted from Markdown: retain at most 24h / 64 MiB / ten
 * sessions per server, and clear on session/server deletion. User-saved files are not owned here.
 */
internal class ExportCache(
    private val dao: ExportCacheDao? = null,
    private val maxBytes: Long = 64L * 1024 * 1024,
    private val keepSessions: Int = 10,
    private val ttl: Long = 24L * 60 * 60 * 1000,
    private val clock: () -> Long = ::nowMillis,
) {
    private val state = Mutex()
    private val fetch = Mutex()
    private val connected = mutableSetOf<String>()
    private val revisions = mutableMapOf<Pair<String, String>, String>()

    suspend fun connection(serverId: String, open: Boolean) = state.withLock {
        revisions.keys.removeAll { it.first == serverId }
        if (open) connected.add(serverId) else connected.remove(serverId)
        bestEffort { dao?.clearServer(serverId) }
        bestEffort { dao?.prune(clock(), maxBytes, keepSessions, ttl) }
    }

    suspend fun invalidate(serverId: String, sessionId: String) = state.withLock {
        // Streaming produces many deltas: only the first event after an export needs disk IO.
        if (revisions.remove(serverId to sessionId) != null) {
            bestEffort { dao?.remove(serverId, sessionId) }
        }
    }

    suspend fun clearServer(serverId: String) = connection(serverId, false)

    suspend fun load(serverId: String, sessionId: String, remote: suspend () -> NativeExport): NativeExport = fetch.withLock {
        val key = serverId to sessionId
        val revision = state.withLock {
            if (serverId in connected) revisions.getOrPut(key) { Uuid.random().toString() } else null
        }
        val cached = state.withLock {
            if (revision == null || revisions[key] != revision) null else bestEffort {
                dao?.read(serverId, sessionId, revision, clock(), maxBytes, keepSessions, ttl)
            }
        }
        if (cached != null) return@withLock NativeExport(cached)
        val result = remote()
        state.withLock {
            // An event, disconnection or server removal during the request must not resurrect stale data.
            if (revision != null && revisions[key] == revision && serverId in connected) {
                bestEffort { dao?.write(serverId, sessionId, revision, result.json, clock(), maxBytes, keepSessions, ttl) }
            }
        }
        result
    }

    private suspend fun <T> bestEffort(block: suspend () -> T): T? = try {
        block()
    } catch (_: SQLiteException) {
        // Disposable optimization: a full disk or unreadable database must not break export or SSE.
        null
    }
}
