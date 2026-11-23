package io.github.beppe2880.opencodemobile.core.data.cache

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.SQLiteException
import io.github.beppe2880.opencodemobile.core.data.nowMillis

/** A cached transcript entry; [seq] orders entries within a session (oldest first, may be negative). */
@Entity(
    tableName = "message",
    primaryKeys = ["serverId", "sessionId", "seq"],
    indices = [Index("serverId", "sessionId", "messageId")],
)
internal data class MessageEntity(
    val serverId: String,
    val sessionId: String,
    val seq: Long,
    val messageId: String,
    val json: String,
)

/** Sync state of a session: when it was last synced and the cursor to its older history. */
@Entity(tableName = "session_sync", primaryKeys = ["serverId", "sessionId"])
internal data class SessionSyncEntity(
    val serverId: String,
    val sessionId: String,
    val syncedAt: Long,
    val olderCursor: String?,
)

@Dao
internal abstract class MessageCacheDao {

    @Query("SELECT * FROM message WHERE serverId = :serverId AND sessionId = :sessionId ORDER BY seq")
    abstract suspend fun messages(serverId: String, sessionId: String): List<MessageEntity>

    @Query("SELECT * FROM session_sync WHERE serverId = :serverId AND sessionId = :sessionId")
    abstract suspend fun sync(serverId: String, sessionId: String): SessionSyncEntity?

    @Query("SELECT seq FROM message WHERE serverId = :serverId AND sessionId = :sessionId AND messageId = :messageId LIMIT 1")
    abstract suspend fun seqOf(serverId: String, sessionId: String, messageId: String): Long?

    @Query("SELECT MIN(seq) FROM message WHERE serverId = :serverId AND sessionId = :sessionId")
    abstract suspend fun minSeq(serverId: String, sessionId: String): Long?

    @Query("SELECT COUNT(*) FROM message WHERE serverId = :serverId AND sessionId = :sessionId")
    abstract suspend fun count(serverId: String, sessionId: String): Int

    @Query("DELETE FROM message WHERE serverId = :serverId AND sessionId = :sessionId AND seq > :after")
    abstract suspend fun deleteAfter(serverId: String, sessionId: String, after: Long)

    @Query("DELETE FROM message WHERE serverId = :serverId AND sessionId = :sessionId")
    abstract suspend fun deleteSession(serverId: String, sessionId: String)

