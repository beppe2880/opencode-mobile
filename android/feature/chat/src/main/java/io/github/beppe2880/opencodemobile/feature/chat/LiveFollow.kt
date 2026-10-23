package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.Part

/**
 * Which running work stays open while the agent works, from the settings. Shell commands
 * show their output as it arrives; edits, reads and other tools open as soon as their
 * result lands (the server sends a tool's input and result whole, not as it is written).
 */
data class LiveFollow(
    val thinking: Boolean = true,
    val shell: Boolean = true,
    val edits: Boolean = true,
    val reads: Boolean = false,
    val other: Boolean = false,
) {
    fun covers(tool: Part.Tool): Boolean = when (ToolInfo.kind(tool.name)) {
        ToolInfo.Kind.Shell -> shell
        ToolInfo.Kind.Edit -> edits
        ToolInfo.Kind.Read -> reads
        ToolInfo.Kind.Other -> other
    }

    companion object {
        /** Null when following running work is off. */
        fun of(settings: AppSettings): LiveFollow? = if (!settings.followRunningTools) {
            null
        } else {
            LiveFollow(
                thinking = settings.liveThinking,
                shell = settings.liveShell,
                edits = settings.liveEdits,
                reads = settings.liveReads,
                other = settings.liveOther,
            )
        }
    }
}
