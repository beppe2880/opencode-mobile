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

    /** Extracts a top-level-ish JSON string field without a full parser; works on partial input. */
    internal fun stringField(json: String, key: String): String? {
        val match = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)").find(json) ?: return null
        return unescape(match.groupValues[1])
    }

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
