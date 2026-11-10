package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToolFileTest {

    @Test
    fun inlineContentIsRecognizedAndMeasured() {
        // "hello" is 5 bytes: aGVsbG8=
        val file = ToolFile("data:image/png;base64,aGVsbG8=", "image/png", "shot.png")
        assertTrue(file.isDataUri)
        assertTrue(file.hasInlinePayload)
        assertEquals(5L, file.inlineBytes)
        assertEquals(6L, ToolFile("data:image/png;base64,aGVsbG8h", "image/png", null).inlineBytes)
    }

    @Test
    fun storedCopiesHaveNoPayload() {
        val stored = ToolFile("data:image/png;base64,", "image/png", "shot.png")
        assertTrue(stored.isDataUri)
        assertFalse(stored.hasInlinePayload)
        assertNull(stored.inlineBytes)
    }

    @Test
    fun serverPathComesFromFileUrisPathsOrTheReadName() {
        assertEquals("/home/me/a b.png", ToolFile("file:///home/me/a%20b.png", "image/png", null).serverPath)
        assertEquals("/srv/x.png", ToolFile("file://host/srv/x.png", "image/png", null).serverPath)
        assertEquals("/home/me/città.png", ToolFile("file:///home/me/citt%C3%A0.png", "image/png", null).serverPath)
        assertEquals("/tmp/clip.mp4", ToolFile("/tmp/clip.mp4", "video/mp4", null).serverPath)
        assertEquals("docs/shot.png", ToolFile("data:image/png;base64,AA==", "image/png", "docs/shot.png").serverPath)
        assertNull(ToolFile("data:image/png;base64,AA==", "image/png", null).serverPath)
        assertNull(ToolFile("data:image/png;base64,AA==", "image/png", "skill://x/y.png").serverPath)
        assertNull(ToolFile("https://example.com/a.png", "image/png", "a.png").serverPath)
    }

    @Test
    fun malformedPercentEscapesAreKept() {
        assertEquals("/a%zz/100%", ToolFile("file:///a%zz/100%", "text/plain", null).serverPath)
    }

    @Test
    fun mimeParametersAreIgnored() {
        assertEquals("image/png", ToolFile("x", "Image/PNG; q=1", null).baseMime)
    }

    @Test
    fun webUrlsAreRecognized() {
        assertTrue(ToolFile("HTTPS://example.com/a.mp4", "video/mp4", null).isWebUrl)
        assertFalse(ToolFile("/a.mp4", "video/mp4", null).isWebUrl)
    }

    @Test
    fun aLiveToolSuccessAddsItsFiles() {
        val file = ToolFile("data:image/png;base64,AA==", "image/png", "a.png")
        val running = Message.Assistant(
            id = "a1",
            createdAt = 1,
            agent = "build",
            model = ModelRef("m", "p"),
            parts = listOf(Part.Tool("c1", "read", ToolStatus.Running, "{}")),
        )
        val done = TranscriptReducer.reduce(
            listOf(running),
            ServerEvent.ToolSucceeded("s1", "a1", "c1", "Image read successfully", emptyMap(), at = 5, files = listOf(file)),
        )
        val tool = (done.single() as Message.Assistant).parts.single() as Part.Tool
        assertEquals(listOf(file), tool.files)
        assertEquals(ToolStatus.Completed, tool.status)
    }
}
