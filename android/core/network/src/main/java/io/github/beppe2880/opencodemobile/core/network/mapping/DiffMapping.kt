package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.FileDiffInfo
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * File changes of a tool call: the unified diffs the server reports in `metadata.files`
 * (edit, patch) or, for a whole-file `write` that has none, its content as added lines.
 */
internal fun toolDiffs(toolName: String, input: JsonObject?, metadata: JsonObject?): List<FileDiff> =
    metadata.fileDiffs().ifEmpty { listOfNotNull(input?.let { writeDiff(toolName, it) }) }

internal fun JsonObject?.fileDiffs(): List<FileDiff> =
    (this?.get("files") as? JsonArray).orEmpty().mapNotNull { entry ->
        val file = entry as? JsonObject ?: return@mapNotNull null
        val patch = file.string("patch") ?: return@mapNotNull null
        FileDiff(
            path = file.string("file") ?: file.string("path").orEmpty(),
            status = file.string("status"),
            additions = file.int("additions") ?: 0,
            deletions = file.int("deletions") ?: 0,
            patch = patch,
        )
    }

fun FileDiffInfo.toDomain(): FileDiff =
    FileDiff(path = file, status = status.value, additions = additions.toInt(), deletions = deletions.toInt(), patch = patch)

private fun writeDiff(toolName: String, input: JsonObject): FileDiff? {
    if (toolName != "write") return null
    val content = input.string("content") ?: return null
    val lines = if (content.isEmpty()) emptyList() else content.removeSuffix("\n").split("\n")
    val patch = buildString {
        append("@@ -0,0 +1,${lines.size} @@")
        lines.forEach { append("\n+").append(it) }
    }
    return FileDiff(
        path = input.string("path") ?: input.string("filePath").orEmpty(),
        status = FileDiff.WRITTEN,
        additions = lines.size,
        deletions = 0,
        patch = patch,
    )
}

private fun JsonObject.string(key: String): String? = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun JsonObject.int(key: String): Int? = (get(key) as? JsonPrimitive)?.intOrNull
