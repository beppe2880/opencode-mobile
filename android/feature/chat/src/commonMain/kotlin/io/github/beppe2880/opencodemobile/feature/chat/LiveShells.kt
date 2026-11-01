package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.RunningCommand

/**
 * Finds the shell each running shell tool prints to, so the chat can read its output live.
 *
 * The server names the shell only in the transient `session.tool.progress` event
 * (`metadata.shellID`); the stored tool state never has it, so a transcript loaded from the
 * server (opening the chat mid-command, a reconnect, a reload) loses it. The session's running
 * shells (`/api/shell`, owned through `metadata.sessionID`) then give it back, matched by command.
 */
internal object LiveShells {

    const val SHELL_ID = "shellID"

    /**
     * Shell IDs by call ID for the [running] tools: from the tool metadata, then from [known]
     * (found earlier), then the session's [commands] with the same command line, paired in start
     * order so repeated commands each get their own shell.
     */
    fun match(sessionId: String, running: List<Part.Tool>, commands: List<RunningCommand>, known: Map<String, String> = emptyMap()): Map<String, String> {
        val found = running.mapNotNull { tool -> (tool.metadata[SHELL_ID] ?: known[tool.callId])?.let { tool.callId to it } }.toMap()
        val claimed = found.values.toSet()
        val free = commands.filter { it.sessionId == sessionId && it.id !in claimed }.sortedBy { it.startedAt }.toMutableList()
        if (free.isEmpty()) return found
        val matched = running
            .filter { it.callId !in found && ToolInfo.kind(it.name) == ToolInfo.Kind.Shell }
            .sortedBy { it.startedAt ?: Long.MAX_VALUE }
            .mapNotNull { tool ->
                val command = ToolInfo.stringField(tool.input, "command") ?: return@mapNotNull null
                val shell = free.firstOrNull { it.command == command } ?: return@mapNotNull null
                free.remove(shell)
                tool.callId to shell.id
            }
        return found + matched
    }
}
