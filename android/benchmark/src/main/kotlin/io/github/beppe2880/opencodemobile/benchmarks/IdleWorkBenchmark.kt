package io.github.beppe2880.opencodemobile.benchmarks

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Frames and CPU of screens left alone for [IDLE_MS]. Sessions at work must not keep the
 * app redrawing: compare `idleFrameCount` of the running screens with the still ones.
 */
@OptIn(ExperimentalMetricApi::class)
@RunWith(AndroidJUnit4::class)
class IdleWorkBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /** Running marks on screen: the demo list has sessions at work. */
    @Test
    fun sessionListWithRunningSessions() = idle { }

    /** A chat whose agent is working: running line, tool rows and chips. */
    @Test
    fun chatWithRunningAgent() = idle { openRunningChat() }

    /** Nothing at work on screen: the reference the two above should come close to. */
    @Test
    fun chatOfIdleSession() = idle { openIdleChat() }

    private fun idle(open: MacrobenchmarkScope.() -> Unit) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(IdleWorkMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = 3,
        setupBlock = {
            restartDemo()
            open()
            // Entering transitions and first loads are not idle work.
            Thread.sleep(SETTLE_MS)
        },
    ) {
        Thread.sleep(IDLE_MS)
    }

    private companion object {
        const val SETTLE_MS = 3_000L
        const val IDLE_MS = 10_000L
    }
}
