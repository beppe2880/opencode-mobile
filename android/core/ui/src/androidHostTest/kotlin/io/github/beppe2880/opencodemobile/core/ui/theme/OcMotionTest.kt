package io.github.beppe2880.opencodemobile.core.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.LayoutDirection
import io.github.beppe2880.opencodemobile.core.domain.AnimationMode
import io.github.beppe2880.opencodemobile.core.domain.AnimationSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcMotionTest {

    private val reduced = MotionConfig(reduced = true)
    private val normal = MotionConfig()

    @Test
    fun `only an animator duration scale of zero asks for less motion`() {
        assertTrue(OcMotion.isReducedMotion(0f))
        assertFalse(OcMotion.isReducedMotion(1f))
        assertFalse(OcMotion.isReducedMotion(0.5f))
        assertFalse(OcMotion.isReducedMotion(10f))
    }

    @Test
    fun `the animations setting decides over the system only when it is not following it`() {
        for (speed in AnimationSpeed.entries) {
            assertFalse(motionConfig(AnimationMode.System, speed, systemReduced = false).reduced)
            assertTrue(motionConfig(AnimationMode.System, speed, systemReduced = true).reduced)
            assertFalse(motionConfig(AnimationMode.On, speed, systemReduced = false).reduced)
            assertFalse(motionConfig(AnimationMode.On, speed, systemReduced = true).reduced)
            assertTrue(motionConfig(AnimationMode.Off, speed, systemReduced = false).reduced)
            assertTrue(motionConfig(AnimationMode.Off, speed, systemReduced = true).reduced)
            assertEquals(speed.scale, motionConfig(AnimationMode.System, speed, systemReduced = false).scale)
        }
        assertEquals(MotionConfig(), motionConfig(AnimationMode.System, AnimationSpeed.Normal, systemReduced = false))
    }

    @Test
    fun `speeds scale durations and round to whole milliseconds`() {
        assertEquals(0.6f, AnimationSpeed.Fast.scale)
        assertEquals(1f, AnimationSpeed.Normal.scale)
        assertEquals(1.6f, AnimationSpeed.Slow.scale)
        assertEquals(220, OcMotion.scaledDuration(220, 1f))
        assertEquals(132, OcMotion.scaledDuration(220, 0.6f))
        assertEquals(352, OcMotion.scaledDuration(220, 1.6f))
        assertEquals(90, OcMotion.scaledDuration(150, 0.6f))
        assertEquals(240, OcMotion.scaledDuration(150, 1.6f))
        assertEquals(0, OcMotion.scaledDuration(0, 1.6f))
        assertEquals(0, OcMotion.scaledDuration(150, -1f))
    }

    @Test
    fun `reduced motion makes every spec instant whatever the speed`() {
        assertTrue(OcMotion.spec<Float>(OcMotion.MEDIUM_MS, reduced) is SnapSpec)
        assertTrue(OcMotion.sizeSpec(reduced) is SnapSpec)
        assertTrue(OcMotion.spec<Float>(OcMotion.MEDIUM_MS, reduced.copy(scale = 1.6f)) is SnapSpec)
        assertEquals(0, OcMotion.duration(OcMotion.LONG_MS, reduced))
    }

    @Test
    fun `full motion uses tweens of the token durations with the standard easing`() {
        val spec = OcMotion.spec<Float>(OcMotion.SHORT_MS, normal)
        assertTrue(spec is TweenSpec)
        spec as TweenSpec
        assertEquals(OcMotion.SHORT_MS, spec.durationMillis)
        assertEquals(0, spec.delay)
        assertEquals(OcMotion.Standard, spec.easing)
        assertEquals(OcMotion.MEDIUM_MS, OcMotion.duration(OcMotion.MEDIUM_MS, normal))
    }

    @Test
    fun `the speed scales durations and delays of the specs`() {
        val slow = MotionConfig(scale = AnimationSpeed.Slow.scale)
        val spec = OcMotion.spec<Float>(OcMotion.MEDIUM_MS, slow, delayMs = 50) as TweenSpec
        assertEquals(352, spec.durationMillis)
        assertEquals(80, spec.delay)
        val fast = OcMotion.spec<Float>(OcMotion.SHORT_MS, MotionConfig(scale = AnimationSpeed.Fast.scale)) as TweenSpec
        assertEquals(90, fast.durationMillis)
        assertEquals(132, OcMotion.duration(OcMotion.MEDIUM_MS, MotionConfig(scale = 0.6f)))
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
        assertEquals(EnterTransition.None, OcMotion.expandEnter(reduced))
        assertEquals(ExitTransition.None, OcMotion.collapseExit(reduced))
        assertEquals(EnterTransition.None, OcMotion.popoverEnter(origin, reduced))
        assertEquals(ExitTransition.None, OcMotion.popoverExit(origin, reduced))
        assertEquals(EnterTransition.None, OcMotion.inlineEnter(reduced))
        assertEquals(ExitTransition.None, OcMotion.inlineExit(reduced))
        assertNotEquals(EnterTransition.None, OcMotion.expandEnter(normal))
        assertNotEquals(ExitTransition.None, OcMotion.popoverExit(origin, normal))
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
