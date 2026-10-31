package io.github.beppe2880.opencodemobile.core.domain

/** Non-interactive commands only; PTY terminals are managed by the terminal screen. */
data class RunningCommand(
    val id: String,
    val command: String,
    val directory: String,
    /** Server clock, epoch milliseconds. */
    val startedAt: Long,
    val sessionId: String? = null,
)

/** [startedAt] is on the server clock; null when unknown (counts only). */
data class RunningAgent(val session: Session, val startedAt: Long?)

data class RunningWork(
    val agents: List<RunningAgent> = emptyList(),
    val commands: List<RunningCommand> = emptyList(),
) {
    val isEmpty: Boolean get() = agents.isEmpty() && commands.isEmpty()
}

data class RunningSnapshot(
    val sessions: Map<String, RunningWork> = emptyMap(),
    /** Commands without a proven session owner are never attributed just by directory. */
    val unassigned: List<RunningCommand> = emptyList(),
    /** Roots whose work could not be read completely; a missing project must not hide work in other projects. */
    val errors: Map<String, Throwable> = emptyMap(),
)

/**
 * Reads what runs under some root sessions: running child sessions (subagents) and background
 * commands. Owned by one screen and refreshed only while that screen is visible; no jobs.
 *
 * Costs per refresh: one active-sessions request, one shell list per distinct directory, and
 * for each running session that is not a root one session request. A session's parent never
 * changes, so without [details] parents are remembered and the session request happens once per
 * running session. Only with [details] (the chat sheet) are child transcripts read, to show when
 * each subagent started and which agent it runs.
 */
class RunningWorkLoader(
    private val sessions: SessionRepository,
    private val chat: ChatRepository,
) {
    private val parents = mutableMapOf<String, String?>()

    /**
     * [shellOwners] maps shell IDs to the session whose transcript started them (older servers do
     * not put the session on the shell); callers pass what they already have in memory.
     * One running session that cannot be read is skipped instead of failing the refresh.
     */
    suspend fun load(
        roots: List<Session>,
        details: Boolean = false,
        shellOwners: Map<String, String> = emptyMap(),
    ): Result<RunningSnapshot> {
        val active = sessions.active().getOrElse { return Result.failure(it) }
        parents.keys.retainAll(active)
        val rootIds = roots.mapTo(HashSet()) { it.id }
        val errors = mutableMapOf<String, Throwable>()
        val children = mutableListOf<RunningAgent>()
        for (id in active) {
            if (id in rootIds) continue
            if (!details && id in parents) {
                val parent = parents[id]
                if (parent in rootIds) children += RunningAgent(placeholder(id, parent!!, roots), null)
                continue
            }
            // Ended or deleted after the active snapshot, or unreadable: skipped, retried next time.
            val session = sessions.get(id).getOrNull() ?: continue
            parents[id] = session.parentId
            val parent = session.parentId ?: continue
            if (parent !in rootIds) continue
            if (!details) {
                children += RunningAgent(session, null)
                continue
            }
            val messages = chat.messages(id).getOrElse {
                errors[parent] = it
                emptyList()
            }
            val start = messages.lastOrNull { it is Message.User || it is Message.Synthetic }?.createdAt
            val agent = session.agent ?: messages.filterIsInstance<Message.Assistant>().lastOrNull()?.agent
            children += RunningAgent(session.copy(agent = agent), start)
        }
        val commands = mutableListOf<RunningCommand>()
        for (directory in roots.map { it.directory }.filter { it.isNotEmpty() }.distinct()) {
            commands += chat.runningShells(directory).getOrElse { error ->
                roots.filter { it.directory == directory }.forEach { errors[it.id] = error }
                emptyList()
            }
        }
        val attributed = commands.map { it.copy(sessionId = it.sessionId ?: shellOwners[it.id]) }
        return Result.success(
            RunningSnapshot(
                sessions = roots.associate { root ->
                    root.id to RunningWork(children.filter { it.session.parentId == root.id }, attributed.filter { it.sessionId == root.id })
                },
                unassigned = attributed.filter { it.sessionId == null },
                errors = errors,
            ),
        )
    }

    /** A remembered child for counting: only its ID and parent matter. */
    private fun placeholder(id: String, parent: String, roots: List<Session>): Session =
        roots.first { it.id == parent }.copy(id = id, parentId = parent, title = "", agent = null)
}

/** Shell IDs started by tools in [messages], with the session that owns them. */
fun shellOwners(sessionId: String, messages: List<Message>): Map<String, String> = buildMap {
    for (message in messages) {
        if (message !is Message.Assistant) continue
        for (part in message.parts) {
            if (part is Part.Tool) part.metadata["shellID"]?.let { put(it, sessionId) }
        }
    }
}
