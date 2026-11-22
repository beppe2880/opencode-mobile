package io.github.beppe2880.opencodemobile.core.domain

/** Waiting and downloading are skipped for a cache hit; cached JSON still needs decoding for Markdown. */
enum class SessionExportStage { WaitingForServer, Downloading, Decoding, PreparingMessages, Preparing }

/** Work completed in this stage (bytes or messages), never a time estimate. */
data class SessionExportProgress(val stage: SessionExportStage, val completed: Long = 0, val total: Long? = null) {
    val fraction: Float?
        get() = total?.takeIf { it > 0 && completed in 0..it }?.let { (completed.toDouble() / it).toFloat() }
}
