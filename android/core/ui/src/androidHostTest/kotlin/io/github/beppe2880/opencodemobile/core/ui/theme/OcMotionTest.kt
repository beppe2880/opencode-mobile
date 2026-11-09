package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcMotionTest {

    @Test
    fun `only an animator duration scale of zero asks for less motion`() {
        assertTrue(OcMotion.isReducedMotion(0f))
        assertFalse(OcMotion.isReducedMotion(1f))
        assertFalse(OcMotion.isReducedMotion(0.5f))
        assertFalse(OcMotion.isReducedMotion(10f))
    }

    @Test
    fun `reduced motion makes every spec instant`() {
        assertTrue(OcMotion.spec<Float>(OcMotion.MEDIUM_MS, reduced = true) is SnapSpec)
        assertTrue(OcMotion.sizeSpec(reduced = true) is SnapSpec)
        assertEquals(0, OcMotion.duration(OcMotion.LONG_MS, reduced = true))
    }

    @Test
    fun `full motion uses tweens of the token durations with the standard easing`() {
        val spec = OcMotion.spec<Float>(OcMotion.SHORT_MS, reduced = false)
        assertTrue(spec is TweenSpec)
        spec as TweenSpec
        assertEquals(OcMotion.SHORT_MS, spec.durationMillis)
        assertEquals(0, spec.delay)
        assertEquals(OcMotion.Standard, spec.easing)
        assertEquals(OcMotion.MEDIUM_MS, OcMotion.duration(OcMotion.MEDIUM_MS, reduced = false))
    }

    @Test
    fun `durations stay short and ordered`() {
        assertTrue(OcMotion.SHORT_MS < OcMotion.MEDIUM_MS)
        assertTrue(OcMotion.MEDIUM_MS < OcMotion.LONG_MS)
        assertTrue(OcMotion.LONG_MS <= 300)
    }

    @Test
    fun `reduced motion transitions are none`() {
        val origin = TransformOrigin.Center
        assertEquals(EnterTransition.None, OcMotion.expandEnter(reduced = true))
        assertEquals(ExitTransition.None, OcMotion.collapseExit(reduced = true))
        assertEquals(EnterTransition.None, OcMotion.popoverEnter(origin, reduced = true))
        assertEquals(ExitTransition.None, OcMotion.popoverExit(origin, reduced = true))
        assertEquals(EnterTransition.None, OcMotion.inlineEnter(reduced = true))
        assertEquals(ExitTransition.None, OcMotion.inlineExit(reduced = true))
        assertNotEquals(EnterTransition.None, OcMotion.expandEnter(reduced = false))
        assertNotEquals(ExitTransition.None, OcMotion.popoverExit(origin, reduced = false))
    }

    @Test
    fun `a popover above its anchor grows from its bottom edge on the start side`() {
        assertEquals(TransformOrigin(0f, 1f), OcMotion.anchoredOrigin(above = true, layoutDirection = LayoutDirection.Ltr))
        assertEquals(TransformOrigin(1f, 1f), OcMotion.anchoredOrigin(above = true, layoutDirection = LayoutDirection.Rtl))
    }

    @Test
    fun `a popover below its anchor grows from its top edge`() {
        assertEquals(TransformOrigin(0f, 0f), OcMotion.anchoredOrigin(above = false, layoutDirection = LayoutDirection.Ltr))
        assertEquals(TransformOrigin(1f, 0f), OcMotion.anchoredOrigin(above = false, layoutDirection = LayoutDirection.Rtl))
    }

    @Test
    fun `a popover aligned to the end grows from the end side`() {
        assertEquals(TransformOrigin(1f, 1f), OcMotion.anchoredOrigin(above = true, layoutDirection = LayoutDirection.Ltr, alignStart = false))
        assertEquals(TransformOrigin(0f, 0f), OcMotion.anchoredOrigin(above = false, layoutDirection = LayoutDirection.Rtl, alignStart = false))
    }
}
