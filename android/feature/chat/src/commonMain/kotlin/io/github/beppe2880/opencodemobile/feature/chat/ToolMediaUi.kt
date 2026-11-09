package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ToolMediaSize
import io.github.beppe2880.opencodemobile.core.ui.components.OcSkeleton
import io.github.beppe2880.opencodemobile.core.ui.text.formatFileSize
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_open_file
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_open_image
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_tool_media_file
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_tool_media_image
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_tool_media_video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private val ChipMaxWidth = 280.dp

/** Longer side, in pixels, images are decoded at for cards (the viewer decodes its own copy). */
private const val CARD_DECODE_SIDE = 768

/**
 * The files tools of one group returned, under the group's row whether it is folded or not:
 * one image as a card of part of the column width, several as a row of smaller cards, both as
 * big as [size] (the setting) says; other files, and images with [ToolMediaSize.Hidden], as
 * chips. [loaded] holds the [ToolMedia.fetchKey]s already fetched in this chat, so a card
 * without content becomes a chip instead of waiting forever.
 */
@Composable
internal fun ToolMediaStrip(
    media: List<ToolMedia>,
    size: ToolMediaSize,
    loaded: Set<String>,
    onLoad: (ToolMedia) -> Unit,
    onOpen: (ToolMedia, MediaOpen) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (media.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth().padding(top = OcSpacing.sm)) {
        val available = maxWidth
        if (media.size == 1) {
            ToolMediaItem(media.single(), single = true, size, available, loaded, onLoad, onOpen)
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                media.forEach { item -> ToolMediaItem(item, single = false, size, available, loaded, onLoad, onOpen) }
            }
        }
    }
}

private sealed interface Decoded {
    data object Pending : Decoded
    data object Failed : Decoded
    class Ready(val bitmap: ImageBitmap) : Decoded
}

@Composable
private fun ToolMediaItem(
    item: ToolMedia,
    single: Boolean,
    size: ToolMediaSize,
    available: Dp,
    loaded: Set<String>,
    onLoad: (ToolMedia) -> Unit,
    onOpen: (ToolMedia, MediaOpen) -> Unit,
) {
    val file = item.file
    if (size == ToolMediaSize.Hidden) {
        // Nothing is decoded or fetched until the chip opens the viewer.
        ToolFileChip(item, chipTarget(file, hidden = true), onOpen)
        return
    }
    val decodable = item.kind == MediaKind.Image && file.isDecodable()
    val waiting = item.kind == MediaKind.Image && !decodable && file.needsFetch() && item.fetchKey !in loaded
    if (!decodable && !waiting) {
        ToolFileChip(item, chipTarget(file, hidden = false), onOpen)
        return
    }
    val currentOnLoad by rememberUpdatedState(onLoad)
    if (waiting) LaunchedEffect(item.fetchKey) { currentOnLoad(item) }
    val decoded by produceState<Decoded>(Decoded.Pending, file.uri, decodable) {
        if (decodable) {
            value = withContext(Dispatchers.Default) {
                dataUriBytes(file)?.let { decodeImage(it, CARD_DECODE_SIDE) }
            }?.let { Decoded.Ready(it) } ?: Decoded.Failed
        }
    }
    val image = decoded
    if (image is Decoded.Failed) {
        ToolFileChip(item, chipTarget(file, hidden = false), onOpen)
        return
    }
    val headerAspect = remember(file.uri) { inlineAspect(file) }
    val aspect = (image as? Decoded.Ready)?.bitmap?.let { it.width.toFloat() / it.height } ?: headerAspect
    // Never null here: hidden images returned above.
    val (width, height) = (if (single) singleCardSize(available.value, aspect, size) else stripCardSize(aspect, size))
        ?.let { (w, h) -> w.dp to h.dp } ?: return
    val shape = RoundedCornerShape(OcRadius.lg)
    val c = OcTheme.colors
    val label = stringResource(Res.string.chat_tool_media_image, file.displayName)
    Box(Modifier.testTag("tool_media_${item.tagId}")) {
        Box(
            modifier = Modifier
                .size(width, height)
                .clip(shape)
                .background(c.layer02)
                .border(BorderStroke(1.dp, c.borderMuted), shape)
                .clickable(
                    enabled = image is Decoded.Ready,
                    role = Role.Image,
                    onClickLabel = stringResource(Res.string.chat_open_image),
                    onClick = { onOpen(item, MediaOpen.Viewer) },
                )
                .semantics { contentDescription = label }
                .testTag("tool_media_open_${item.tagId}"),
            contentAlignment = Alignment.Center,
        ) {
            when (image) {
                is Decoded.Ready -> Image(
                    bitmap = image.bitmap,
                    contentDescription = null,
                    contentScale = if (single) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> OcSkeleton(Modifier.fillMaxSize(), shape)
            }
        }
    }
}

/** A file that is not drawn: icon, name and type with size; tapping opens [target] when there is one. */
@Composable
private fun ToolFileChip(item: ToolMedia, target: MediaOpen?, onOpen: (ToolMedia, MediaOpen) -> Unit) {
    val c = OcTheme.colors
    val file = item.file
    val size = file.inlineBytes?.let { formatFileSize(it) }
    val details = listOfNotNull(file.baseMime, size).joinToString(" · ")
    val label = when (item.kind) {
        MediaKind.Image -> stringResource(Res.string.chat_tool_media_image, file.displayName)
        MediaKind.Video -> stringResource(Res.string.chat_tool_media_video, file.displayName, details)
        MediaKind.File -> stringResource(Res.string.chat_tool_media_file, file.displayName, details)
    }
    val icon = when (item.kind) {
        MediaKind.Image -> Icons.Outlined.Image
        MediaKind.Video -> Icons.Outlined.Movie
        MediaKind.File -> Icons.Outlined.Description
    }
    val shape = RoundedCornerShape(OcRadius.lg)
    val openLabel = stringResource(if (target == MediaOpen.Viewer) Res.string.chat_open_image else Res.string.chat_open_file)
    Box(Modifier.testTag("tool_media_${item.tagId}")) {
        Row(
            modifier = Modifier
                .widthIn(max = ChipMaxWidth)
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(c.layer02)
                .border(BorderStroke(1.dp, c.borderMuted), shape)
                .then(
                    if (target != null) {
                        Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = { onOpen(item, target) })
                    } else {
                        Modifier
                    },
                )
                .semantics { contentDescription = label }
                .testTag("tool_media_open_${item.tagId}")
                .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            Icon(icon, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(20.dp))
            Column(Modifier.clearAndSetSemantics {}) {
                Text(
                    file.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textBase,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    details,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
