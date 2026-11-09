package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.ToolFile
import io.github.beppe2880.opencodemobile.core.domain.ToolMediaSize
import kotlin.io.encoding.Base64

/**
 * The [index]th file tool call [callId] of message [messageId] returned, shown under its tool
 * row (for example the screenshot the agent read).
 */
@Immutable
data class ToolMedia(val messageId: String, val callId: String, val index: Int, val file: ToolFile) {
    /** Identifies the tool call whose files are fetched together. */
    val fetchKey: String get() = toolMediaKey(messageId, callId)

    /** Suffix of the test tags: `tool_media_<callId>_<index>`. */
    val tagId: String get() = "${callId}_$index"

    val kind: MediaKind get() = mediaKind(file)
}

enum class MediaKind { Image, Video, File }

/** Where tapping a tool file leads. */
internal sealed interface MediaOpen {
    /** The full-screen image viewer. */
    data object Viewer : MediaOpen

    /** The read-only file viewer; [path] is absolute or relative to the session folder. */
    data class ServerFile(val path: String) : MediaOpen

    /** The system browser or player. */
    data class Web(val url: String) : MediaOpen
}

/** Key of one tool call's fetched files in [ImageMemory]; distinct from prompt message ids. */
internal fun toolMediaKey(messageId: String, callId: String): String = "tool:$messageId/$callId"

/** Raster images both platforms decode; anything else (SVG, HEIC, ...) is a file chip. */
private val INLINE_IMAGE_MIMES = setOf("image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp", "image/bmp")

/**
 * Inline content larger than this is not decoded: the server never sends more
 * (`MAX_MEDIA_INGEST_BYTES` in the read tool), and a bigger payload is shown as a file chip.
 */
internal const val MAX_INLINE_MEDIA_BYTES = 20L * 1024 * 1024

internal fun mediaKind(file: ToolFile): MediaKind {
    val mime = file.baseMime
    return when {
        mime in INLINE_IMAGE_MIMES && !file.isWebUrl -> MediaKind.Image
        mime.startsWith("video/") -> MediaKind.Video
        else -> MediaKind.File
    }
}

/** An image whose content is not here: a stored copy without payload, or a file on the server. */
internal fun ToolFile.needsFetch(): Boolean =
    mediaKind(this) == MediaKind.Image && !hasInlinePayload && (isDataUri || serverPath != null)

/** Inline content small enough to decode. */
internal fun ToolFile.isDecodable(): Boolean = hasInlinePayload && (inlineBytes ?: Long.MAX_VALUE) <= MAX_INLINE_MEDIA_BYTES

/** The file name shown on cards: the last path segment, or the media type when it has no name. */
internal val ToolFile.displayName: String
    get() = (name ?: serverPath)?.trimEnd('/')?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: baseMime

/** Where tapping opens [file]; [showsImage] when it is drawn as an image. Null: not openable. */
internal fun openTarget(file: ToolFile, showsImage: Boolean): MediaOpen? = when {
    showsImage -> MediaOpen.Viewer
    file.isWebUrl -> MediaOpen.Web(file.uri)
    else -> file.serverPath?.let { MediaOpen.ServerFile(it) }
}

/** The first [maxBytes] (about) of a base64 `data:` URI, without decoding the rest. */
internal fun dataUriHead(uri: String, maxBytes: Int = 64 * 1024): ByteArray? {
    val start = uri.indexOf(',') + 1
    if (start <= 0 || start >= uri.length || !uri.substring(0, start - 1).endsWith(";base64", ignoreCase = true)) return null
    val chars = (maxBytes + 2) / 3 * 4
    val end = if (uri.length - start <= chars) uri.length else start + chars
    return try {
        Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL).decode(uri, start, end)
    } catch (e: IllegalArgumentException) {
        null
    }
}

