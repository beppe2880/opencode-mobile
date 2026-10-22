package io.github.beppe2880.opencodemobile.core.domain

/**
 * An image attached to a prompt. [dataUri] is null when the local cache dropped the payload
 * (it is too large to store); [ChatRepository.messageImages] fetches it again.
 */
data class ImageAttachment(val name: String?, val mime: String, val dataUri: String?)

/** One rendered entry of a session transcript. */
sealed interface Message {
    val id: String
    val createdAt: Long

    data class User(
        override val id: String,
        override val createdAt: Long,
        val text: String,
        val attachments: Int = 0,
        /** True while the prompt is queued in the session inbox and not yet delivered. */
        val pending: Boolean = false,
        /** Images attached to the prompt, in order. */
        val images: List<ImageAttachment> = emptyList(),
        /** Server files attached by path (`@` mentions), absolute; counted in [attachments]. */
        val files: List<String> = emptyList(),
    ) : Message

    data class Assistant(
        override val id: String,
        override val createdAt: Long,
        val agent: String,
        val model: ModelRef,
        val parts: List<Part>,
        val completedAt: Long? = null,
        val finish: String? = null,
        val error: String? = null,
        val cost: Double = 0.0,
        val tokens: TokenUsage = TokenUsage(),
        /** When the provider finished streaming this step; tools may still run until [completedAt]. */
        val streamedAt: Long? = null,
    ) : Message {
        val isStreaming: Boolean get() = completedAt == null && error == null
    }

    data class Shell(
        override val id: String,
        override val createdAt: Long,
        val command: String,
        val status: String,
        val output: String,
        val exit: String?,
    ) : Message

    /** Agent/model switches, compactions and other inline notices. */
    data class Notice(
        override val id: String,
        override val createdAt: Long,
        val kind: NoticeKind,
        val text: String,
    ) : Message

    /**
     * End of one server execution. Executions, not single prompts, are the turns OpenCode
     * summarizes: prompts steered or queued while running belong to the same turn.
     * Only rendered when it carries an [error].
     */
    data class Idle(
        override val id: String,
        override val createdAt: Long,
        val outcome: SessionOutcome,
        val error: String? = null,
    ) : Message

    /** Server-injected context (e.g. a continuation). Never rendered, but it can start a turn. */
    data class Synthetic(
        override val id: String,
        override val createdAt: Long,
    ) : Message
}

/** [Compacting] is a compaction still running; [Compaction] a completed one (text: summary). */
enum class NoticeKind { AgentSwitched, ModelSwitched, LocationSwitched, Compacting, Compaction, Skill, Retry, Error }

sealed interface Part {
    /** Stable key inside one assistant message. */
    val key: String

    data class Text(val ordinal: Int, val text: String, val streaming: Boolean = false) : Part {
        override val key: String get() = "text-$ordinal"
    }

    data class Reasoning(
        val ordinal: Int,
        val text: String,
        val streaming: Boolean = false,
        val startedAt: Long? = null,
        val completedAt: Long? = null,
    ) : Part {
        override val key: String get() = "reasoning-$ordinal"
    }

    data class Tool(
        val callId: String,
        val name: String,
        val status: ToolStatus,
        /** Raw JSON input, possibly partial while streaming. */
        val input: String,
        val output: String? = null,
        val error: String? = null,
        val metadata: Map<String, String> = emptyMap(),
        val startedAt: Long? = null,
        val completedAt: Long? = null,
        /** File changes made by the tool (edit, write, patch), in display order. */
        val diffs: List<FileDiff> = emptyList(),
    ) : Part {
        override val key: String get() = "tool-$callId"
    }
}

/**
 * One file changed by a tool. [patch] is a unified diff (`@@ -a,b +c,d @@` hunks); for whole-file
 * writes without a server diff it contains a single all-added hunk and [status] is [FileDiff.WRITTEN].
 */
data class FileDiff(
    val path: String,
    val status: String?,
    val additions: Int,
    val deletions: Int,
    val patch: String,
) {
    companion object {
        const val WRITTEN = "written"
    }
}

enum class ToolStatus { Streaming, Running, Completed, Error }
