package io.github.beppe2880.opencodemobile.core.data.files

import io.github.beppe2880.opencodemobile.core.data.OpenCodeClients
import io.github.beppe2880.opencodemobile.core.data.resultOf
import io.github.beppe2880.opencodemobile.core.domain.FileContent
import io.github.beppe2880.opencodemobile.core.domain.FileRepository
import io.github.beppe2880.opencodemobile.core.network.FileBytes

internal class RemoteFileRepository(private val clients: OpenCodeClients) : FileRepository {

    override suspend fun read(directory: String, path: String): Result<FileContent> = resultOf {
        val client = clients.active()
        val (location, relative) = fileLocation(directory, path)
        val read = client.readFile(location, relative, MAX_TEXT_BYTES)
        val image = read.mime?.let(::imageMime)
        if (image != null && read.size > read.bytes.size && read.size <= MAX_IMAGE_BYTES) {
            // A partial image cannot be decoded: read the whole of it.
            fileContent(client.readFile(location, relative, MAX_IMAGE_BYTES.toInt()))
        } else {
            fileContent(read)
        }
    }

    internal companion object {
        const val MAX_TEXT_BYTES = 512 * 1024
        const val MAX_IMAGE_BYTES = 8L * 1024 * 1024
    }
}

/**
 * The server reads paths relative to a location directory. Tools report absolute paths, which may
 * also point outside the session folder: those are read from their own folder.
 */
internal fun fileLocation(directory: String, path: String): Pair<String, String> {
    val base = directory.trimEnd('/')
    return when {
        !path.startsWith("/") -> base to path.removePrefix("./")
        base.isNotEmpty() && path.startsWith("$base/") -> base to path.removePrefix("$base/")
        else -> path.substringBeforeLast('/').ifEmpty { "/" } to path.substringAfterLast('/')
    }
}

internal fun fileContent(read: FileBytes): FileContent {
    val bytes = read.bytes
    val truncated = read.size > bytes.size
    val image = read.mime?.let(::imageMime)
    if (image != null) {
        return if (truncated) FileContent.Binary(image, read.size) else FileContent.Image(bytes, image, read.size)
    }
    // Text files have no NUL bytes; nearly every binary format has some near the start.
    val sniffed = minOf(bytes.size, SNIFF_BYTES)
    if ((0 until sniffed).any { bytes[it] == 0.toByte() }) {
        return FileContent.Binary(read.mime ?: "application/octet-stream", read.size)
    }
    val text = bytes.decodeToString()
    // A cut read can end inside a multi-byte character.
    return FileContent.Text(if (truncated) text.removeSuffix("\uFFFD") else text, read.size, truncated)
}

/** Raster images Android decodes; SVG is shown as its source text. */
private fun imageMime(mime: String): String? = mime.lowercase().takeIf { it in IMAGE_MIMES }

private val IMAGE_MIMES = setOf("image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp", "image/x-icon", "image/vnd.microsoft.icon")

private const val SNIFF_BYTES = 8 * 1024
