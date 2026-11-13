package io.github.beppe2880.opencodemobile.core.ui.markdown

/** A [display] formula is drawn as a block of its own; [raw] is the formula as written, delimiters included. */
internal class MathFormula(val latex: String, val display: Boolean, val raw: String)

/**
 * [text] is the markdown with every formula replaced by one placeholder character (see
 * [placeholderIndex]), so the markdown parser never sees LaTeX; [formulas] are indexed by it.
 */
internal class MathContent(val text: String, val formulas: List<MathFormula>)

private const val PLACEHOLDER_BASE = 0xE000
private const val MAX_FORMULAS = 0x1800
private const val PRIVATE_USE_END = 0xF8FF
private const val MAX_INLINE_LENGTH = 1000

/** The formula index a placeholder character stands for, or -1 for any other character. */
internal fun placeholderIndex(c: Char): Int {
    val index = c.code - PLACEHOLDER_BASE
    return if (index in 0 until MAX_FORMULAS) index else -1
}

/**
 * Finds the formulas of [source]: `$...$`, `$$...$$`, `\(...\)` and `\[...\]`. Code spans and
 * fenced code are left alone, `\$` is a literal dollar, and a formula that is not closed (a reply
 * that is still streaming) stays plain text. An inline `$` pair needs a non-blank character next to
 * both dollars and no digit after the closing one, so prices like `$5 and $10` are not math.
 * A display formula alone on its lines becomes its own paragraph.
 */
internal fun extractMath(source: String): MathContent {
    if ('$' !in source && "\\(" !in source && "\\[" !in source) return MathContent(source, emptyList())
    val out = StringBuilder(source.length)
    val formulas = ArrayList<MathFormula>()
    var i = 0
    while (i < source.length) {
        if ((i == 0 || source[i - 1] == '\n') && fenceOf(source, i) != null) {
            val end = fenceEnd(source, i)
            out.append(source, i, end)
            i = end
            continue
        }
        val c = source[i]
        when {
            c == '`' -> {
                val end = codeSpanEnd(source, i)
                out.append(source, i, end)
                i = end
            }
            c == '\\' && i + 1 < source.length -> {
                val next = source[i + 1]
                val close = when (next) {
                    '(' -> "\\)"
                    '[' -> "\\]"
                    else -> null
                }
                val end = if (close != null && formulas.size < MAX_FORMULAS) {
                    findClose(source, i + 2, close, allowBlankLine = next == '[')
                } else {
                    -1
                }
                if (end >= 0) {
                    i = emit(source, out, formulas, i, i + 2, end, end + 2, display = next == '[')
                } else {
                    out.append(c).append(next)
                    i += 2
                }
            }
            c == '$' && formulas.size < MAX_FORMULAS -> i = dollar(source, out, formulas, i)
            else -> {
                out.append(if (c.code in PLACEHOLDER_BASE..PRIVATE_USE_END) '\uFFFD' else c)
                i++
            }
        }
    }
    return MathContent(out.toString(), formulas)
}

private fun dollar(source: String, out: StringBuilder, formulas: MutableList<MathFormula>, i: Int): Int {
    if (source.startsWith("$$", i)) {
        val end = findClose(source, i + 2, "$$", allowBlankLine = true)
        return if (end >= 0) {
            emit(source, out, formulas, i, i + 2, end, end + 2, display = true)
        } else {
            out.append("$$")
            i + 2
        }
    }
    val end = inlineDollarEnd(source, i)
    return if (end >= 0) {
        emit(source, out, formulas, i, i + 1, end, end + 1, display = false)
    } else {
        out.append('$')
        i + 1
    }
}

private fun inlineDollarEnd(source: String, start: Int): Int {
    if (start + 1 >= source.length || source[start + 1].isWhitespace()) return -1
    var j = start + 1
    val limit = minOf(source.length, start + MAX_INLINE_LENGTH)
    while (j < limit) {
        when (source[j]) {
            '\\' -> j++
            '\n' -> if (isBlankLineAt(source, j + 1)) return -1
            '$' -> if (!source[j - 1].isWhitespace() && source.getOrNull(j + 1)?.isDigit() != true) return j
        }
        j++
    }
    return -1
}

/** The index of [close] after [from], skipping escaped characters, or -1 when there is none. */
private fun findClose(source: String, from: Int, close: String, allowBlankLine: Boolean): Int {
    var j = from
    while (j < source.length) {
        if (source.startsWith(close, j)) return j
        when (source[j]) {
            '\\' -> j++
            '\n' -> if (!allowBlankLine && isBlankLineAt(source, j + 1)) return -1
        }
        j++
    }
    return -1
}

private fun isBlankLineAt(source: String, lineStart: Int): Boolean {
    var k = lineStart
    while (k < source.length && source[k] != '\n') {
        if (!source[k].isWhitespace()) return false
        k++
    }
    return true
}

private fun emit(
    source: String,
    out: StringBuilder,
    formulas: MutableList<MathFormula>,
    start: Int,
    contentStart: Int,
    contentEnd: Int,
    end: Int,
    display: Boolean,
): Int {
    val latex = source.substring(contentStart, contentEnd).trim()
    if (latex.isEmpty()) {
        out.append(source, start, end)
        return end
    }
    val placeholder = (PLACEHOLDER_BASE + formulas.size).toChar()
    val raw = source.substring(start, end)
    if (display) {
        val lineStart = source.lastIndexOf('\n', start - 1) + 1
        var lineEnd = source.indexOf('\n', end)
        if (lineEnd < 0) lineEnd = source.length
        if (source.substring(lineStart, start).isBlank() && source.substring(end, lineEnd).isBlank()) {
            // Alone on its lines: a paragraph of its own, so it is drawn as a block.
            val indent = source.substring(lineStart, start)
            out.setLength(out.length - indent.length)
            out.append('\n').append(indent).append(placeholder)
            formulas += MathFormula(latex, display = true, raw)
            return lineEnd
        }
    }
    formulas += MathFormula(latex, display = false, raw)
    out.append(placeholder)
    return end
}

/** The end of the code span that starts at [start], or just past its backticks when it is not closed. */
private fun codeSpanEnd(source: String, start: Int): Int {
    var runEnd = start
    while (runEnd < source.length && source[runEnd] == '`') runEnd++
    val length = runEnd - start
    var j = runEnd
    while (j < source.length) {
        if (source[j] != '`') {
            j++
            continue
        }
        var k = j
        while (k < source.length && source[k] == '`') k++
        if (k - j == length) return k
        j = k
    }
    return runEnd
}

private class Fence(val char: Char, val length: Int)

private fun fenceOf(source: String, lineStart: Int): Fence? {
    var k = lineStart
    while (k < source.length && (source[k] == ' ' || source[k] == '\t' || source[k] == '>')) k++
    val c = source.getOrNull(k)
    if (c != '`' && c != '~') return null
    var n = 0
    while (source.getOrNull(k + n) == c) n++
    return if (n >= 3) Fence(c, n) else null
}

/** The index after the fenced block opening at [lineStart]; an unclosed fence runs to the end. */
private fun fenceEnd(source: String, lineStart: Int): Int {
    val fence = fenceOf(source, lineStart)!!
    var lineEnd = source.indexOf('\n', lineStart)
    while (lineEnd >= 0) {
        val next = lineEnd + 1
        val closing = fenceOf(source, next)
        if (closing != null && closing.char == fence.char && closing.length >= fence.length) {
            val closeEnd = source.indexOf('\n', next)
            return if (closeEnd < 0) source.length else closeEnd
        }
        lineEnd = source.indexOf('\n', next)
    }
    return source.length
}
