package io.github.beppe2880.opencodemobile.core.ui.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenCapabilitiesTest {

    @Test
    fun `a phone is neither foldable nor large`() {
        assertEquals(ScreenCapabilities.Phone, ScreenSignals(smallestWidthDp = 411, largestWindowShortSideDp = 411).capabilities())
        assertEquals(ScreenCapabilities.Phone, ScreenSignals().capabilities())
    }

    @Test
    fun `a hinge makes a foldable and a large screen even while folded`() {
        // Folded: the outer screen is phone sized.
        assertEquals(ScreenCapabilities.Foldable, ScreenSignals(hinge = true, smallestWidthDp = 360, largestWindowShortSideDp = 360).capabilities())
        assertEquals(ScreenCapabilities.Foldable, ScreenSignals(hinge = true, smallestWidthDp = 673, largestWindowShortSideDp = 673).capabilities())
    }

    @Test
    fun `a wide screen or a wide largest window makes a tablet`() {
        assertEquals(ScreenCapabilities.Tablet, ScreenSignals(smallestWidthDp = 600).capabilities())
        // In a narrow split screen the configuration is small, the largest window is not.
        assertEquals(ScreenCapabilities.Tablet, ScreenSignals(smallestWidthDp = 320, largestWindowShortSideDp = 800).capabilities())
        assertEquals(ScreenCapabilities.Phone, ScreenSignals(smallestWidthDp = 599, largestWindowShortSideDp = 599).capabilities())
    }
}
