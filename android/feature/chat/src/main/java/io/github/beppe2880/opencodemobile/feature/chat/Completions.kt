package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Command
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.ui.text.UiText

/** `/` commands and `@` file mentions typed in the composer. */
internal object Completions {
    /** Commands the app runs itself, like the OpenCode TUI, instead of sending them. */
    enum class Builtin(val command: String, val description: Int) {
        Undo("undo", R.string.chat_command_undo),
        Redo("redo", R.string.chat_command_redo),
        Compact("compact", R.string.chat_command_compact),
        Fork("fork", R.string.chat_command_fork),
        ;

        companion object {
            fun of(name: String?): Builtin? = entries.firstOrNull { it.command == name }
        }
    }

    /** A complete `/name arguments` prompt. */
    data class Slash(val name: String, val arguments: String)

    /** `/name` being typed as the whole draft. */
    private val SLASH_PREFIX = Regex("/(\\S*)")

    private val SLASH_COMMAND = Regex("/(\\S+)(?:\\s+(.*))?", RegexOption.DOT_MATCHES_ALL)

    /** `@query` as the last word of the draft (start of text or after whitespace). */
    private val MENTION = Regex("(?:^|(?<=\\s))@(\\S*)$")

    /** The command of a `!command` draft, run in the session's shell; null for other drafts. */
    fun shell(text: String): String? =
        text.trim().takeIf { it.startsWith("!") }?.drop(1)?.trim()

    fun slash(text: String): Slash? =
        SLASH_COMMAND.matchEntire(text)?.let { Slash(it.groupValues[1], it.groupValues[2].trim()) }

    /** Built-in and server commands matching a draft that is only `/name`; null for other drafts. */
    fun slashSuggestions(draft: String, commands: List<Command>): List<Suggestion.Slash>? {
        val query = SLASH_PREFIX.matchEntire(draft)?.groupValues?.get(1) ?: return null
        val builtins = Builtin.entries.map { Suggestion.Slash(it.command, UiText.Res(it.description)) }
        val server = commands.map { Suggestion.Slash(it.name, it.description?.let(UiText::Raw)) }
        return (builtins + server).filter { it.name.startsWith(query, ignoreCase = true) }
    }

    /** The file query of an `@mention` being typed at the end of [draft]. */
    fun mentionQuery(draft: String): String? = MENTION.find(draft)?.groupValues?.get(1)

    /** Replaces the `@mention` being typed with [path]; the file goes with the prompt as an attachment. */
    fun pickFile(draft: String, directory: String, path: String): Pair<String, PromptAttachment>? {
        val match = MENTION.find(draft) ?: return null
        val token = "@$path"
        val attachment = PromptAttachment(uri = "file://" + directory.trimEnd('/') + "/" + path, name = path, mention = token)
        return draft.substring(0, match.range.first) + token + " " to attachment
    }
}
