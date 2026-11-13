package io.github.beppe2880.opencodemobile.core.ui.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.unit.TextUnit
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.components.CurrentComponentsBridge
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
import com.mikepenz.markdown.model.markdownInlineContent
import dev.snipme.highlights.Highlights
import io.github.beppe2880.opencodemobile.core.ui.theme.OcColors
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMarkdownPalette
import io.github.beppe2880.opencodemobile.core.ui.theme.OcMonoTypography
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
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
    val highlights = remember(colors.isDark, colors.syntax) {
        Highlights.Builder().theme(syntaxTheme(colors.syntax, colors.isDark))
    }
    // The active OpenCode theme's markdown colors; the app's own themes have none.
    val palette = colors.markdown
    val mdColors = remember(colors) { markdownColors(colors) }
    val parsed = cache.parsedFor(id, content)
    val formulas = parsed.formulas
    val growLines = formulas.any { !it.display }
    val mdTypography = remember(typography, colors, mono, growLines) { markdownTypography(typography, mono, colors, palette, growLines = growLines) }
    // The renderer lays out the paragraphs of a quote like any other, with the paragraph style of
    // the current typography: colored quotes provide one whose text is the quote style.
    val quoteTypography = remember(typography, colors, mono, growLines) {
        palette?.let { markdownTypography(typography, mono, colors, it, quoteText = true, growLines = growLines) }
    }
    val annotator = remember(palette, formulas) {
        mathAnnotator(if (palette != null) coloredAnnotator(palette) else markdownAnnotator(), formulas)
    }
    val mathContent = rememberMathInlineContent(formulas, mdTypography.paragraph, colors.isDark)
    val components = remember(highlights, quoteTypography, formulas, colors.isDark) {
        val defaultBlockQuote = CurrentComponentsBridge.blockQuote
        val defaultParagraph = CurrentComponentsBridge.paragraph
        markdownComponents(
            paragraph = { model ->
                val formula = soleDisplayFormula(model.content.substring(model.node.startOffset, model.node.endOffset), formulas)
                if (formula != null) MathBlock(formula, model.typography.paragraph, colors.isDark) else defaultParagraph(model)
            },
            blockQuote = if (quoteTypography == null) {
                defaultBlockQuote
            } else {
                { model ->
                    CompositionLocalProvider(LocalMarkdownTypography provides quoteTypography) { defaultBlockQuote(model) }
                }
            },
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
    Markdown(
        state = parsed.state,
        colors = mdColors,
        typography = mdTypography,
        components = components,
        annotator = annotator,
        inlineContent = markdownInlineContent(mathContent),
        modifier = modifier,
    )
}

private fun markdownColors(c: OcColors): MarkdownColors = DefaultMarkdownColors(
    text = c.markdown?.text ?: c.textBase,
    codeBackground = if (c.isDark) c.layer01 else c.layer02,
    inlineCodeBackground = c.layer02,
    dividerColor = c.markdown?.horizontalRule ?: c.borderBase,
    tableBackground = if (c.isDark) c.layer01 else c.layer02,
    // Search matches in the text; the app does not highlight any yet.
    highlightText = c.warning.fg,
    highlightBackground = c.warning.bg,
)

/**
 * The text styles of markdown; with a [palette] (an OpenCode theme) the text, headings, inline
 * code, link text, quotes, list markers and code block text take its colors like in the OpenCode
 * terminal app. [quoteText] gives the text the quote style, for the content of a quote: inline
 * code, links and (through the annotator) bold and italic text inside keep their own colors.
 */
internal fun markdownTypography(
    t: Typography,
    mono: OcMonoTypography,
    c: OcColors,
    palette: OcMarkdownPalette?,
    quoteText: Boolean = false,
    growLines: Boolean = false,
): MarkdownTypography {
    // A line with a tall formula (a fraction, a root) grows instead of being drawn over its neighbors.
    fun TextStyle.growing() = if (growLines) {
        copy(lineHeight = TextUnit.Unspecified)
    } else {
        this
    }
    val body: TextStyle = t.bodyMedium.copy(color = c.textBase).growing()
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
            table = t.bodySmall.growing(),
        )
    }
    val paragraph = body.copy(color = palette.text)
    fun TextStyle.heading() = copy(color = palette.heading, fontWeight = FontWeight.Bold)
    val quote = paragraph.copy(color = palette.blockQuote, fontStyle = FontStyle.Italic)
    val text = if (quoteText) quote else paragraph
    return DefaultMarkdownTypography(
        h1 = t.titleLarge.heading().copy(textDecoration = TextDecoration.Underline),
        h2 = t.titleMedium.heading(),
        h3 = t.titleSmall.heading(),
        h4 = t.titleSmall.heading(),
        h5 = paragraph.heading(),
        h6 = paragraph.heading(),
        text = text,
        code = mono.codeSmall.copy(color = palette.codeBlock),
        inlineCode = inlineCode.copy(color = palette.inlineCode),
        quote = quote,
        paragraph = text,
        // The list renderers draw their marker (number or bullet) with these styles and the
        // item content as paragraphs.
        ordered = paragraph.copy(color = palette.listNumber),
        bullet = paragraph.copy(color = palette.listBullet),
        list = text,
        textLink = TextLinkStyles(
            style = paragraph.copy(color = palette.linkText, textDecoration = TextDecoration.Underline).toSpanStyle(),
        ),
        table = t.bodySmall.copy(color = palette.text).growing(),
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
