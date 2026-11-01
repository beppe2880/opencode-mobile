package io.github.beppe2880.opencodemobile.feature.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WaveformTest {

    @Test
    fun barsKeepTheNewestOnesOldestFirst() {
        val bars = WaveformBars(3)
        bars.push(0.1f)
        bars.push(0.2f)
        assertEquals(listOf(0.1f, 0.2f), bars.toList())
        bars.push(0.3f)
        bars.push(0.4f)
        bars.push(2f)
        assertEquals(listOf(0.3f, 0.4f, 1f), bars.toList())
        assertEquals(1f, bars.fromNewest(0))
        assertEquals(0.3f, bars.fromNewest(2))
        assertFailsWith<IllegalArgumentException> { bars.fromNewest(3) }
    }

    @Test
    fun eachBarIsTheLoudestLevelOfItsInterval() {
        val sampler = WaveformSampler(intervalMs = 50, capacity = 10)
        sampler.onFrame(1_000, 0.2f)
        sampler.onFrame(1_016, 0.7f)
        sampler.onFrame(1_033, 0.1f)
        assertEquals(emptyList(), sampler.bars.toList())

        sampler.onFrame(1_050, 0.3f)
        assertEquals(listOf(0.7f), sampler.bars.toList())

        // The next bar starts from scratch.
        sampler.onFrame(1_066, 0.05f)
        sampler.onFrame(1_100, 0f)
        assertEquals(listOf(0.7f, 0.05f), sampler.bars.toList())
    }

    @Test
    fun aLateFrameFillsTheBarsItSkipped() {
        val sampler = WaveformSampler(intervalMs = 50, capacity = 4)
        sampler.onFrame(0, 0f)
        sampler.onFrame(160, 0.6f)
        assertEquals(listOf(0.6f, 0.6f, 0.6f), sampler.bars.toList())
        // A long pause (the app in the background) only fills the capacity.
        sampler.onFrame(60_000, 0.1f)
        assertEquals(listOf(0.1f, 0.1f, 0.1f, 0.1f), sampler.bars.toList())
    }

    @Test
    fun progressIsHowFarTheNextBarHasComeIn() {
        val sampler = WaveformSampler(intervalMs = 50, capacity = 4)
        assertEquals(0f, sampler.progress(123))
        sampler.onFrame(1_000, 0f)
        assertEquals(0f, sampler.progress(1_000))
        assertEquals(0.5f, sampler.progress(1_025))
        assertEquals(0.2f, sampler.progress(1_060))
    }

    @Test
    fun silenceStillShowsAShortBar() {
        assertEquals(0.08f, barHeight(0f))
        assertEquals(1f, barHeight(1f), 0.0001f)
        assertEquals(1f, barHeight(3f), 0.0001f)
        assertEquals(0.54f, barHeight(0.5f), 0.0001f)
    }

    @Test
    fun thePreviewVoiceStaysInRange() {
        val levels = (0L..10_000L step 40).map(::previewLevel)
        assertEquals(true, levels.all { it in 0f..1f })
        assertEquals(true, levels.any { it > 0.5f } && levels.any { it < 0.1f })
    }
}
