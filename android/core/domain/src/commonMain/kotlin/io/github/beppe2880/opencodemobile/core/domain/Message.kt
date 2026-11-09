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
        /** Files returned with the result (for example the image `read` loaded), in order. */
        val files: List<ToolFile> = emptyList(),
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

/**
 * A file a tool returned with its result. The `read` tool returns images and PDFs inline as a
 * base64 `data:` URI, with the path it was asked for as [name]; other tools may return `file://`
 * URIs, absolute paths or http(s) URLs. Stored transcripts keep a `data:` URI without its payload
 * (`data:image/png;base64,`, see `MessageCodec`); [ChatRepository.toolFileData] fetches it again.
 */
data class ToolFile(val uri: String, val mime: String, val name: String?) {

    /** The media type without parameters, lower case (`image/png`). */
    val baseMime: String get() = mime.substringBefore(';').trim().lowercase()

    val isDataUri: Boolean get() = uri.startsWith("data:", ignoreCase = true)

    /** A `data:` URI that still carries its content (stored copies have it removed). */
    val hasInlinePayload: Boolean get() = isDataUri && uri.indexOf(',').let { it >= 0 && it < uri.length - 1 }

    val isWebUrl: Boolean get() = uri.startsWith("http://", ignoreCase = true) || uri.startsWith("https://", ignoreCase = true)

    /**
     * Server path of the file: the path of a `file://` URI or an absolute path; for inline
     * content the [name] the tool reports (the `read` tool's input path, which may be relative to
     * the session folder). Null when the file has no path on the server.
     */
    val serverPath: String?
        get() = when {
            uri.startsWith("file://", ignoreCase = true) -> fileUriPath(uri)
            uri.startsWith("/") -> uri
            isDataUri -> name?.takeIf { it.isNotBlank() && !it.contains("://") }
            else -> null
        }

    /** Size of the inline content in bytes, computed from the base64 length; null when unknown. */
    val inlineBytes: Long?
        get() {
            if (!hasInlinePayload) return null
            val start = uri.indexOf(',') + 1
            if (!uri.substring(5, start - 1).endsWith(";base64", ignoreCase = true)) return (uri.length - start).toLong()
            var end = uri.length
            var padding = 0
            while (end > start && uri[end - 1] == '=' && padding < 2) {
                end--
                padding++
            }
            return (uri.length - start).toLong() * 3 / 4 - padding
        }

    private companion object {
        /** `file:///a/b` and `file://host/a/b` to `/a/b`, percent-decoded. */
        fun fileUriPath(uri: String): String? {
            val rest = uri.substring("file://".length)
            val path = if (rest.startsWith('/')) rest else rest.substringAfter('/', "").let { if (it.isEmpty()) "" else "/$it" }
            return percentDecode(path.substringBefore('?').substringBefore('#')).takeIf { it.isNotEmpty() }
        }

        /** Decodes `%XX` sequences as UTF-8; malformed sequences are kept as written. */
        fun percentDecode(text: String): String {
            if ('%' !in text) return text
            val out = StringBuilder(text.length)
            val bytes = ArrayList<Byte>()
            var i = 0
            while (i < text.length) {
                val escape = if (text[i] == '%' && i + 2 <= text.lastIndex) text.substring(i + 1, i + 3) else null
                val hex = escape?.takeIf { s -> s.all { it.digitToIntOrNull(16) != null } }?.toInt(16)
                if (hex != null) {
                    bytes += hex.toByte()
                    i += 3
                } else {
                    if (bytes.isNotEmpty()) {
                        out.append(bytes.toByteArray().decodeToString())
                        bytes.clear()
                    }
                    out.append(text[i])
                    i++
                }
            }
            if (bytes.isNotEmpty()) out.append(bytes.toByteArray().decodeToString())
            return out.toString()
        }
    }
}
