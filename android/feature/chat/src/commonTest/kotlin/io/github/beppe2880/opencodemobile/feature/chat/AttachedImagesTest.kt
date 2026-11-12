package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.AttachedImageSize
import io.github.beppe2880.opencodemobile.core.domain.ToolMediaSize
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttachedImagesTest {

    /** The sizes that draw the image in its own shape, with the tool image size they copy. */
    private val cards = listOf(
        AttachedImageSize.Small to ToolMediaSize.Small,
        AttachedImageSize.Medium to ToolMediaSize.Medium,
        AttachedImageSize.Large to ToolMediaSize.Large,
    )

    private val aspects = listOf(null, 0.2f, 0.5f, 1f, 1.5f, 2f, 4f, 12f)

    private fun assertSize(width: Float, height: Float, actual: Pair<Float, Float>) {
        assertEquals(width, actual.first, 0.01f, "width of $actual")
        assertEquals(height, actual.second, 0.01f, "height of $actual")
    }

    @Test
    fun everySizeButThumbnailsCopiesAToolImageSize() {
        assertNull(AttachedImageSize.Thumbnails.toolSize)
        assertEquals(cards, AttachedImageSize.entries.mapNotNull { size -> size.toolSize?.let { size to it } })
        // No attached size hides the images.
        assertTrue(AttachedImageSize.entries.none { it.toolSize == ToolMediaSize.Hidden })
    }

    @Test
    fun theOnlyImageIsAsBigAsAToolImageOfTheSameSize() {
        for ((attached, tool) in cards) {
            for (available in listOf(313f, 400f, 1200f)) {
                for (aspect in aspects) {
                    assertEquals(
                        singleCardSize(available, aspect, tool),
                        attachedImageSize(aspect, 1, attached, available),
                        "$attached, aspect $aspect in $available dp",
                    )
                }
            }
        }
    }

    @Test
    fun severalImagesAreAsBigAsSeveralToolImagesOfTheSameSize() {
        for ((attached, tool) in cards) {
            for (count in listOf(2, 3, 8)) {
                for (aspect in aspects) {
                    assertEquals(
                        stripCardSize(aspect, tool),
                        attachedImageSize(aspect, count, attached, 400f),
                        "$attached, aspect $aspect, $count images",
                    )
                }
            }
        }
    }

    @Test
    fun theSizesOfTheSetting() {
        // A tall screenshot (1:2) in a 400 dp transcript: limited by the height.
        assertSize(110f, 220f, attachedImageSize(0.5f, 1, AttachedImageSize.Small, 400f))
        assertSize(180f, 360f, attachedImageSize(0.5f, 1, AttachedImageSize.Medium, 400f))
        assertSize(260f, 520f, attachedImageSize(0.5f, 1, AttachedImageSize.Large, 400f))
        // A landscape image (2:1): 45 %, 70 % and all of the width.
        assertSize(180f, 90f, attachedImageSize(2f, 1, AttachedImageSize.Small, 400f))
        assertSize(280f, 140f, attachedImageSize(2f, 1, AttachedImageSize.Medium, 400f))
        assertSize(400f, 200f, attachedImageSize(2f, 1, AttachedImageSize.Large, 400f))
        // Several share one row height.
        assertSize(165f, 110f, attachedImageSize(1.5f, 2, AttachedImageSize.Small, 400f))
        assertSize(240f, 160f, attachedImageSize(1.5f, 3, AttachedImageSize.Medium, 400f))
        assertSize(330f, 220f, attachedImageSize(1.5f, 2, AttachedImageSize.Large, 400f))
    }

    @Test
    fun thumbnailsAreSmallSquaresWhateverTheImage() {
        for (count in listOf(1, 3)) {
            for (aspect in aspects) {
                assertSize(72f, 72f, attachedImageSize(aspect, count, AttachedImageSize.Thumbnails, 400f))
            }
            assertSize(72f, 72f, attachedImageSize(1f, count, AttachedImageSize.Thumbnails, 40f))
        }
    }

    @Test
    fun aTranscriptNotMeasuredYetStillGivesASize() {
        for (available in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val (width, height) = attachedImageSize(1f, 1, AttachedImageSize.Medium, available)
            assertTrue(width.isFinite() && width > 0f && height.isFinite() && height > 0f, "$available")
        }
    }

    @Test
    fun imagesAreDecodedForTheSizeTheyShowAt() {
        assertEquals(256, AttachedImageSize.Thumbnails.decodeSide)
        assertEquals(512, AttachedImageSize.Small.decodeSide)
        // As the cards of the images read by tools.
        assertEquals(CARD_DECODE_SIDE, AttachedImageSize.Medium.decodeSide)
        assertEquals(CARD_DECODE_SIDE, AttachedImageSize.Large.decodeSide)
        assertTrue(AttachedImageSize.entries.zipWithNext().all { (smaller, larger) -> smaller.decodeSide <= larger.decodeSide })
    }

    @Test
    fun theShapeIsReadFromTheHeaderOfADataUri() {
        fun be32(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())
        val head = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)
        val png = head + byteArrayOf(0, 0, 0, 13) + "IHDR".encodeToByteArray() + be32(1080) + be32(2400) + byteArrayOf(8, 6, 0, 0, 0)
        assertEquals(0.45f, dataUriAspect("data:image/png;base64," + Base64.encode(png))!!, 0.001f)
        assertNull(dataUriAspect(null))
        assertNull(dataUriAspect("data:image/png;base64,AAAA"))
        assertNull(dataUriAspect("https://example.com/a.png"))
    }

    @Test
    fun theChatFollowsTheSettingAndStartsLikeTheImagesReadByTools() {
        assertEquals(AttachedImageSize.Medium, ChatState().attachedImageSize)
        val defaults = ChatState().withSettings(AppSettings())
        assertEquals(AttachedImageSize.Medium, defaults.attachedImageSize)
        assertEquals(defaults.toolMediaSize, defaults.attachedImageSize.toolSize)
        for (choice in AttachedImageSize.entries) {
            val state = ChatState().withSettings(AppSettings(attachedImageSize = choice))
            assertEquals(choice, state.attachedImageSize)
            // The two settings are independent.
            assertEquals(ToolMediaSize.Medium, state.toolMediaSize)
        }
        assertEquals(AttachedImageSize.Medium, ChatState().withSettings(AppSettings(toolMediaSize = ToolMediaSize.Hidden)).attachedImageSize)
    }
}
