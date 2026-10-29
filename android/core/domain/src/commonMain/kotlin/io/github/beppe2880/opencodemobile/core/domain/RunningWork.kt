package io.github.beppe2880.opencodemobile.core.domain

/** Non-interactive commands only; PTY terminals are managed by the terminal screen. */
data class RunningCommand(
    val id: String,
    val command: String,
    val directory: String,
    val startedAt: Long,
    val sessionId: String? = null,
)

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
    /** A missing/unreachable project must not hide work in other projects. */
    val errors: Map<String, Throwable> = emptyMap(),
)

/** One refresh owned by the visible screen. No jobs, persistent cache or server-global state. */
suspend fun loadRunningWork(
    roots: List<Session>,
    sessions: SessionRepository,
    chat: ChatRepository,
): Result<RunningSnapshot> {
    val active = sessions.active().getOrElse { return Result.failure(it) }
    val errors = mutableMapOf<String, Throwable>()
    val children = mutableListOf<RunningAgent>()
    for (id in active) {
        if (roots.any { it.id == id }) continue
        val result = sessions.get(id)
        if (result.exceptionOrNull() is OpenCodeError.NotFound) continue // Ended/deleted after the active snapshot.
        val session = result.getOrElse { return Result.failure(it) }
        if (roots.none { it.id == session.parentId }) continue
        val messages = chat.messages(id).getOrElse {
            errors[session.parentId!!] = it
            emptyList()
        }
        val start = messages.lastOrNull { it is Message.User || it is Message.Synthetic }?.createdAt
        val agent = session.agent ?: messages.filterIsInstance<Message.Assistant>().lastOrNull()?.agent
        children += RunningAgent(session.copy(agent = agent), start)
    }
    val commands = mutableListOf<RunningCommand>()
    for (directory in roots.map { it.directory }.distinct()) {
        commands += chat.runningShells(directory).getOrElse { error ->
            roots.filter { it.directory == directory }.forEach { errors[it.id] = error }
            emptyList()
        }
    }
    // Older servers/tools put shellID on the transcript tool rather than sessionID on the shell.
    val owners = mutableMapOf<String, String>()
    for (root in roots) {
        if (commands.none { it.directory == root.directory && it.sessionId == null }) continue
        val messages = chat.messages(root.id).getOrElse {
            errors[root.id] = it
            emptyList()
        }
        for (tool in messages.filterIsInstance<Message.Assistant>().flatMap { it.parts }.filterIsInstance<Part.Tool>()) {
            tool.metadata["shellID"]?.let { owners[it] = root.id }
        }
    }
    val attributed = commands.map { it.copy(sessionId = it.sessionId ?: owners[it.id]) }
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
