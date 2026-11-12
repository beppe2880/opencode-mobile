package io.github.beppe2880.opencodemobile.benchmarks

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cold start to the session list, as installed from an APK and as compiled with its profile. */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /** Nothing compiled ahead of time: a fresh install from the APK. */
    @Test
    fun coldStartNotCompiled() = coldStart(CompilationMode.None())

    /** Compiled with the profile shipped in the APK: an install from a store, or after idle compilation. */
    @Test
    fun coldStartWithShippedProfile() = coldStart(CompilationMode.Partial())

    private fun coldStart(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startDemo()
    }
}
