package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.AttachedImageSize
import io.github.beppe2880.opencodemobile.core.domain.ToolMediaSize

/** Side in dp of the square thumbnails of [AttachedImageSize.Thumbnails]. */
internal const val ATTACHED_THUMBNAIL_SIDE = 72f

/** Longer side, in pixels, the square thumbnails are decoded at. */
internal const val THUMBNAIL_DECODE_SIDE = 256

/**
 * The size of images read by tools that attached images share the dimensions of; null for
 * [AttachedImageSize.Thumbnails], which has none.
 */
internal val AttachedImageSize.toolSize: ToolMediaSize?
    get() = when (this) {
        AttachedImageSize.Thumbnails -> null
        AttachedImageSize.Small -> ToolMediaSize.Small
        AttachedImageSize.Medium -> ToolMediaSize.Medium
        AttachedImageSize.Large -> ToolMediaSize.Large
    }

/**
 * Longer side in pixels an attached image is decoded at (about, see [decodeImage]): that of
 * the cards of images read by tools, less for the sizes that never show it that big.
 */
internal val AttachedImageSize.decodeSide: Int
    get() = when (this) {
        AttachedImageSize.Thumbnails -> THUMBNAIL_DECODE_SIDE
        AttachedImageSize.Small -> 512
        AttachedImageSize.Medium, AttachedImageSize.Large -> CARD_DECODE_SIDE
    }

/**
 * Width and height in dp of one of the [count] images of a prompt at [size], in a transcript
 * [available] dp wide. [aspect] is the image's width over height, null while it is not known.
 *
 * [AttachedImageSize.Thumbnails] is a square thumbnail. The other sizes are those of an image
 * read by a tool at the same size: the only image of a prompt as [singleCardSize], several in
 * one row as [stripCardSize].
 */
internal fun attachedImageSize(aspect: Float?, count: Int, size: AttachedImageSize, available: Float): Pair<Float, Float> {
    // A transcript not measured yet counts as a narrow phone.
    val width = available.takeIf { it.isFinite() && it > 0f } ?: UNMEASURED_WIDTH
    val card = size.toolSize?.let { if (count > 1) stripCardSize(aspect, it) else singleCardSize(width, aspect, it) }
    return card ?: (ATTACHED_THUMBNAIL_SIDE to ATTACHED_THUMBNAIL_SIDE)
}

private const val UNMEASURED_WIDTH = 320f

/** Width over height of the image in a base64 `data:` URI, from its header; null when unknown. */
internal fun dataUriAspect(dataUri: String?): Float? {
    val (width, height) = dataUri?.let { dataUriHead(it) }?.let(::imageDimensions) ?: return null
    return width.toFloat() / height
}
