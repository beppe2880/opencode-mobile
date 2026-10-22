package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToolInfoTest {

    private fun tool(name: String, input: String, status: ToolStatus = ToolStatus.Completed) =
        Part.Tool(callId = "c1", name = name, status = status, input = input)

    @Test
    fun `file tools expose the file they work on`() {
        assertEquals("/app/src/Main.kt", ToolInfo.filePath(tool("read", """{"filePath":"/app/src/Main.kt","offset":1}""")))
        assertEquals("/app/a.kt", ToolInfo.filePath(tool("edit", """{"path":"/app/a.kt","oldString":"x"}""")))
        assertEquals("/app/new.kt", ToolInfo.filePath(tool("write", """{"filePath":"/app/new.kt","content":""}""", ToolStatus.Error)))
    }

    @Test
    fun `other tools and inputs still streaming have no file`() {
        assertNull(ToolInfo.filePath(tool("bash", """{"command":"cat a.kt","path":"/app"}""")))
        assertNull(ToolInfo.filePath(tool("glob", """{"pattern":"*.kt","path":"/app"}""")))
        assertNull(ToolInfo.filePath(tool("read", """{"filePath":"/app/sr""", ToolStatus.Streaming)))
    }
}
