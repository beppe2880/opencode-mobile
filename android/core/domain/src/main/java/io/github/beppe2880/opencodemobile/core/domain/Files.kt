package io.github.beppe2880.opencodemobile.core.domain

/** A file read from the server, as much of it as the viewer shows. */
sealed interface FileContent {
    /** Total size of the file in bytes. */
    val size: Long

    /** [truncated]: only the start of the file was read. */
    data class Text(val text: String, override val size: Long, val truncated: Boolean) : FileContent

    class Image(val bytes: ByteArray, val mime: String, override val size: Long) : FileContent

    /** Not text and not an image the viewer can show. */
    data class Binary(val mime: String, override val size: Long) : FileContent
}

interface FileRepository {
    /**
     * Reads [path] (absolute, or relative to [directory]) from the server, up to a few hundred
     * kilobytes. Fails with [OpenCodeError.NotFound] when the file does not exist.
     */
    suspend fun read(directory: String, path: String): Result<FileContent>
}
