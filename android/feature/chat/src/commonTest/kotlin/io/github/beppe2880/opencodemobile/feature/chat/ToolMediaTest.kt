package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.ToolFile
import io.github.beppe2880.opencodemobile.core.domain.ToolMediaSize
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToolMediaTest {

    private fun dataUri(mime: String, bytes: ByteArray) = "data:$mime;base64," + Base64.encode(bytes)

    private fun png(width: Int, height: Int): ByteArray {
        val head = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)
        val ihdr = byteArrayOf(0, 0, 0, 13) + "IHDR".encodeToByteArray()
        return head + ihdr + be32(width) + be32(height) + byteArrayOf(8, 6, 0, 0, 0)
    }

    private fun be32(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    @Test
    fun mediaKindFollowsTheMediaType() {
        assertEquals(MediaKind.Image, mediaKind(ToolFile("data:image/png;base64,AA==", "image/png", null)))
        assertEquals(MediaKind.Image, mediaKind(ToolFile("/a.jpg", "IMAGE/JPEG; x=1", null)))
        assertEquals(MediaKind.Video, mediaKind(ToolFile("/a.mp4", "video/mp4", null)))
        assertEquals(MediaKind.File, mediaKind(ToolFile("data:application/pdf;base64,AA==", "application/pdf", "a.pdf")))
        // Not decodable here, or not ours to load.
        assertEquals(MediaKind.File, mediaKind(ToolFile("/a.svg", "image/svg+xml", null)))
        assertEquals(MediaKind.File, mediaKind(ToolFile("https://x.dev/a.png", "image/png", null)))
    }

    @Test
    fun onlyImagesWithoutContentButWithASourceAreFetched() {
        assertTrue(ToolFile("data:image/png;base64,", "image/png", "a.png").needsFetch())
        assertTrue(ToolFile("file:///a.png", "image/png", null).needsFetch())
        assertFalse(ToolFile("data:image/png;base64,AA==", "image/png", null).needsFetch())
        assertFalse(ToolFile("data:application/pdf;base64,", "application/pdf", "a.pdf").needsFetch())
        assertFalse(ToolFile("https://x.dev/a.png", "image/png", null).needsFetch())
    }

    @Test
    fun displayNameIsTheLastSegmentOrTheType() {
        assertEquals("shot.png", ToolFile("data:image/png;base64,AA==", "image/png", "docs/shot.png").displayName)
        assertEquals("a b.png", ToolFile("file:///x/a%20b.png", "image/png", null).displayName)
        assertEquals("image/png", ToolFile("data:image/png;base64,AA==", "image/png", null).displayName)
    }

    @Test
    fun openTargets() {
        assertEquals(MediaOpen.Viewer, openTarget(ToolFile("data:image/png;base64,AA==", "image/png", "a.png"), showsImage = true))
        assertEquals(MediaOpen.ServerFile("docs/a.pdf"), openTarget(ToolFile("data:application/pdf;base64,AA==", "application/pdf", "docs/a.pdf"), false))
        assertEquals(MediaOpen.ServerFile("/tmp/c.mp4"), openTarget(ToolFile("file:///tmp/c.mp4", "video/mp4", null), false))
        assertEquals(MediaOpen.Web("https://x.dev/v.mp4"), openTarget(ToolFile("https://x.dev/v.mp4", "video/mp4", null), false))
        assertNull(openTarget(ToolFile("data:application/pdf;base64,AA==", "application/pdf", null), false))
    }

    @Test
    fun dimensionsComeFromImageHeaders() {
        assertEquals(1080 to 2400, imageDimensions(png(1080, 2400)))
        val gif = "GIF89a".encodeToByteArray() + byteArrayOf(0x40, 0x01, 0xF0.toByte(), 0x00)
        assertEquals(320 to 240, imageDimensions(gif))
        // SOI, an APP0 segment of 16 bytes, then SOF0 with height 600 and width 800.
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10) + ByteArray(14) +
            byteArrayOf(0xFF.toByte(), 0xC0.toByte(), 0x00, 0x11, 0x08, 0x02, 0x58, 0x03, 0x20, 0x03)
        assertEquals(800 to 600, imageDimensions(jpeg))
        val vp8x = "RIFF".encodeToByteArray() + ByteArray(4) + "WEBPVP8X".encodeToByteArray() + ByteArray(8) +
            byteArrayOf(0x1F, 0x03, 0x00, 0xDF.toByte(), 0x01, 0x00)
        assertEquals(800 to 480, imageDimensions(vp8x))
        assertNull(imageDimensions("not an image at all, really".encodeToByteArray()))
        assertNull(imageDimensions(png(1, 1).copyOf(20)))
    }

    @Test
    fun aspectIsReadFromTheStartOfTheDataUri() {
        val file = ToolFile(dataUri("image/png", png(1080, 2400) + ByteArray(200_000)), "image/png", "shot.png")
        assertEquals(1080f / 2400f, inlineAspect(file))
        assertNull(inlineAspect(ToolFile("data:image/png;base64,", "image/png", null)))
        assertNull(inlineAspect(ToolFile("data:image/png;base64,***", "image/png", null)))
    }

    @Test
    fun payloadsOverTheLimitAreNotDecoded() {
        val file = ToolFile(dataUri("image/png", ByteArray(12) { it.toByte() }), "image/png", null)
        assertEquals(12, dataUriBytes(file)?.size)
        assertNull(dataUriBytes(file, maxBytes = 11))
        assertNull(dataUriBytes(ToolFile("data:image/png;base64,***=", "image/png", null)))
        assertNull(dataUriBytes(ToolFile("data:text/plain,hello", "text/plain", null)))
        assertTrue(file.isDecodable())
        assertFalse(ToolFile("data:image/png;base64,", "image/png", null).isDecodable())
    }

    private fun assertSize(width: Float, height: Float, actual: Pair<Float, Float>) {
        assertEquals(width, actual.first, 0.01f)
        assertEquals(height, actual.second, 0.01f)
    }

    @Test
    fun singleCardsKeepTheAspectWithinTheBounds() {
        // Landscape: 70 % of the column.
        assertSize(280f, 140f, singleCardSize(available = 400f, aspect = 2f, maxHeight = 360f, minHeight = 48f))
        // A tall screenshot is capped in height and gets narrower.
        assertSize(180f, 360f, singleCardSize(400f, 0.5f, 360f, 48f))
        // A banner keeps a minimum height.
        assertSize(280f, 48f, singleCardSize(400f, 20f, 360f, 48f))
        // Unknown size: the default shape.
        assertSize(280f, 210f, singleCardSize(400f, null, 360f, 48f))
        assertSize(280f, 210f, singleCardSize(400f, Float.NaN, 360f, 48f))
    }

    @Test
    fun singleCardsFollowTheSizeSetting() {
        // A tall screenshot (1:2) in a 400 dp column.
        assertNull(singleCardSize(400f, 0.5f, ToolMediaSize.Hidden))
        assertSize(110f, 220f, singleCardSize(400f, 0.5f, ToolMediaSize.Small)!!)
        assertSize(180f, 360f, singleCardSize(400f, 0.5f, ToolMediaSize.Medium)!!)
        assertSize(260f, 520f, singleCardSize(400f, 0.5f, ToolMediaSize.Large)!!)
        // A landscape image (2:1) is limited by the width: 45 %, 70 % and all of the column.
        assertSize(180f, 90f, singleCardSize(400f, 2f, ToolMediaSize.Small)!!)
        assertSize(280f, 140f, singleCardSize(400f, 2f, ToolMediaSize.Medium)!!)
        assertSize(400f, 200f, singleCardSize(400f, 2f, ToolMediaSize.Large)!!)
    }

    @Test
    fun severalImagesShareTheRowHeightOfTheSetting() {
        assertNull(stripCardSize(1.5f, ToolMediaSize.Hidden))
        assertSize(165f, 110f, stripCardSize(1.5f, ToolMediaSize.Small)!!)
        assertSize(240f, 160f, stripCardSize(1.5f, ToolMediaSize.Medium)!!)
        assertSize(330f, 220f, stripCardSize(1.5f, ToolMediaSize.Large)!!)
        // Widths stay between 0.6 and 1.75 times the height: Medium keeps 96..280 dp.
        assertSize(96f, 160f, stripCardSize(0.2f, ToolMediaSize.Medium)!!)
        assertSize(280f, 160f, stripCardSize(4f, ToolMediaSize.Medium)!!)
        assertSize(385f, 220f, stripCardSize(4f, ToolMediaSize.Large)!!)
    }

    @Test
    fun hiddenImagesStillOpenTheViewer() {
        val inline = ToolFile("data:image/png;base64,AA==", "image/png", "a.png")
        val stored = ToolFile("data:image/png;base64,", "image/png", "a.png")
        assertEquals(MediaOpen.Viewer, chipTarget(inline, hidden = true))
        assertEquals(MediaOpen.Viewer, chipTarget(stored, hidden = true))
        // Not hidden: the chip of an image that could not be drawn opens the file instead.
        assertEquals(MediaOpen.ServerFile("a.png"), chipTarget(inline, hidden = false))
        val pdf = ToolFile("data:application/pdf;base64,AA==", "application/pdf", "b.pdf")
        assertEquals(MediaOpen.ServerFile("b.pdf"), chipTarget(pdf, hidden = true))
        assertNull(chipTarget(ToolFile("data:image/png;base64,", "image/png", null), hidden = false))
    }

    @Test
    fun stripCardsShareOneHeight() {
        assertEquals(240f, stripCardWidth(1.5f, height = 160f, minWidth = 96f, maxWidth = 280f))
        assertEquals(96f, stripCardWidth(0.2f, 160f, 96f, 280f))
        assertEquals(280f, stripCardWidth(4f, 160f, 96f, 280f))
    }

    @Test
    fun toolGroupsCarryTheirMediaInOrder() {
        val shot = ToolFile("data:image/png;base64,AA==", "image/png", "a.png")
        val pdf = ToolFile("data:application/pdf;base64,AA==", "application/pdf", "b.pdf")
        val messages = listOf(
            Message.User("u1", 1, "look"),
            Message.Assistant(
                id = "a1",
                createdAt = 2,
                agent = "build",
                model = ModelRef("m", "p"),
                completedAt = 5,
                parts = listOf(
                    Part.Tool("c1", "read", ToolStatus.Completed, "{}", files = listOf(shot)),
                    Part.Tool("c2", "read", ToolStatus.Completed, "{}", files = listOf(pdf, shot)),
                    Part.Text(0, "Done"),
                ),
            ),
        )
        val items = ChatItemsBuilder.build(messages, running = false, showReasoning = true, modelLabel = { ModelLabel(it.id, it.providerId) })
        val tools = items.filterIsInstance<ChatItem.Tools>().single()

        assertEquals(
            listOf(ToolMedia("a1", "c1", 0, shot), ToolMedia("a1", "c2", 0, pdf), ToolMedia("a1", "c2", 1, shot)),
            tools.media,
        )
        assertEquals("c2_1", tools.media.last().tagId)
        // The media stay with the group, before the text that follows.
        assertTrue(items.indexOf(tools) < items.indexOfFirst { it is ChatItem.Text })

        // Thinking before the tools: with activity grouping the group shows the same media.
        val thinking = messages.map { m ->
            if (m is Message.Assistant) m.copy(parts = listOf(Part.Reasoning(0, "Plan", startedAt = 2, completedAt = 3)) + m.parts) else m
        }
        val grouped = ChatItemsBuilder.build(thinking, false, true, { ModelLabel(it.id, it.providerId) }, groupActivity = true)
        assertEquals(tools.media, grouped.filterIsInstance<ChatItem.Activity>().single().media)
    }
}
