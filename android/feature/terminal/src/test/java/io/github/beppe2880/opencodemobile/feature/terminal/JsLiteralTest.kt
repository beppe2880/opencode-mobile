package io.github.beppe2880.opencodemobile.feature.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class JsLiteralTest {

    @Test
    fun `terminal output becomes a JavaScript string literal that cannot break out of the call`() {
        assertEquals("\"plain\"", js("plain"))
        assertEquals("\"a\\\"b\\\\c\"", js("a\"b\\c"))
        assertEquals("\"\\u001b[31mred\\r\\n\"", js("\u001b[31mred\r\n"))
        assertEquals("\"\\u2028\\u2029\\u007f\\u0000\"", js("\u2028\u2029\u007f\u0000"))
        assertEquals("\"</script>é😀\"", js("</script>é😀"))
    }
}
