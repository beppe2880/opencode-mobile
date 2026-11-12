package io.github.beppe2880.opencodemobile.benchmarks

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor

/**
 * What the app does while nobody touches it: frames it produces and CPU time of all its
 * threads over the measured window. Unlike frame timings these are counts and sums, so they
 * compare across machines and emulators: a still screen should produce close to no frames.
 */
@OptIn(ExperimentalMetricApi::class)
class IdleWorkMetric : TraceMetric() {
    override fun getMeasurements(captureInfo: CaptureInfo, traceSession: TraceProcessor.Session): List<Measurement> {
        val process = captureInfo.targetPackageName
        val frames = traceSession.query(
            """
            SELECT COUNT(*) AS n FROM slice
            JOIN thread_track ON slice.track_id = thread_track.id
            JOIN thread USING (utid)
            JOIN process USING (upid)
            WHERE process.name = '$process' AND slice.name LIKE 'Choreographer#doFrame%'
            """.trimIndent(),
        ).first().long("n")
        val cpuNs = traceSession.query(
            """
            SELECT IFNULL(SUM(sched.dur), 0) AS ns FROM sched
            JOIN thread USING (utid)
            JOIN process USING (upid)
            WHERE process.name = '$process'
            """.trimIndent(),
        ).first().long("ns")
        return listOf(
            Measurement("idleFrameCount", frames.toDouble()),
            Measurement("idleCpuMs", cpuNs / 1_000_000.0),
        )
    }
}
