package io.github.beppe2880.opencodemobile.share

import io.github.beppe2880.opencodemobile.core.domain.MAX_SHARED_IMAGES
import io.github.beppe2880.opencodemobile.core.domain.MAX_SHARED_TEXT

/** What a share intent asks for, checked; the images still have to be read. */
data class ShareRequest(
    val text: String,
    /** `content:` URIs of images, at most [MAX_SHARED_IMAGES]. */
    val images: List<String>,
    /** Streams refused: not `content:`, from this app, or beyond [MAX_SHARED_IMAGES]. */
    val rejected: Int,
    val truncated: Boolean,
)

/**
 * Checks `ACTION_SEND` / `ACTION_SEND_MULTIPLE` intents from other apps, which are untrusted:
 * only plain text and images, text cleaned and bounded, images only as `content:` URIs of
 * other apps (a `file:` URI or one of this app's providers could expose its private files).
 */
object ShareIntents {
    const val ACTION_SEND = "android.intent.action.SEND"
    const val ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE"

    /** Null when the intent is not a share this app accepts or carries nothing usable. */
    fun parse(
        action: String?,
        type: String?,
        text: CharSequence?,
        subject: CharSequence?,
        streams: List<String?>,
        ownPackage: String,
    ): ShareRequest? {
        val mime = type?.substringBefore(';')?.trim()?.lowercase() ?: return null
        val isText = mime == "text/plain"
        val isImage = mime.startsWith("image/")
        val accepted = when (action) {
            ACTION_SEND -> isText || isImage
            ACTION_SEND_MULTIPLE -> isImage
            else -> false
        }
        if (!accepted) return null
        val (body, truncated) = combine(clean(subject), clean(text))
        // A text share's stream is a file, not an image: left out.
        val candidates = if (isImage) streams else emptyList()
        val valid = candidates.filterNotNull().filter { isForeignContentUri(it, ownPackage) }.distinct()
        val images = valid.take(MAX_SHARED_IMAGES)
        val rejected = candidates.size - images.size
        if (body.isEmpty() && images.isEmpty()) return null
        return ShareRequest(body, images, rejected, truncated)
    }

    /** The subject (a page title) above the text (its link), unless the text already has it. */
    private fun combine(subject: String, text: String): Pair<String, Boolean> {
        val combined = when {
            subject.isEmpty() -> text
            text.isEmpty() -> subject
            subject in text -> text
            else -> "$subject\n$text"
        }
        return if (combined.length > MAX_SHARED_TEXT) combined.take(MAX_SHARED_TEXT) to true else combined to false
    }

    /** Line breaks unified; control characters other than tabs and line breaks removed. */
    private fun clean(value: CharSequence?): String {
        if (value == null) return ""
        // Cut early: the rest would be dropped anyway.
        val bounded = if (value.length > MAX_SHARED_TEXT * 2) value.subSequence(0, MAX_SHARED_TEXT * 2) else value
        return bounded.toString()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .filter { it == '\n' || it == '\t' || !it.isISOControl() }
            .trim()
    }

    private fun isForeignContentUri(uri: String, ownPackage: String): Boolean {
        if (!uri.startsWith("content://", ignoreCase = true)) return false
        val authority = uri.substring("content://".length).substringBefore('/').substringAfterLast('@').lowercase()
        if (authority.isEmpty()) return false
        // Authorities of this app's providers start with its package name.
        return !authority.startsWith(ownPackage.lowercase())
    }
}
