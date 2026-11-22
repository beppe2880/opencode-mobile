package io.github.beppe2880.opencodemobile.feature.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToolInputFieldTest {

    @Test
    fun readsAStringFieldWithEscapes() {
        assertEquals("echo \"hi\"\n", ToolInfo.stringField("""{"command":"echo \"hi\"\n","description":"x"}""", "command"))
        assertEquals("a\\b/c\u00e9", ToolInfo.stringField("""{"path" : "a\\b\/c\u00e9"}""", "path"))
        assertEquals("x", ToolInfo.stringField("{\n  \"description\":\t\"x\"\n}", "description"))
    }

    @Test
    fun readsPartialInput() {
        assertEquals("/src/Ma", ToolInfo.stringField("""{"filePath":"/src/Ma""", "filePath"))
        // An escape cut off at the end is left out.
        assertEquals("abc", ToolInfo.stringField("""{"command":"abc\""", "command"))
        assertEquals("", ToolInfo.stringField("""{"command":"""", "command"))
        assertNull(ToolInfo.stringField("""{"command":""", "command"))
    }

    @Test
    fun skipsTheKeyWhereItIsNotAStringField() {
        // The key as a value, or with a value that is not a string: the next match counts.
        assertEquals("ls", ToolInfo.stringField("""{"description":"command","n":{"command":1},"command":"ls"}""", "command"))
        assertNull(ToolInfo.stringField("""{"commands":"ls"}""", "command"))
        assertNull(ToolInfo.stringField("{}", "command"))
    }

    @Test
    fun aVeryLongValueDoesNotOverflowTheStack() {
        // A whole file written by the agent: the regex this replaced crashed iOS on such input.
        val escaped = buildString { repeat(50_000) { append("line \\\"$it\\\"\\n") } }
        val expected = buildString { repeat(50_000) { append("line \"$it\"\n") } }
        val input = """{"filePath":"/tmp/big.txt","content":"$escaped"}"""
        assertEquals(expected, ToolInfo.stringField(input, "content"))
        assertEquals("/tmp/big.txt", ToolInfo.primaryArgument(input))
        val command = "x".repeat(500_000)
        assertEquals(command, ToolInfo.primaryArgument("""{"command":"$command"}"""))
    }
}
