package io.github.beppe2880.opencodemobile.core.data.cache

import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persisted JSON schema of cached session and project lists (domain data, not server payloads).
 * Unreadable data yields null, so the list is loaded from the server again.
 */
internal object SessionListCodec {

    @Serializable
    private data class StoredModel(val id: String, val providerId: String, val variant: String? = null)

    @Serializable
    private data class StoredTokens(
        val input: Long = 0,
        val output: Long = 0,
        val reasoning: Long = 0,
        val cacheRead: Long = 0,
        val cacheWrite: Long = 0,
    )

    @Serializable
    private data class StoredSession(
        val id: String,
        val parentId: String? = null,
        val projectId: String,
        val title: String,
        val directory: String,
        val agent: String? = null,
        val model: StoredModel? = null,
        val cost: Double = 0.0,
        val tokens: StoredTokens = StoredTokens(),
        val outcome: String? = null,
        val createdAt: Long,
        val updatedAt: Long,
        val archivedAt: Long? = null,
        val idleAt: Long? = null,
        val viewedAt: Long? = null,
        val revertMessageId: String? = null,
    )

    @Serializable
    private data class StoredProject(
        val id: String,
        val directory: String,
        val name: String? = null,
        val vcs: String? = null,
        val updatedAt: Long,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val sessions = ListSerializer(StoredSession.serializer())
    private val projects = ListSerializer(StoredProject.serializer())

    fun encodeSessions(list: List<Session>): String = json.encodeToString(sessions, list.map { it.toStored() })

    fun decodeSessions(raw: String?): List<Session>? = decode(raw) { json.decodeFromString(sessions, it).map { s -> s.toDomain() } }

    fun encodeProjects(list: List<Project>): String =
        json.encodeToString(projects, list.map { StoredProject(it.id, it.directory, it.name, it.vcs, it.updatedAt) })

    fun decodeProjects(raw: String?): List<Project>? =
        decode(raw) { json.decodeFromString(projects, it).map { p -> Project(p.id, p.directory, p.name, p.vcs, p.updatedAt) } }

    private fun <T> decode(raw: String?, block: (String) -> T): T? {
        if (raw.isNullOrBlank()) return null
        return try {
            block(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun Session.toStored() = StoredSession(
        id = id,
        parentId = parentId,
        projectId = projectId,
        title = title,
        directory = directory,
        agent = agent,
        model = model?.let { StoredModel(it.id, it.providerId, it.variant) },
        cost = cost,
        tokens = with(tokens) { StoredTokens(input, output, reasoning, cacheRead, cacheWrite) },
        outcome = outcome?.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt,
        idleAt = idleAt,
        viewedAt = viewedAt,
        revertMessageId = revertMessageId,
    )

    private fun StoredSession.toDomain() = Session(
        id = id,
        parentId = parentId,
        projectId = projectId,
        title = title,
        directory = directory,
        agent = agent,
        model = model?.let { ModelRef(it.id, it.providerId, it.variant) },
        cost = cost,
        tokens = with(tokens) { TokenUsage(input, output, reasoning, cacheRead, cacheWrite) },
        outcome = SessionOutcome.entries.firstOrNull { it.name == outcome },
        createdAt = createdAt,
        updatedAt = updatedAt,
        archivedAt = archivedAt,
        idleAt = idleAt,
        viewedAt = viewedAt,
        revertMessageId = revertMessageId,
    )
}
