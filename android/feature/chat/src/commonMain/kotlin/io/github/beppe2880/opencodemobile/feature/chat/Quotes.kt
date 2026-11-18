package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

/** Marks a quoted part of a message: `↳ "quoted text"`. */
internal const val QUOTE_MARK = "↳"

/** Longer selections are cut, so quoting a whole answer does not fill the composer. */
internal const val MAX_QUOTE_LENGTH = 1000

/**
 * Wraps the transcript so its text selection menu offers "Ask", which hands the selected text
 * to [onQuote] without touching the clipboard. Where the platform menu cannot be extended, the
 * content is shown as it is.
 */
@Composable
internal expect fun QuoteSelection(onQuote: (String) -> Unit, content: @Composable () -> Unit)

/** The quote line for [selection]: spaces and line breaks folded, cut to [MAX_QUOTE_LENGTH]. */
internal fun quoteLine(selection: String): String? {
    val text = selection.trim().replace(Regex("\\s+"), " ")
    if (text.isEmpty()) return null
    val shown = if (text.length > MAX_QUOTE_LENGTH) text.take(MAX_QUOTE_LENGTH).trimEnd() + "…" else text
    return "$QUOTE_MARK \"$shown\""
}

/**
 * [draft] with a quote of [selection] added on a new line, followed by a space for the question;
 * the draft as it is when the selection is blank.
 */
internal fun withQuote(draft: String, selection: String): String {
    val line = quoteLine(selection) ?: return draft
    val before = draft.trimEnd()
    return if (before.isEmpty()) "$line " else "$before\n$line "
}

/** The quote lines of [text]: from a line starting with `↳ "` to its closing quote mark. */
internal fun quoteRanges(text: String): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var lineStart = 0
    while (lineStart <= text.length) {
        val lineEnd = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
        val prefix = "$QUOTE_MARK \""
        if (text.startsWith(prefix, lineStart)) {
            val close = text.lastIndexOf('"', lineEnd - 1)
            if (close >= lineStart + prefix.length) ranges += lineStart..close
        }
        lineStart = lineEnd + 1
    }
    return ranges
}

/** [text] with its quote lines in [style]. */
internal fun styledQuotes(text: String, style: SpanStyle): AnnotatedString {
    val ranges = quoteRanges(text)
    if (ranges.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var at = 0
        for (range in ranges) {
            append(text.substring(at, range.first))
            withStyle(style) { append(text.substring(range.first, range.last + 1)) }
            at = range.last + 1
        }
        append(text.substring(at))
    }
}

/** Shows the composer's quote lines in [style]; the text itself is unchanged. */
internal class QuoteVisualTransformation(private val style: SpanStyle) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(styledQuotes(text.text, style), OffsetMapping.Identity)

    override fun equals(other: Any?): Boolean = other is QuoteVisualTransformation && other.style == style

    override fun hashCode(): Int = style.hashCode()
}
