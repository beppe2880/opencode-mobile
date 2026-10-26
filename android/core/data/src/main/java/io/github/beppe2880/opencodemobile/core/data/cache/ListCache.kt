package io.github.beppe2880.opencodemobile.core.data.cache

import android.database.sqlite.SQLiteException
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * Last loaded lists (sessions per directory filter, projects) per saved server, so switching
 * servers or starting offline shows them at once. Payloads are [SessionListCodec] JSON.
 */
internal interface ListCache {
    /** The stored payload of [key] on [serverId]; null when none. */
    suspend fun read(serverId: String, key: String): String?

    suspend fun write(serverId: String, key: String, json: String)

    /** Removes a deleted session from every cached directory list of [serverId]. */
    suspend fun removeSession(serverId: String, sessionId: String)

    /** Forgets every list of [serverId], e.g. when the server is removed. */
    suspend fun clearServer(serverId: String)

    companion object {
        fun sessionsKey(directory: String?): String = "sessions:${directory.orEmpty()}"
        const val PROJECTS_KEY = "projects"

        /** Keeps nothing; for repositories used without a device cache. */
        val None: ListCache = object : ListCache {
            override suspend fun read(serverId: String, key: String): String? = null
            override suspend fun write(serverId: String, key: String, json: String) = Unit
            override suspend fun removeSession(serverId: String, sessionId: String) = Unit
            override suspend fun clearServer(serverId: String) = Unit
        }
    }
}

@Entity(tableName = "list_cache", primaryKeys = ["serverId", "key"])
internal data class ListCacheEntity(
    val serverId: String,
    val key: String,
    val json: String,
    val savedAt: Long,
)

@Dao
internal abstract class ListCacheDao {

    @Query("SELECT json FROM list_cache WHERE serverId = :serverId AND `key` = :key")
    abstract suspend fun read(serverId: String, key: String): String?

    @Query("SELECT * FROM list_cache WHERE serverId = :serverId AND `key` LIKE 'sessions:%'")
    abstract suspend fun sessionLists(serverId: String): List<ListCacheEntity>

    @Upsert
    abstract suspend fun upsert(entity: ListCacheEntity)

    @Query(
        "DELETE FROM list_cache WHERE serverId = :serverId AND `key` NOT IN " +
            "(SELECT `key` FROM list_cache WHERE serverId = :serverId ORDER BY savedAt DESC LIMIT :keep)",
    )
    abstract suspend fun trim(serverId: String, keep: Int)

    @Query("DELETE FROM list_cache WHERE serverId = :serverId AND `key` = :key")
    abstract suspend fun delete(serverId: String, key: String)

    @Query("DELETE FROM list_cache WHERE serverId = :serverId")
    abstract suspend fun clear(serverId: String)

    @Transaction
    open suspend fun write(entity: ListCacheEntity, keep: Int) {
        upsert(entity)
        trim(entity.serverId, keep)
    }
}

/** [ListCache] in the Room cache database. Keeps the [keepLists] most recently saved lists per server. */
internal class RoomListCache(
    private val dao: ListCacheDao,
    private val keepLists: Int = 20,
    private val clock: () -> Long = System::currentTimeMillis,
) : ListCache {

    /** An unreadable row is dropped: the list is simply loaded from the server again. */
    override suspend fun read(serverId: String, key: String): String? = try {
        dao.read(serverId, key)
    } catch (e: SQLiteException) {
        dao.delete(serverId, key)
        null
    }

    /** A failed write only leaves the previous copy, never fails the load that produced it. */
    override suspend fun write(serverId: String, key: String, json: String) {
        try {
            dao.write(ListCacheEntity(serverId, key, json, clock()), keepLists)
        } catch (_: SQLiteException) {
        }
    }

    override suspend fun removeSession(serverId: String, sessionId: String) {
        try {
            dao.sessionLists(serverId).forEach { entity ->
                val sessions = SessionListCodec.decodeSessions(entity.json) ?: return@forEach
                val remaining = sessions.filterNot { it.id == sessionId }
                if (remaining.size != sessions.size) {
                    dao.upsert(entity.copy(json = SessionListCodec.encodeSessions(remaining)))
                }
            }
        } catch (_: SQLiteException) {
            // Cache invalidation is best effort; the next successful list refresh rewrites it.
        }
    }

    override suspend fun clearServer(serverId: String) = dao.clear(serverId)
}
