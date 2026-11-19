package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.Part

/** Portable Markdown: no hidden system context, reasoning, image payloads or internal notices. */
internal fun transcriptMarkdown(title: String, messages: List<Message>): String = buildString {
    append("# ").append(title.replace('\n', ' ').ifBlank { "OpenCode" }).append("\n\n")
    for (message in messages) {
        when (message) {
            is Message.User -> {
                append("## User\n\n").append(message.text).append("\n\n")
            }
            is Message.Assistant -> {
                append("## Assistant\n\n")
                for (part in message.parts) {
                    when (part) {
                        is Part.Text -> append(part.text).append("\n\n")
                        is Part.Tool -> {
                            append("<details>\n<summary>").append(html(part.name)).append("</summary>\n\n")
                            append(codeBlock(part.input)).append("\n\n")
                            (part.output ?: part.error)?.let { append(codeBlock(it)).append("\n\n") }
                            append("</details>\n\n")
                        }
                        is Part.Reasoning -> Unit
                    }
                }
                message.error?.let { append(codeBlock(it)).append("\n\n") }
            }
            is Message.Shell -> {
                append("## Shell\n\n<details>\n<summary>Shell</summary>\n\n")
                append(codeBlock(message.command)).append("\n\n").append(codeBlock(message.output))
                append("\n\n</details>\n\n")
            }
            else -> Unit
        }
    }
}

private fun html(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/** A longer fence keeps embedded Markdown/code from breaking out of tool output. */
private fun codeBlock(text: String): String {
    val longest = Regex("`+").findAll(text).maxOfOrNull { it.value.length } ?: 0
    val fence = "`".repeat(maxOf(3, longest + 1))
    return "$fence\n$text\n$fence"
}
