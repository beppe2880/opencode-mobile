package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuotesTest {
    @Test
    fun aQuoteLineFoldsSpacesAndCutsLongSelections() {
        assertEquals("↳ \"one two three\"", quoteLine("  one\n two\t three "))
        assertNull(quoteLine("  \n "))
        val long = quoteLine("x".repeat(MAX_QUOTE_LENGTH + 50))!!
        assertTrue(long.endsWith("…\""))
        assertEquals(MAX_QUOTE_LENGTH + 5, long.length)
    }

    @Test
    fun quotesGoOnTheirOwnLineWithRoomForTheQuestion() {
        assertEquals("↳ \"first\" ", withQuote("", "first"))
        assertEquals("↳ \"first\"\n↳ \"second\" ", withQuote("↳ \"first\" ", "second"))
        assertEquals("Why?\n↳ \"part\" ", withQuote("Why?\n\n", "part"))
        assertEquals("draft", withQuote("draft", "   "))
    }

    @Test
    fun onlyQuoteLinesAreFound() {
        val text = "↳ \"a \"b\" c\" and why?\nplain \"text\"\n↳ \"second\""
        assertEquals(listOf(0..10, 34..43), quoteRanges(text))
        assertEquals(emptyList(), quoteRanges("↳ no quote\n↳ \""))
    }

    @Test
    fun quoteLinesAreStyledAndTheRestIsNot() {
        val style = SpanStyle(fontStyle = FontStyle.Italic)
        val styled = styledQuotes("↳ \"part\" why?", style)
        assertEquals("↳ \"part\" why?", styled.text)
        assertEquals(listOf(0 to 8), styled.spanStyles.map { it.start to it.end })
        assertTrue(styledQuotes("no quotes", style).spanStyles.isEmpty())
    }
}
