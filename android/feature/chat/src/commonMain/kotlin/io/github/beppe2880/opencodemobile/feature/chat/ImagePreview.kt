package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcDarkColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_close_preview
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_image_failed
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_message_actions
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_open_image
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import kotlin.io.encoding.Base64

/** Decodes a base64 `data:` image, downsampled so its longer side is about [maxSide] pixels. */
internal fun decodeDataImage(dataUri: String, maxSide: Int): ImageBitmap? = try {
    val payload = dataUri.substringAfter(',').filterNot { it.isWhitespace() }
    decodeImage(Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL).decode(payload), maxSide)
} catch (e: IllegalArgumentException) {
    null
}

/**
 * Decodes an encoded image (JPEG, PNG, ...), halving its size while the longer side stays at
 * least twice [maxSide]; null when the format is not supported.
 */
internal expect fun decodeImage(bytes: ByteArray, maxSide: Int): ImageBitmap?

/** Full-screen dialog drawn behind the system bars. */
internal expect fun edgeToEdgeDialogProperties(): DialogProperties

/**
 * Thumbnails decoded recently, by data URI: a row created again shows them at once instead of
 * blinking while they decode, as when a sent prompt is confirmed under its server id.
 */
private object Thumbnails {
    private const val MAX = 12
    private val lock = SynchronizedObject()

    // Least recently used first.
    private val bitmaps = LinkedHashMap<String, ImageBitmap>()

    fun get(dataUri: String): ImageBitmap? = synchronized(lock) { bitmaps.remove(dataUri)?.also { bitmaps[dataUri] = it } }

    fun put(dataUri: String, bitmap: ImageBitmap) {
        synchronized(lock) {
            bitmaps.remove(dataUri)
            bitmaps[dataUri] = bitmap
            if (bitmaps.size > MAX) bitmaps.remove(bitmaps.keys.first())
        }
    }
}

@Composable
private fun rememberDataImage(dataUri: String?, maxSide: Int, cached: Boolean = false): ImageBitmap? {
    val initial = remember { if (cached) dataUri?.let(Thumbnails::get) else null }
    val image by produceState(initial, dataUri) {
        val known = if (cached) dataUri?.let(Thumbnails::get) else null
        value = known ?: dataUri?.let { uri ->
            withContext(Dispatchers.Default) { decodeDataImage(uri, maxSide) }?.also { if (cached) Thumbnails.put(uri, it) }
        }
    }
    return image
}

/** A square thumbnail of a sent image; an image icon while it is not available locally. */
@Composable
internal fun ImageThumbnail(
    dataUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    onLongClick: (() -> Unit)? = null,
) {
    val c = OcTheme.colors
    val bitmap = rememberDataImage(dataUri, maxSide = 256, cached = true)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(OcRadius.md))
            .background(c.layer02)
            .combinedClickable(
                role = Role.Image,
                onClickLabel = stringResource(Res.string.chat_open_image),
                onLongClickLabel = onLongClick?.let { stringResource(Res.string.chat_message_actions) },
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * Full-screen viewer: swipe between the images, pinch to zoom, drag when zoomed, double tap to
 * toggle zoom, back to close. [onPage] reports the image on screen.
 */
@Composable
internal fun ImagePreviewDialog(preview: ImagePreview, onPage: (Int) -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = edgeToEdgeDialogProperties(),
    ) {
        // Photos are viewed on dark whatever the app theme.
        val viewer = OcDarkColors
        val count = preview.images.size
        val pager = rememberPagerState(initialPage = preview.index.coerceIn(0, (count - 1).coerceAtLeast(0))) { count }
        // Pages zoomed in: a drag pans the image instead of turning the page.
        var zoomed by remember { mutableStateOf(emptySet<Int>()) }
        val currentOnPage by rememberUpdatedState(onPage)
        LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { currentOnPage(it) } }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(viewer.bgDeep)
                .testTag("chat_image_preview"),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pager,
                userScrollEnabled = pager.currentPage !in zoomed,
                key = { it },
                modifier = Modifier.fillMaxSize().testTag("chat_image_preview_pager"),
            ) { page ->
                PreviewPage(
                    image = preview.images[page],
                    onZoomed = { zoomedIn -> zoomed = if (zoomedIn) zoomed + page else zoomed - page },
                    modifier = Modifier.testTag("chat_image_preview_page_$page"),
                )
            }
            if (count > 1) {
                Text(
                    "${pager.currentPage + 1} / $count",
                    style = MaterialTheme.typography.labelLarge,
                    color = viewer.textBase,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(OcSpacing.md)
                        .clip(RoundedCornerShape(50))
                        .background(viewer.scrim)
                        .padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs)
                        .testTag("chat_image_preview_position"),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(OcSpacing.md)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(viewer.scrim)
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .testTag("chat_image_preview_close"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Close, stringResource(Res.string.chat_close_preview), tint = viewer.textBase)
            }
        }
    }
}

/**
 * One image of the viewer with its own zoom. One finger at full size is left to the pager, so
 * it turns the page; two fingers zoom, and once zoomed in one finger pans.
 */
@Composable
private fun PreviewPage(image: PreviewImage, onZoomed: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val viewer = OcDarkColors
    val bitmap = rememberDataImage(image.dataUri, maxSide = 2048)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val currentOnZoomed by rememberUpdatedState(onZoomed)
    LaunchedEffect(Unit) { snapshotFlow { scale > 1f }.collect { currentOnZoomed(it) } }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = image.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } > 1 || scale > 1f) {
                                    scale = (scale * event.calculateZoom()).coerceIn(1f, 6f)
                                    offset = if (scale == 1f) Offset.Zero else offset + event.calculatePan()
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        })
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            image.failed -> Text(
                stringResource(Res.string.chat_image_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = viewer.textBase,
                modifier = Modifier.padding(OcSpacing.lg),
            )
            else -> OcSpinner(size = 24.dp)
        }
    }
}
