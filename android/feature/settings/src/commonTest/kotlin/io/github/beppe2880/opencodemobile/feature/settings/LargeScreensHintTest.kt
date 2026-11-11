package io.github.beppe2880.opencodemobile.feature.settings

import io.github.beppe2880.opencodemobile.core.ui.platform.ScreenCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals

class LargeScreensHintTest {

    @Test
    fun aFoldedFoldableIsInvitedToUnfold() {
        assertEquals(LargeScreensHint.Unfold, largeScreensHint(ScreenCapabilities.Foldable, wide = false))
    }

    @Test
    fun aNarrowTabletKeepsThePlainNote() {
        assertEquals(LargeScreensHint.NarrowWindow, largeScreensHint(ScreenCapabilities.Tablet, wide = false))
    }

    @Test
    fun aWideWindowNeedsNoHint() {
        assertEquals(LargeScreensHint.None, largeScreensHint(ScreenCapabilities.Foldable, wide = true))
        assertEquals(LargeScreensHint.None, largeScreensHint(ScreenCapabilities.Tablet, wide = true))
    }
}
