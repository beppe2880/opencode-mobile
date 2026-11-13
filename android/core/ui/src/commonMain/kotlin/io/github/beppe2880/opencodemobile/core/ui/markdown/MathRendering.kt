package io.github.beppe2880.opencodemobile.core.ui.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import org.intellij.markdown.MarkdownTokenTypes
import kotlin.coroutines.cancellation.CancellationException

private fun mathKey(index: Int) = "math$index"

private fun latexOf(formula: MathFormula) = if (formula.display) "\\displaystyle ${formula.latex}" else formula.latex

private fun latexConfig(fontSize: TextUnit, color: Color) =
    LatexConfig(fontSize = fontSize, theme = LatexTheme.light(color = color))

/** Draws the placeholders of [formulas] in text as inline content; other text is left to [base]. */
internal fun mathAnnotator(base: MarkdownAnnotator, formulas: List<MathFormula>, sourceStyle: SpanStyle): MarkdownAnnotator {
    if (formulas.isEmpty()) return base
    return markdownAnnotator(base.config) { content, child ->
        if (child.type == MarkdownTokenTypes.TEXT) {
            val text = child.getUnescapedTextInNode(content)
            if (text.any { placeholderIndex(it).let { index -> index in formulas.indices } }) {
                text.forEach { c ->
                    val index = placeholderIndex(c)
                    val formula = formulas.getOrNull(index)
                    when {
                        formula == null -> append(c)
                        formula.renderable -> appendInlineContent(mathKey(index), formula.raw)
                        else -> withStyle(sourceStyle) { append(formula.raw) }
                    }
                }
                return@markdownAnnotator true
            }
        }
        base.annotate?.invoke(this, content, child) ?: false
    }
}

/**
 * The typeset formulas for [mathAnnotator], measured at the size and color of [style]. A formula
 * that is not [MathFormula.renderable] or that the library fails on is left out: its source is shown.
 */
@Composable
internal fun rememberMathInlineContent(formulas: List<MathFormula>, style: TextStyle, isDark: Boolean): Map<String, InlineTextContent> {
    if (formulas.isEmpty()) return emptyMap()
    val config = remember(style.fontSize, style.color) { latexConfig(style.fontSize, style.color) }
    val measurer = rememberLatexMeasurer(config)
    return remember(measurer, formulas, config, isDark) {
        buildMap {
            formulas.forEachIndexed { index, formula ->
                typesetOrNull(measurer, formula, config, isDark)?.let { put(mathKey(index), it) }
            }
        }
    }
}

/** The typeset [formula] as inline content, or null when it is not renderable or the library fails on it. */
private fun typesetOrNull(measurer: LatexMeasurerState, formula: MathFormula, config: LatexConfig, isDark: Boolean): InlineTextContent? {
    if (!formula.renderable) return null
    return try {
        measurer.inlineContent(latexOf(formula), config, isDark)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        // Only a last resort: a StackOverflowError while composing is not catchable, the
        // nesting limit of isSafeLatex is what prevents it.
        null
    }
}

/** The display formula a paragraph consists of alone, if it is one. */
internal fun soleDisplayFormula(paragraph: String, formulas: List<MathFormula>): MathFormula? {
    val text = paragraph.trim()
    if (text.length != 1) return null
    return formulas.getOrNull(placeholderIndex(text[0]))?.takeIf { it.display }
}

/** The typeset [formula] of [formulas] from [inlineContent] (see [rememberMathInlineContent]), if any. */
internal fun typesetOf(formula: MathFormula, formulas: List<MathFormula>, inlineContent: Map<String, InlineTextContent>): InlineTextContent? =
    inlineContent[mathKey(formulas.indexOf(formula))]

private const val BLOCK_KEY = "mathBlock"

/**
 * A display formula, centered when it fits and scrolled sideways when it is wider than the screen.
 * A formula that is not renderable, or that the library cannot measure, is shown as its source.
 *
 * The typeset formula is covered by a transparent text of the same size whose only content is a
 * placeholder with the formula's source as alternate text: a text selection across the message
 * then includes the formula and copies its source. The formula itself cannot be that placeholder's
 * content: inside a display block the library's inline content is laid out but not drawn.
 */
@Composable
internal fun MathBlock(formula: MathFormula, typeset: InlineTextContent?, style: TextStyle, sourceStyle: TextStyle, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.horizontalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
            if (typeset == null) {
                Text(text = formula.raw, style = sourceStyle, softWrap = false)
            } else {
                typeset.children(formula.raw)
                Text(
                    text = buildAnnotatedString { appendInlineContent(BLOCK_KEY, formula.raw) },
                    inlineContent = mapOf(BLOCK_KEY to InlineTextContent(typeset.placeholder) {}),
                    modifier = Modifier.matchParentSize(),
                    style = style.copy(lineHeight = TextUnit.Unspecified),
                    softWrap = false,
                )
            }
        }
    }
}