/** Decodes the whole base64 payload of a `data:` URI; null when malformed or over [maxBytes]. */
internal fun dataUriBytes(file: ToolFile, maxBytes: Long = MAX_INLINE_MEDIA_BYTES): ByteArray? {
    if (!file.hasInlinePayload || (file.inlineBytes ?: Long.MAX_VALUE) > maxBytes) return null
    val uri = file.uri
    val start = uri.indexOf(',') + 1
    if (!uri.substring(0, start - 1).endsWith(";base64", ignoreCase = true)) return null
    return try {
        Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL).decode(uri, start, uri.length)
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * Width and height in pixels read from the header of a PNG, GIF, JPEG or WebP image, so a
 * placeholder has the right shape before the image is decoded; null for other or cut data.
 */
internal fun imageDimensions(head: ByteArray): Pair<Int, Int>? {
    fun u8(i: Int) = head[i].toInt() and 0xFF
    fun be16(i: Int) = (u8(i) shl 8) or u8(i + 1)
    fun le16(i: Int) = u8(i) or (u8(i + 1) shl 8)
    fun le24(i: Int) = u8(i) or (u8(i + 1) shl 8) or (u8(i + 2) shl 16)
    fun be32(i: Int) = (u8(i) shl 24) or (u8(i + 1) shl 16) or (u8(i + 2) shl 8) or u8(i + 3)
    fun ascii(i: Int, text: String) = head.size >= i + text.length && text.indices.all { head[i + it].toInt() == text[it].code }
    fun valid(w: Int, h: Int) = if (w > 0 && h > 0) w to h else null

    return when {
        head.size >= 24 && u8(0) == 0x89 && ascii(1, "PNG") && ascii(12, "IHDR") -> valid(be32(16), be32(20))
        head.size >= 10 && ascii(0, "GIF8") -> valid(le16(6), le16(8))
        head.size >= 30 && ascii(0, "RIFF") && ascii(8, "WEBP") -> when {
            ascii(12, "VP8 ") -> valid(le16(26) and 0x3FFF, le16(28) and 0x3FFF)
            ascii(12, "VP8L") -> valid(
                1 + (((u8(22) and 0x3F) shl 8) or u8(21)),
                1 + (((u8(24) and 0x0F) shl 10) or (u8(23) shl 2) or ((u8(22) and 0xC0) shr 6)),
            )
            ascii(12, "VP8X") -> valid(1 + le24(24), 1 + le24(27))
            else -> null
        }
        head.size >= 4 && u8(0) == 0xFF && u8(1) == 0xD8 -> jpegDimensions(head)
        else -> null
    }
}

/** Walks the JPEG segments up to the first start-of-frame marker. */
private fun jpegDimensions(head: ByteArray): Pair<Int, Int>? {
    fun u8(i: Int) = head[i].toInt() and 0xFF
    var i = 2
    while (i + 3 < head.size) {
        if (u8(i) != 0xFF) return null
        val marker = u8(i + 1)
        when {
            // Fill bytes before a marker.
            marker == 0xFF -> i++
            // Markers without a length.
            marker == 0x01 || marker in 0xD0..0xD8 -> i += 2
            marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC -> {
                if (i + 8 >= head.size) return null
                val height = (u8(i + 5) shl 8) or u8(i + 6)
                val width = (u8(i + 7) shl 8) or u8(i + 8)
                return if (width > 0 && height > 0) width to height else null
            }
            else -> i += 2 + ((u8(i + 2) shl 8) or u8(i + 3))
        }
    }
    return null
}

/** Width over height of an inline image from its header; null when unknown. */
internal fun inlineAspect(file: ToolFile): Float? {
    if (!file.hasInlinePayload) return null
    val (width, height) = dataUriHead(file.uri)?.let(::imageDimensions) ?: return null
    return width.toFloat() / height
}

/** Shape of a placeholder while the size of an image is not known. */
internal const val DEFAULT_MEDIA_ASPECT = 4f / 3f

/**
 * Size of the card of a tool's only image: [widthFraction] of the [available] width with the
 * image's [aspect] (width over height), at most [maxHeight] tall (tall screenshots get narrower)
 * and at least [minHeight] (very wide images are letterboxed).
 */
internal fun singleCardSize(
    available: Float,
    aspect: Float?,
    maxHeight: Float,
    minHeight: Float,
    widthFraction: Float = 0.7f,
): Pair<Float, Float> {
    val ratio = aspect?.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_MEDIA_ASPECT
    var width = available * widthFraction
    var height = width / ratio
    if (height > maxHeight) {
        height = maxHeight
        width = maxHeight * ratio
    }
    return width to height.coerceAtLeast(minHeight)
}

/** Width of one card in a row of several, all [height] tall, clamped to [minWidth]..[maxWidth]. */
internal fun stripCardWidth(aspect: Float?, height: Float, minWidth: Float, maxWidth: Float): Float {
    val ratio = aspect?.takeIf { it.isFinite() && it > 0f } ?: DEFAULT_MEDIA_ASPECT
    return (height * ratio).coerceIn(minWidth, maxWidth)
}

/**
 * The card bounds of a [ToolMediaSize], in dp: a tool's only image takes [widthFraction] of the
 * column and at most [maxHeight]; several share one row [stripHeight] tall.
 */
internal class MediaCardBounds(val widthFraction: Float, val maxHeight: Float, val stripHeight: Float)

/** Null for [ToolMediaSize.Hidden]: images show as file chips. */
internal val ToolMediaSize.bounds: MediaCardBounds?
    get() = when (this) {
        ToolMediaSize.Hidden -> null
        ToolMediaSize.Small -> MediaCardBounds(widthFraction = 0.45f, maxHeight = 220f, stripHeight = 110f)
        ToolMediaSize.Medium -> MediaCardBounds(widthFraction = 0.7f, maxHeight = 360f, stripHeight = 160f)
        ToolMediaSize.Large -> MediaCardBounds(widthFraction = 1f, maxHeight = 520f, stripHeight = 220f)
    }

/** Lowest card of a single image, in dp: very wide images are letterboxed. */
internal const val MIN_CARD_HEIGHT = 48f

/** Width and height in dp of a tool's only image at [size] in a column [available] dp wide; null when hidden. */
internal fun singleCardSize(available: Float, aspect: Float?, size: ToolMediaSize): Pair<Float, Float>? =
    size.bounds?.let { singleCardSize(available, aspect, it.maxHeight, MIN_CARD_HEIGHT, it.widthFraction) }

/**
 * Width and height in dp of one card in a row of several at [size]; null when hidden. The width
 * follows the aspect between 0.6 and 1.75 times the row height.
 */
internal fun stripCardSize(aspect: Float?, size: ToolMediaSize): Pair<Float, Float>? =
    size.bounds?.let { b -> stripCardWidth(aspect, b.stripHeight, b.stripHeight * 0.6f, b.stripHeight * 1.75f) to b.stripHeight }

/**
 * Where a file chip leads: with images [hidden] by the setting, an image chip still opens the
 * viewer (fetching the content when needed); otherwise as [openTarget] for a file not drawn.
 */
internal fun chipTarget(file: ToolFile, hidden: Boolean): MediaOpen? =
    if (hidden && mediaKind(file) == MediaKind.Image && (file.isDecodable() || file.needsFetch())) {
        MediaOpen.Viewer
    } else {
        openTarget(file, showsImage = false)
    }
