package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncedTextTest {

    private val typed = mutableListOf<String>()

    private fun SyncedText.type(text: String) = input(TextFieldValue(text, TextRange(text.length))) { typed += it }

    @Test
    fun `the cursor stays after the typed text when the caller catches up late`() {
        val text = SyncedText("")
        text.type("a")
        // Recomposition before the caller's state arrives (iOS sheets).
        assertEquals(TextFieldValue("a", TextRange(1)), text.sync(""))
        assertEquals(TextFieldValue("a", TextRange(1)), text.sync("a"))
    }

    @Test
    fun `fast keystrokes survive late echoes of earlier ones`() {
        val text = SyncedText("")
        text.type("g")
        text.sync("")
        text.type("gr")
        text.sync("")
        text.type("gro")
        assertEquals(TextFieldValue("gro", TextRange(3)), text.sync("g"))
        assertEquals(TextFieldValue("gro", TextRange(3)), text.sync("gr"))
        text.type("groq")
        assertEquals(TextFieldValue("groq", TextRange(4)), text.sync("gro"))
        assertEquals(TextFieldValue("groq", TextRange(4)), text.sync("groq"))
        assertEquals(listOf("g", "gr", "gro", "groq"), typed)
    }

    @Test
    fun `an external change replaces the text with the cursor at the end`() {
        val text = SyncedText("")
        text.type("groq")
        text.sync("groq")
        assertEquals(TextFieldValue("", TextRange(0)), text.sync(""))
        assertEquals(TextFieldValue("/home/me", TextRange(8)), text.sync("/home/me"))
    }

    @Test
    fun `rejected input settles on the caller's value`() {
        val text = SyncedText("abc")
        text.type("abcd")
        assertEquals("abcd", text.sync("abc").text)
        text.settle("abc")
        assertEquals(TextFieldValue("abc", TextRange(3)), text.sync("abc"))
    }

    @Test
    fun `settling after the caller caught up changes nothing`() {
        val text = SyncedText("")
        text.type("a")
        text.sync("a")
        text.input(TextFieldValue("a", TextRange(0))) { typed += it }
        text.settle("a")
        assertEquals(TextFieldValue("a", TextRange(0)), text.sync("a"))
    }

    @Test
    fun `moving the cursor does not report a change`() {
        val text = SyncedText("abc")
        text.input(TextFieldValue("abc", TextRange(1))) { typed += it }
        assertEquals(emptyList<String>(), typed)
        assertEquals(TextRange(1), text.sync("abc").selection)
    }
}
