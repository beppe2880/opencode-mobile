package io.github.beppe2880.opencodemobile.core.ui.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MathExtractionTest {

    private val p0 = "\uE000"
    private val p1 = "\uE001"
    private val p2 = "\uE002"

    private fun latex(source: String) = extractMath(source).formulas.map { it.latex }

    @Test
    fun `text without math is returned as is`() {
        val math = extractMath("plain **text** with `code`")
        assertEquals("plain **text** with `code`", math.text)
        assertTrue(math.formulas.isEmpty())
    }

    @Test
    fun `inline dollars become one inline formula`() {
        val math = extractMath("Energy \$E = mc^2\$ is famous")
        assertEquals("Energy $p0 is famous", math.text)
        assertEquals("E = mc^2", math.formulas.single().latex)
        assertFalse(math.formulas.single().display)
        assertEquals("\$E = mc^2\$", math.formulas.single().raw)
    }

    @Test
    fun `inline parentheses become an inline formula`() {
        val math = extractMath("The value \\(x_1 + y\\) here")
        assertEquals("The value $p0 here", math.text)
        assertEquals("x_1 + y", math.formulas.single().latex)
        assertFalse(math.formulas.single().display)
    }

    @Test
    fun `double dollars and brackets alone on their lines are display formulas`() {
        val math = extractMath("\$\$\\int_0^1 x\\,dx\$\$\n\n\\[ a + b \\]")
        assertEquals(listOf("\\int_0^1 x\\,dx", "a + b"), math.formulas.map { it.latex })
        assertTrue(math.formulas.all { it.display })
    }

    @Test
    fun `a display formula alone on its lines gets a paragraph of its own`() {
        val math = extractMath("Intro:\n\$\$\nx^2\n\$\$\nafter")
        assertEquals("Intro:\n\n$p0\nafter", math.text)
        assertEquals("x^2", math.formulas.single().latex)
    }

    @Test
    fun `a display formula inside a sentence stays in the line`() {
        val math = extractMath("so \$\$x\$\$ holds")
        assertEquals("so $p0 holds", math.text)
        assertFalse(math.formulas.single().display)
    }

    @Test
    fun `currency is not math`() {
        for (text in listOf("costs \$5 and \$10", "from \$5-\$10 or \$20", "pay \$100 today", "\$5 and \$10 and \$15")) {
            val math = extractMath(text)
            assertEquals(text, math.text, text)
            assertTrue(math.formulas.isEmpty(), text)
        }
    }

    @Test
    fun `an inline dollar needs a non blank character inside both dollars`() {
        assertTrue(latex("a \$ x \$ b").isEmpty())
        assertTrue(latex("a \$x \$ b").isEmpty())
        assertEquals(listOf("x"), latex("a \$x\$ b"))
    }

    @Test
    fun `a closing dollar followed by a digit does not close`() {
        assertTrue(latex("\$a\$5").isEmpty())
    }

    @Test
    fun `an escaped dollar is a literal`() {
        val math = extractMath("price \\\$5 and \\\$6")
        assertEquals("price \\\$5 and \\\$6", math.text)
        assertTrue(math.formulas.isEmpty())
        assertEquals(listOf("a\\\$b"), latex("\$a\\\$b\$"))
    }

    @Test
    fun `inline code and code fences are left alone`() {
        val source = "use `\$x\$` and ``\$y\$``\n```tex\n\$\$z\$\$\n\\[ w \\]\n```\n~~~\n\$q\$\n~~~\nthen \$r\$"
        val math = extractMath(source)
        assertEquals(listOf("r"), math.formulas.map { it.latex })
        assertEquals(source.replace("\$r\$", p0), math.text)
    }

    @Test
    fun `an unclosed code fence swallows the rest`() {
        assertTrue(latex("```\n\$x\$\nmore \$y\$").isEmpty())
    }

    @Test
    fun `an unclosed backtick does not hide math`() {
        assertEquals(listOf("x"), latex("a ` b \$x\$"))
    }

    @Test
    fun `unclosed formulas stay plain text while streaming`() {
        for (text in listOf("so \$x + y", "\$\$\\frac{a}{b}", "\\[ a + b", "\\(a", "ends with \$", "\$\$")) {
            val math = extractMath(text)
            assertEquals(text, math.text, text)
            assertTrue(math.formulas.isEmpty(), text)
        }
    }

    @Test
    fun `an inline formula does not cross a blank line`() {
        assertTrue(latex("a \$x\n\nb\$ c").isEmpty())
        assertTrue(latex("a \\(x\n\nb\\) c").isEmpty())
        assertEquals(listOf("x\ny"), latex("a \$x\ny\$ c"))
    }

    @Test
    fun `a double backslash does not start a formula`() {
        val math = extractMath("line\\\\(not math)")
        assertEquals("line\\\\(not math)", math.text)
        assertTrue(math.formulas.isEmpty())
    }

    @Test
    fun `row separators and environments survive inside a formula`() {
        val source = "\\begin{pmatrix} 1 & 0 \\\\ 0 & -1 \\end{pmatrix}"
        assertEquals(listOf(source), latex("\\[$source\\]"))
    }

    @Test
    fun `a formula with a pipe is one placeholder in a table cell`() {
        val math = extractMath("| a | b |\n|---|---|\n| \$|x|\$ | \$\\alpha\$ |")
        assertEquals("| a | b |\n|---|---|\n| $p0 | $p1 |", math.text)
        assertEquals(listOf("|x|", "\\alpha"), math.formulas.map { it.latex })
    }

    @Test
    fun `blank formulas are kept as written`() {
        val math = extractMath("a \$\$ \$\$ b \\( \\) c")
        assertTrue(math.formulas.isEmpty())
        assertEquals("a \$\$ \$\$ b \\( \\) c", math.text)
    }

    @Test
    fun `private use characters of the source never look like placeholders`() {
        val math = extractMath("icon \uE000 and \$x\$")
        assertEquals("icon \uFFFD and $p0", math.text)
    }

    @Test
    fun `a sole display formula paragraph is recognised`() {
        val formulas = extractMath("\$\$x\$\$\n\n\$y\$").formulas
        assertEquals("x", soleDisplayFormula(" $p0 \n", formulas)?.latex)
        assertEquals(null, soleDisplayFormula(p1, formulas))
        assertEquals(null, soleDisplayFormula("$p0$p1", formulas))
    }

    private fun nested(open: String, close: String, depth: Int, inner: String = "x") = open.repeat(depth) + inner + close.repeat(depth)

    @Test
    fun `deeply nested braces are not renderable`() {
        assertFalse(isSafeLatex(nested("{", "}", 200)))
        assertFalse(isSafeLatex(nested("\\frac{1}{", "}", 200)))
        assertFalse(isSafeLatex(nested("\\sqrt{", "}", 200)))
        assertFalse(isSafeLatex(nested("x^{", "}", 200)))
        assertTrue(isSafeLatex(nested("{", "}", MAX_FORMULA_DEPTH)))
        assertFalse(isSafeLatex(nested("{", "}", MAX_FORMULA_DEPTH + 1)))
    }

    @Test
    fun `left right and begin end count as nesting`() {
        assertFalse(isSafeLatex(nested("\\left(", "\\right)", 200)))
        assertFalse(isSafeLatex(nested("\\begin{align}", "\\end{align}", 200)))
        assertEquals(1, latexNestingDepth("\\left( x \\right)"))
        assertEquals(2, latexNestingDepth("\\begin{pmatrix} a \\end{pmatrix}"))
    }

    @Test
    fun `escaped braces are not counted`() {
        assertEquals(0, latexNestingDepth("\\{".repeat(500) + "\\}".repeat(500)))
        assertEquals(1, latexNestingDepth("\\{ {x} \\}"))
        assertTrue(isSafeLatex("\\left\\{ \\frac{a}{b} \\right\\}"))
    }

    @Test
    fun `a long chain of commands without braces is not renderable`() {
        assertFalse(isSafeLatex("\\sqrt".repeat(500) + "x"))
        assertTrue(isSafeLatex("\\alpha \\beta \\gamma = 1"))
    }

    @Test
    fun `an over long formula is not renderable`() {
        assertTrue(isSafeLatex("a+".repeat(MAX_FORMULA_LENGTH / 2)))
        assertFalse(isSafeLatex("a+".repeat(MAX_FORMULA_LENGTH)))
    }

    @Test
    fun `normal formulas are renderable`() {
        val formulas = listOf(
            "E = mc^2",
            "\\int_0^{\\infty} e^{-x^2}\\,dx = \\frac{\\sqrt{\\pi}}{2}",
            "v = \\sqrt{\\frac{2K}{m}}",
            "\\begin{pmatrix} 1 & 0 & 0 \\\\ 0 & 1 & 0 \\\\ 0 & 0 & 1 \\end{pmatrix}",
            "\\begin{aligned} \\nabla \\cdot \\vec{E} &= \\frac{\\rho}{\\varepsilon_0} \\\\ \\nabla \\times \\vec{B} &= \\mu_0 \\vec{J} + \\mu_0\\varepsilon_0 \\frac{\\partial \\vec{E}}{\\partial t} \\end{aligned}",
            "i\\hbar \\frac{\\partial}{\\partial t} \\Psi(\\vec{r}, t) = \\hat{H} \\Psi(\\vec{r}, t)",
            "\\sum_{n=1}^{\\infty} \\frac{1}{n^2} = \\frac{\\pi^2}{6}",
        )
        for (latex in formulas) assertTrue(isSafeLatex(latex), latex)
    }

    @Test
    fun `an unsafe formula keeps its placeholder and is marked not renderable`() {
        val inline = nested("{", "}", 200)
        val display = nested("\\frac{1}{", "}", 200)
        val math = extractMath("a \$$inline\$ b\n\n\\[ $display \\]\n\nc \$x\$")
        assertEquals(listOf(false, false, true), math.formulas.map { it.renderable })
        assertEquals("\$$inline\$", math.formulas[0].raw)
        assertEquals(listOf(false, true, false), math.formulas.map { it.display })
        assertEquals("a $p0 b\n\n\n$p1\n\nc $p2", math.text)
    }
}
