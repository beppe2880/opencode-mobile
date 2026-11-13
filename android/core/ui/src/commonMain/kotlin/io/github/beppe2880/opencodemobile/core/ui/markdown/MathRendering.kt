package io.github.beppe2880.opencodemobile.core.ui.markdown

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import org.intellij.markdown.MarkdownTokenTypes

private fun mathKey(index: Int) = "math$index"

private fun latexOf(formula: MathFormula) = if (formula.display) "\\displaystyle ${formula.latex}" else formula.latex

private fun latexConfig(fontSize: TextUnit, color: Color) =
    LatexConfig(fontSize = fontSize, theme = LatexTheme.light(color = color))

/** Draws the placeholders of [formulas] in text as inline content; other text is left to [base]. */
internal fun mathAnnotator(base: MarkdownAnnotator, formulas: List<MathFormula>): MarkdownAnnotator {
    if (formulas.isEmpty()) return base
    return markdownAnnotator(base.config) { content, child ->
        if (child.type == MarkdownTokenTypes.TEXT) {
            val text = child.getUnescapedTextInNode(content)
            if (text.any { placeholderIndex(it).let { index -> index in formulas.indices } }) {
                text.forEach { c ->
                    val index = placeholderIndex(c)
                    if (index in formulas.indices) appendInlineContent(mathKey(index), formulas[index].raw) else append(c)
                }
                return@markdownAnnotator true
            }
        }
        base.annotate?.invoke(this, content, child) ?: false
    }
}

/** The typeset formulas for [mathAnnotator], measured at the size and color of [style]. */
@Composable
internal fun rememberMathInlineContent(formulas: List<MathFormula>, style: TextStyle, isDark: Boolean): Map<String, InlineTextContent> {
    if (formulas.isEmpty()) return emptyMap()
    val config = remember(style.fontSize, style.color) { latexConfig(style.fontSize, style.color) }
    val measurer = rememberLatexMeasurer(config)
    return remember(measurer, formulas, config, isDark) {
        buildMap {
            formulas.forEachIndexed { index, formula ->
                measurer.inlineContent(latexOf(formula), config, isDark)?.let { put(mathKey(index), it) }
            }
        }
    }
}

/** The display formula a paragraph consists of alone, if it is one. */
internal fun soleDisplayFormula(paragraph: String, formulas: List<MathFormula>): MathFormula? {
    val text = paragraph.trim()
    if (text.length != 1) return null
    return formulas.getOrNull(placeholderIndex(text[0]))?.takeIf { it.display }
}

/** A display formula, centered when it fits and scrolled sideways when it is wider than the screen. */
@Composable
internal fun MathBlock(formula: MathFormula, style: TextStyle, isDark: Boolean, modifier: Modifier = Modifier) {
    val config = remember(style.fontSize, style.color) { latexConfig(style.fontSize, style.color) }
    BoxWithConstraints(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        val minWidth = maxWidth
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Box(Modifier.widthIn(min = minWidth), contentAlignment = Alignment.Center) {
                Latex(latex = latexOf(formula), config = config, isDarkTheme = isDark)
            }
        }
    }
}
