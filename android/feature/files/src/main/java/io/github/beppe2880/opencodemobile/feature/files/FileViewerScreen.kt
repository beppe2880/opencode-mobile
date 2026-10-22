package io.github.beppe2880.opencodemobile.feature.files

import android.graphics.BitmapFactory
import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.WrapText
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.FileContent
import io.github.beppe2880.opencodemobile.core.ui.components.OcEmptyState
import io.github.beppe2880.opencodemobile.core.ui.components.OcErrorState
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTopBar
import io.github.beppe2880.opencodemobile.core.ui.markdown.highlightCode
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.beppe2880.opencodemobile.core.ui.R as UiR

@Composable
fun FileViewerScreen(
    state: FileViewerState,
    onEvent: (FileViewerEvent) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val c = OcTheme.colors
    val content = state.content
    Scaffold(
        modifier = modifier,
        containerColor = c.bgBase,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            OcTopBar(
                title = state.name,
                subtitle = state.displayPath.takeIf { it != state.name },
                leading = {
                    OcIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(UiR.string.oc_back),
                        onClick = { onEvent(FileViewerEvent.Back) },
                        modifier = Modifier.testTag("file_back_button"),
                    )
                },
                titleAccessory = { if (state.loading && content != null) OcSpinner(Modifier.testTag("file_reloading")) },
                actions = {
                    if (content is FileContent.Text && content.text.isNotEmpty()) {
                        val wrapLabel = stringResource(R.string.file_wrap_lines)
                        val stateLabel = stringResource(if (state.wrap) R.string.file_wrap_on else R.string.file_wrap_off)
                        OcIconButton(
                            icon = Icons.AutoMirrored.Outlined.WrapText,
                            contentDescription = wrapLabel,
                            onClick = { onEvent(FileViewerEvent.ToggleWrap) },
                            tint = if (state.wrap) c.textAccent else c.iconBase,
                            modifier = Modifier
                                .semantics {
                                    role = Role.Switch
                                    stateDescription = stateLabel
                                }
                                .testTag("file_wrap_button"),
                        )
                        OcIconButton(
                            icon = Icons.Outlined.ContentCopy,
                            contentDescription = stringResource(R.string.file_copy),
                            onClick = { onCopy(content.text) },
                            modifier = Modifier.testTag("file_copy_button"),
                        )
                    }
                    OcIconButton(
                        icon = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.file_reload),
                        onClick = { onEvent(FileViewerEvent.Reload) },
                        enabled = !state.loading,
                        modifier = Modifier.testTag("file_reload_button"),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (content) {
                null -> if (state.error != null) {
                    OcErrorState(
                        title = stringResource(R.string.file_error_title),
                        message = state.error.resolve(),
                        onRetry = { onEvent(FileViewerEvent.Reload) },
                        retryTestTag = "file_retry_button",
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    OcSpinner(Modifier.align(Alignment.Center).testTag("file_loading"), size = 20.dp)
                }

                is FileContent.Text -> if (content.text.isEmpty()) {
                    OcEmptyState(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.file_empty),
                        modifier = Modifier.align(Alignment.Center).testTag("file_empty"),
                    )
                } else {
                    CodeView(content, state.name, state.wrap, bottom)
                }

                is FileContent.Image -> ImageView(content, state.name, Modifier.padding(bottom = bottom))

                is FileContent.Binary -> OcEmptyState(
                    icon = Icons.Outlined.InsertDriveFile,
                    title = stringResource(R.string.file_binary_title),
                    message = stringResource(R.string.file_binary_message, content.mime, fileSize(content.size)),
                    modifier = Modifier.align(Alignment.Center).testTag("file_binary"),
                )
            }
        }
    }
}

