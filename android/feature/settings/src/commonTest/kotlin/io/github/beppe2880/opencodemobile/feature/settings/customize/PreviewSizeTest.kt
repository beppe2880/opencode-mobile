package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreviewSizeTest {

    private val phone = 700.dp

    @Test
    fun hiddenTakesNoRoom() {
        assertEquals(0.dp, previewHeight(0f, phone))
        assertEquals(0.dp, previewHeight(PreviewSize.Hidden.fraction, phone))
    }

    @Test
    fun sizesStayWithinTheMinimums() {
        assertEquals(phone * 0.5f, previewHeight(PreviewSize.Half.fraction, phone))
        // A sliver grows to the minimum the preview needs.
        assertEquals(MinPreviewHeight, previewHeight(0.05f, phone))
        // Large on a short window leaves the options their room.
        val short = 400.dp
        assertEquals(short - MinOptionsHeight, previewHeight(PreviewSize.Large.fraction, short))
        // Never below the preview's minimum, even when the window cannot fit both.
        assertEquals(MinPreviewHeight, previewHeight(0.9f, 300.dp))
    }

    @Test
    fun sizesGrowInOrder() {
        val heights = PreviewSize.entries.map { previewHeight(it.fraction, phone) }
        assertEquals(heights.sorted(), heights)
        assertTrue(PreviewSize.DEFAULT_FRACTION in PreviewSize.Small.fraction..PreviewSize.Half.fraction)
    }

    @Test
    fun aDraggedDividerSnapsToHiddenOrAKeptShare() {
        assertEquals(0f, snapFraction(MinPreviewHeight / 2 - 1.dp, phone))
        assertEquals(0f, snapFraction(10.dp, phone))
        assertEquals(0.4f, snapFraction(280.dp, phone), 0.001f)
        // Between half the minimum and the minimum it keeps the minimum.
        assertEquals(MinPreviewHeight / phone, snapFraction(MinPreviewHeight - 10.dp, phone), 0.001f)
        // Dragged past the end: the options keep their room.
        assertEquals((phone - MinOptionsHeight) / phone, snapFraction(phone, phone), 0.001f)
    }
}
