package io.github.beppe2880.opencodemobile.core.data.share

import io.github.beppe2880.opencodemobile.core.domain.PendingShare
import java.io.File
import java.io.IOException

/**
 * The waiting share in [dir] (app-private, excluded from backups): `share.json` and one file per
 * image. A new share is written beside the old one and swapped in, so a crash leaves either.
 */
class FileShareStore(private val dir: File) : ShareStore {

    override fun read(): PendingShare? {
        val description = File(dir, DESCRIPTION)
        if (!description.isFile) return null
        return try {
            val uris = generateSequence(0) { it + 1 }
                .map { File(dir, "$ATTACHMENT$it") }
                .takeWhile { it.isFile }
                .map { it.readText() }
                .toList()
            ShareCodec.decode(description.readText(), uris)
        } catch (_: IOException) {
            null
        } ?: run {
            // Unreadable: dropped rather than offered again at every start.
            dir.deleteRecursively()
            null
        }
    }

    override fun write(share: PendingShare?) {
        if (share == null) {
            dir.deleteRecursively()
            return
        }
        val parent = dir.parentFile ?: return
        val next = File(parent, dir.name + ".new").apply { deleteRecursively() }
        try {
            next.mkdirs()
            val (description, uris) = ShareCodec.encode(share)
            uris.forEachIndexed { i, uri -> File(next, "$ATTACHMENT$i").writeText(uri) }
            File(next, DESCRIPTION).writeText(description)
            dir.deleteRecursively()
            if (!next.renameTo(dir)) throw IOException("Cannot store the share")
        } catch (_: IOException) {
            // Kept in memory for this run only.
            next.deleteRecursively()
        }
    }

    private companion object {
        const val DESCRIPTION = "share.json"
        const val ATTACHMENT = "attachment-"
    }
}
