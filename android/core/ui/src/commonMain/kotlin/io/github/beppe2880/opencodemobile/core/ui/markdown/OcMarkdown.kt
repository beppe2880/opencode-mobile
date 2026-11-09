package io.github.beppe2880.opencodemobile.core.ui.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.markdownAnnotator
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import io.github.beppe2880.opencodemobile.core.ui.theme.OcColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMarkdownPalette
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMonoTypography
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.core.ui.theme.markdownPalette
import org.intellij.markdown.MarkdownElementTypes

/**
 * Renders assistant markdown with highlighted code fences (with copy header).
 * Parsing goes through [cache] so only the streaming message re-parses as it grows.
 */
@Composable
fun OcMarkdown(
    id: String,
    content: String,
    cache: MarkdownCache,
    modifier: Modifier = Modifier,
) {
    val colors = OcTheme.colors
    val typography = MaterialTheme.typography
    val mono = OcTheme.mono
    val highlights = remember(colors.isDark) {
        Highlights.Builder().theme(SyntaxThemes.atom(darkMode = colors.isDark))
    }
    val components = remember(highlights) {
        markdownComponents(
            codeFence = {
                MarkdownHighlightedCodeFence(
                    content = it.content,
                    node = it.node,
                    style = it.typography.code,
                    highlightsBuilder = highlights,
                    showHeader = true,
                )
            },
            codeBlock = {
                MarkdownHighlightedCodeBlock(
                    content = it.content,
                    node = it.node,
                    style = it.typography.code,
                    highlightsBuilder = highlights,
                    showHeader = true,
                )
            },
            // Cells wrap instead of being cut to one line: on a phone a cut cell cannot be read
            // even by scrolling the table sideways.
            table = {
                MarkdownTable(
                    content = it.content,
                    node = it.node,
                    style = it.typography.table,
                    headerBlock = { content, header, tableWidth, style ->
                        MarkdownTableHeader(content, header, tableWidth, style, maxLines = Int.MAX_VALUE, overflow = TextOverflow.Clip)
                    },
                    rowBlock = { content, row, tableWidth, style ->
                        MarkdownTableRow(
                            content, row, tableWidth, style,
                            verticalAlignment = Alignment.Top,
                            maxLines = Int.MAX_VALUE,
                            overflow = TextOverflow.Clip,
                        )
                    },
                )
            },
        )
    }
    val colored = LocalColoredMarkdown.current
    val mdColors = remember(colors) { markdownColors(colors) }
    val mdTypography = remember(typography, colors, mono, colored) {
        markdownTypography(typography, mono, colors, if (colored) colors.markdownPalette else null)
    }
    val annotator = remember(colors.isDark, colored) {
        if (colored) coloredAnnotator(colors.markdownPalette) else markdownAnnotator()
    }
    val state = cache.stateFor(id, content)
    Markdown(
        state = state,
        colors = mdColors,
        typography = mdTypography,
        components = components,
        annotator = annotator,
        modifier = modifier,
    )
}

/**
 * Whether [OcMarkdown] colors headings, bold and italic text, inline code, links and quotes with
 * the [OcMarkdownPalette] of the OpenCode terminal app; off renders them in the text color.
 */
val LocalColoredMarkdown = compositionLocalOf { false }

private fun markdownColors(c: OcColors): MarkdownColors = DefaultMarkdownColors(
    text = c.textBase,
    codeBackground = if (c.isDark) c.layer01 else c.layer02,
    inlineCodeBackground = c.layer02,
    dividerColor = c.borderBase,
    tableBackground = if (c.isDark) c.layer01 else c.layer02,
)

/**
 * The text styles of markdown; with a [palette] (colored markdown) headings, inline code, links
 * and quotes take its colors like in the OpenCode terminal app. Code blocks never change.
 */
private fun markdownTypography(t: Typography, mono: OcMonoTypography, c: OcColors, palette: OcMarkdownPalette?): MarkdownTypography {
    val body: TextStyle = t.bodyMedium.copy(color = c.textBase)
    val inlineCode = body.merge(mono.code.copy(fontSize = body.fontSize * 0.92f))
    if (palette == null) {
        return DefaultMarkdownTypography(
            h1 = t.titleLarge,
            h2 = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            h3 = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            h4 = t.titleSmall,
            h5 = body,
            h6 = body,
            text = body,
            code = mono.codeSmall,
            inlineCode = inlineCode,
            quote = body.copy(color = c.textMuted),
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
            textLink = TextLinkStyles(
                style = body.copy(color = c.textAccent, textDecoration = TextDecoration.Underline).toSpanStyle(),
            ),
            table = t.bodySmall,
        )
    }
    fun TextStyle.heading() = copy(color = palette.heading, fontWeight = FontWeight.Bold)
    return DefaultMarkdownTypography(
        h1 = t.titleLarge.heading().copy(textDecoration = TextDecoration.Underline),
        h2 = t.titleMedium.heading(),
        h3 = t.titleSmall.heading(),
        h4 = t.titleSmall.heading(),
        h5 = body.heading(),
        h6 = body.heading(),
        text = body,
        code = mono.codeSmall,
        inlineCode = inlineCode.copy(color = palette.inlineCode),
        quote = body.copy(color = palette.blockQuote, fontStyle = FontStyle.Italic),
        paragraph = body,
        ordered = body,
        bullet = body,
        list = body,
        textLink = TextLinkStyles(
            style = body.copy(color = palette.link, textDecoration = TextDecoration.Underline).toSpanStyle(),
        ),
        table = t.bodySmall,
    )
}

/**
 * Colors bold and italic text with [palette]. The renderer styles them itself (bold, italic)
 * without a hook for the color, so the color is pushed when it reaches the node and popped at its
 * closing delimiter, the node's last child: both styles then end at the same place.
 */
private fun coloredAnnotator(palette: OcMarkdownPalette): MarkdownAnnotator = markdownAnnotator { _, child ->
    when (child.type) {
        MarkdownElementTypes.STRONG -> pushStyle(SpanStyle(color = palette.strong, fontWeight = FontWeight.Bold))
        MarkdownElementTypes.EMPH -> pushStyle(SpanStyle(color = palette.emphasis, fontStyle = FontStyle.Italic))
        else -> {
            val parent = child.parent
            if (parent != null && parent.type in COLORED_SPANS && child === parent.children.lastOrNull()) pop()
        }
    }
    // The renderer still handles every node: it appends the text and its own bold or italic.
    false
}

private val COLORED_SPANS = setOf(MarkdownElementTypes.STRONG, MarkdownElementTypes.EMPH)
