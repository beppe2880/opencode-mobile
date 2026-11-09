package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.SessionMessageInfo
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ToolFile
import io.github.beppe2880.opencodemobile.core.network.Fixtures
import io.github.beppe2880.opencodemobile.core.network.MessageCodec
import io.github.beppe2880.opencodemobile.core.network.decodeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolFileMappingTest {

    private fun tool(content: String): Part.Tool {
        val json = """{"id":"msg_1","time":{"created":5,"completed":9},"type":"assistant","agent":"build",
            "model":{"id":"m","providerID":"p"},
            "content":[{"type":"tool","id":"call_1","name":"read","time":{"created":6,"completed":8},
              "state":{"status":"completed","input":{"path":"shot.png"},"content":$content}}]}"""
        val assistant = Fixtures.json.decodeFromString<SessionMessageInfo>(json).toDomain() as Message.Assistant
        return assistant.parts.single() as Part.Tool
    }

    @Test
    fun `the read tool's image keeps its data URI next to the text`() {
        // What the read tool returns (packages/core/src/tool/plugin/read.ts, toModelContent).
        val read = tool(
            """[{"type":"text","text":"Image read successfully"},
                {"type":"file","uri":"data:image/png;base64,iVBORw0KGgo=","mime":"image/png","name":"docs/shot.png"}]""",
        )

        assertEquals("Image read successfully", read.output)
        assertEquals(listOf(ToolFile("data:image/png;base64,iVBORw0KGgo=", "image/png", "docs/shot.png")), read.files)
    }

    @Test
    fun `file URIs, paths and nameless files are kept in order`() {
        val read = tool(
            """[{"type":"file","uri":"file:///home/me/a%20b.pdf","mime":"application/pdf"},
                {"type":"text","text":"two"},
                {"type":"file","uri":"/tmp/clip.mp4","mime":"video/mp4","name":"clip.mp4"}]""",
        )

        assertEquals("two", read.output)
        assertEquals(
            listOf(ToolFile("file:///home/me/a%20b.pdf", "application/pdf", null), ToolFile("/tmp/clip.mp4", "video/mp4", "clip.mp4")),
            read.files,
        )
    }

    @Test
    fun `a text-only result has no files`() {
        assertTrue(tool("""[{"type":"text","text":"1: hello"}]""").files.isEmpty())
    }

    @Test
    fun `a stored copy keeps the media type without the payload`() {
        val payload = "A".repeat(MessageCodec.MAX_STORED_STRING + 4)
        val json = """{"id":"msg_1","time":{"created":5,"completed":9},"type":"assistant","agent":"build",
            "model":{"id":"m","providerID":"p"},
            "content":[{"type":"tool","id":"call_1","name":"read","time":{"created":6,"completed":8},
              "state":{"status":"completed","input":{},"content":[
                {"type":"file","uri":"data:image/png;base64,$payload","mime":"image/png","name":"shot.png"}]}}]}"""
        val stored = MessageCodec.decode(MessageCodec.encode(Fixtures.json.decodeFromString<SessionMessageInfo>(json)).json)!!
        val file = ((stored.toDomain() as Message.Assistant).parts.single() as Part.Tool).files.single()

        assertEquals("data:image/png;base64,", file.uri)
        assertTrue(file.isDataUri)
        assertFalse(file.hasInlinePayload)
    }

    @Test
    fun `a live tool success carries its files`() {
        val payload = """{"id":"evt_1","created":7,"type":"session.tool.success",
            "durable":{"aggregateID":"ses_1","seq":3,"version":2},
            "data":{"sessionID":"ses_1","assistantMessageID":"msg_1","id":"call_1","executed":true,"content":[
              {"type":"text","text":"Image read successfully"},
              {"type":"file","uri":"data:image/jpeg;base64,/9j/","mime":"image/jpeg","name":"/repo/a.jpg"}]}}"""

        val event = decodeEvent(payload)?.toDomain() as ServerEvent.ToolSucceeded

        assertEquals("Image read successfully", event.output)
        assertEquals(listOf(ToolFile("data:image/jpeg;base64,/9j/", "image/jpeg", "/repo/a.jpg")), event.files)
    }
}