/** Numbered, highlighted lines; without wrapping, the whole file scrolls sideways together. */
@Composable
private fun CodeView(content: FileContent.Text, name: String, wrap: Boolean, bottomPadding: Dp) {
    val c = OcTheme.colors
    val style = OcTheme.mono.code
    val dark = c.isDark
    val lines by produceState<List<AnnotatedString>?>(null, content, dark) {
        value = withContext(Dispatchers.Default) { splitLines(highlightCode(content.text, name, dark)) }
    }
    val shown = lines ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { OcSpinner(size = 20.dp) }
        return
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val charWidth = remember(style, density) { with(density) { measurer.measure("0", style).size.width.toDp() } }
    val digits = shown.size.toString().length
    val gutter = charWidth * digits + OcSpacing.md
    val longest = remember(shown) { shown.maxOf { it.length } }
    val readBytes = remember(content) { content.text.encodeToByteArray().size.toLong() }
    val padding = PaddingValues(start = OcSpacing.sm, end = OcSpacing.md, top = OcSpacing.sm, bottom = OcSpacing.lg + bottomPadding)
    val list = @Composable { listModifier: Modifier ->
        SelectionContainer {
            LazyColumn(modifier = listModifier.testTag("file_lines"), contentPadding = padding) {
                items(shown.size, key = { it }) { index ->
                    Row {
                        Text(
                            text = (index + 1).toString().padStart(digits),
                            style = style,
                            color = c.textFaint,
                            softWrap = false,
                            modifier = Modifier.width(gutter),
                        )
                        Text(
                            text = shown[index],
                            style = style,
                            color = c.textBase,
                            softWrap = wrap,
                            modifier = if (wrap) Modifier.weight(1f) else Modifier,
                        )
                    }
                }
                if (content.truncated) {
                    item(key = "truncated") {
                        Text(
                            text = stringResource(R.string.file_truncated, fileSize(readBytes), fileSize(content.size)),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                            modifier = Modifier.padding(start = gutter, top = OcSpacing.md).testTag("file_truncated"),
                        )
                    }
                }
            }
        }
    }
    if (wrap) {
        list(Modifier.fillMaxSize())
    } else {
        val width = OcSpacing.sm + gutter + charWidth * (longest + 1) + OcSpacing.md
        Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
            list(Modifier.width(width).fillMaxHeight())
        }
    }
}

@Composable
private fun ImageView(content: FileContent.Image, name: String, modifier: Modifier = Modifier) {
    // Null while decoding; a decoded null bitmap is a file Android cannot read.
    val decoded by produceState<Pair<ImageBitmap?, Boolean>?>(null, content) {
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(content.bytes, 0, content.bytes.size)?.asImageBitmap() to true
        }
    }
    Box(modifier.fillMaxSize().background(OcTheme.colors.bgDeep), contentAlignment = Alignment.Center) {
        val image = decoded?.first
        if (decoded != null && image == null) {
            OcEmptyState(
                icon = Icons.Outlined.InsertDriveFile,
                title = stringResource(R.string.file_binary_title),
                message = stringResource(R.string.file_binary_message, content.mime, fileSize(content.size)),
                modifier = Modifier.testTag("file_binary"),
            )
        } else if (image != null) {
            Image(
                bitmap = image,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(OcSpacing.lg).testTag("file_image"),
            )
        } else {
            OcSpinner(size = 20.dp)
        }
    }
}

@Composable
private fun fileSize(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

/** One entry per line, without the line breaks; a final newline adds no empty line. */
internal fun splitLines(text: AnnotatedString): List<AnnotatedString> {
    val raw = text.text
    val lines = ArrayList<AnnotatedString>()
    var start = 0
    while (start <= raw.length) {
        val newline = raw.indexOf('\n', start)
        val end = if (newline < 0) raw.length else newline
        if (newline < 0 && start == raw.length && lines.isNotEmpty()) break
        val cut = if (end > start && raw[end - 1] == '\r') end - 1 else end
        lines += text.subSequence(start, cut)
        if (newline < 0) break
        start = newline + 1
    }
    return lines
}
