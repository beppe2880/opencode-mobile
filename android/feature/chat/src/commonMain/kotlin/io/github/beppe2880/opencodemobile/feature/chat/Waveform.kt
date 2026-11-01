package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** The newest [capacity] bar levels (0 to 1) of a waveform, in a ring. */
internal class WaveformBars(val capacity: Int) {
    private val values = FloatArray(capacity)
    private var first = 0

    var size: Int = 0
        private set

    fun push(level: Float) {
        val value = level.coerceIn(0f, 1f)
        if (size < capacity) {
            values[(first + size) % capacity] = value
            size++
        } else {
            values[first] = value
            first = (first + 1) % capacity
        }
    }

    /** The bar [index] places before the newest one (0 is the newest). */
    fun fromNewest(index: Int): Float {
        require(index in 0 until size) { "No bar $index of $size" }
        return values[(first + size - 1 - index) % capacity]
    }

    /** Oldest first. */
    fun toList(): List<Float> = List(size) { fromNewest(size - 1 - it) }
}

/**
 * Turns the level read on every frame into one bar every [intervalMs]: each bar is the loudest
 * level of its interval, so short syllables still show. A late frame fills the bars it skipped
 * with the same level rather than leaving a gap.
 */
internal class WaveformSampler(private val intervalMs: Long, capacity: Int) {
    val bars = WaveformBars(capacity)
    private var startMs = -1L
    private var pushed = 0L
    private var peak = 0f

    fun onFrame(nowMs: Long, level: Float) {
        if (startMs < 0) startMs = nowMs
        peak = max(peak, level.coerceIn(0f, 1f))
        val due = (nowMs - startMs) / intervalMs
        if (due <= pushed) return
        repeat(min(due - pushed, bars.capacity.toLong()).toInt()) { bars.push(peak) }
        pushed = due
        peak = 0f
    }

    /** How far (0 to 1) the next bar has come in: the bars scroll by this part of a step between two bars. */
    fun progress(nowMs: Long): Float = if (startMs < 0) 0f else ((nowMs - startMs).coerceAtLeast(0) % intervalMs).toFloat() / intervalMs
}

/** Height of a bar as a part of the waveform's height: silence stays a short visible tick. */
internal fun barHeight(level: Float): Float = MIN_BAR + (1f - MIN_BAR) * level.coerceIn(0f, 1f)

/** A speech-like level for previews: syllables inside words, with short pauses. */
internal fun previewLevel(ms: Long): Float {
    val t = ms / 1000.0
    val word = sin(2 * PI * 0.7 * t)
    if (word < -0.6) return 0.04f
    val syllable = abs(sin(2 * PI * 2.6 * t + sin(t * 1.3)))
    return (0.15 + 0.7 * syllable * (0.6 + 0.4 * abs(word))).toFloat().coerceIn(0f, 1f)
}

/**
 * The live waveform of a recording: bars scroll from the end where the newest arrives (the
 * right, or the left in right-to-left layouts), following [level] read on every frame. With
 * [reducedMotion] the bars move one step at a time, four times a second, instead of scrolling.
 * Only the drawing is invalidated each frame, nothing recomposes.
 */
@Composable
internal fun VoiceWaveform(level: () -> Float, color: Color, emptyColor: Color, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val sampler = remember(reducedMotion) { WaveformSampler(if (reducedMotion) REDUCED_BAR_MS else BAR_MS, MAX_BARS) }
    val currentLevel by rememberUpdatedState(level)
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sampler) {
        while (true) {
            withFrameMillis { now ->
                sampler.onFrame(now, currentLevel())
                frame = now
            }
        }
    }
    Canvas(modifier) {
        val now = frame
        val barWidth = BAR_WIDTH.toPx()
        val step = barWidth + BAR_GAP.toPx()
        val shift = if (reducedMotion) 0f else sampler.progress(now) * step
        val bars = sampler.bars
        val rtl = layoutDirection == LayoutDirection.Rtl
        val centerY = size.height / 2
        val minHeight = barWidth
        val fade = FADE.toPx()
        val count = (size.width / step).toInt() + 1
        for (i in 0 until count) {
            // Empty slots (dots) scroll with the bars, like a tape.
            val fromEnd = barWidth / 2 + i * step + shift
            if (fromEnd > size.width - barWidth / 2) break
            val x = if (rtl) fromEnd else size.width - fromEnd
            val recorded = i < bars.size
            val height = if (recorded) max(minHeight, barHeight(bars.fromNewest(i)) * size.height) else minHeight
            drawLine(
                color = if (recorded) color else emptyColor,
                start = Offset(x, centerY - height / 2 + barWidth / 2),
                end = Offset(x, centerY + height / 2 - barWidth / 2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
                // The oldest bars fade out at the far edge.
                alpha = ((size.width - fromEnd) / fade).coerceIn(0f, 1f),
            )
        }
    }
}

/** One bar every 50 ms: 20 bars a second scroll by. */
private const val BAR_MS = 50L
private const val REDUCED_BAR_MS = 250L

/** Enough bars for the widest message box. */
private const val MAX_BARS = 400
private const val MIN_BAR = 0.08f
private val BAR_WIDTH = 3.dp
private val BAR_GAP = 2.dp
private val FADE = 24.dp
