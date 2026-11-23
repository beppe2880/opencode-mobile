package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus

/** Presentation helpers for tool calls; tolerant of partial JSON input while streaming. */
object ToolInfo {

    fun title(name: String): String = when (name.lowercase()) {
        "read" -> "Read"
        "write" -> "Write"
        "edit", "multiedit" -> "Edit"
        "bash", "shell" -> "Shell"
        "grep" -> "Grep"
        "glob" -> "Glob"
        "list", "ls" -> "List"
        "webfetch" -> "Fetch"
        "websearch" -> "Web search"
        "codesearch" -> "Code search"
        "task" -> "Task"
        "todowrite", "todoread" -> "Todo"
        "patch", "apply_patch" -> "Patch"
        "skill" -> "Skill"
        "question" -> "Question"
        else -> name.substringAfterLast('_').replaceFirstChar { it.uppercase() }
    }

    fun icon(name: String): ImageVector = when (name.lowercase()) {
        "read" -> Icons.AutoMirrored.Outlined.Article
        "write" -> Icons.Outlined.NoteAdd
        "edit", "multiedit" -> Icons.Outlined.Edit
        "bash", "shell" -> Icons.Outlined.Terminal
        "grep", "codesearch" -> Icons.Outlined.Search
        "glob" -> Icons.Outlined.FolderOpen
        "list", "ls" -> Icons.AutoMirrored.Outlined.List
        "webfetch" -> Icons.Outlined.Language
        "websearch" -> Icons.Outlined.TravelExplore
        "task" -> Icons.Outlined.AccountTree
        "todowrite", "todoread" -> Icons.Outlined.Checklist
        "patch", "apply_patch" -> Icons.Outlined.Difference
        else -> Icons.Outlined.Build
    }

    enum class Kind { Shell, Edit, Read, Other }

    /** Kind of tool, for the settings that follow running work. */
    fun kind(name: String): Kind = when (name.lowercase()) {
        "bash", "shell" -> Kind.Shell
        "write", "edit", "multiedit", "patch", "apply_patch" -> Kind.Edit
        "read", "grep", "glob", "list", "ls", "codesearch" -> Kind.Read
        else -> Kind.Other
    }

    /** The file a read, write or edit call works on, once its input is complete. */
    fun filePath(tool: Part.Tool): String? {
        if (tool.status == ToolStatus.Streaming || tool.name.lowercase() !in FILE_TOOLS) return null
        return listOf("filePath", "path").firstNotNullOfOrNull { key -> stringField(tool.input, key)?.takeIf { it.isNotBlank() } }
    }

    private val FILE_TOOLS = setOf("read", "write", "edit", "multiedit")

    private val PRIMARY_KEYS = listOf("command", "filePath", "path", "pattern", "url", "query", "description", "prompt", "name")

    /** Most meaningful argument (path, command, pattern...) for the one-line summary. */
    fun primaryArgument(input: String): String? {
        for (key in PRIMARY_KEYS) {
            val value = stringField(input, key) ?: continue
            if (value.isNotBlank()) return value.lineSequence().first().trim()
        }
        return null
    }

    /**
     * Extracts a top-level-ish JSON string field without a full parser; works on partial input.
     * A plain scan rather than a regex: the Kotlin/Native regex engine recurses once per character
     * of a repeated group, and the long values of tool input (a whole file written) overflowed
     * the stack on iOS.
     */
    internal fun stringField(json: String, key: String): String? {
        val quoted = "\"" + key + "\""
        var from = 0
        while (true) {
            val at = json.indexOf(quoted, from)
            if (at < 0) return null
            from = at + 1
            var i = json.skipSpaces(at + quoted.length)
            if (i >= json.length || json[i] != ':') continue
            i = json.skipSpaces(i + 1)
            if (i >= json.length || json[i] != '"') continue
            val start = i + 1
            var end = start
            while (end < json.length) {
                when (json[end]) {
                    '"' -> break
                    // An escape cut off at the end of partial input is left out.
                    '\\' -> if (end + 1 < json.length) end += 2 else break
                    else -> end++
                }
            }
            return unescape(json.substring(start, end))
        }
    }

    private fun String.skipSpaces(from: Int): Int {
        var i = from
        while (i < length && this[i].isJsonSpace()) i++
        return i
    }

    /** What the regex `\s` matched before: JSON only uses space, tab, CR and LF. */
    private fun Char.isJsonSpace() = this == ' ' || this == '\t' || this == '\n' || this == '\r' || this == '\u000B' || this == '\u000C'

    private fun unescape(value: String): String {
        if ('\\' !in value) return value
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (val next = value[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> Unit
                    '"', '\\', '/' -> sb.append(next)
                    'u' -> if (i + 5 < value.length) {
                        value.substring(i + 2, i + 6).toIntOrNull(16)?.let { sb.append(it.toChar()) }
                        i += 4
                    }
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(ch)
                i++
            }
        }
        return sb.toString()
    }
}
