package io.github.beppe2880.opencodemobile.feature.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decodes a base64 `data:` image, downsampled so its longer side is about [maxSide] pixels. */
internal fun decodeDataImage(dataUri: String, maxSide: Int): ImageBitmap? = try {
    val bytes = Base64.decode(dataUri.substringAfter(','), Base64.DEFAULT)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        ?.asImageBitmap()
} catch (e: IllegalArgumentException) {
    null
}

@Composable
private fun rememberDataImage(dataUri: String?, maxSide: Int): ImageBitmap? {
    val image by produceState<ImageBitmap?>(null, dataUri) {
        value = dataUri?.let { withContext(Dispatchers.Default) { decodeDataImage(it, maxSide) } }
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
    val bitmap = rememberDataImage(dataUri, maxSide = 256)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(OcRadius.md))
            .background(c.layer02)
            .combinedClickable(
                role = Role.Image,
                onClickLabel = stringResource(R.string.chat_open_image),
                onLongClickLabel = onLongClick?.let { stringResource(R.string.chat_message_actions) },
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

/** Full-screen viewer: pinch to zoom, drag when zoomed, double tap to toggle zoom, back to close. */
@Composable
internal fun ImagePreviewDialog(preview: ImagePreview, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // Photos are viewed on dark whatever the app theme.
        val viewer = OcDarkColors
        val bitmap = rememberDataImage(preview.dataUri, maxSide = 2048)
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(viewer.bgDeep)
                .testTag("chat_image_preview"),
            contentAlignment = Alignment.Center,
        ) {
            when {
                bitmap != null -> Image(
                    bitmap = bitmap,
                    contentDescription = preview.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 6f)
                                offset = if (scale == 1f) Offset.Zero else offset + pan
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
                preview.failed -> Text(
                    stringResource(R.string.chat_image_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = viewer.textBase,
                    modifier = Modifier.padding(OcSpacing.lg),
                )
                else -> OcSpinner(size = 24.dp)
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
                Icon(Icons.Outlined.Close, stringResource(R.string.chat_close_preview), tint = viewer.textBase)
            }
        }
    }
}