    /** Keeps the newest [max] entries; returns how many were dropped. */
    @Query(
        "DELETE FROM message WHERE serverId = :serverId AND sessionId = :sessionId AND seq NOT IN " +
            "(SELECT seq FROM message WHERE serverId = :serverId AND sessionId = :sessionId ORDER BY seq DESC LIMIT :max)",
    )
    abstract suspend fun trim(serverId: String, sessionId: String, max: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(rows: List<MessageEntity>)

    @Upsert
    abstract suspend fun upsertSync(sync: SessionSyncEntity)

    @Query("SELECT sessionId FROM session_sync WHERE serverId = :serverId ORDER BY syncedAt DESC LIMIT -1 OFFSET :keep")
    abstract suspend fun staleSessions(serverId: String, keep: Int): List<String>

    @Query("DELETE FROM message WHERE serverId = :serverId AND sessionId IN (:sessionIds)")
    abstract suspend fun deleteMessages(serverId: String, sessionIds: List<String>)

    @Query("DELETE FROM session_sync WHERE serverId = :serverId AND sessionId IN (:sessionIds)")
    abstract suspend fun deleteSyncs(serverId: String, sessionIds: List<String>)

    @Query("DELETE FROM message WHERE serverId = :serverId")
    abstract suspend fun clearMessages(serverId: String)

    @Query("DELETE FROM session_sync WHERE serverId = :serverId")
    abstract suspend fun clearSyncs(serverId: String)

    @Transaction
    open suspend fun read(serverId: String, sessionId: String): CachedTranscript = CachedTranscript(
        entries = messages(serverId, sessionId).map { CachedEntry(it.messageId, it.json) },
        olderCursor = sync(serverId, sessionId)?.olderCursor,
    )

    @Transaction
    open suspend fun replaceTail(
        serverId: String,
        sessionId: String,
        keepThrough: String?,
        newer: List<CachedEntry>,
        olderCursor: String?,
        max: Int,
        now: Long,
        keepSessions: Int,
    ) {
        val anchor = keepThrough?.let { seqOf(serverId, sessionId, it) }
        if (anchor == null) deleteSession(serverId, sessionId) else deleteAfter(serverId, sessionId, anchor)
        val first = (anchor ?: -1L) + 1
        insert(newer.mapIndexed { i, e -> MessageEntity(serverId, sessionId, first + i, e.id, e.json) })
        val trimmed = trim(serverId, sessionId, max) > 0
        finish(serverId, sessionId, olderCursor.takeUnless { trimmed }, now, keepSessions)
    }

    @Transaction
    open suspend fun prepend(
        serverId: String,
        sessionId: String,
        older: List<CachedEntry>,
        olderCursor: String?,
        max: Int,
        now: Long,
        keepSessions: Int,
    ) {
        val kept = older.takeLast((max - count(serverId, sessionId)).coerceAtLeast(0))
        val first = (minSeq(serverId, sessionId) ?: 0L) - kept.size
        insert(kept.mapIndexed { i, e -> MessageEntity(serverId, sessionId, first + i, e.id, e.json) })
        finish(serverId, sessionId, olderCursor.takeIf { kept.size == older.size }, now, keepSessions)
    }

    private suspend fun finish(serverId: String, sessionId: String, olderCursor: String?, now: Long, keepSessions: Int) {
        upsertSync(SessionSyncEntity(serverId, sessionId, now, olderCursor))
        val stale = staleSessions(serverId, keepSessions)
        if (stale.isNotEmpty()) {
            deleteMessages(serverId, stale)
            deleteSyncs(serverId, stale)
        }
    }

    @Transaction
    open suspend fun clear(serverId: String) {
        clearMessages(serverId)
        clearSyncs(serverId)
    }
}

@Database(entities = [MessageEntity::class, SessionSyncEntity::class, ListCacheEntity::class], version = 3, exportSchema = false)
@ConstructedBy(CacheDatabaseConstructor::class)
internal abstract class CacheDatabase : RoomDatabase() {
    abstract fun messages(): MessageCacheDao
    abstract fun lists(): ListCacheDao

    companion object {
        const val FILE_NAME = "message-cache.db"

        /** A disposable cache: schema changes simply drop it and history is fetched again. */
        fun build(builder: RoomDatabase.Builder<CacheDatabase>): CacheDatabase =
            builder.fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}

/** Implemented by the Room compiler for each platform. */
@Suppress("KotlinNoActualForExpect")
internal expect object CacheDatabaseConstructor : RoomDatabaseConstructor<CacheDatabase> {
    override fun initialize(): CacheDatabase
}

/** [MessageCache] backed by Room. Keeps the [keepSessions] most recently synced sessions per server. */
internal class RoomMessageCache(
    private val dao: MessageCacheDao,
    private val keepSessions: Int = 50,
    private val clock: () -> Long = ::nowMillis,
) : MessageCache {

    /**
     * An unreadable session (e.g. a row stored before payloads were slimmed, too big for the
     * cursor window) is dropped and synced again rather than failing the chat forever.
     */
    override suspend fun read(serverId: String, sessionId: String): CachedTranscript = try {
        dao.read(serverId, sessionId)
    } catch (e: SQLiteException) {
        dao.deleteSession(serverId, sessionId)
        dao.deleteSyncs(serverId, listOf(sessionId))
        CachedTranscript()
    }

    override suspend fun write(
        serverId: String,
        sessionId: String,
        keepThrough: String?,
        newer: List<CachedEntry>,
        olderCursor: String?,
        max: Int,
    ) = dao.replaceTail(serverId, sessionId, keepThrough, newer, olderCursor, max, clock(), keepSessions)

    override suspend fun prepend(serverId: String, sessionId: String, older: List<CachedEntry>, olderCursor: String?, max: Int) =
        dao.prepend(serverId, sessionId, older, olderCursor, max, clock(), keepSessions)

    override suspend fun clearServer(serverId: String) = dao.clear(serverId)
}
