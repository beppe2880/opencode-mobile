package io.github.beppe2880.opencodemobile.core.network.mapping

import io.github.beppe2880.opencodemobile.api.models.SessionMessageInfo
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.NoticeKind
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import io.github.beppe2880.opencodemobile.core.network.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageMappingTest {

    private fun map(json: String): Message? =
        Fixtures.json.decodeFromString<SessionMessageInfo>(json).toDomain()

    @Test
    fun `files mentioned by path are listed apart from images and uploads`() {
        val user = map(
            """{"id":"msg_u","time":{"created":1},"type":"user","text":"look at @src/a b.kt","files":[
                 {"data":"","mime":"text/plain","name":"src/a b.kt","source":{"type":"uri","uri":"file:///home/me/app/src/a%20b.kt"}},
                 {"data":"","mime":"text/plain","name":"raw.kt","source":{"type":"uri","uri":"file:///home/me/app/raw.kt"}},
                 {"data":"","mime":"image/png","name":"shot.png","source":{"type":"uri","uri":"file:///home/me/shot.png"}},
                 {"data":"aGk=","mime":"text/plain","name":"notes.txt","source":{"type":"inline"}}
               ]}""",
        ) as Message.User

        assertEquals(listOf("/home/me/app/src/a b.kt", "/home/me/app/raw.kt"), user.files)
        assertEquals(4, user.attachments)
        assertEquals(1, user.images.size)
    }

    @Test
    fun `idle markers keep outcome and error as turn boundaries`() {
        val failed = map(
            """{"id":"msg_1","time":{"created":5},"type":"idle","outcome":"failed",
               "error":{"type":"provider","message":"Rate limited"}}""",
        )
        assertEquals(Message.Idle("msg_1", 5, SessionOutcome.Failed, "Rate limited"), failed)
        assertEquals(
            Message.Idle("msg_2", 6, SessionOutcome.Succeeded),
            map("""{"id":"msg_2","time":{"created":6},"type":"idle","outcome":"succeeded"}"""),
        )
    }

    @Test
    fun `synthetic entries keep only their position and system entries are dropped`() {
        assertEquals(Message.Synthetic("msg_2", 5), map("""{"id":"msg_2","time":{"created":5},"type":"synthetic","text":"ctx"}"""))
        assertNull(map("""{"id":"msg_3","time":{"created":5},"type":"system","text":"instructions"}"""))
    }

    @Test
    fun `model switch notice carries the model key`() {
        val notice = map(
            """{"id":"msg_1","time":{"created":5},"type":"model-switched",
               "model":{"id":"claude-sonnet-5","providerID":"anthropic"}}""",
        )
        assertEquals(Message.Notice("msg_1", 5, NoticeKind.ModelSwitched, "anthropic/claude-sonnet-5"), notice)
    }

    @Test
    fun `shell message keeps output and exit code`() {
        val shell = map(
            """{"id":"msg_1","time":{"created":5},"type":"shell","shellID":"sh_1","command":"ls",
               "status":"exited","exit":0,"output":{"output":"a\nb","cursor":3,"size":3,"truncated":false}}""",
        )
        assertEquals(Message.Shell("msg_1", 5, "ls", "exited", "a\nb", "0"), shell)
    }

    @Test
    fun `streaming assistant marks open parts and failed tools`() {
        val assistant = map(
            """{"id":"msg_1","time":{"created":5},"type":"assistant","agent":"build",
               "model":{"id":"m","providerID":"p"},
               "content":[
                 {"type":"text","text":"Hel"},
                 {"type":"tool","id":"call_1","name":"edit","time":{"created":6},
                  "state":{"status":"error","input":{"file":"x"},"error":{"type":"tool","message":"denied"}}},
                 {"type":"tool","id":"call_2","name":"read","time":{"created":7},
                  "state":{"status":"streaming","input":"{\"pa"}}
               ]}""",
        ) as Message.Assistant

        assertTrue(assistant.isStreaming)
        assertEquals(Part.Text(0, "Hel", streaming = true), assistant.parts[0])
        val failed = assistant.parts[1] as Part.Tool
        assertEquals(ToolStatus.Error, failed.status)
        assertEquals("denied", failed.error)
        val streaming = assistant.parts[2] as Part.Tool
        assertEquals(ToolStatus.Streaming, streaming.status)
        assertEquals("{\"pa", streaming.input)
    }
}
